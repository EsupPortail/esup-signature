export class PdfRendererController {

    constructor(viewer) {
        this.viewer = viewer;
    }

    async startRender(pdf) {
        if (pdf != null && this.viewer.pdfDoc == null) {
            this.viewer.pdfDoc = pdf;
        }
        if (this.viewer.pdfDoc == null) {
            return;
        }
        this.viewer.renderFailed = false;
        if (this.viewer.isRendering || this.viewer.activeRenders > 0 || this.viewer.renderQueue.length > 0) {
            this.viewer.pendingRender = true;
            this.viewer.pendingRenderPdf = pdf ?? this.viewer.pdfDoc;
            return;
        }

        this.viewer.isRendering = true;
        this.viewer.renderComplete = false;
        this.viewer.renderFinishedFired = false;
        this.viewer.pendingRender = false;
        this.viewer.pendingRenderPdf = null;
        this.viewer.renderScale = this.getTargetRenderScale();
        this.clearInitialRenderTransform();
        const currentRenderCycleId = ++this.viewer.renderCycleId;
        this.viewer.fireEvent("renderStarted", ['ok']);
        this.viewer.pdfDiv.css('opacity', 0);
        this.viewer.numPages = this.viewer.pdfDoc.numPages;
        document.getElementById('page_count').textContent = this.viewer.pdfDoc.numPages;
        this.viewer.renderedPages = 0;
        this.viewer.pages = [];
        this.viewer.renderQueue = [];
        this.viewer.renderedPageNums.clear();
        this.viewer.renderingPageNums.clear();
        this.viewer.queuedPageNums.clear();
        this.viewer.textLayerPageNums.clear();
        this.viewer.textLayerRenderingPageNums.clear();
        this.viewer.textLayerRenderPromise = null;
        this.viewer.pageViewports.clear();
        this.viewer.activeRenders = 0;
        this.viewer.disableScrollBtn();
        this.viewer.resetProgress();
        $("#pdf-progress-bar").addClass("es-progress-visible");
        this.viewer.startProgress();

        try {
            const config = await this.viewer.pdfDoc.getOptionalContentConfig().catch(e => {
                console.error("Error getting optional content config: " + e);
                return null;
            });
            this.viewer._optionalContentConfigPromise = Promise.resolve(config);
        } catch (e) {
            console.error("Error getting optional content config: " + e);
            this.viewer._optionalContentConfigPromise = Promise.resolve(null);
        }

        await this.preparePagePlaceholders();
        this.queueVisiblePages();
        this.processRenderQueue(currentRenderCycleId);

        this.viewer.refreshTools();
        this.viewer.fireEvent("ready", ['ok']);
    }

    processRenderQueue(renderCycleId = this.viewer.renderCycleId) {
        if (this.viewer.renderFailed) {
            return;
        }
        while (this.viewer.activeRenders < this.viewer.maxConcurrentRenders && this.viewer.renderQueue.length > 0) {
            const pageNum = this.viewer.renderQueue.shift();
            this.viewer.queuedPageNums.delete(pageNum);
            if (this.viewer.renderedPageNums.has(pageNum) || this.viewer.renderingPageNums.has(pageNum)) {
                continue;
            }
            this.viewer.renderingPageNums.add(pageNum);
            this.viewer.activeRenders++;

            this.viewer.pdfDoc.getPage(pageNum).then(page => {
                return this.renderTask(page, pageNum, this.viewer._optionalContentConfigPromise.catch(e => {
                    console.error("Error in optionalContentConfigPromise: " + e);
                    return null;
                }), renderCycleId);
            }).then(async () => {
                this.viewer.activeRenders = Math.max(0, this.viewer.activeRenders - 1);
                this.viewer.renderingPageNums.delete(pageNum);
                if (this.viewer.renderFailed) {
                    return;
                }
                if (renderCycleId !== this.viewer.renderCycleId) {
                    if (this.viewer.activeRenders === 0 && this.viewer.pendingRender) {
                        this.viewer.isRendering = false;
                        const pendingPdf = this.viewer.pendingRenderPdf ?? this.viewer.pdfDoc;
                        this.viewer.pendingRender = false;
                        this.viewer.pendingRenderPdf = null;
                        await this.startRender(pendingPdf);
                    }
                    return;
                }
                this.viewer.renderedPages++;
                this.viewer.updateRenderProgress();
                this.viewer.renderedPageNums.add(pageNum);
                this.releaseDistantPages();

                if(this.viewer.renderQueue.length === 0 && this.viewer.activeRenders === 0) {
                    this.viewer.isRendering = false;
                    if (this.viewer.pendingRender) {
                        const pendingPdf = this.viewer.pendingRenderPdf ?? this.viewer.pdfDoc;
                        this.viewer.pendingRender = false;
                        this.viewer.pendingRenderPdf = null;
                        await this.startRender(pendingPdf);
                        return;
                    }
                    if (this.viewer._isRefreshingOCG) {
                        this.viewer._isRefreshingOCG = false;
                        this.viewer.applyLinkAnnotationsVisibility().catch(err => console.error('Erreur masquage liens OCG:', err));
                    } else {
                        await this.finishInitialRender();
                    }
                } else {
                    this.processRenderQueue(renderCycleId);
                }
            })
                .catch(async err => {
                    this.viewer.renderingPageNums.delete(pageNum);
                    if (this.viewer.renderFailed || renderCycleId !== this.viewer.renderCycleId) {
                        this.viewer.activeRenders = Math.max(0, this.viewer.activeRenders - 1);
                        if (this.viewer.activeRenders === 0 && this.viewer.pendingRender) {
                            this.viewer.isRendering = false;
                            const pendingPdf = this.viewer.pendingRenderPdf ?? this.viewer.pdfDoc;
                            this.viewer.pendingRender = false;
                            this.viewer.pendingRenderPdf = null;
                            await this.startRender(pendingPdf);
                        }
                        return;
                    }
                    console.error(`Erreur rendu page ${pageNum}:`, err);
                    this.viewer.activeRenders = Math.max(0, this.viewer.activeRenders - 1);
                    this.viewer._isRefreshingOCG = false;
                    this.viewer.failRender(err, "Impossible de rendre la page " + pageNum + " du document PDF.");
                });
        }
    }

    async preparePagePlaceholders() {
        this.viewer.pdfDiv[0].querySelectorAll('.pdf-page').forEach(pageElement => pageElement.remove());
        for (let i = 1; i <= this.viewer.numPages; i++) {
            const page = await this.viewer.pdfDoc.getPage(i);
            this.viewer.pages[i - 1] = page;
            const pageRotation = this.viewer.getPageRotation(page);
            const viewport = page.getViewport({
                scale: this.viewer.renderScale,
                rotation: pageRotation
            });
            this.viewer.pageViewports.set(i, viewport);
            const renderedWidth = Math.floor(viewport.width);
            const renderedHeight = Math.floor(viewport.height);
            const scaleRatio = this.getDisplayScaleRatio();
            const displayWidth = Math.floor(renderedWidth * scaleRatio);
            const displayHeight = Math.floor(renderedHeight * scaleRatio);
            const container = document.createElement("div");
            container.id = `page_${i}`;
            container.setAttribute("page-num", i);
            container.className = "drop-shadows pdf-page";
            container.style.marginBottom = `${10 * this.viewer.scale}px`;
            container.style.width = `${displayWidth}px`;
            container.style.height = `${displayHeight}px`;
            container.style.overflow = 'hidden';
            container.dataset.renderedWidth = `${renderedWidth}`;
            container.dataset.renderedHeight = `${renderedHeight}`;
            container.dataset.pdfRendered = "false";
            this.viewer.pdfDiv[0].appendChild(container);
            $(container).droppable({
                drop: (event, ui) => ui.helper.attr("page", i)
            });
        }
        this.updateHorizontalOverflowState();
    }

    queueVisiblePages() {
        if (!this.viewer.pdfDoc || this.viewer.renderFailed) {
            return;
        }
        const pagesToRender = this.getPagesAroundViewport();
        pagesToRender.forEach(pageNum => this.queuePageRender(pageNum));
        this.processRenderQueue();
    }

    queuePageRender(pageNum) {
        if (!Number.isFinite(pageNum) || pageNum < 1 || pageNum > this.viewer.numPages) {
            return;
        }
        if (this.viewer.renderedPageNums.has(pageNum) || this.viewer.renderingPageNums.has(pageNum) || this.viewer.queuedPageNums.has(pageNum)) {
            return;
        }
        this.viewer.renderQueue.push(pageNum);
        this.viewer.queuedPageNums.add(pageNum);
    }

    getPagesAroundViewport(extraPages = this.viewer.renderBufferPages) {
        const visiblePages = this.viewer.getVisiblePages();
        const pages = new Set();
        if (visiblePages.length === 0) {
            const currentPage = Math.min(Math.max(this.viewer.pageNum || 1, 1), this.viewer.numPages);
            for (let i = currentPage - extraPages; i <= currentPage + extraPages; i++) {
                if (i >= 1 && i <= this.viewer.numPages) {
                    pages.add(i);
                }
            }
        }
        visiblePages.forEach(pageNum => {
            for (let i = pageNum - extraPages; i <= pageNum + extraPages; i++) {
                if (i >= 1 && i <= this.viewer.numPages) {
                    pages.add(i);
                }
            }
        });
        return Array.from(pages).sort((a, b) => a - b);
    }

    releaseDistantPages() {
        if (!this.viewer.pdfDoc || this.viewer.renderedPageNums.size <= this.viewer.maxRenderedPages) {
            return;
        }
        const pagesToKeep = new Set(this.getPagesAroundViewport(this.viewer.renderBufferPages + 1));
        for (const pageNum of Array.from(this.viewer.renderedPageNums)) {
            if (pagesToKeep.has(pageNum)) {
                continue;
            }
            const container = document.getElementById(`page_${pageNum}`);
            if (!container) {
                this.viewer.renderedPageNums.delete(pageNum);
                continue;
            }
            container.querySelectorAll('.canvasWrapper, .annotationLayer').forEach(element => element.remove());
            container.dataset.pdfRendered = "false";
            this.viewer.renderedPageNums.delete(pageNum);
        }
    }

    async finishInitialRender() {
        if (this.viewer.renderFinishedFired) {
            this.viewer.enableScrollBtn();
            return;
        }
        this.viewer.renderFinishedFired = true;
        this.viewer.renderedScale = this.viewer.renderScale;
        this.viewer.applyScaleWithoutRerender();
        this.clearInitialRenderTransform();
        this.viewer.stopProgress();
        const progressBar = $("#pdf-progress-bar");
        progressBar.removeClass("es-progress-visible");
        try {
            await this.postRenderAll();
            this.viewer.enableScrollBtn();
            this.viewer.fireEvent("renderFinished", ['ok']);
            $(document).trigger("renderFinished");
            this.fireRenderCompleteAfterProgressHidden(progressBar);
            this.ensureAllTextLayers();
        } catch (error) {
            this.viewer.failRender(error, "Impossible de finaliser l’affichage du document PDF.");
        }
    }

    ensureAllTextLayers() {
        if (!globalThis.pdfjsViewer?.TextLayerBuilder) {
            return;
        }
        if (this.viewer.textLayerRenderPromise) {
            return;
        }
        const renderCycleId = this.viewer.renderCycleId;
        this.viewer.textLayerRenderPromise = (async () => {
            for (let pageNum = 1; pageNum <= this.viewer.numPages; pageNum++) {
                if (this.viewer.renderFailed || renderCycleId !== this.viewer.renderCycleId) {
                    return;
                }
                await this.ensureTextLayer(pageNum, renderCycleId).catch(error => {
                    console.error("Impossible de rendre la couche texte de la page " + pageNum, error);
                });
                await new Promise(resolve => window.setTimeout(resolve, 0));
            }
        })().finally(() => {
            this.viewer.textLayerRenderPromise = null;
        });
    }

    async ensureTextLayer(pageNum, renderCycleId = this.viewer.renderCycleId) {
        if (this.viewer.textLayerPageNums.has(pageNum) || this.viewer.textLayerRenderingPageNums.has(pageNum)) {
            return;
        }
        const container = document.getElementById(`page_${pageNum}`);
        const viewport = this.viewer.pageViewports.get(pageNum);
        if (!container || !viewport || container.querySelector('.textLayer')) {
            if (container?.querySelector('.textLayer')) {
                this.viewer.textLayerPageNums.add(pageNum);
            }
            return;
        }
        this.viewer.textLayerRenderingPageNums.add(pageNum);
        try {
            const page = this.viewer.pages[pageNum - 1] ?? await this.viewer.pdfDoc.getPage(pageNum);
            if (renderCycleId !== this.viewer.renderCycleId) {
                return;
            }
            this.viewer.pages[pageNum - 1] = page;
            const renderedWidth = Number.parseFloat(container.dataset.renderedWidth || '0');
            const renderedHeight = Number.parseFloat(container.dataset.renderedHeight || '0');
            const scaleRatio = this.getDisplayScaleRatio();
            const pageDiv = this.ensurePageDiv(container, renderedWidth, renderedHeight, scaleRatio);
            const textLayerBuilder = new globalThis.pdfjsViewer.TextLayerBuilder({
                pdfPage: page,
                onAppend: textLayerDiv => {
                    if (renderCycleId === this.viewer.renderCycleId) {
                        pageDiv.appendChild(textLayerDiv);
                    }
                }
            });
            await textLayerBuilder.render({viewport});
            if (renderCycleId !== this.viewer.renderCycleId) {
                return;
            }
            const textLayer = pageDiv.querySelector('.textLayer');
            if (textLayer) {
                textLayer.style.width = `${renderedWidth}px`;
                textLayer.style.height = `${renderedHeight}px`;
                textLayer.style.left = '0';
                textLayer.style.top = '0';
                textLayer.style.setProperty('--scale-factor', this.viewer.renderScale);
                textLayer.style.setProperty('--total-scale-factor', this.viewer.renderScale);
                this.viewer.textLayerPageNums.add(pageNum);
            }
        } finally {
            this.viewer.textLayerRenderingPageNums.delete(pageNum);
        }
    }

    ensurePageDiv(container, renderedWidth, renderedHeight, scaleRatio) {
        let pageDiv = container.querySelector('.page');
        if (!pageDiv) {
            pageDiv = document.createElement('div');
            pageDiv.className = 'page';
            pageDiv.style.position = 'relative';
            pageDiv.style.padding = '0';
            pageDiv.style.margin = '0';
            pageDiv.style.transformOrigin = 'top left';
            container.appendChild(pageDiv);
        }
        pageDiv.style.width = `${renderedWidth}px`;
        pageDiv.style.height = `${renderedHeight}px`;
        pageDiv.style.transform = scaleRatio === 1 ? '' : `scale(${scaleRatio})`;
        return pageDiv;
    }

    fireRenderCompleteAfterProgressHidden(progressBar) {
        const progressElement = progressBar?.get?.(0);
        let completed = false;
        const complete = () => {
            if (completed) {
                return;
            }
            completed = true;
            this.viewer.renderComplete = true;
            this.viewer.fireEvent("renderComplete", ['ok']);
        };
        if (progressElement == null) {
            complete();
            return;
        }
        const computedOpacity = Number.parseFloat(window.getComputedStyle(progressElement).opacity);
        if (Number.isFinite(computedOpacity) && computedOpacity === 0) {
            complete();
            return;
        }
        progressElement.addEventListener("transitionend", event => {
            if (event.target === progressElement && event.propertyName === "opacity") {
                complete();
            }
        });
        window.setTimeout(complete, 1000);
    }

    clearInitialRenderTransform() {
        this.viewer.pdfDiv.css({
            transform: '',
            transformOrigin: '',
        });
    }

    getDisplayScaleRatio() {
        const scaleRatio = this.viewer.scale / this.viewer.renderScale;
        if (!Number.isFinite(scaleRatio) || scaleRatio <= 0) {
            return 1;
        }
        return scaleRatio;
    }

    getTargetRenderScale() {
        return Number.isFinite(this.viewer.maxRenderScale) && this.viewer.maxRenderScale > 0
            ? this.viewer.maxRenderScale
            : 2.5;
    }

    async renderTask(page, i, configPromise, renderCycleId = this.viewer.renderCycleId) {
        return new Promise((resolve, reject) => {
            let container = document.getElementById(`page_${i}`);
            if (!container) {
                container = document.createElement("div");
                container.id = `page_${i}`;
                container.setAttribute("page-num", i);
                container.className = "drop-shadows pdf-page";
                container.style.marginBottom = `${10 * this.viewer.scale}px`;
                this.insertPageAtCorrectPosition(container, i);
            } else {
                container.innerHTML = "";
                container.style.marginBottom = `${10 * this.viewer.scale}px`;
            }
            $(container).droppable({
                drop: (event, ui) => ui.helper.attr("page", i)
            });

            const pageRotation = this.viewer.getPageRotation(page);

            const viewport = page.getViewport({
                scale: this.viewer.renderScale,
                rotation: pageRotation
            });
            const renderedWidth = Math.floor(viewport.width);
            const renderedHeight = Math.floor(viewport.height);
            const scaleRatio = this.getDisplayScaleRatio();
            const displayWidth = Math.floor(renderedWidth * scaleRatio);
            const displayHeight = Math.floor(renderedHeight * scaleRatio);
            container.style.width = `${displayWidth}px`;
            container.style.height = `${displayHeight}px`;
            container.style.overflow = 'hidden';
            container.style.visibility = 'hidden';

            const pdfPageView = new pdfjsViewer.PDFPageView({
                eventBus: this.viewer.eventBus,
                container: container,
                id: i,
                scale: this.viewer.renderScale,
                defaultViewport: viewport,
                useOnlyCssZoom: true,
                defaultZoomDelay: 0,
                textLayerMode: 1,
                annotationMode: pdfjsLib.AnnotationMode.ENABLE_FORMS,
                optionalContentConfigPromise: configPromise
            });

            pdfPageView.setPdfPage(page);

            pdfPageView.draw().then(async () => {
                if (renderCycleId !== this.viewer.renderCycleId || this.viewer.pendingRender) {
                    resolve("obsolete");
                    return;
                }
                const container = document.getElementById(`page_${i}`);
                if (!container) {
                    if (renderCycleId !== this.viewer.renderCycleId || this.viewer.pendingRender) {
                        resolve("obsolete");
                        return;
                    }
                    reject(new Error("Container disparu"));
                    return;
                }
                const canvas = container.querySelector('canvas');
                if (!canvas) {
                    if (renderCycleId !== this.viewer.renderCycleId || this.viewer.pendingRender) {
                        resolve("obsolete");
                        return;
                    }
                    reject(new Error("Pas de canvas"));
                    return;
                }
                canvas.style.width = `${renderedWidth}px`;
                canvas.style.height = `${renderedHeight}px`;
                const canvasWrapper = container.querySelector('.canvasWrapper');
                if (canvasWrapper) {
                    canvasWrapper.style.width = `${renderedWidth}px`;
                    canvasWrapper.style.height = `${renderedHeight}px`;
                    canvasWrapper.style.padding = '0';
                    canvasWrapper.style.margin = '0';
                    canvasWrapper.style.overflow = 'hidden';
                }
                const pageDiv = container.querySelector('.page');
                if (pageDiv) {
                    pageDiv.style.width = `${renderedWidth}px`;
                    pageDiv.style.height = `${renderedHeight}px`;
                    pageDiv.style.padding = '0';
                    pageDiv.style.margin = '0';
                    pageDiv.style.transformOrigin = 'top left';
                    pageDiv.style.transform = scaleRatio === 1 ? '' : `scale(${scaleRatio})`;
                }
                container.style.width = `${displayWidth}px`;
                container.style.height = `${displayHeight}px`;
                container.style.overflow = 'hidden';
                container.dataset.renderedWidth = `${renderedWidth}`;
                container.dataset.renderedHeight = `${renderedHeight}`;
                const layerWidth = renderedWidth;
                const layerHeight = renderedHeight;
                const annotationLayer = container.querySelector('.annotationLayer');
                if (annotationLayer) {
                    annotationLayer.setAttribute("data-main-rotation", pageRotation);
                    annotationLayer.style.width = `${layerWidth}px`;
                    annotationLayer.style.height = `${layerHeight}px`;
                    annotationLayer.style.setProperty('--scale-factor', this.viewer.renderScale);
                    annotationLayer.style.setProperty('--total-scale-factor', this.viewer.renderScale);
                }
                const textLayer = container.querySelector('.textLayer');
                if (textLayer) {
                    textLayer.style.width = `${layerWidth}px`;
                    textLayer.style.height = `${layerHeight}px`;
                    textLayer.style.left = '0';
                    textLayer.style.top = '0';
                    textLayer.style.setProperty('--scale-factor', this.viewer.renderScale);
                    textLayer.style.setProperty('--total-scale-factor', this.viewer.renderScale);
                    this.viewer.textLayerPageNums.add(i);
                }
                this.viewer.pages[i - 1] = page;
                await this.postRender(page);
                const annotations = await page.getAnnotations();
                this.viewer.restoreValues(annotations);
                this.viewer.annotationLinkTargetBlank();
                await this.viewer.applyLinkAnnotationsVisibilityForPage(i, null)
                    .catch(err => console.error('Erreur masquage liens OCG page ' + i + ':', err));
                container.dataset.pdfRendered = "true";
                container.style.visibility = '';
                this.updateHorizontalOverflowState();
                resolve("ok");
            }).catch(err => {
                if (renderCycleId !== this.viewer.renderCycleId || this.viewer.pendingRender) {
                    resolve("obsolete");
                    return;
                }
                console.error("Erreur dans pdfPageView.draw() page", i, ":", err);
                reject(err);
            });
        });
    }

    insertPageAtCorrectPosition(container, pageNum) {
        const pdfDivElement = this.viewer.pdfDiv[0];
        const allPages = pdfDivElement.querySelectorAll('.pdf-page');
        let inserted = false;
        for (let page of allPages) {
            const currentPageNum = parseInt(page.getAttribute('page-num'));
            if (currentPageNum > pageNum) {
                pdfDivElement.insertBefore(container, page);
                inserted = true;
                break;
            }
        }
        if (!inserted) {
            pdfDivElement.appendChild(container);
        }
    }

    async postRenderAll() {
        this.viewer.annotationLinkTargetBlank();
        await this.viewer.promiseRestoreValue();
        this.viewer.restoreScrolling();
        this.updateHorizontalOverflowState();
        await this.viewer.applyLinkAnnotationsVisibility().catch(err => console.error('Erreur postRenderAll liens OCG:', err));
    }

    updateHorizontalOverflowState() {
        const workspace = this.viewer.getWorkspaceElement();
        if (!workspace) {
            return;
        }
        workspace.style.overflowX = 'auto';
        const firstPage = document.getElementById('page_1');
        if (!firstPage) {
            return;
        }
        const hasHorizontalOverflow = firstPage.offsetWidth > workspace.clientWidth;
        if (hasHorizontalOverflow) {
            workspace.style.setProperty('justify-content', 'flex-start', 'important');
        } else {
            workspace.style.removeProperty('justify-content');
        }
        this.viewer.pdfDiv.css('align-items', hasHorizontalOverflow ? 'flex-start' : 'center');
        if (!hasHorizontalOverflow) {
            workspace.scrollLeft = 0;
        }
    }

    async postRender(page) {
        await this.viewer.promiseRenderForm(false, page);
        console.groupEnd();
    }
}
