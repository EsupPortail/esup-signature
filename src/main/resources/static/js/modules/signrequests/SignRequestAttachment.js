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
                    .pdf-preview-page {
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
                        renderPromise: null
                    });
                }
                return pdfPreviewState.get(attachmentId);
            }

            function updatePdfZoomLabel(modalElement, scale) {
                const zoomLabel = modalElement?.querySelector('.js-pdf-zoom-value');
                if (!zoomLabel || typeof scale !== 'number' || Number.isNaN(scale)) {
                    return;
                }
                zoomLabel.textContent = `${Math.round(scale * 100)}%`;
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
                    state.loadingPromise = pdfjsLib.getDocument({
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
                const container = modalElement?.querySelector('.js-pdf-container');
                const state = getPdfPreviewState(modalElement);
                if (!container || !state) {
                    return;
                }
                const pdf = await loadPdfDocument(modalElement);
                if (!pdf) {
                    return;
                }

                updatePdfZoomLabel(modalElement, state.scale);
                container.innerHTML = '';
                for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber++) {
                    const page = await pdf.getPage(pageNumber);
                    const viewport = page.getViewport({ scale: state.scale });
                    const { pageWrapper, canvas, linksLayer } = createPdfPreviewPage(pageNumber, viewport);
                    const context = canvas.getContext('2d');
                    await page.render({ canvasContext: context, viewport }).promise;
                    await renderPdfLinkAnnotations(page, pdf, viewport, linksLayer, container);
                    container.appendChild(pageWrapper);
                }
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
                if (state.renderPromise) {
                    await state.renderPromise;
                }
                await renderPdfPreview(modalElement, { force: true });
            }

            function createPdfPreviewPage(pageNumber, viewport) {
                const pageWrapper = document.createElement('div');
                pageWrapper.className = 'pdf-preview-page';
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
                        <button type="button" class="btn btn-info float-end js-open-attachment-preview" title="Voir" data-attachment-id="${attachment.id}">
                            <i class="fi fi-rr-eye"></i>
                        </button>
                    </td>
                    <td>
                        <a href="${getAttachmentDownloadUrl(attachment.id)}" target="_blank" class="btn btn-primary float-end" title="Télécharger">
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
                            <button type="submit" class="btn btn-danger float-end" title="Supprimer" style="bottom: 10px;">
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

            function createAttachmentModalElement(attachment) {
                const wrapper = document.createElement('div');
                wrapper.innerHTML = `
                    <div class="modal fade modal-pdf js-attachment-preview-modal"
                         id="pdfModal${attachment.id}"
                         tabindex="-1"
                         role="dialog"
                         data-attachment-id="${attachment.id}"
                         data-file-name="${escapeHtml(attachment.fileName)}">
                        <div class="modal-dialog modal-xl modal-pdf" role="document">
                            <div class="modal-content">
                                <div class="modal-header">
                                    <h5 class="modal-title">${escapeHtml(attachment.fileName)}</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                                </div>
                                <div class="modal-body modal-pdf bg-dark-subtle" style="width:100%; height: 90vh;">
                                    ${isPdfFile(attachment.fileName)
                                        ? `<div class="d-flex justify-content-center align-items-center gap-2 mb-3">
                                                <button type="button" class="btn btn-outline-secondary btn-sm js-pdf-zoom-out" title="Zoom arrière" data-attachment-id="${attachment.id}">-</button>
                                                <span class="badge text-bg-light js-pdf-zoom-value" data-attachment-id="${attachment.id}">${Math.round(DEFAULT_PDF_SCALE * 100)}%</span>
                                                <button type="button" class="btn btn-outline-secondary btn-sm js-pdf-zoom-in" title="Zoom avant" data-attachment-id="${attachment.id}">+</button>
                                           </div>
                                           <div id="pdfContainer_${attachment.id}" class="js-pdf-container d-flex flex-column align-items-center" style="height: calc(100% - 48px); overflow: auto;"></div>`
                                        : `<div class="alert alert-info">Aperçu indisponible pour ce format. <a href="${getAttachmentInlineUrl(attachment.id)}" target="_blank">Télécharger le document</a></div>`}
                                </div>
                            </div>
                        </div>
                    </div>`;
                return wrapper.firstElementChild;
            }

            function ensureAttachmentModal(attachment) {
                if (!previewModalsContainer || document.getElementById(`pdfModal${attachment.id}`)) {
                    return;
                }
                const modalElement = createAttachmentModalElement(attachment);
                previewModalsContainer.appendChild(modalElement);
                setupPreviewModal(modalElement);
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
                    state.renderPromise = renderPdfPages(modalElement);
                    await state.renderPromise;
                    modalElement.dataset.pdfLoaded = 'true';
                } catch (error) {
                    alert(error.message);
                    const container = modalElement.querySelector('.js-pdf-container');
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
                    renderPdfPreview(modalElement);
                });
            }

            document.querySelectorAll('.js-attachment-preview-modal').forEach(setupPreviewModal);

            document.addEventListener('click', async (event) => {
                const zoomInButton = event.target.closest('.js-pdf-zoom-in');
                if (zoomInButton) {
                    const modalElement = zoomInButton.closest('.js-attachment-preview-modal');
                    await changePdfZoom(modalElement, PDF_SCALE_STEP);
                    return;
                }

                const zoomOutButton = event.target.closest('.js-pdf-zoom-out');
                if (zoomOutButton) {
                    const modalElement = zoomOutButton.closest('.js-attachment-preview-modal');
                    await changePdfZoom(modalElement, -PDF_SCALE_STEP);
                }
            });

            window.openPdfModal = function (attachmentId) {
                const modalElement = document.getElementById(`pdfModal${attachmentId}`);
                if (!modalElement) {
                    return;
                }
                setupPreviewModal(modalElement);
                const modal = new bootstrap.Modal(modalElement, {
                    backdrop: 'static',
                    keyboard: false
                });
                modal.show();
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
                        document.getElementById(`pdfModal${attachmentId}`)?.remove();
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
                window.openPdfModal(previewButton.dataset.attachmentId);
            }
        });
    })();
