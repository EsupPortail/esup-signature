import {EventBus} from "../../customs/ui_utils.js?version=@version@";
import {EventFactory} from "./EventFactory.js?version=@version@";
import {DataField} from "../../prototypes/DataField.js?version=@version@";
import { LayerHighlighter } from './LayerHighlighter.js';

export class PdfViewer extends EventFactory {

    constructor(url, signable, editable, currentStepNumber, forcePageNum, fields, disableAllFields) {
        super();
        console.info("Starting PDF Viewer, signable : " + signable);
        this.highlighter = new LayerHighlighter(this);
        this.timer = null;
        this.viewed = false;
        this.url = url;
        this.interval = null;
        this.pages = [];
        this.signable = signable;
        this.editable = editable;
        this.currentStepNumber = currentStepNumber;
        this.saveScrolling = 0;
        this.pageNum = 1;
        this.eventBus = new EventBus({dispatchToDOM: false});
        this._optionalContentConfigPromise = null;
        const testUrl = new URL(window.location.href);
        if(forcePageNum != null) {
            this.pageNum = forcePageNum;
        }
        let jsFields = [];
        if(fields) {
            fields.forEach(function (e){
                jsFields.push(new DataField(e));
            });
        }
        this.disableAllFields = disableAllFields;
        this.scale = 1.2;
        this.zoomStep = 0.1;
        this.pdfDiv = $("#pdf");
        this.pdfDoc = null;
        this.numPages = 1;
        this.page = null;
        this.dataFields = jsFields;
        this.savedFields = new Map();
        this.linkValidationStates = new Map();
        this.linkValidationTimers = new Map();
        this.linkValidationControllers = new Map();
        this.linkValidationSeq = new Map();
        this.events = {};
        this.rotationOverride = null;
        this.renderedPages = 0;
        this.renderQueue = [];
        this.activeRenders = 0;
        this.maxConcurrentRenders = 5;
        this.renderCycleId = 0;
        this.isRendering = false;
        this.pendingRender = false;
        this.pendingRenderPdf = null;
        this.renderComplete = false;
        this.renderedPagesMap = new Map();
        this.displayedPagesMap = new Map();
        this.lastWidth = window.innerWidth;
        this.lastHeight = window.innerHeight;
        this.currentOptionalContentConfig = null;
        this._activeLayerView = null; // { stepNumber: number, solo: boolean } | null
        let self = this;
        $(document).ready(function() {
            if (!globalThis.pdfjsLib || !Promise.withResolvers) {
                bootbox.alert("Votre navigateur ne support pas pdfJs pour l'affichage des PDF.<br>Version minimales : Firefox 121, Chrome 119, Safari 17.4", function () {
                    document.location = "https://www.mozilla.org/fr/firefox/new/"
                });
            } else {
                if (!self.url) {
                    throw new Error("PdfViewer: self.url est vide");
                }
                if (globalThis.pdfjsLib.GlobalWorkerOptions) {
                    globalThis.pdfjsLib.GlobalWorkerOptions.workerSrc = '/webjars/pdfjs-dist/legacy/build/pdf.worker.min.mjs';
                }
                let loadingTask = globalThis.pdfjsLib.getDocument({
                    url: self.url,
                    useWasm: true,
                    wasmUrl: `/webjars/pdfjs-dist/wasm/`
                });
                loadingTask.promise.then(async function(pdf) {
                    await self.startRender(pdf)
                });
            }
        });
        this.initListeners();
    }

    initListeners() {
        let self = this;

        $('#zoomin').on('click', e => this.zoomIn());
        $('#zoomout').on('click', e => this.zoomOut());
        $('#fullwidth').on('click', e => this.fullWidth());
        $('#fullheight').on('click', e => this.fullHeight());
        $('#autoRotate').on('click', e => this.autoRotate());
        $(document).on('click', '.display-layer-btn', (e) => {
            const stepNumber = parseInt($(e.currentTarget).data('step'));
            const layerId = $(e.currentTarget).data('layer-id');
            self.toggleLayerByStep(stepNumber, false, layerId);
        });

        $(document).on('click', '.toggle-layer-btn', (e) => {
            const stepNumber = parseInt($(e.currentTarget).data('step'));
            const layerId = $(e.currentTarget).data('layer-id');
            self.toggleLayerByStep(stepNumber, true, layerId);
        });

        $(document).on('mouseenter', '.toggle-layer-btn', (e) => {
            const stepNumber = parseInt($(e.currentTarget).data('step'));
            const layerId = $(e.currentTarget).data('layer-id');
            self.highlightStep(stepNumber, layerId);
        });

        $(document).on('mouseleave', '.toggle-layer-btn', (e) => {
            self.clearHighlight();
        });

        $(document).on('mouseenter', '.toggle-layer-div', (e) => {
            const stepNumber = parseInt($(e.currentTarget).data('step'));
            const layerId = $(e.currentTarget).find('[data-layer-id]').first().data('layer-id');
            self.highlightStep(stepNumber, layerId);
        });

        $(document).on('mouseleave', '.toggle-layer-div', (e) => {
            self.clearHighlight();
        });

        const THRESHOLD = 100;
        const DEBOUNCE_DELAY = 100;
        let resizeTimer = null;
        $(window).on("resize", () => {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(() => {
                const w = window.innerWidth;
                const h = window.innerHeight;
                const deltaW = Math.abs(w - self.lastWidth);
                const deltaH = Math.abs(h - self.lastHeight);
                if (w === self.lastWidth || (deltaW < THRESHOLD && deltaH < THRESHOLD)) return;
                self.adjustZoom();
                self.lastWidth = w;
                self.lastHeight = h;
            }, DEBOUNCE_DELAY);
        });
        $('#page_num').on('change', e => this.scrollToPage(e.target.value));
        if(localStorage.getItem("scale")) {
            this.scale = parseFloat(localStorage.getItem("scale"));
        } else {
            this.adjustZoom();
        }
    }

    getWorkspaceElement() {
        return document.getElementById('workspace');
    }

    getScrollTop() {
        const workspace = this.getWorkspaceElement();
        return workspace ? workspace.scrollTop : window.scrollY;
    }

    getViewportHeight() {
        const workspace = this.getWorkspaceElement();
        return workspace ? workspace.clientHeight : window.innerHeight;
    }

    scrollToPosition(top, behavior = 'auto') {
        const workspace = this.getWorkspaceElement();
        if (workspace) {
            workspace.scrollTo({
                top: Math.max(0, top),
                left: 0,
                behavior: behavior,
            });
            return;
        }
        window.scrollTo({
            top: Math.max(0, top),
            left: 0,
            behavior: behavior,
        });
    }

    animateScrollToPosition(top) {
        const targetTop = Math.max(0, top);
        const workspace = this.getWorkspaceElement();
        if (workspace) {
            $(workspace).stop().animate({
                scrollTop: targetTop
            }, 100);
            return;
        }
        $('html, body').stop().animate({
            scrollTop: targetTop
        }, 100);
    }

    getPageRelativeTop(pageNum) {
        const page = $("#page_" + pageNum);
        if (!page.length) {
            return 0;
        }
        const firstPage = $("#page_1");
        const pageTop = page.position()?.top ?? 0;
        if (!firstPage.length) {
            return Math.round(pageTop);
        }
        const firstPageTop = firstPage.position()?.top ?? 0;
        return Math.round(pageTop - firstPageTop);
    }

    // Absolute top within #pdf positioning context (used by absolute overlays).
    getPageTopInPdf(pageNum) {
        const page = $("#page_" + pageNum);
        if (!page.length) {
            return 0;
        }
        return Math.round(page.position()?.top ?? 0);
    }

    // Absolute left within #pdf positioning context (used by absolute overlays).
    getPageLeftInPdf(pageNum) {
        const page = $("#page_" + pageNum);
        if (!page.length) {
            return 0;
        }
        return Math.round(page.position()?.left ?? 0);
    }

    set optionalContentConfigPromise(promise) {
        this._optionalContentConfigPromise = promise;
        this.eventBus.dispatch("optionalcontentconfigchanged", {
            source: this,
            promise: promise
        });
        this.refreshVisiblePages(promise);
    }

    get optionalContentConfigPromise() {
        return this._optionalContentConfigPromise;
    }

    refreshVisiblePages(configPromise) {
        if (!configPromise) {
            return;
        }
        this._isRefreshingOCG = true;
        const visiblePages = this.getVisiblePages();
        visiblePages.forEach(pageNum => {
            const pageContainer = document.getElementById(`page_${pageNum}`);
            if (pageContainer) {
                // Vider le canvas, garder l'enveloppe
                const canvasWrapper = pageContainer.querySelector('.canvasWrapper');
                if (canvasWrapper) {
                    canvasWrapper.innerHTML = '';
                }
                this.renderQueue.unshift(pageNum);
            }
        });
        this.processRenderQueue();
    }

