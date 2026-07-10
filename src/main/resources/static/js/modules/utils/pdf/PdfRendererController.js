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
        this.viewer.pendingRender = false;
        this.viewer.pendingRenderPdf = null;
        this.viewer.renderScale = Math.max(this.viewer.scale, this.viewer.getMaxZoomLimit());
        this.clearInitialRenderTransform();
        const currentRenderCycleId = ++this.viewer.renderCycleId;
        this.viewer.fireEvent("renderStarted", ['ok']);
        this.viewer.pdfDiv.css('opacity', 0);
        this.viewer.numPages = this.viewer.pdfDoc.numPages;
        document.getElementById('page_count').textContent = this.viewer.pdfDoc.numPages;
        this.viewer.renderedPages = 0;
        this.viewer.pages = [];
        this.viewer.renderQueue = [];
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

        for (let i = 1; i <= this.viewer.numPages; i++) {
            this.viewer.renderQueue.push(i);
        }
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
            this.viewer.activeRenders++;

            this.viewer.pdfDoc.getPage(pageNum).then(page => {
                return this.renderTask(page, pageNum, this.viewer._optionalContentConfigPromise.catch(e => {
                    console.error("Error in optionalContentConfigPromise: " + e);
                    return null;
                }), renderCycleId);
            }).then(async () => {
                this.viewer.activeRenders = Math.max(0, this.viewer.activeRenders - 1);
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
                        const renderedPageCount = this.viewer.pages.filter(page => page != null).length;
                        if(this.viewer.renderedPages === this.viewer.numPages && renderedPageCount === this.viewer.numPages) {
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
                            } catch (error) {
                                this.viewer.failRender(error, "Impossible de finaliser l’affichage du document PDF.");
                            }
                        } else {
                            this.viewer.failRender(
                                new Error("Rendu PDF incomplet : " + renderedPageCount + "/" + this.viewer.numPages + " pages disponibles."),
                                "Impossible de rendre toutes les pages du document PDF."
                            );
                        }
                    }
                } else {
                    this.processRenderQueue(renderCycleId);
                }
            })
                .catch(async err => {
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

    applyInitialRenderTransform() {
        const scaleRatio = this.viewer.scale / this.viewer.renderScale;
        if (!Number.isFinite(scaleRatio) || scaleRatio <= 0 || scaleRatio === 1) {
            this.clearInitialRenderTransform();
            return;
        }
        this.viewer.pdfDiv.css({
            transform: `scale(${scaleRatio})`,
            transformOrigin: 'top center',
        });
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

            pdfPageView.draw().then(() => {
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
                }
                this.viewer.pages[i - 1] = page;
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
        const renderTasks = [];
        for(let i = 0; i < this.viewer.numPages; i++) {
            if (this.viewer.pages[i] != null) {
                renderTasks.push(this.postRender(this.viewer.pages[i]));
            }
        }
        await Promise.all(renderTasks);
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
