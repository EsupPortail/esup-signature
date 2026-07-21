import {getDocument, GlobalWorkerOptions} from "/webjars/pdfjs-dist/legacy/build/pdf.mjs";

GlobalWorkerOptions.workerSrc = "/webjars/pdfjs-dist/legacy/build/pdf.worker.min.mjs";

(() => {
            const urlProfil = document.body.dataset.esupSignrequestProfilePath || 'user';
            const signRequestId = document.body.dataset.esupSignrequestId || '0';
            const csrfParameterName = document.querySelector('meta[name="_csrf_parameter"]')?.getAttribute('content') || '_csrf';
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
            const userEppn = document.body.dataset.esupUserEppn || '';
            const signRequestCreatorEppn = document.body.dataset.esupCreatorEppn || '';
            const signRequestStatus = document.body.dataset.esupSignrequestStatus || '';

            const attachmentsTbody = document.getElementById('attachments-tbody');
            const linksTbody = document.getElementById('links-tbody');
            const attachmentTableWrapper = document.getElementById('attachment-table-wrapper');
            const linksTableWrapper = document.getElementById('links-table-wrapper');
            const emptyState = document.getElementById('attachment-empty-state');
            const addAttachmentForm = document.getElementById('add-attachment-form');
            const feedback = document.getElementById('attachment-feedback');
            const previewModalsContainer = document.getElementById('attachment-preview-modals');
            const attachmentBadge = document.getElementById('attachment-badge');
            const DEFAULT_PDF_SCALE = 1.5;
            const MIN_PDF_SCALE = 0.5;
            const MAX_PDF_SCALE = 2.5;
            const PDF_SCALE_STEP = 0.25;
            const MIN_PDF_PREVIEW_LOADING_MS = 300;
            const pdfPreviewState = new Map();

            function escapeHtml(value) {
                return String(value ?? '')
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;')
                    .replace(/'/g, '&#039;');
            }

            function showMessage(type, message) {
                const normalizedType = ['success', 'error', 'warn', 'info', 'custom'].includes(type) ? type : 'info';
                const sanitizedMessage = escapeHtml(message);
                if (typeof window.showSnackbar === 'function') {
                    if (feedback) {
                        feedback.innerHTML = '';
                    }
                    window.showSnackbar(sanitizedMessage, normalizedType, {
                        delay: normalizedType === 'success' ? 2000 : 4000
                    });
                    return;
                }
                if (!feedback) {
                    return;
                }
                const alertClass = normalizedType === 'error' ? 'danger' : 'success';
                feedback.innerHTML = `
                    <div class="alert alert-${alertClass} alert-dismissible fade show" role="alert">
                        ${sanitizedMessage}
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                    </div>`;
            }

            function updateAttachmentBadge() {
                if (!attachmentBadge) {
                    return;
                }
                const count = (attachmentsTbody?.children.length || 0) + (linksTbody?.children.length || 0);
                attachmentBadge.textContent = String(count);
                attachmentBadge.classList.toggle('d-none', count === 0);
            }

            function updateEmptyState() {
                const hasAttachments = !!attachmentsTbody && attachmentsTbody.children.length > 0;
                const hasLinks = !!linksTbody && linksTbody.children.length > 0;
                if (attachmentTableWrapper) {
                    attachmentTableWrapper.style.display = hasAttachments ? '' : 'none';
                }
                if (linksTableWrapper) {
                    linksTableWrapper.style.display = hasLinks ? '' : 'none';
                }
                if (emptyState) {
                    emptyState.style.display = (!hasAttachments && !hasLinks) ? '' : 'none';
                }
                updateAttachmentBadge();
            }

            function isPdfFile(fileName) {
                return String(fileName || '').toLowerCase().endsWith('.pdf');
            }

            function ensurePdfPreviewLinkStyles() {
                if (document.getElementById('pdf-preview-link-style')) {
                    return;
                }
                const style = document.createElement('style');
                style.id = 'pdf-preview-link-style';
                style.textContent = `
                    .esup-pdf-preview-page {
                        position: relative;
                    }
                    .pdf-preview-links-layer {
                        position: absolute;
                        inset: 0;
                        z-index: 2;
                        pointer-events: none;
                    }
                    .pdf-preview-link {
                        position: absolute;
                        display: block;
                        cursor: pointer;
                        pointer-events: auto;
                    }
                    .pdf-preview-link > a {
                        display: block;
                        width: 100%;
                        height: 100%;
                    }
                `;
                document.head.appendChild(style);
            }

            function getPdfPreviewState(modalElement) {
                if (!modalElement) {
                    return null;
                }
                const attachmentId = modalElement.dataset.attachmentId;
                if (!attachmentId) {
                    return null;
                }
                if (!pdfPreviewState.has(attachmentId)) {
                    pdfPreviewState.set(attachmentId, {
                        pdf: null,
                        scale: DEFAULT_PDF_SCALE,
                        loadingPromise: null,
                        renderPromise: null,
                        loadingStartedAt: 0,
                        modalShown: false
                    });
                }
                return pdfPreviewState.get(attachmentId);
            }

            function updatePdfZoomLabel(modalElement, scale) {
                const zoomLabel = modalElement?.querySelector('.js-esup-filepond-pdf-zoom-value');
                if (!zoomLabel || typeof scale !== 'number' || Number.isNaN(scale)) {
                    return;
                }
                zoomLabel.textContent = `${Math.round(scale * 100)}%`;
            }

            function getPdfPreviewContainer(modalElement) {
                return modalElement?.querySelector('.esup-filepond-preview-pdf');
            }

            function setPdfZoomControlsVisible(modalElement, visible) {
                modalElement?.querySelector('.js-esup-filepond-pdf-zoom-controls')?.classList.toggle('d-none', !visible);
            }

            function showPdfPreviewLoading(container) {
                if (!container) {
                    return;
                }
                container.setAttribute('aria-busy', 'true');
                container.innerHTML = `
                    <div class="esup-pdf-preview-loading" role="status">
                        <span class="spinner-border text-dark" aria-hidden="true"></span>
                        <span>Chargement du document...</span>
                    </div>`;
            }

            function waitForPdfPreviewReveal(modalElement, state) {
                const waits = [];
                const startedAt = state?.loadingStartedAt || performance.now();
                const remainingDelay = MIN_PDF_PREVIEW_LOADING_MS - (performance.now() - startedAt);
                if (remainingDelay > 0) {
                    waits.push(new Promise(resolve => window.setTimeout(resolve, remainingDelay)));
                }
                if (!state?.modalShown) {
                    waits.push(new Promise(resolve => modalElement.addEventListener('shown.bs.modal', resolve, { once: true })));
                }
                return Promise.all(waits).then(() => new Promise(resolve => {
                    window.requestAnimationFrame(() => window.requestAnimationFrame(resolve));
                }));
            }

            async function loadPdfDocument(modalElement) {
                const state = getPdfPreviewState(modalElement);
                if (!state) {
                    return null;
                }
                if (state.pdf) {
                    return state.pdf;
                }
                if (!state.loadingPromise) {
                    const attachmentId = modalElement.dataset.attachmentId;
                    state.loadingPromise = getDocument({
                            verbosity: 0,
                            url: getAttachmentInlineUrl(attachmentId),
                            useWasm: true,
                            wasmUrl: `/webjars/pdfjs-dist/wasm/`
                        }).promise
                        .then((pdf) => {
                            state.pdf = pdf;
                            return pdf;
                        })
                        .finally(() => {
                            state.loadingPromise = null;
                        });
                }
                return state.loadingPromise;
            }

            async function renderPdfPages(modalElement) {
                const container = getPdfPreviewContainer(modalElement);
                const state = getPdfPreviewState(modalElement);
                if (!container || !state) {
                    return;
                }
                const pdf = await loadPdfDocument(modalElement);
                if (!pdf) {
                    return;
                }

                updatePdfZoomLabel(modalElement, state.scale);
                const scaleFrame = document.createElement('div');
                scaleFrame.className = 'esup-pdf-preview-scale-frame';
                const documentPreview = document.createElement('div');
                documentPreview.className = 'esup-pdf-preview-document';
                scaleFrame.appendChild(documentPreview);
                const pages = [];
                for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber++) {
                    const page = await pdf.getPage(pageNumber);
                    const viewport = page.getViewport({ scale: DEFAULT_PDF_SCALE });
                    const { pageWrapper, canvas, linksLayer } = createPdfPreviewPage(pageNumber, viewport);
                    documentPreview.appendChild(pageWrapper);
                    pages.push({ page, viewport, canvas, linksLayer });
                }
                for (const { page, viewport, canvas, linksLayer } of pages) {
                    await page.render({ canvasContext: canvas.getContext('2d'), viewport }).promise;
                    await renderPdfLinkAnnotations(page, pdf, viewport, linksLayer, container);
                    page.cleanup();
                }
                await waitForPdfPreviewReveal(modalElement, state);
                container.replaceChildren(scaleFrame);
                container.removeAttribute('aria-busy');
                applyPdfZoom(modalElement, state.scale);
            }

            async function changePdfZoom(modalElement, delta) {
                const state = getPdfPreviewState(modalElement);
                if (!state) {
                    return;
                }
                const nextScale = Math.min(MAX_PDF_SCALE, Math.max(MIN_PDF_SCALE, Number((state.scale + delta).toFixed(2))));
                if (nextScale === state.scale) {
                    return;
                }
                state.scale = nextScale;
                applyPdfZoom(modalElement, state.scale);
                updatePdfZoomLabel(modalElement, state.scale);
            }

            function applyPdfZoom(modalElement, scale) {
                const container = getPdfPreviewContainer(modalElement);
                const documentPreview = container?.querySelector('.esup-pdf-preview-document');
                const scaleFrame = container?.querySelector('.esup-pdf-preview-scale-frame');
                if (!documentPreview || !scaleFrame) {
                    return;
                }
                const zoom = scale / DEFAULT_PDF_SCALE;
                scaleFrame.style.width = `${Math.ceil(documentPreview.offsetWidth * zoom)}px`;
                scaleFrame.style.height = `${Math.ceil(documentPreview.offsetHeight * zoom)}px`;
                documentPreview.style.transform = `scale(${zoom})`;
            }

            function createPdfPreviewPage(pageNumber, viewport) {
                const pageWrapper = document.createElement('div');
                pageWrapper.className = 'esup-pdf-preview-page';
                pageWrapper.dataset.pageNumber = String(pageNumber);
                pageWrapper.style.width = `${viewport.width}px`;
                pageWrapper.style.height = `${viewport.height}px`;
                pageWrapper.style.marginBottom = '10px';

                const canvas = document.createElement('canvas');
                canvas.width = viewport.width;
                canvas.height = viewport.height;
                canvas.style.display = 'block';
                canvas.style.width = `${viewport.width}px`;
                canvas.style.height = `${viewport.height}px`;

                const linksLayer = document.createElement('div');
                linksLayer.className = 'pdf-preview-links-layer';
                linksLayer.setAttribute('aria-hidden', 'false');

                pageWrapper.appendChild(canvas);
                pageWrapper.appendChild(linksLayer);

                return { pageWrapper, canvas, linksLayer };
            }

            function getSafePdfExternalLink(annotation) {
                const candidate = annotation?.url || annotation?.unsafeUrl || '';
                if (!candidate) {
                    return '';
                }
                try {
                    const parsedUrl = new URL(candidate, window.location.origin);
                    return ['http:', 'https:', 'mailto:', 'tel:'].includes(parsedUrl.protocol)
                        ? parsedUrl.href
                        : '';
                } catch (error) {
                    return '';
                }
            }

            async function resolvePdfInternalDestination(pdf, destination) {
                if (!destination) {
                    return null;
                }
                const explicitDestination = typeof destination === 'string'
                    ? await pdf.getDestination(destination)
                    : destination;
                if (!Array.isArray(explicitDestination) || explicitDestination.length === 0) {
                    return null;
                }
                const pageRef = explicitDestination[0];
                if (typeof pageRef === 'number') {
                    return pageRef + 1;
                }
                if (pageRef && typeof pageRef === 'object') {
                    const pageIndex = await pdf.getPageIndex(pageRef);
                    return pageIndex + 1;
                }
                return null;
            }

            async function renderPdfLinkAnnotations(page, pdf, viewport, linksLayer, scrollContainer) {
                if (!page || !pdf || !viewport || !linksLayer) {
                    return;
                }

                const annotations = await page.getAnnotations({ intent: 'display' });
                for (const annotation of annotations) {
                    if (annotation?.subtype !== 'Link' || !Array.isArray(annotation.rect)) {
                        continue;
                    }

                    const linkUrl = getSafePdfExternalLink(annotation);
                    const targetPageNumber = linkUrl ? null : await resolvePdfInternalDestination(pdf, annotation.dest);
                    if (!linkUrl && !targetPageNumber) {
                        continue;
                    }

                    const [x1, y1, x2, y2] = viewport.convertToViewportRectangle(annotation.rect);
                    const left = Math.min(x1, x2);
                    const top = Math.min(y1, y2);
                    const width = Math.abs(x2 - x1);
                    const height = Math.abs(y2 - y1);
                    if (!width || !height) {
                        continue;
                    }

                    const linkAnnotationElement = document.createElement('div');
                    linkAnnotationElement.className = 'pdf-preview-link linkAnnotation';
                    linkAnnotationElement.style.left = `${left}px`;
                    linkAnnotationElement.style.top = `${top}px`;
                    linkAnnotationElement.style.width = `${width}px`;
                    linkAnnotationElement.style.height = `${height}px`;

                    const linkElement = document.createElement('a');

                    if (linkUrl) {
                        linkElement.href = linkUrl;
                        linkElement.target = '_blank';
                        linkElement.rel = 'noopener noreferrer';
                        linkElement.title = linkUrl;
                        linkElement.setAttribute('aria-label', `Ouvrir le lien ${linkUrl}`);
                    } else {
                        linkElement.href = '#';
                        linkElement.title = `Aller a la page ${targetPageNumber}`;
                        linkElement.setAttribute('aria-label', `Aller a la page ${targetPageNumber}`);
                        linkElement.addEventListener('click', (event) => {
                            event.preventDefault();
                            const targetPage = scrollContainer?.querySelector(`[data-page-number="${targetPageNumber}"]`);
                            targetPage?.scrollIntoView({
                                behavior: 'smooth',
                                block: 'start'
                            });
                        });
                    }

                    linkAnnotationElement.appendChild(linkElement);
                    linksLayer.appendChild(linkAnnotationElement);
                }
            }

            function getAttachmentDownloadUrl(attachmentId) {
                return `/${urlProfil}/signrequests/get-attachment/${signRequestId}/${attachmentId}`;
            }

            function getAttachmentInlineUrl(attachmentId) {
                return `/${urlProfil}/signrequests/get-attachment-inline/${signRequestId}/${attachmentId}`;
            }

            function getRemoveAttachmentUrl(attachmentId) {
                return `/${urlProfil}/signrequests/remove-attachment/${signRequestId}/${attachmentId}`;
            }

            function getRemoveLinkUrl(linkIndex) {
                return `/${urlProfil}/signrequests/remove-link/${signRequestId}/${linkIndex}`;
            }

            function canDeleteAttachment(attachment) {
                const attachmentCreatorEppn = attachment && attachment.createBy ? attachment.createBy.eppn : null;
                return (attachmentCreatorEppn === userEppn && signRequestStatus === 'pending') || signRequestCreatorEppn === userEppn;
            }

            function creatorDisplayName(createBy) {
                if (!createBy) {
                    return '';
                }
                return `${createBy.firstname || ''} ${createBy.name || ''}`.trim();
            }

            function createAttachmentRow(attachment) {
                const tr = document.createElement('tr');
                tr.id = `attachment-row-${attachment.id}`;
                const deleteCellHtml = canDeleteAttachment(attachment)
                    ? `
                        <form class="js-remove-attachment-form"
                              action="${getRemoveAttachmentUrl(attachment.id)}"
                              method="post"
                              data-attachment-id="${attachment.id}">
                            <input type="hidden" name="_method" value="delete">
                            <input type="hidden" name="${escapeHtml(csrfParameterName)}" value="${escapeHtml(csrfToken)}">
                            <button type="submit" class="btn btn-danger float-end" title="Supprimer" style="bottom: 10px;">
                                <i class="fi fi-rr-trash"></i>
                            </button>
                        </form>`
                    : '';
                tr.innerHTML = `
                    <td>${escapeHtml(attachment.fileName)}</td>
                    <td>${escapeHtml(creatorDisplayName(attachment.createBy))}</td>
                    <td>
                        <button type="button" class="btn btn-info float-end js-open-attachment-preview" data-ui-tooltip="false" title="Voir" data-attachment-id="${attachment.id}" data-file-name="${escapeHtml(attachment.fileName)}">
                            <i class="fi fi-rr-eye"></i>
                        </button>
                    </td>
                    <td>
                        <a href="${getAttachmentDownloadUrl(attachment.id)}" target="_blank" data-ui-tooltip="false" class="btn btn-primary float-end" title="Télécharger">
                            <i class="fi fi-rr-download"></i>
                        </a>
                    </td>
                    <td>${deleteCellHtml}</td>`;
                return tr;
            }

            function createLinkRow(link, index) {
                const tr = document.createElement('tr');
                tr.id = `link-row-${index}`;
                const linkValue = escapeHtml(link);
                tr.innerHTML = `
                    <td>
                        <a href="${linkValue}" target="_blank">${linkValue}</a>
                    </td>
                    <td>
                        <form class="js-remove-link-form"
                              action="${getRemoveLinkUrl(index)}"
                              method="post"
                              data-link-index="${index}">
                            <input type="hidden" name="_method" value="delete">
                            <input type="hidden" name="${escapeHtml(csrfParameterName)}" value="${escapeHtml(csrfToken)}">
                            <button type="submit" class="btn btn-danger float-end" title="Supprimer" data-ui-tooltip="false" style="bottom: 10px;">
                                <i class="fi fi-rr-trash"></i>
                            </button>
                        </form>
                    </td>`;
                return tr;
            }

            function renderLinks(links) {
                if (!linksTbody) {
                    return;
                }
                linksTbody.innerHTML = '';
                (links || []).forEach((link, index) => {
                    linksTbody.appendChild(createLinkRow(link, index));
                });
                updateEmptyState();
            }

            function getAttachmentPreviewModal() {
                let modalElement = document.getElementById('esup-attachment-preview-modal');
                if (modalElement) {
                    return modalElement;
                }
                modalElement = document.createElement('div');
                modalElement.id = 'esup-attachment-preview-modal';
                modalElement.className = 'modal fade esup-filepond-preview-modal js-attachment-preview-modal';
                modalElement.tabIndex = -1;
                modalElement.setAttribute('aria-hidden', 'true');
                modalElement.innerHTML = `
                    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
                        <div class="modal-content">
                            <div class="modal-header esup-filepond-preview-header">
                                <div class="esup-filepond-preview-title-row">
                                    <h5 class="modal-title js-esup-filepond-preview-title">Aperçu du fichier</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Fermer"></button>
                                </div>
                                <div class="d-none align-items-center justify-content-center gap-2 js-esup-filepond-pdf-zoom-controls">
                                    <button type="button" class="btn btn-outline-secondary btn-sm js-esup-filepond-pdf-zoom-out" title="Zoom arrière" aria-label="Zoom arrière" data-ui-tooltip="false">-</button>
                                    <span class="badge text-bg-light js-esup-filepond-pdf-zoom-value">150%</span>
                                    <button type="button" class="btn btn-outline-secondary btn-sm js-esup-filepond-pdf-zoom-in" title="Zoom avant" aria-label="Zoom avant" data-ui-tooltip="false">+</button>
                                </div>
                            </div>
                            <div class="modal-body js-esup-filepond-preview-body"></div>
                            <div class="modal-footer">
                                <a class="btn btn-outline-secondary js-esup-filepond-preview-download" target="_blank" rel="noopener">Télécharger</a>
                                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal" data-ui-tooltip="false">Fermer</button>
                            </div>
                        </div>
                    </div>`;
                setupPreviewModal(modalElement);
                document.body.appendChild(modalElement);
                return modalElement;
            }

            function ensureAttachmentModal(attachment) {
                return getAttachmentPreviewModal();
            }

            async function fetchJson(url, options = {}) {
                const response = await fetch(url, {
                    ...options,
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        ...(options.headers || {})
                    }
                });
                const contentType = response.headers.get('content-type') || '';
                const payload = contentType.includes('application/json') ? await response.json() : { message: await response.text() };
                if (!response.ok) {
                    throw new Error(payload.message || 'Une erreur est survenue');
                }
                return payload;
            }

            async function submitDeleteForm(form) {
                const body = new URLSearchParams(new FormData(form));
                return fetchJson(form.action, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                    },
                    body: body.toString()
                });
            }

            async function renderPdfPreview(modalElement, options = {}) {
                if (!modalElement || !isPdfFile(modalElement.dataset.fileName)) {
                    return;
                }
                const state = getPdfPreviewState(modalElement);
                if (!state) {
                    return;
                }
                if (modalElement.dataset.pdfLoaded === 'true' && !options.force) {
                    updatePdfZoomLabel(modalElement, state.scale);
                    return;
                }
                if (state.renderPromise && !options.force) {
                    return state.renderPromise;
                }
                if (state.renderPromise && options.force) {
                    await state.renderPromise;
                }
                try {
                    ensurePdfPreviewLinkStyles();
                    state.loadingStartedAt = performance.now();
                    showPdfPreviewLoading(getPdfPreviewContainer(modalElement));
                    state.renderPromise = renderPdfPages(modalElement);
                    await state.renderPromise;
                    modalElement.dataset.pdfLoaded = 'true';
                } catch (error) {
                    alert(error.message);
                    const container = getPdfPreviewContainer(modalElement);
                    if (container) {
                        container.innerHTML = '<div class="alert alert-danger m-3">Impossible de charger l’aperçu du document.</div>';
                    }
                } finally {
                    state.renderPromise = null;
                }
            }

            function setupPreviewModal(modalElement) {
                if (!modalElement || modalElement.dataset.previewReady === 'true') {
                    return;
                }
                modalElement.dataset.previewReady = 'true';
                modalElement.addEventListener('shown.bs.modal', () => {
                    const previewState = getPdfPreviewState(modalElement);
                    if (previewState) {
                        previewState.modalShown = true;
                    }
                    renderPdfPreview(modalElement).then(() => {
                        const state = getPdfPreviewState(modalElement);
                        if (state) {
                            applyPdfZoom(modalElement, state.scale);
                        }
                    });
                });
                modalElement.addEventListener('hidden.bs.modal', () => {
                    const previewState = getPdfPreviewState(modalElement);
                    if (previewState) {
                        previewState.modalShown = false;
                    }
                    if (modalElement.dataset.returnToAttachmentModal === 'true') {
                        delete modalElement.dataset.returnToAttachmentModal;
                        const attachmentModal = document.getElementById('attachment');
                        if (attachmentModal && window.bootstrap?.Modal) {
                            window.bootstrap.Modal.getOrCreateInstance(attachmentModal).show();
                        }
                    }
                });
            }

            document.querySelectorAll('.js-attachment-preview-modal').forEach(setupPreviewModal);

            document.addEventListener('click', async (event) => {
                const zoomInButton = event.target.closest('.js-esup-filepond-pdf-zoom-in');
                if (zoomInButton) {
                    const modalElement = zoomInButton.closest('.js-attachment-preview-modal');
                    await changePdfZoom(modalElement, PDF_SCALE_STEP);
                    return;
                }

                const zoomOutButton = event.target.closest('.js-esup-filepond-pdf-zoom-out');
                if (zoomOutButton) {
                    const modalElement = zoomOutButton.closest('.js-attachment-preview-modal');
                    await changePdfZoom(modalElement, -PDF_SCALE_STEP);
                }
            });

            function getAttachmentFileName(attachmentId, fallback = '') {
                if (fallback) {
                    return fallback;
                }
                const button = document.querySelector(`.js-open-attachment-preview[data-attachment-id="${attachmentId}"]`);
                if (button?.dataset.fileName) {
                    return button.dataset.fileName;
                }
                return document.getElementById(`attachment-row-${attachmentId}`)?.querySelector('td')?.textContent?.trim() || '';
            }

            function prepareAttachmentPreview(modalElement, attachmentId, fileName) {
                const isPdf = isPdfFile(fileName);
                const body = modalElement.querySelector('.js-esup-filepond-preview-body');
                const downloadLink = modalElement.querySelector('.js-esup-filepond-preview-download');
                modalElement.dataset.attachmentId = String(attachmentId);
                modalElement.dataset.fileName = fileName;
                delete modalElement.dataset.pdfLoaded;
                modalElement.querySelector('.js-esup-filepond-preview-title').textContent = fileName || 'Aperçu du fichier';
                downloadLink.href = getAttachmentDownloadUrl(attachmentId);
                setPdfZoomControlsVisible(modalElement, isPdf);
                body.replaceChildren();
                if (isPdf) {
                    const pdfPreview = document.createElement('div');
                    pdfPreview.className = 'esup-filepond-preview-pdf';
                    pdfPreview.setAttribute('aria-busy', 'true');
                    showPdfPreviewLoading(pdfPreview);
                    body.appendChild(pdfPreview);
                    const state = getPdfPreviewState(modalElement);
                    if (state) {
                        state.scale = DEFAULT_PDF_SCALE;
                        state.loadingStartedAt = performance.now();
                        state.modalShown = modalElement.classList.contains('show');
                    }
                    updatePdfZoomLabel(modalElement, DEFAULT_PDF_SCALE);
                    return;
                }
                const fallback = document.createElement('div');
                fallback.className = 'esup-filepond-preview-fallback';
                fallback.innerHTML = '<i class="fi fi-rr-file text-muted" aria-hidden="true"></i><p class="mb-3">Aucun aperçu intégré disponible pour ce type de fichier.</p>';
                const openLink = document.createElement('a');
                openLink.className = 'btn btn-primary';
                openLink.href = getAttachmentInlineUrl(attachmentId);
                openLink.target = '_blank';
                openLink.rel = 'noopener';
                openLink.textContent = 'Ouvrir le fichier';
                fallback.appendChild(openLink);
                body.appendChild(fallback);
            }

            function showPreparedAttachmentPreview(modalElement) {
                if (window.bootstrap?.Modal) {
                    window.bootstrap.Modal.getOrCreateInstance(modalElement, {
                        backdrop: 'static',
                        keyboard: false
                    }).show();
                    return;
                }
                $(modalElement).modal({
                    backdrop: 'static',
                    keyboard: false
                });
            }

            window.openPdfModal = function (attachmentId, fileName = '') {
                const modalElement = getAttachmentPreviewModal();
                if (!modalElement || !attachmentId) {
                    return;
                }
                prepareAttachmentPreview(modalElement, attachmentId, getAttachmentFileName(attachmentId, fileName));
                const attachmentModal = document.getElementById('attachment');
                if (attachmentModal?.classList.contains('show') && window.bootstrap?.Modal) {
                    modalElement.dataset.returnToAttachmentModal = 'true';
                    attachmentModal.addEventListener('hidden.bs.modal', () => showPreparedAttachmentPreview(modalElement), { once: true });
                    window.bootstrap.Modal.getOrCreateInstance(attachmentModal).hide();
                    return;
                }
                showPreparedAttachmentPreview(modalElement);
            };

            if (addAttachmentForm) {
                addAttachmentForm.addEventListener('submit', async (event) => {
                    event.preventDefault();
                    try {
                        const payload = await fetchJson(addAttachmentForm.action, {
                            method: 'POST',
                            body: new FormData(addAttachmentForm)
                        });
                        (payload.addedAttachments || []).forEach((attachment) => {
                            if (attachmentsTbody) {
                                attachmentsTbody.appendChild(createAttachmentRow(attachment));
                            }
                            ensureAttachmentModal(attachment);
                        });
                        if (Array.isArray(payload.links)) {
                            renderLinks(payload.links);
                        }
                        addAttachmentForm.reset();
                        updateEmptyState();
                        showMessage('success', payload.message || 'La pièce jointe a bien été ajoutée');
                    } catch (error) {
                        showMessage('error', error.message || 'Erreur lors de l’ajout');
                    }
                });
            }

            document.addEventListener('submit', async (event) => {
                const removeAttachmentForm = event.target.closest('.js-remove-attachment-form');
                if (removeAttachmentForm) {
                    event.preventDefault();
                    if (!confirm('Confirmez-vous la suppression ?')) {
                        return;
                    }
                    try {
                        const payload = await submitDeleteForm(removeAttachmentForm);
                        const attachmentId = payload.attachmentId || removeAttachmentForm.dataset.attachmentId;
                        document.getElementById(`attachment-row-${attachmentId}`)?.remove();
                        updateEmptyState();
                        showMessage('success', payload.message || 'La pièce jointe a été supprimée');
                    } catch (error) {
                        showMessage('error', error.message || 'Erreur lors de la suppression');
                    }
                    return;
                }

                const removeLinkForm = event.target.closest('.js-remove-link-form');
                if (removeLinkForm) {
                    event.preventDefault();
                    if (!confirm('Confirmez-vous la suppression ?')) {
                        return;
                    }
                    try {
                        const payload = await submitDeleteForm(removeLinkForm);
                        if (Array.isArray(payload.links)) {
                            renderLinks(payload.links);
                        }
                        showMessage('success', payload.message || 'Le lien a été supprimé');
                    } catch (error) {
                        showMessage('error', error.message || 'Erreur lors de la suppression');
                    }
                }
            });

            updateEmptyState();
        document.addEventListener("click", event => {
            const previewButton = event.target.closest(".js-open-attachment-preview");
            if (previewButton) {
                window.openPdfModal(previewButton.dataset.attachmentId, previewButton.dataset.fileName);
            }
        });
    })();