    getVisiblePages() {
        const visiblePages = [];
        const scrollTop = this.getScrollTop();
        const scrollBottom = scrollTop + this.getViewportHeight();

        for (let i = 1; i <= this.numPages; i++) {
            const pageElement = document.getElementById(`page_${i}`);
            if (pageElement) {
                const elementTop = this.getPageRelativeTop(i);
                const elementBottom = elementTop + pageElement.offsetHeight;
                if (elementBottom > scrollTop && elementTop < scrollBottom) {
                    visiblePages.push(i);
                }
            }
        }
        return visiblePages;
    }

    restoreScrolling() {
        let newScrolling = Math.round(this.saveScrolling * this.scale);
        this.scrollToPosition(newScrolling);
    }

    listenToSearchCompletion() {
        let controller = new AbortController();
        let signal = controller.signal;
        console.info("listen to search autocompletion");
        $(".search-completion").each(function () {
            const $input = $(this);
            if ($input.data("esAutocompleteBound") === true) {
                return;
            }
            let serviceName = $input.attr("search-completion-service-name");
            let searchType = $input.attr("search-completion-type");
            let searchReturn = $input.attr("search-completion-return");
            $input.autocomplete({
                delay: 500,
                source: function( request, response ) {
                    if(request.term.length > 2) {
                        controller.abort();
                        controller = new AbortController()
                        signal = controller.signal;
                        $.ajax({
                            url: "/user/users/search-extvalue?searchType=" + searchType + "&searchString=" + request.term + "&serviceName=" + serviceName + "&searchReturn=" + searchReturn,
                            dataType: "json",
                            signal: signal,
                            data: {
                                q: request.term
                            },
                            success: function (data) {
                                console.debug("debug - " + "search user " + request.term);
                                response($.map(data, function (item) {
                                    return {
                                        label: item.text,
                                        value: item.value
                                    };
                                }));
                            }
                        });
                    }
                }
            });
            $input.data("esAutocompleteBound", true);
        });
    }

    annotationLinkTargetBlank() {
        $('.linkAnnotation').each(function (){
            const $linkAnnotation = $(this);
            $linkAnnotation.children().attr('target', '_blank');
            if ($linkAnnotation.data('esDroppableBound') === true) {
                return;
            }
            $linkAnnotation.droppable({
                tolerance: "touch",
                drop: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).addClass("cross-warning");
                    }
                },
                over: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).addClass("cross-warning");
                    }
                },
                out: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).removeClass("cross-warning");
                    }
                }
            });
            $linkAnnotation.data('esDroppableBound', true);
        });
    }

    annotationLinkRemove() {
        $('.linkAnnotation').each(function (){
            $(this).css("opacity", 0);
            $(this).click(function(e) {
                e.preventDefault();
            });
        });
    }

    async applyLinkAnnotationsVisibility() {
        if (!this.pdfDoc) {
            return;
        }
        const config = await Promise.resolve(this._optionalContentConfigPromise);
        if (!config) {
            return;
        }

        const visibleLayerNames = new Set();
        for (const [id, group] of config) {
            if (group?.visible) {
                visibleLayerNames.add(group.name);
            }
        }

        const tasks = [];
        for (let pageNum = 1; pageNum <= this.numPages; pageNum++) {
            tasks.push(this.applyLinkAnnotationsVisibilityForPage(pageNum, visibleLayerNames));
        }
        await Promise.all(tasks);
    }

    async applyLinkAnnotationsVisibilityForPage(pageNum, visibleLayerNames) {
        const pageContainer = document.getElementById(`page_${pageNum}`);
        if (!pageContainer) {
            return;
        }
        const annotationLayer = pageContainer.querySelector('.annotationLayer');
        if (!annotationLayer) {
            return;
        }

        const page = await this.pdfDoc.getPage(pageNum);
        const annotations = await page.getAnnotations();
        const annotationsById = new Map();
        annotations.forEach(annotation => {
            if (annotation?.id != null) {
                annotationsById.set(String(annotation.id), annotation);
            }
        });

        annotationLayer.querySelectorAll('section.linkAnnotation[data-annotation-id]').forEach(section => {
            const annotation = annotationsById.get(String(section.dataset.annotationId));
            if (!annotation) {
                section.style.removeProperty('display');
                return;
            }

            const layerIds = this.extractAnnotationLayerIds(annotation);
            if (!layerIds.length) {
                section.style.removeProperty('display');
                return;
            }

            const shouldDisplay = layerIds.some(layerId => visibleLayerNames.has(layerId));
            section.style.display = shouldDisplay ? '' : 'none';
        });
    }

    extractAnnotationLayerIds(annotation) {
        const layerIds = new Set();
        if (!annotation) {
            return [];
        }

        [annotation.annotationName, annotation.name, annotation.ocgName, annotation.layerName].forEach(value => {
            if (typeof value === 'string' && value.trim()) {
                layerIds.add(value.trim());
            }
        });

        [annotation.contents, annotation.contentsObj?.str, annotation.title].forEach(value => {
            if (typeof value !== 'string' || !value.trim()) {
                return;
            }
            try {
                const parsed = JSON.parse(value);
                if (parsed?.layer_id) {
                    layerIds.add(String(parsed.layer_id).trim());
                }
            } catch (e) {
                const match = value.match(/"layer_id"\s*:\s*"([^"]+)"/);
                if (match?.[1]) {
                    layerIds.add(match[1].trim());
                }
            }
        });

        return Array.from(layerIds);
    }

    isApplicationLayerName(layerName) {
        if (typeof layerName !== 'string' || !layerName.trim()) {
            return false;
        }
        return /^layer_\d+$/.test(layerName)
            || /^sign_\d+_.+/.test(layerName)
            || /^SignStep_\d+_.+/.test(layerName);
    }

    checkCurrentPage(e) {
        if(this.renderedPages < this.numPages) return;
        let numPages = this.pdfDoc.numPages;

        for(let i = 1; i < numPages + 1; i++) {
            let pagePos = this.getPageRelativeTop(i);

            if(e > pagePos - 250) {
                this.pageNum = i;
                document.getElementById('page_num').value = this.pageNum;
                if((this.pageNum === this.numPages || this.numPages === 1) && !this.viewed) {
                    this.viewed = true;
                    this.fireEvent("reachEnd", ['ok'])
                }
            }
        }
    }

    adjustZoom() {
        const workspaceDiv = document.getElementById('workspace');
        const workspaceWidth = workspaceDiv ? workspaceDiv.offsetWidth : window.innerWidth;
        let newScale = Math.round(workspaceWidth / 1000 * 10) / 10;
        console.info("adjust zoom to workspace width " + workspaceWidth);
        this.scale = newScale;
        console.info('zoom in, scale = ' + this.scale);
        this.fireEvent("scaleChange", ['in']);
    }

    async startRender(pdf) {
        if (pdf != null && this.pdfDoc == null) {
            this.pdfDoc = pdf;
        }
        if (this.pdfDoc == null) {
            return;
        }
        if (this.isRendering || this.activeRenders > 0 || this.renderQueue.length > 0) {
            this.pendingRender = true;
            this.pendingRenderPdf = pdf ?? this.pdfDoc;
            return;
        }

        this.isRendering = true;
        this.renderComplete = false;
        this.pendingRender = false;
        this.pendingRenderPdf = null;
        const currentRenderCycleId = ++this.renderCycleId;
        this.fireEvent("renderStarted", ['ok']);
        this.pdfDiv.css('opacity', 0);
        this.numPages = this.pdfDoc.numPages;
        document.getElementById('page_count').textContent = this.pdfDoc.numPages;
        this.renderedPages = 0;
        this.pages = [];
        this.renderQueue = [];
        this.activeRenders = 0;
        this.disableScrollBtn();
        this.resetProgress();
        $("#pdf-progress-bar").addClass("es-progress-visible");
        this.startProgress();

        try {
            const config = await this.pdfDoc.getOptionalContentConfig().catch(e => {
                console.error("Error getting optional content config: " + e);
                return null;
            });
            this._optionalContentConfigPromise = Promise.resolve(config);
        } catch (e) {
            console.error("Error getting optional content config: " + e);
            this._optionalContentConfigPromise = Promise.resolve(null);
        }

        for (let i = 1; i <= this.numPages; i++) {
            this.renderQueue.push(i);
        }
        this.processRenderQueue(currentRenderCycleId);

        this.refreshTools();
        this.fireEvent("ready", ['ok']);
    }

    processRenderQueue(renderCycleId = this.renderCycleId) {
        while (this.activeRenders < this.maxConcurrentRenders && this.renderQueue.length > 0) {
            const pageNum = this.renderQueue.shift();
            this.activeRenders++;

            let self = this;
            this.pdfDoc.getPage(pageNum).then(page => {
                return self.renderTask(page, pageNum, self._optionalContentConfigPromise.catch(e => {
                    console.error("Error in optionalContentConfigPromise: " + e);
                    return null;
                }), renderCycleId);
            }).then(async function() {
                self.activeRenders--;
                if (renderCycleId !== self.renderCycleId) {
                    if (self.activeRenders === 0 && self.pendingRender) {
                        self.isRendering = false;
                        const pendingPdf = self.pendingRenderPdf ?? self.pdfDoc;
                        self.pendingRender = false;
                        self.pendingRenderPdf = null;
                        await self.startRender(pendingPdf);
                    }
                    return;
                }
                self.renderedPages++;
                self.updateRenderProgress();

                if(self.renderQueue.length === 0 && self.activeRenders === 0) {
                    self.isRendering = false;
                    if (self.pendingRender) {
                        const pendingPdf = self.pendingRenderPdf ?? self.pdfDoc;
                        self.pendingRender = false;
                        self.pendingRenderPdf = null;
                        await self.startRender(pendingPdf);
                        return;
                    }
                    // Si c'est un refresh OCG, ne pas lancer postRenderAll
                    if (self._isRefreshingOCG) {
                        self._isRefreshingOCG = false;
                        self.applyLinkAnnotationsVisibility().catch(err => console.error('Erreur masquage liens OCG:', err));
                    } else {
                        if(self.pages.length === self.numPages) {
                            self.stopProgress();
                            const progressBar = $("#pdf-progress-bar");
                            progressBar.removeClass("es-progress-visible");
                            await self.postRenderAll();
                            self.enableScrollBtn();
                            self.fireEvent("renderFinished", ['ok']);
                            $(document).trigger("renderFinished");
                            self.fireRenderCompleteAfterProgressHidden(progressBar);
                        }
                    }
                } else {
                    self.processRenderQueue(renderCycleId);
                }
            })
                .catch(async err => {
                    if (renderCycleId !== self.renderCycleId) {
                        self.activeRenders--;
                        if (self.activeRenders === 0 && self.pendingRender) {
                            self.isRendering = false;
                            const pendingPdf = self.pendingRenderPdf ?? self.pdfDoc;
                            self.pendingRender = false;
                            self.pendingRenderPdf = null;
                            await self.startRender(pendingPdf);
                        }
                        return;
                    }
                    console.error(`Erreur rendu page ${pageNum}:`, err);
                    self.activeRenders--;
                    self.isRendering = false;
                    self._isRefreshingOCG = false;
                    if (self.pendingRender && self.activeRenders === 0) {
                        const pendingPdf = self.pendingRenderPdf ?? self.pdfDoc;
                        self.pendingRender = false;
                        self.pendingRenderPdf = null;
                        await self.startRender(pendingPdf);
                        return;
                    }
                    self.processRenderQueue(renderCycleId);
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
            this.renderComplete = true;
            this.fireEvent("renderComplete", ['ok']);
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
        // Keep the visual fade-out in sync with the CSS opacity transition (1000ms)
        window.setTimeout(complete, 1000);
    }

    scrollToPage(num) {
        let page = $("#page_" + num);
        if(page.length) {
            let scrollTo = this.getPageRelativeTop(num);
            this.animateScrollToPosition(scrollTo);
        }
    }

    enableScrollBtn() {
        $('#prev').prop('readonly', false);
        $('#next').prop('readonly', false);
        $('#page_num').prop('readonly', false);
    }

    disableScrollBtn() {
        $('#prev').prop('readonly', true);
        $('#next').prop('readonly', true);
        $('#page_num').prop('readonly', true);
    }

    refreshTools() {
        document.getElementById('page_num').value = this.pageNum;
        document.getElementById('zoom').textContent = Math.round(100 * this.scale);
        if(this.pdfDoc.numPages === 1) {
            if((this.pageNum === this.numPages || this.numPages === 1) && !this.viewed) {
                this.viewed = true;
                this.fireEvent("reachEnd", ['ok']);
            }
            this.disableScrollBtn();
        }
    }

    getPageRotation(page) {
        if (this.rotationOverride == null) {
            return page.rotate;
        }
        return this.rotationOverride;
    }

    async renderTask(page, i, configPromise, renderCycleId = this.renderCycleId) {
        return new Promise((resolve, reject) => {
            let container = document.getElementById(`page_${i}`);
            if (!container) {
                container = document.createElement("div");
                container.id = `page_${i}`;
                container.setAttribute("page-num", i);
                container.className = "drop-shadows pdf-page";
                container.style.marginBottom = `${10 * this.scale}px`;
                this.insertPageAtCorrectPosition(container, i);
            } else {
                container.innerHTML = "";
                container.style.marginBottom = `${10 * this.scale}px`;
            }
            $(container).droppable({
                drop: (event, ui) => ui.helper.attr("page", i)
            });

            const browserZoom = this.getBrowserZoom();
            const pageRotation = this.getPageRotation(page);

            const viewport = page.getViewport({
                scale: this.scale,
                rotation: pageRotation
            });

            const dispatchToDOM = false;
            const pdfPageView = new pdfjsViewer.PDFPageView({
                eventBus: this.eventBus,
                container: container,
                id: i,
                scale: this.scale,
                defaultViewport: viewport,
                useOnlyCssZoom: true,
                defaultZoomDelay: 0,
                textLayerMode: 1,
                annotationMode: pdfjsLib.AnnotationMode.ENABLE_FORMS,
                optionalContentConfigPromise: configPromise
            });

            pdfPageView.setPdfPage(page);

            pdfPageView.draw().then(() => {
                if (renderCycleId !== this.renderCycleId || this.pendingRender) {
                    resolve("obsolete");
                    return;
                }
                const container = document.getElementById(`page_${i}`);
                if (!container) {
                    if (renderCycleId !== this.renderCycleId || this.pendingRender) {
                        resolve("obsolete");
                        return;
                    }
                    reject(new Error("Container disparu"));
                    return;
                }
                const canvas = container.querySelector('canvas');
                if (!canvas) {
                    if (renderCycleId !== this.renderCycleId || this.pendingRender) {
                        resolve("obsolete");
                        return;
                    }
                    reject(new Error("Pas de canvas"));
                    return;
                }
                canvas.style.width = `${Math.floor(viewport.width)}px`;
                canvas.style.height = `${Math.floor(viewport.height)}px`;
                const canvasWrapper = container.querySelector('.canvasWrapper');
                if (canvasWrapper) {
                    canvasWrapper.style.width = `${Math.floor(viewport.width)}px`;
                    canvasWrapper.style.height = `${Math.floor(viewport.height)}px`;
                    canvasWrapper.style.padding = '0';
                    canvasWrapper.style.margin = '0';
                    canvasWrapper.style.overflow = 'hidden';
                }
                const pageDiv = container.querySelector('.page');
                if (pageDiv) {
                    pageDiv.style.width = `${Math.floor(viewport.width)}px`;
                    pageDiv.style.height = `${Math.floor(viewport.height)}px`;
                    pageDiv.style.padding = '0';
                    pageDiv.style.margin = '0';
                }
                container.style.width = `${Math.floor(viewport.width)}px`;
                container.style.height = `${Math.floor(viewport.height)}px`;
                container.style.overflow = 'hidden';
                const layerWidth = Math.floor(viewport.width);
                const layerHeight = Math.floor(viewport.height);
                const annotationLayer = container.querySelector('.annotationLayer');
                if (annotationLayer) {
                    annotationLayer.setAttribute("data-main-rotation", pageRotation);
                    annotationLayer.style.width = `${layerWidth}px`;
                    annotationLayer.style.height = `${layerHeight}px`;
                    annotationLayer.style.setProperty('--scale-factor', this.scale);
                    annotationLayer.style.setProperty('--total-scale-factor', this.scale);
                }
                const textLayer = container.querySelector('.textLayer');
                if (textLayer) {
                    textLayer.style.width = `${layerWidth}px`;
                    textLayer.style.height = `${layerHeight}px`;
                    textLayer.style.left = '0';
                    textLayer.style.top = '0';
                    textLayer.style.setProperty('--scale-factor', this.scale);
                    textLayer.style.setProperty('--total-scale-factor', this.scale);
                }
                this.pages[i - 1] = page;
                resolve("ok");
            }).catch(err => {
                if (renderCycleId !== this.renderCycleId || this.pendingRender) {
                    resolve("obsolete");
                    return;
                }
                console.error("Erreur dans pdfPageView.draw() page", i, ":", err);
                reject(err);
            });
        });
    }

    insertPageAtCorrectPosition(container, pageNum) {
        const pdfDivElement = this.pdfDiv[0];
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
        for(let i = 0; i < this.numPages; i++) {
            if (this.pages[i] != null) {
                renderTasks.push(this.postRender(this.pages[i]));
            }
        }
        await Promise.all(renderTasks);
        this.annotationLinkTargetBlank();
        await this.promiseRestoreValue();
        this.restoreScrolling();
        this.updateHorizontalOverflowState();
        await this.applyLinkAnnotationsVisibility().catch(err => console.error('Erreur postRenderAll liens OCG:', err));
    }

    updateHorizontalOverflowState() {
        const workspace = this.getWorkspaceElement();
        if (!workspace) {
            return;
        }
        workspace.style.overflowX = 'auto';
        const firstPage = document.getElementById('page_1');
        if (!firstPage) {
            return;
        }
        const hasHorizontalOverflow = firstPage.offsetWidth > workspace.clientWidth;
        // Bootstrap's justify-content-center uses !important; override with inline !important on overflow.
        if (hasHorizontalOverflow) {
            workspace.style.setProperty('justify-content', 'flex-start', 'important');
        } else {
            workspace.style.removeProperty('justify-content');
        }
        this.pdfDiv.css('align-items', hasHorizontalOverflow ? 'flex-start' : 'center');
        if (!hasHorizontalOverflow) {
            workspace.scrollLeft = 0;
        }
    }

    async postRender(page) {
        await this.promiseRenderForm(false, page);
        console.groupEnd();
    }

    promiseRenderForm(isField, page) {
        return page.getAnnotations().then(items => {
            this.renderPdfFormWithFields(items);
            return "Réussite";
        });
    }

    promiseToggleFields(enable) {
        if(this.pdfDoc != null) {
            for (let i = 1; i < this.pdfDoc.numPages + 1; i++) {
                this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.toggleItems(items, enable)));
            }
        }
    }

    toggleItems(items, enable) {
        console.info("toggle fields " + items.length);
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldName != null) {
                let inputField = $('input[name=\'' + items[i].fieldName.split(/\$|#|!/)[0] + '\'], textarea[name=\'' + items[i].fieldName.split(/\$|#|!/)[0] + '\']');
                inputField.prop("disabled", !enable);
            }
        }
    }

    async promiseSaveValues() {
        console.log("save");
        console.info("launch save values");
        const tasks = [];
        for (let i = 1; i < this.pdfDoc.numPages + 1; i++) {
            tasks.push(this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.saveValues(items))));
        }
        await Promise.all(tasks);
    }

    saveValues(items) {
        console.log("saving " + items.length + " fields");
        if(this.dataFields.length > 0) {
            for (let i = 0; i < this.dataFields.length; i++) {
                let dataField = this.dataFields[i];
                let item = items.filter(function (e) {
                    return e.fieldName != null && e.fieldName === dataField.name
                })[0];
                if (item != null && item.fieldName != null) {
                    this.saveValue(item);
                } else {
                    if(this.savedFields.get(dataField.name) == null) {
                        this.savedFields.set(dataField.name, dataField.defaultValue);
                    }
                }
            }
        } else {
            for (let i = 0; i < items.length; i++) {
                this.saveValue(items[i]);
            }
        }
    }

    saveValue(item) {
        if(item != null && item.fieldName != null) {
            let inputName = item.fieldName;
            let inputField = $("[name='" + $.escapeSelector(inputName) + "']");
            if (inputField.length > 0) {
                if (inputField.val() != null) {
                    if (inputField.is(':checkbox')) {
                        if (!inputField[0].checked) {
                            this.savedFields.set(item.fieldName, 'off');
                        } else {
                            this.savedFields.set(item.fieldName, 'on');
                        }
                        return;
                    }
                    if (inputField.is(':radio')) {
                        let radio = $('input[name=\'' + inputField.attr("name") + '\']');
                        let self = this;
                        radio.each(function() {
                            if ($(this).prop("checked")) {
                                self.savedFields.set(item.fieldName, $(this).val());
                            }
                        });
                        return;
                    }
                    if (inputField.is('select')) {
                        let value = inputField.val();
                        this.savedFields.set(item.fieldName, value);
                        return;
                    }
                    let value = inputField.val();
                    this.savedFields.set(item.fieldName, value);
                }
            }
        }
    }

    async promiseRestoreValue() {
        if(this.savedFields.size === 0) {
            await this.promiseSaveValues();
        }
        const tasks = [];
        for(let i = 1; i < this.pdfDoc.numPages + 1; i++) {
            tasks.push(this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.restoreValues(items))));
        }
        await Promise.all(tasks);
        this.fireEvent("render", ['end']);
    }

    restoreValues(items) {
        console.log("set fields " + items.length);
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldName != null) {
                let inputName = items[i].fieldName.split(/\$|#|!/)[0];
                let savedValue = this.savedFields.get(items[i].fieldName);
                let inputField = $('[name="' + inputName + '"]');
                if (inputField.val() != null) {
                    if(savedValue != null) {
                        if (inputField.is(':checkbox')) {
                            if (savedValue === 'on') {
                                inputField.prop("checked", true);
                            } else {
                                inputField.prop("checked", false);
                            }
                            continue;
                        }
                        if (inputField.is(':radio')) {
                            let radio = $('input[name=\'' + inputName + '\'][value=\'' + items[i].buttonValue + '\']');
                            if (savedValue === radio.val()) {
                                radio.prop("checked", true);
                            }
                            continue;
                        }
                        inputField.val(savedValue);
                        continue;
                    }
                }
                let textareaField = $('textarea[name=\'' + inputName + '\']');
                if (textareaField.val() != null) {
                    if (savedValue != null) {
                        textareaField.val(savedValue);
                        continue;
                    }
                }
                if (inputField.is('select')) {
                    $("#" + inputName + " option[value='" + savedValue + "']").prop('selected', true);
                    inputField.val(savedValue);
                    continue;
                }
                let selectField = $('select[name=\'' + inputName + '\']');
                if (selectField.val() != null) {
                    let savedFields = this.savedFields;
                    $('#' + inputName + ' option').each(function() {
                        let fieldName = items[i].fieldName;
                        let value = $(this).val();
                        if(savedFields.get(fieldName) === value) {
                            $(this).prop("selected", true);
                        }
                    });
                }
            }
        }
    }

    renderPdfFormWithFields(items) {
        let self = this;
        let datePickerIndex = 40;
        console.debug("debug - " + "rending pdfForm items");
        let signFieldNumber = 0;
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldType === undefined) {
                if(items[i].title && items[i].title.toLowerCase().includes('sign')) {
                    signFieldNumber = signFieldNumber + 1;
                    $('.popupWrapper').remove();
                }
                continue;
            }
            let inputName = items[i].fieldName.split(/\$|#|!/)[0];
            let dataField;
            if(this.dataFields != null && items[i].fieldName != null) {
                dataField = this.dataFields.filter(obj => {
                    return obj.name === inputName
                })[0];
            }
            let canvasField = $('section[data-annotation-id=' + items[i].id + '] > canvas');
            if (canvasField.length) {
                canvasField.remove();
            }
            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            if (inputField.length) {
                inputField.addClass("field-type-text");
                inputField.on('input', function (e) {
                    clearTimeout(self.timer);
                    self.timer = setTimeout(e => self.fireEvent("change", ['checked']), 500);
                });
                inputField.removeAttr("hidden");
                if (dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                if (this.disableAllFields) continue;
                let section = $('section[data-annotation-id=' + items[i].id + ']');
                inputField.attr('name', inputName);
                inputField.attr('placeholder', " ");
                inputField.removeAttr("maxlength");
                inputField.attr('id', inputName);
                inputField.attr('title', dataField.description);
                if (dataField.favorisable && !$("#div_" + inputField.attr('id')).length) {
                    let sendField = inputField;
                    $.ajax({
                        type: "GET",
                        url: '/ws-secure/ui/favorites/fields/' + dataField.id,
                        success: response => this.autocomplete(response, sendField)
                    });
                }
                if (dataField.editable) {
                    inputField.val(items[i].fieldValue);
                    if (dataField.defaultValue != null) {
                        inputField.val(dataField.defaultValue);
                    }
                    this.enableInputField(inputField, dataField)
                } else {
                    inputField.val(items[i].fieldValue);
                }

                if (dataField.searchServiceName) {
                    inputField.addClass("search-completion");
                    inputField.attr("search-completion-service-name", dataField.searchServiceName);
                    inputField.attr("search-completion-return", dataField.searchReturn);
                    inputField.attr("search-completion-type", dataField.searchType);
                }

                if (dataField.type === "number") {
                    inputField.get(0).type = "number";
                }

                if (dataField.type === "link") {
                    this.ensureLinkFieldStyles();
                    inputField.addClass("field-type-link pdf-link-input");

                    const currentValue = (items[i].fieldValue && items[i].fieldValue.length)
                        ? items[i].fieldValue
                        : (dataField.defaultValue || '');

                    const testerId = inputName + "_test_btn";
                    const disabledLinkId = inputName + "_link_btn";
                    section.css('overflow', 'visible');
                    section.find('#' + testerId + ', #' + disabledLinkId).remove();

                    if (this.isFieldEnable(dataField)) {
                        inputField.attr('type', 'url');
                        inputField.attr('inputmode', 'url');
                        inputField.attr('placeholder', 'https://exemple.org');
                        inputField.show();
                        if (currentValue) {
                            inputField.val(currentValue);
                        }

                        const $tester = $('<button type="button" id="' + testerId + '" class="pdf-link-test-btn" disabled>Tester</button>');
                        section.append($tester);

                        const getLiveInputField = () => {
                            const $liveInput = $('section[data-annotation-id=' + items[i].id + '] > input[name="' + inputName + '"]');
                            if ($liveInput.length) {
                                return $liveInput;
                            }
                            return $('section[data-annotation-id=' + items[i].id + '] > input').first();
                        };

                        const updateLinkState = () => {
                            const $liveInput = getLiveInputField();
                            const value = ($liveInput.val() || '').trim();
                            const element = $liveInput.get(0);
                            const normalizedValue = this.normalizeLinkValue(value);
                            const currentState = this.linkValidationStates.get(inputName);

                            if (!value) {
                                this.clearLinkReachabilityCheck(inputName);
                                this.linkValidationStates.set(inputName, {
                                    status: 'empty',
                                    value: '',
                                    normalizedValue: ''
                                });
                                $tester.prop('disabled', true);
                                $tester.data('href', '');
                                $liveInput.removeClass('pdf-link-invalid pdf-link-valid');
                                if (element && typeof element.setCustomValidity === 'function') {
                                    element.setCustomValidity('');
                                }
                                return;
                            }

                            if (!this.isValidLinkValue(value)) {
                                this.clearLinkReachabilityCheck(inputName);
                                this.linkValidationStates.set(inputName, {
                                    status: 'format-invalid',
                                    value: value,
                                    normalizedValue: ''
                                });
                                $liveInput.addClass('pdf-link-invalid');
                                $liveInput.removeClass('pdf-link-valid');
                                $tester.prop('disabled', true);
                                $tester.data('href', '');
                                if (element && typeof element.setCustomValidity === 'function') {
                                    element.setCustomValidity('URL invalide');
                                }
                                return;
                            }

                            if (currentState && currentState.normalizedValue === normalizedValue) {
                                if (currentState.status === 'reachable') {
                                    $tester.prop('disabled', false);
                                    $tester.data('href', currentState.normalizedValue || '');
                                    $liveInput.removeClass('pdf-link-invalid').addClass('pdf-link-valid');
                                    if (element && typeof element.setCustomValidity === 'function') {
                                        element.setCustomValidity('');
                                    }
                                    return;
                                }
                                if (currentState.status === 'checking') {
                                    $tester.prop('disabled', true);
                                    $tester.data('href', '');
                                    $liveInput.removeClass('pdf-link-invalid pdf-link-valid');
                                    if (element && typeof element.setCustomValidity === 'function') {
                                        element.setCustomValidity('Vérification du lien en cours');
                                    }
                                    return;
                                }
                                if (currentState.status === 'unreachable') {
                                    $tester.prop('disabled', true);
                                    $tester.data('href', '');
                                    $liveInput.addClass('pdf-link-invalid').removeClass('pdf-link-valid');
                                    if (element && typeof element.setCustomValidity === 'function') {
                                        element.setCustomValidity('Lien inaccessible');
                                    }
                                    return;
                                }
                            }

                            this.scheduleLinkReachabilityCheck(inputName, value, (state) => {
                                const $currentInput = getLiveInputField();
                                const currentElement = $currentInput.get(0);

                                if (state.status === 'checking') {
                                    $tester.prop('disabled', true);
                                    $tester.data('href', '');
                                    $currentInput.removeClass('pdf-link-invalid pdf-link-valid');
                                    if (currentElement && typeof currentElement.setCustomValidity === 'function') {
                                        currentElement.setCustomValidity('Vérification du lien en cours');
                                    }
                                    return;
                                }

                                if (state.status === 'reachable') {
                                    $tester.prop('disabled', false);
                                    $tester.data('href', state.normalizedValue || '');
                                    $currentInput.removeClass('pdf-link-invalid').addClass('pdf-link-valid');
                                    if (currentElement && typeof currentElement.setCustomValidity === 'function') {
                                        currentElement.setCustomValidity('');
                                    }
                                    return;
                                }

                                $tester.prop('disabled', true);
                                $tester.data('href', '');
                                $currentInput.addClass('pdf-link-invalid').removeClass('pdf-link-valid');
                                if (currentElement && typeof currentElement.setCustomValidity === 'function') {
                                    currentElement.setCustomValidity('Lien inaccessible');
                                }
                            });
                        };

                        section.off('.pdf_link_' + items[i].id);
                        section.on('input.pdf_link_' + items[i].id + ' keyup.pdf_link_' + items[i].id + ' change.pdf_link_' + items[i].id + ' blur.pdf_link_' + items[i].id, 'input[name="' + inputName + '"]', () => {
                            updateLinkState();
                        });

                        $tester.off('click.pdf_link');
                        $tester.off('mousedown.pdf_link');
                        $tester.on('mousedown.pdf_link', (e) => {
                            e.preventDefault();
                        });
                        $tester.on('click.pdf_link', (e) => {
                            e.preventDefault();
                            const href = $tester.data('href');
                            if (!href) {
                                return;
                            }
                            window.open(href, '_blank', 'noopener,noreferrer');
                        });

                        updateLinkState();
                    } else {
                        inputField.attr('type', 'url');
                        inputField.attr('inputmode', 'url');
                        inputField.show();
                        inputField.prop('readonly', true);
                        inputField.prop('disabled', false);
                        inputField.removeClass('disabled-field disable-selection');
                        inputField.parent().removeClass('disable-div-selection');
                        inputField.addClass('pdf-link-disabled-input');
                        const rawValue = (currentValue || '').trim();
                        const valid = this.isValidLinkValue(rawValue);
                        const href = valid ? this.normalizeLinkValue(rawValue) : '#';
                        const label = valid ? 'Ouvrir le lien' : 'Lien indisponible';
                        const $linkButton = $('<a id="' + disabledLinkId + '" class="pdf-link-display-btn" target="_blank" rel="noopener noreferrer"></a>');

                        $linkButton.attr('href', href);
                        $linkButton.text(label);

                        if (!valid) {
                            $linkButton.addClass('is-disabled');
                            $linkButton.on('click', (e) => e.preventDefault());
                        }

                        inputField.off('click.pdf_link_disabled keydown.pdf_link_disabled');
                        if (valid) {
                            inputField.attr('title', rawValue);
                            inputField.on('click.pdf_link_disabled', (e) => {
                                e.preventDefault();
                                window.open(href, '_blank', 'noopener,noreferrer');
                            });
                            inputField.on('keydown.pdf_link_disabled', (e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    window.open(href, '_blank', 'noopener,noreferrer');
                                }
                            });
                        }

                        section.append($linkButton);
                    }

                    if (this.isFieldEnable(dataField)) {
                        if (dataField.required) {
                            inputField.prop('required', true);
                            inputField.addClass('required-field');
                        } else {
                            inputField.prop('required', false);
                            inputField.removeClass('required-field');
                        }
                    } else {
                        inputField.prop('required', false);
                    }
                }

                if (dataField.type === "radio") {
                    inputField.addClass("field-type-radio");
                    if (this.isFieldEnable(dataField)) {
                        if (dataField.required) {
                            inputField.parent().addClass('required-field');
                        }
                    }
                    inputField.val(items[i].buttonValue);
                    inputField.attr("id", dataField.name + items[i].buttonValue);
                    if (dataField.defaultValue === items[i].buttonValue) {
                        inputField.attr("checked", "checked");
                        inputField.prop("checked", true);
                    }
                    inputField.unbind();
                    inputField.on('click', e => this.fireEvent("change", ['checked']));
                }
                if (dataField.type === 'checkbox') {
                    inputField.addClass("field-type-checkbox");
                    inputField.val('on');
                    if (dataField.defaultValue === 'on') {
                        inputField.attr("checked", "checked");
                        inputField.prop("checked", true);
                    }
                    inputField.unbind();
                    inputField.on('click', e => this.fireEvent("change", ['checked']));
                }

                if (dataField.type === "date") {
                    datePickerIndex--;
                    const inputElement = inputField[0];

                    const picker = new tempusDominus.TempusDominus(inputElement, {
                        localization: {
                            today: 'Aller à aujourd\'hui',
                            clear: 'Effacer la sélection',
                            close: 'Fermer le sélecteur',
                            selectMonth: 'Sélectionner le mois',
                            previousMonth: 'Mois précédent',
                            nextMonth: 'Mois suivant',
                            selectYear: 'Sélectionner l\'année',
                            previousYear: 'Année précédente',
                            nextYear: 'Année suivante',
                            selectDecade: 'Sélectionner la décennie',
                            previousDecade: 'Décennie précédente',
                            nextDecade: 'Décennie suivante',
                            previousCentury: 'Siècle précédent',
                            nextCentury: 'Siècle suivant',
                            pickHour: 'Choisir l\'heure',
                            incrementHour: 'Augmenter l\'heure',
                            decrementHour: 'Diminuer l\'heure',
                            pickMinute: 'Choisir les minutes',
                            incrementMinute: 'Augmenter les minutes',
                            decrementMinute: 'Diminuer les minutes',
                            pickSecond: 'Choisir les secondes',
                            incrementSecond: 'Augmenter les secondes',
                            decrementSecond: 'Diminuer les secondes',
                            toggleMeridiem: 'Basculer AM/PM',
                            selectTime: 'Sélectionner l\'heure',
                            selectDate: 'Sélectionner la date',
                            locale: 'fr',
                            startOfTheWeek: 1,
                            format: 'dd/MM/yyyy',
                            toggleAriaLabel: 'Modifier la date',
                        },
                        display: {
                            icons: {
                                time: 'fi fi-rr-clock',
                                date: 'fi fi-rr-calendar-day',
                                up: 'fi fi-rr-angle-small-up',
                                down: 'fi fi-rr-angle-small-down',
                                previous: 'fi fi-rr-angle-small-left',
                                next: 'fi fi-rr-angle-small-right',
                                today: 'fi fi-rr-calendar-check',
                                clear: 'fi fi-rr-empty-set',
                                close: 'fi fi-rr-check'
                            },
                            components: {
                                calendar: true,
                                date: true,
                                month: true,
                                year: true,
                                decades: false,
                                clock: false,
                                hours: false,
                                minutes: false,
                                seconds: false
                            },
                            toolbarPlacement: 'bottom',
                            buttons: {
                                today: true,
                                clear: true,
                                close: true
                            }
                        }
                    });

                    inputField.on("focus", function () {
                        section.css("z-index", datePickerIndex + 2000);
                    });
                    inputField.on("focusout", function () {
                        section.css("z-index", 4);
                    });

                    inputElement.addEventListener('change', (e) => {
                        this.fireEvent("change", ['date']);
                    });
                }

                if (dataField.type === "time") {
                    datePickerIndex--;
                    const inputElement = inputField[0];

                    const picker = new tempusDominus.TempusDominus(inputElement, {
                        localization: {
                            locale: 'fr',
                            format: 'HH:mm',
                        },
                        stepping: 5,
                        display: {
                            viewMode: 'clock',
                            icons: {
                                time: 'fi fi-rr-clock',
                                date: 'fi fi-rr-calendar-day',
                                up: 'fi fi-rr-angle-small-up',
                                down: 'fi fi-rr-angle-small-down',
                                previous: 'fi fi-rr-angle-small-left',
                                next: 'fi fi-rr-angle-small-right',
                                today: 'fi fi-rr-calendar-check',
                                clear: 'fi fi-rr-trash',
                                close: 'fi fi-rr-check'
                            },
                            components: {
                                calendar: false,
                                date: false,
                                month: false,
                                year: false,
                                decades: false,
                                clock: true,
                                hours: true,
                                minutes: true,
                                seconds: false
                            },
                            toolbarPlacement: 'bottom',
                            buttons: {
                                today: true,
                                clear: true,
                                close: true
                            }
                        }
                    });

                    inputField.on("focus", function () {
                        section.css("z-index", datePickerIndex + 2000);
                    });
                    inputField.on("focusout", function () {
                        section.css("z-index", datePickerIndex);
                    });

                    inputElement.addEventListener('change', (e) => {
                        this.fireEvent("change", ['time']);
                    });
                }
            }

            inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
            if (inputField.length) {
                inputField.addClass("field-type-textarea");
                inputField.on('input', function(e) {
                    clearTimeout(self.timer);
                    self.timer = setTimeout(e => self.fireEvent("change", ['checked']), 500);
                });
                inputField.removeAttr("hidden");
                if(dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                if(this.disableAllFields) continue;
                let sendField = inputField;
                if (dataField.favorisable) {
                    $.ajax({
                        type: "GET",
                        url: '/ws-secure/ui/favorites/fields/' + dataField.id,
                        success: response => this.autocomplete(response, sendField)
                    });
                }
                inputField.attr('name', inputName);
                inputField.attr('placeholder', " ");
                inputField.removeAttr("maxlength");
                inputField.attr('id', inputName);
                if (this.isFieldEnable(dataField)) {
                    if(dataField.defaultValue != null) {
                        inputField.val(dataField.defaultValue);
                    }
                    this.enableInputField(inputField, dataField)
                }
            }

            inputField = $('section[data-annotation-id=' + items[i].id + '] > select');
            if (inputField.length) {
                inputField.addClass("field-type-select");
                inputField.on('change', e => this.fireEvent("change", ['checked']));
                if(dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                inputField.removeAttr("hidden");
                if(this.disableAllFields) continue;
                inputField.removeAttr('size');
                inputField.attr('name', inputName);
                inputField.attr('id', inputName);
                if (dataField.editable) {
                    inputField.val(dataField.defaultValue);
                    this.enableInputField(inputField, dataField)
                }
            }
        }
        console.debug("debug - " + ">>End compute field");
        $(".annotationLayer").each(function() {
            $(this).removeClass("d-none");
        });
        this.listenToSearchCompletion();
    }

    ensureLinkFieldStyles() {
        if (document.getElementById('pdf-link-style')) {
            return;
        }
        const css = `
            .pdf-link-input {
                border-radius: 0 !important;
                padding-right: 72px !important;
            }
            .pdf-link-disabled-input {
                cursor: pointer;
                background: #fff !important;
                color: inherit !important;
            }
            .pdf-link-disabled-input:focus {
                outline: 1px solid #0d6efd;
                outline-offset: 0;
            }
            .pdf-link-test-btn,
            .pdf-link-display-btn {
                position: absolute;
                top: 0;
                right: 0;
                height: 100%;
                min-width: 68px;
                border: 1px solid #6c757d;
                background: #fff;
                color: #212529;
                padding: 0 10px;
                cursor: pointer;
                text-decoration: none;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                z-index: 2;
            }
            .pdf-link-test-btn[disabled],
            .pdf-link-display-btn.is-disabled {
                color: #6c757d;
                background: #f1f3f5;
                cursor: not-allowed;
                pointer-events: none;
            }
            .pdf-link-invalid {
                border-color: #c82333 !important;
            }
            .pdf-link-valid {
                border-color: #198754 !important;
            }
        `;
        const style = document.createElement('style');
        style.id = 'pdf-link-style';
        style.appendChild(document.createTextNode(css));
        document.head.appendChild(style);
    }

    normalizeLinkValue(value) {
        const trimmedValue = (value || '').trim();
        if (!trimmedValue) {
            return '';
        }
        if (/^[a-zA-Z][a-zA-Z0-9+\-.]*:/.test(trimmedValue)) {
            return trimmedValue;
        }
        return 'https://' + trimmedValue;
    }

    isValidLinkValue(value) {
        const normalizedValue = this.normalizeLinkValue(value);
        if (!normalizedValue) {
            return false;
        }
        try {
            const url = new URL(normalizedValue);
            if (!['http:', 'https:'].includes(url.protocol)) {
                return false;
            }
            if (!url.hostname || !url.hostname.includes('.')) {
                return false;
            }
            return true;
        } catch (e) {
            return false;
        }
    }

    clearLinkReachabilityCheck(fieldName) {
        const timer = this.linkValidationTimers.get(fieldName);
        if (timer) {
            clearTimeout(timer);
            this.linkValidationTimers.delete(fieldName);
        }
        const controller = this.linkValidationControllers.get(fieldName);
        if (controller) {
            controller.abort();
            this.linkValidationControllers.delete(fieldName);
        }
    }

    async checkLinkReachability(url, signal) {
        try {
            await fetch(url, {
                method: 'HEAD',
                mode: 'no-cors',
                cache: 'no-store',
                redirect: 'follow',
                signal: signal,
            });
            return true;
        } catch (headError) {
            if (signal?.aborted) {
                throw headError;
            }
            await fetch(url, {
                method: 'GET',
                mode: 'no-cors',
                cache: 'no-store',
                redirect: 'follow',
                signal: signal,
            });
            return true;
        }
    }

    scheduleLinkReachabilityCheck(fieldName, value, onStateChange) {
        const normalizedValue = this.normalizeLinkValue(value);
        if (!normalizedValue) {
            const emptyState = { status: 'empty', value: value, normalizedValue: '' };
            this.linkValidationStates.set(fieldName, emptyState);
            onStateChange(emptyState);
            return;
        }

        this.clearLinkReachabilityCheck(fieldName);
        const sequence = (this.linkValidationSeq.get(fieldName) || 0) + 1;
        this.linkValidationSeq.set(fieldName, sequence);

        const checkingState = {
            status: 'checking',
            value: value,
            normalizedValue: normalizedValue,
        };
        this.linkValidationStates.set(fieldName, checkingState);
        onStateChange(checkingState);

        const timer = setTimeout(async () => {
            const controller = new AbortController();
            this.linkValidationControllers.set(fieldName, controller);
            const timeoutId = setTimeout(() => controller.abort(), 4000);

            try {
                await this.checkLinkReachability(normalizedValue, controller.signal);
                if (this.linkValidationSeq.get(fieldName) !== sequence) {
                    return;
                }
                const reachableState = {
                    status: 'reachable',
                    value: value,
                    normalizedValue: normalizedValue,
                };
                this.linkValidationStates.set(fieldName, reachableState);
                onStateChange(reachableState);
            } catch (error) {
                if (this.linkValidationSeq.get(fieldName) !== sequence) {
                    return;
                }
                const unreachableState = {
                    status: 'unreachable',
                    value: value,
                    normalizedValue: normalizedValue,
                };
                this.linkValidationStates.set(fieldName, unreachableState);
                onStateChange(unreachableState);
            } finally {
                clearTimeout(timeoutId);
                this.linkValidationControllers.delete(fieldName);
            }
        }, 350);

        this.linkValidationTimers.set(fieldName, timer);
    }

    isFieldEnable(dataField) {
        return dataField.editable && !dataField.readOnly;
    }

    enableInputField(inputField, dataField) {
        if (!dataField.required) {
            inputField.prop('required', false);
            inputField.removeClass('required-field');
        } else {
            inputField.prop('required', true);
            inputField.addClass('required-field');
        }
        if (!dataField.readOnly) {
            inputField.prop('disabled', false);
            inputField.removeClass('disabled-field disable-selection');
        }
        inputField.attr('title', dataField.description);
    }

    disableInput(inputField, dataField, readOnly) {
        if (readOnly || dataField == null || dataField.readOnly || this.disableAllFields || !this.isFieldEnable(dataField)) {
            inputField.addClass('disabled-field disable-selection');
            inputField.prop('disabled', true);
            inputField.prop('required', false);
            inputField.parent().addClass('disable-div-selection');
        }
    }

    prevPage() {
        this.fireEvent("beforeChange", ['prev']);
        if (!this.isFirstPage()) {
            this.pageNum--;
        }
        this.scrollToPage(this.pageNum);
        return true;
    }

    nextPage() {
        if (this.isLastPage()) {
            return false;
        }
        this.pageNum++;
        this.scrollToPage(this.pageNum);
        return true;
    }

    isFirstPage() {
        return this.pageNum <= 1;
    }

    isLastPage() {
        return this.pageNum >= this.numPages;
    }

    zoomInit(e) {
        this.scale = 1.2;
        console.info('zoom in, scale = ' + this.scale);
        this.fireEvent("scaleChange", ['in']);
    }

    zoomIn(e) {
        const workspaceDiv = document.getElementById('workspace');
        const workspaceWidth = workspaceDiv ? workspaceDiv.offsetWidth : window.innerWidth;
        const baseLimit = Math.round(workspaceWidth / 600 * 10) / 10 - 0.1;
        // On small screens, allow controlled overflow so text stays readable.
        const overflowBonus = workspaceWidth < 1200
            ? ((1200 - workspaceWidth) / 1200) * 1.2
            : 0;
        const maxZoomLimit = Math.min(3.2, Math.max(baseLimit + overflowBonus, 1.8));
        if (this.scale >= maxZoomLimit) {
            return;
        }
        this.saveScrolling = Math.round(this.getScrollTop() / this.scale);
        this.scale = Math.round((this.scale + this.zoomStep) * 1000) / 1000;
        console.info('zoom in, scale = ' + this.scale);
        this.fireEvent("scaleChange", ['in']);
    }

    zoomOut(e) {
        const workspaceDiv = document.getElementById('workspace');
        const workspaceWidth = workspaceDiv ? workspaceDiv.offsetWidth : window.innerWidth;
        // On small screens, keep a higher minimum zoom to preserve readability.
        const smallScreenPenalty = workspaceWidth < 1200
            ? ((1200 - workspaceWidth) / 1200) * 0.5
            : 0;
        const minZoomLimit = Math.min(0.9, Math.max(0.2 + smallScreenPenalty, 0.2));
        if (this.scale <= minZoomLimit) {
            return;
        }
        this.scale = Math.max(minZoomLimit, Math.round((this.scale - this.zoomStep) * 1000) / 1000);
        console.info('zoom out, scale = ' + this.scale);
        this.fireEvent("scaleChange", ['out']);
    }

    fullWidth() {
        const workspaceDiv = document.getElementById('workspace');
        const workspaceWidth = workspaceDiv ? workspaceDiv.offsetWidth : window.innerWidth;
        let newScale = Math.round(workspaceWidth / 600 * 10) / 10 - .1;
        console.info("full width " + newScale);
        if (newScale !== this.scale) {
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.fireEvent("scaleChange", ['in']);
        }
    }

    fullHeight() {
        console.info("full height " + window.innerHeight);
        let newScale = (Math.round((window.innerHeight - 200) / 100) / 10) - 0.1;
        if (newScale !== this.scale) {
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.fireEvent("scaleChange", ['in']);
        }
    }

    autoRotate() {
        console.info('rotate left');
        this.rotationOverride = this.rotationOverride === 0 ? null : 0;
        let autorotatebtn = $("#autoRotate");
        autorotatebtn.toggleClass("btn-light btn-dark");
        autorotatebtn.toggleClass("btn-outline-dark border-dark");
        autorotatebtn.children().toggleClass("fi-rr-navigation fi-rr-compass-north")
        this.startRender(this.pdfDoc)
        this.fireEvent("rotate", ['left']);
    }

    autocomplete(response, inputField) {
        let id = inputField.attr('id');
        let div = "<div class='custom-autocompletion' id='div_" + id +"'></div>";
        $(div).insertAfter(inputField);
        inputField.autocomplete({
            delay: 500,
            source: response,
            appendTo: "#div_" + id,
            minLength:0
        }).bind('focus', function(){ $(this).autocomplete("search"); } );
    }

    checkForm() {
        return new Promise((resolve, reject) => {
            let formData = new Map();
            console.info("check data name");
            let self = this;
            let resolveOk = "ok";
            let warningFields = [];
            $(self.dataFields).each(function (e, item) {
                let savedField = self.savedFields.get(item.name)
                formData[item.name] = savedField;
                item.validationError = null;
                const isMissingRequiredValue = item.required && self.isFieldEnable(item) &&
                    (!savedField || (savedField === "off" && item.type === "checkbox"));
                const isInvalidLinkValue = item.type === "link" && self.isFieldEnable(item)
                    && !!savedField && !self.isValidLinkValue(savedField);
                const linkValidationState = item.type === "link"
                    ? self.linkValidationStates.get(item.name)
                    : null;
                const isUnverifiedOrUnreachableLink = item.type === "link" && self.isFieldEnable(item)
                    && !!savedField
                    && self.isValidLinkValue(savedField)
                    && (!linkValidationState || !['reachable'].includes(linkValidationState.status));

                if (isMissingRequiredValue || isInvalidLinkValue || isUnverifiedOrUnreachableLink) {
                    if (isInvalidLinkValue) {
                        item.validationError = 'invalid_link';
                    } else if (isUnverifiedOrUnreachableLink) {
                        item.validationError = linkValidationState?.status === 'checking'
                            ? 'checking_link'
                            : 'unreachable_link';
                    } else {
                        item.validationError = 'required';
                    }
                    let addWarning = true;
                    for(let i = 0; i < warningFields.length; i++) {
                        if(warningFields[i].name === item.name) {
                            addWarning = false;
                        }
                    }
                    if(addWarning) {
                        warningFields.push($(this)[0]);
                    }
                }
            });
            if (warningFields.length > 0) {
                warningFields.sort((a, b) => a.compareByPage(b))
                let text = "Certain champs requis n'ont pas été remplis dans ce formulaire<ul>";
                if (warningFields.length < 2 && warningFields[0].name != null) {
                    const fieldLabel = warningFields[0].description != null && warningFields[0].description !== ""
                        ? warningFields[0].description
                        : warningFields[0].name;
                    if (warningFields[0].validationError === 'invalid_link') {
                        text = "Le champ " + fieldLabel + " contient une URL invalide";
                    } else if (warningFields[0].validationError === 'checking_link') {
                        text = "Le champ " + fieldLabel + " est encore en cours de vérification";
                    } else if (warningFields[0].validationError === 'unreachable_link') {
                        text = "Le champ " + fieldLabel + " contient un lien inaccessible";
                    } else {
                        text = "Le champ " + fieldLabel + " n'est pas rempli";
                    }
                    if (warningFields[0].page != null) {
                        text += " en page " + warningFields[0].page;
                    }
                } else {
                    warningFields.forEach(function (field) {
                        let suffix = '';
                        if (field.validationError === 'invalid_link') {
                            suffix = ' : URL invalide';
                        } else if (field.validationError === 'checking_link') {
                            suffix = ' : vérification en cours';
                        } else if (field.validationError === 'unreachable_link') {
                            suffix = ' : lien inaccessible';
                        }
                        if (field.description != null && field.description !== "") {
                            text += "<li>" + field.description + suffix;
                            if(field.page != null) {
                                text += " (en page " + (field.page + 1) + ")";
                            }
                            text +="</li>";
                        } else {
                            text += "<li>" + field.name + suffix;
                            if(field.page != null) {
                                text += " (en page " + (field.page + 1) + ")";
                            }
                            text +="</li>";
                        }
                    });
                }
                text += "</ul>"
                bootbox.alert(text, function () {
                    let field = $('#' + warningFields[0].name);
                    setTimeout(function () {
                        self.focusField(field)
                    }, 100);
                });
                resolveOk = $(this)[0].name;
                $('#sendModal').modal('hide');
            }
            resolve(resolveOk);
        });
    }

    focusField(field) {
        if(field.attr("type") === "radio") {
            this.highlightRadio(field);
        }
        field.focus();
        let offset = field.offset();
        if(offset != null) {
            const workspace = this.getWorkspaceElement();
            if (workspace) {
                const workspaceOffset = $(workspace).offset();
                if (workspaceOffset != null) {
                    $(workspace).animate({
                        scrollTop: offset.top - workspaceOffset.top + workspace.scrollTop - 170,
                        scrollLeft: 0
                    });
                    return;
                }
            }
            $('html, body').animate({
                scrollTop: offset.top - 170,
                scrollLeft: offset.left
            });
        }
    }

    highlightRadio(field) {
        $("[name='" + field.attr('name') + "']").each(function() {
            let radio = $(this);
            let i = 0;
            let flashInterval = setInterval(
                function() {
                    radio.toggleClass('highlight');
                    if(i > 4) {
                        clearInterval(flashInterval);
                        radio.removeClass('highlight');
                    }
                    i++;
                },
                1000
            );
        });
    }

    startProgress() {
        this.updateProgress(5, "Préparation du rendu…", true);
    }

    stopProgress(){
        this.updateProgress(100, "Chargement terminé", false);
        clearInterval(this.interval);
    }

    resetProgress() {
        $("#pdf-progress-bar").removeClass("es-progress-visible");
        this.updateProgress(0, "", false);
        clearInterval(this.interval);
    }

    updateRenderProgress() {
        const progress = this.numPages > 0 ? Math.round(this.renderedPages / this.numPages * 100) : 0;
        this.updateProgress(progress, "Chargement de la page " + this.renderedPages + "/" + this.numPages, this.renderedPages < this.numPages);
    }

    updateProgress(progress, text, animated) {
        $("#pdf-progress-bar .progress-bar")
            .toggleClass("progress-bar-striped progress-bar-animated", animated)
            .css("width", progress + "%")
            .attr("aria-valuenow", progress)
            .text(text);
    }

    getBrowserZoom() {
        return window.devicePixelRatio || 1;
    }

    getApplicationLayers(config) {
        const layers = [];
        if (!config) {
            return layers;
        }
        for (const [id, group] of config) {
            if (group?.name && this.isApplicationLayerName(group.name)) {
                layers.push({ id, name: group.name, visible: group.visible });
            }
        }
        return layers;
    }

    resolveLayerName(stepNumber, requestedLayerName, layers) {
        if (requestedLayerName) {
            const exactMatch = layers.find(group => group.name === requestedLayerName);
            if (exactMatch) {
                return exactMatch.name;
            }
            const requestedLayerId = Number.parseInt(String(requestedLayerName).replace('layer_', ''), 10);
            if (!Number.isNaN(requestedLayerId)) {
                const compatibleMatch = layers.find(group => {
                    const liveWorkflowStepId = this.extractStableLayerStepId(group.name);
                    return liveWorkflowStepId === requestedLayerId;
                });
                if (compatibleMatch) {
                    return compatibleMatch.name;
                }
            }

            return null;
        }
        return layers[stepNumber - 1]?.name || null;
    }

    extractStableLayerStepId(layerName) {
        if (typeof layerName !== 'string') {
            return null;
        }
        let match = layerName.match(/^layer_(\d+)$/);
        if (match) {
            return Number.parseInt(match[1], 10);
        }
        match = layerName.match(/^sign_(\d+)_/);
        if (match) {
            return Number.parseInt(match[1], 10);
        }
        return null;
    }

    async showLayerByStep(stepNumber, solo, layerName = null) {
        if (!this.pdfDoc) {
            return;
        }
        try {
            const config = await this.pdfDoc.getOptionalContentConfig().catch(e => {
                console.error("Error getting optional content config: " + e);
                return null;
            });
            if (!config) {
                return;
            }
            const allGroups = this.getApplicationLayers(config);
            const resolvedLayerName = this.resolveLayerName(stepNumber, layerName, allGroups);
            if (!resolvedLayerName && stepNumber !== 0) {
                console.warn(`showLayerByStep: layer introuvable pour step=${stepNumber}, layerName=${layerName}`);
                return;
            }
            if(solo) {
                allGroups.forEach((group) => {
                    const shouldBeVisible = group.name === resolvedLayerName;
                    config.setVisibility(group.id, shouldBeVisible);
                });
            } else {
                allGroups.forEach((group) => {
                    const selectedGroupIndex = allGroups.findIndex(candidate => candidate.name === resolvedLayerName);
                    const currentGroupIndex = allGroups.findIndex(candidate => candidate.name === group.name);
                    const shouldBeVisible = stepNumber === 0 ? false : (selectedGroupIndex >= 0 && currentGroupIndex <= selectedGroupIndex);
                    config.setVisibility(group.id, shouldBeVisible);
                });
            }
            this.optionalContentConfigPromise = Promise.resolve(config);
            this._activeLayerView = { stepNumber, solo, layerId: resolvedLayerName };
            this.updateLayerButtonsState();
        } catch(err) {
            console.error('Erreur showLayerByStep:', err);
        }
    }

    updateLayerButtonsState() {
        // Reset all button "on" states
        const onClasses = ['active', 'bg-primary-subtle', 'text-primary', 'border', 'border-primary-subtle'];
        $('.display-layer-btn, .toggle-layer-btn').each(function () {
            onClasses.forEach(c => $(this).removeClass(c));
        });

        if (!this._activeLayerView) {
            return;
        }

        const selector = this._activeLayerView.solo
            ? `.toggle-layer-btn[data-layer-id="${this._activeLayerView.layerId}"]`
            : `.display-layer-btn[data-layer-id="${this._activeLayerView.layerId}"]`;

        const $btn = $(selector);
        if ($btn.length) {
            onClasses.forEach(c => $btn.addClass(c));
        }
    }

    async showAllLayers() {
        if (!this.pdfDoc) {
            return;
        }
        try {
            const config = await this.pdfDoc.getOptionalContentConfig().catch(e => {
                console.error("Error getting optional content config: " + e);
                return null;
            });
            if (!config) {
                return;
            }
            for (const group of this.getApplicationLayers(config)) {
                config.setVisibility(group.id, true);
            }
            this.optionalContentConfigPromise = Promise.resolve(config);
            this._activeLayerView = null;
            this.updateLayerButtonsState();
        } catch(err) {
            console.error('Erreur showAllLayers:', err);
        }
    }

    async toggleLayerByStep(stepNumber, solo, layerId = null) {
        // Si on reclique sur le même stepNumber déjà actif (même mode), on repasse sur tous les calques.
        if (this._activeLayerView
            && this._activeLayerView.solo === solo
            && this._activeLayerView.layerId === (layerId || this._activeLayerView.layerId)) {
            await this.showAllLayers();
            return;
        }
        await this.showLayerByStep(stepNumber, solo, layerId);
    }

    async highlightStep(stepNumber, layerId = null) {
        if (!this.highlighter) {
            console.error('highlightStep: LayerHighlighter non initialisé');
            return;
        }

        try {
            const config = await Promise.resolve(this._optionalContentConfigPromise).catch(e => {
                console.error("Error resolving optionalContentConfigPromise: " + e);
                return null;
            });
            if (!config) {
                console.warn('highlightStep: Aucun calque disponible');
                return;
            }
            const allGroups = this.getApplicationLayers(config);

            const resolvedLayerName = this.resolveLayerName(stepNumber, layerId, allGroups);
            const targetGroup = resolvedLayerName
                ? allGroups.find(group => group.name === resolvedLayerName)
                : null;
            if (!targetGroup) {
                console.warn(`highlightStep: layer introuvable pour step=${stepNumber}, layerId=${layerId}`);
                return;
            }
            this.highlighter.clearHighlights();
            await this.highlighter.highlightLayer(targetGroup.id);
            console.log(`highlightStep(${stepNumber}): Calque "${targetGroup.name}" en ${this.highlighter.highlightColor}`);
        } catch(err) {
            console.error('highlightStep error:', err);
        }
    }

    clearHighlight() {
        if (this.highlighter) {
            this.highlighter.clearHighlights();
            console.log('Highlight effacé');
        }
    }
}
