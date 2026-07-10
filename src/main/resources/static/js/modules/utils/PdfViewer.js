import {EventBus} from "../../customs/ui_utils.js?version=@version@";
import {EventFactory} from "./EventFactory.js?version=@version@";
import {DataField} from "../../prototypes/DataField.js?version=@version@";
import {LayerHighlighter} from "./pdf/LayerHighlighter.js?version=@version@";
import {PdfHandPanController} from "./pdf/PdfHandPanController.js?version=@version@";
import {PdfLayerController} from "./pdf/PdfLayerController.js?version=@version@";
import {PdfLinkFieldValidator} from "./pdf/PdfLinkFieldValidator.js?version=@version@";
import {PdfFormManager} from "./pdf/PdfFormManager.js?version=@version@";
import {PdfNavigationController} from "./pdf/PdfNavigationController.js?version=@version@";
import {PdfProgressController} from "./pdf/PdfProgressController.js?version=@version@";
import {PdfRendererController} from "./pdf/PdfRendererController.js?version=@version@";

export class PdfViewer extends EventFactory {

    constructor(url, signable, editable, currentStepNumber, forcePageNum, fields, disableAllFields, options = {}) {
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
        this.maxConcurrentRenders = Number.isFinite(options.maxConcurrentRenders)
            ? Math.max(1, Math.floor(options.maxConcurrentRenders))
            : 2;
        this.maxRenderScale = Number.isFinite(options.maxRenderScale) ? options.maxRenderScale : 2;
        this.renderScaleBuffer = Number.isFinite(options.renderScaleBuffer) ? options.renderScaleBuffer : 0.25;
        this.renderBufferPages = Number.isFinite(options.renderBufferPages) ? Math.max(0, Math.floor(options.renderBufferPages)) : 2;
        this.maxRenderedPages = Number.isFinite(options.maxRenderedPages) ? Math.max(1, Math.floor(options.maxRenderedPages)) : 12;
        this.renderCycleId = 0;
        this.isRendering = false;
        this.pendingRender = false;
        this.pendingRenderPdf = null;
        this.renderComplete = false;
        this.renderFailed = false;
        this.loadStarted = false;
        this.loadPromise = null;
        this.renderedScale = this.scale;
        this.renderScale = this.scale;
        this.renderFinishedFired = false;
        this.renderedPageNums = new Set();
        this.renderingPageNums = new Set();
        this.queuedPageNums = new Set();
        this.textLayerPageNums = new Set();
        this.textLayerRenderingPageNums = new Set();
        this.textLayerRenderPromise = null;
        this.pageViewports = new Map();
        this.handPanState = null;
        this.handPanEnabled = options.handPanEnabled === true;
        this.renderedPagesMap = new Map();
        this.displayedPagesMap = new Map();
        this.lastWidth = window.innerWidth;
        this.lastHeight = window.innerHeight;
        this.currentOptionalContentConfig = null;
        this._activeLayerView = null; // { stepNumber: number, solo: boolean } | null
        this.navigationController = new PdfNavigationController(this);
        this.handPanController = new PdfHandPanController(this);
        this.layerController = new PdfLayerController(this);
        this.linkFieldValidator = new PdfLinkFieldValidator(this);
        this.formManager = new PdfFormManager(this);
        this.progressController = new PdfProgressController(this);
        this.rendererController = new PdfRendererController(this);
        this.initListeners();
        if (options.autoStart !== false) {
            this.loadDocumentWhenReady();
        }
    }

    whenDocumentReady() {
        if (document.readyState === "loading") {
            return new Promise(resolve => $(document).ready(resolve));
        }
        return Promise.resolve();
    }

    loadDocumentWhenReady() {
        if (this.loadStarted) {
            return this.loadPromise;
        }
        this.loadStarted = true;
        this.loadPromise = this.whenDocumentReady().then(() => this.loadDocument());
        return this.loadPromise;
    }

    async loadDocument() {
        try {
            if (!globalThis.pdfjsLib || !Promise.withResolvers) {
                const message = "Votre navigateur ne supporte pas pdfJs pour l'affichage des PDF.";
                const error = new Error(message);
                this.failRender(error, message);
                bootbox.alert(message + "<br>Versions minimales : Firefox 121, Chrome 119, Safari 17.4", function () {
                    document.location = "https://www.mozilla.org/fr/firefox/new/";
                });
                return null;
            }
            if (!this.url) {
                throw new Error("PdfViewer: url est vide");
            }
            if (globalThis.pdfjsLib.GlobalWorkerOptions) {
                globalThis.pdfjsLib.GlobalWorkerOptions.workerSrc = '/webjars/pdfjs-dist/legacy/build/pdf.worker.min.mjs';
            }
            $("#pdf-progress-bar").addClass("es-progress-visible");
            this.startProgress();
            const loadingTask = globalThis.pdfjsLib.getDocument({
                verbosity: 0,
                url: this.url,
                useWasm: true,
                wasmUrl: `/webjars/pdfjs-dist/wasm/`
            });
            const pdf = await loadingTask.promise;
            await this.startRender(pdf);
            return pdf;
        } catch (error) {
            this.failRender(error, "Impossible de charger le document PDF.");
            return null;
        }
    }

    failRender(error, message = "Impossible d’afficher le document PDF.") {
        if (this.renderFailed) {
            return;
        }
        this.renderFailed = true;
        this.isRendering = false;
        this.pendingRender = false;
        this.pendingRenderPdf = null;
        this.renderQueue = [];
        this.activeRenders = 0;
        this.renderCycleId++;
        this.renderComplete = false;
        this.pdfDiv.css('opacity', 1);
        console.error(message, error);
        this.progressController.failProgress(message);
        this.fireEvent("renderFailed", [error]);
        $(document).trigger("renderFailed", [error]);
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
        const scrollTarget = this.getWorkspaceElement() || window;
        $(scrollTarget).on('scroll.pdfViewerLazyRender', () => {
            if (!this.pdfDoc || this.renderFailed) {
                return;
            }
            clearTimeout(this.lazyRenderTimer);
            this.lazyRenderTimer = setTimeout(() => {
                this.rendererController.queueVisiblePages();
                this.rendererController.releaseDistantPages();
            }, 80);
        });
        $('#page_num').on('change', e => this.scrollToPage(e.target.value));
        if(localStorage.getItem("scale")) {
            this.scale = parseFloat(localStorage.getItem("scale"));
        } else {
            this.adjustZoom();
        }
        this.initHandPan();
    }

    getWorkspaceElement() {
        return this.navigationController.getWorkspaceElement();
    }

    getScrollTop() {
        return this.navigationController.getScrollTop();
    }

    getViewportHeight() {
        return this.navigationController.getViewportHeight();
    }

    getScrollLeft() {
        return this.navigationController.getScrollLeft();
    }

    scrollToPosition(top, behavior = 'auto') {
        return this.navigationController.scrollToPosition(top, behavior);
    }

    animateScrollToPosition(top) {
        return this.navigationController.animateScrollToPosition(top);
    }

    initHandPan() {
        return this.handPanController.initHandPan();
    }

    setHandPanEnabled(enabled) {
        this.handPanEnabled = enabled === true;
        if (this.handPanEnabled) {
            this.initHandPan();
        } else {
            this.handPanController.stopHandPan?.();
        }
    }

    ensureHandPanStyles() {
        return this.handPanController.ensureHandPanStyles();
    }

    ensureHandPanOverlay() {
        return this.handPanController.ensureHandPanOverlay();
    }

    canStartHandPan(event) {
        return this.handPanController.canStartHandPan(event);
    }

    getPageRelativeTop(pageNum) {
        return this.navigationController.getPageRelativeTop(pageNum);
    }

    getPageTopInPdf(pageNum) {
        return this.navigationController.getPageTopInPdf(pageNum);
    }

    getPageLeftInPdf(pageNum) {
        return this.navigationController.getPageLeftInPdf(pageNum);
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

    applyScaleWithoutRerender() {
        if (!Number.isFinite(this.renderedScale) || this.renderedScale <= 0) {
            return false;
        }
        const scaleRatio = this.scale / this.renderedScale;
        if (!Number.isFinite(scaleRatio) || scaleRatio <= 0) {
            return false;
        }

        for (let i = 1; i <= this.numPages; i++) {
            const container = document.getElementById(`page_${i}`);
            if (!container) {
                continue;
            }
            const renderedWidth = Number.parseFloat(container.dataset.renderedWidth || '0');
            const renderedHeight = Number.parseFloat(container.dataset.renderedHeight || '0');
            if (!Number.isFinite(renderedWidth) || !Number.isFinite(renderedHeight) || renderedWidth <= 0 || renderedHeight <= 0) {
                continue;
            }

            container.style.width = `${Math.floor(renderedWidth * scaleRatio)}px`;
            container.style.height = `${Math.floor(renderedHeight * scaleRatio)}px`;
            container.style.marginBottom = `${10 * this.scale}px`;

            const pageDiv = container.querySelector('.page');
            if (pageDiv) {
                pageDiv.style.transformOrigin = 'top left';
                pageDiv.style.transform = scaleRatio === 1 ? '' : `scale(${scaleRatio})`;
            }
        }

        this.refreshTools();
        this.restoreScrolling();
        this.updateHorizontalOverflowState();
        return true;
    }

    getVisiblePages() {
        return this.navigationController.getVisiblePages();
    }

    restoreScrolling() {
        return this.navigationController.restoreScrolling();
    }

    listenToSearchCompletion() {
        return this.formManager.listenToSearchCompletion();
    }

    annotationLinkTargetBlank() {
        return this.formManager.annotationLinkTargetBlank();
    }

    annotationLinkRemove() {
        return this.formManager.annotationLinkRemove();
    }

    async applyLinkAnnotationsVisibility() {
        return this.layerController.applyLinkAnnotationsVisibility();
    }

    async applyLinkAnnotationsVisibilityForPage(pageNum, visibleLayerNames) {
        return this.layerController.applyLinkAnnotationsVisibilityForPage(pageNum, visibleLayerNames);
    }

    extractAnnotationLayerIds(annotation) {
        return this.layerController.extractAnnotationLayerIds(annotation);
    }

    isApplicationLayerName(layerName) {
        return this.layerController.isApplicationLayerName(layerName);
    }

    checkCurrentPage(e) {
        return this.navigationController.checkCurrentPage(e);
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

    getMaxZoomLimit() {
        const workspaceDiv = document.getElementById('workspace');
        const workspaceWidth = workspaceDiv ? workspaceDiv.offsetWidth : window.innerWidth;
        const baseLimit = Math.round(workspaceWidth / 600 * 10) / 10 - 0.1;
        // On small screens, allow controlled overflow so text stays readable.
        const overflowBonus = workspaceWidth < 1200
            ? ((1200 - workspaceWidth) / 1200) * 1.2
            : 0;
        return Math.min(3.2, Math.max(baseLimit + overflowBonus, 1.8));
    }

    async startRender(pdf) {
        return this.rendererController.startRender(pdf);
    }

    processRenderQueue(renderCycleId = this.renderCycleId) {
        return this.rendererController.processRenderQueue(renderCycleId);
    }

    fireRenderCompleteAfterProgressHidden(progressBar) {
        return this.rendererController.fireRenderCompleteAfterProgressHidden(progressBar);
    }

    scrollToPage(num) {
        return this.navigationController.scrollToPage(num);
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
        return this.rendererController.renderTask(page, i, configPromise, renderCycleId);
    }

    insertPageAtCorrectPosition(container, pageNum) {
        return this.rendererController.insertPageAtCorrectPosition(container, pageNum);
    }

    async postRenderAll() {
        return this.rendererController.postRenderAll();
    }

    updateHorizontalOverflowState() {
        return this.rendererController.updateHorizontalOverflowState();
    }

    async postRender(page) {
        return this.rendererController.postRender(page);
    }

    promiseRenderForm(isField, page) {
        return this.formManager.promiseRenderForm(isField, page);
    }

    promiseToggleFields(enable) {
        return this.formManager.promiseToggleFields(enable);
    }

    toggleItems(items, enable) {
        return this.formManager.toggleItems(items, enable);
    }

    async promiseSaveValues() {
        return this.formManager.promiseSaveValues();
    }

    saveValues(items) {
        return this.formManager.saveValues(items);
    }

    saveValue(item) {
        return this.formManager.saveValue(item);
    }

    async promiseRestoreValue() {
        return this.formManager.promiseRestoreValue();
    }

    restoreValues(items) {
        return this.formManager.restoreValues(items);
    }

    renderPdfFormWithFields(items) {
        return this.formManager.renderPdfFormWithFields(items);
    }

    ensureLinkFieldStyles() {
        return this.linkFieldValidator.ensureLinkFieldStyles();
    }

    normalizeLinkValue(value) {
        return this.linkFieldValidator.normalizeLinkValue(value);
    }

    isValidLinkValue(value) {
        return this.linkFieldValidator.isValidLinkValue(value);
    }

    clearLinkReachabilityCheck(fieldName) {
        return this.linkFieldValidator.clearLinkReachabilityCheck(fieldName);
    }

    async checkLinkReachability(url, signal) {
        return this.linkFieldValidator.checkLinkReachability(url, signal);
    }

    scheduleLinkReachabilityCheck(fieldName, value, onStateChange) {
        return this.linkFieldValidator.scheduleLinkReachabilityCheck(fieldName, value, onStateChange);
    }

    isFieldEnable(dataField) {
        return this.formManager.isFieldEnable(dataField);
    }

    enableInputField(inputField, dataField) {
        return this.formManager.enableInputField(inputField, dataField);
    }

    disableInput(inputField, dataField, readOnly) {
        return this.formManager.disableInput(inputField, dataField, readOnly);
    }

    prevPage() {
        return this.navigationController.prevPage();
    }

    nextPage() {
        return this.navigationController.nextPage();
    }

    isFirstPage() {
        return this.navigationController.isFirstPage();
    }

    isLastPage() {
        return this.navigationController.isLastPage();
    }

    zoomInit(e) {
        this.saveScrolling = Math.round(this.getScrollTop() / this.scale);
        this.scale = 1.2;
        console.info('zoom in, scale = ' + this.scale);
        this.fireEvent("scaleChange", ['in']);
    }

    zoomIn(e) {
        const maxZoomLimit = this.getMaxZoomLimit();
        if (this.scale >= maxZoomLimit) {
            return;
        }
        this.saveScrolling = Math.round(this.getScrollTop() / this.scale);
        this.scale = Math.min(maxZoomLimit, Math.round((this.scale + this.zoomStep) * 1000) / 1000);
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
        this.saveScrolling = Math.round(this.getScrollTop() / this.scale);
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
            this.saveScrolling = Math.round(this.getScrollTop() / this.scale);
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.fireEvent("scaleChange", ['in']);
        }
    }

    fullHeight() {
        console.info("full height " + window.innerHeight);
        let newScale = (Math.round((window.innerHeight - 200) / 100) / 10) - 0.1;
        if (newScale !== this.scale) {
            this.saveScrolling = Math.round(this.getScrollTop() / this.scale);
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
        return this.formManager.autocomplete(response, inputField);
    }

    checkForm() {
        return this.formManager.checkForm();
    }

    focusField(field) {
        return this.navigationController.focusField(field);
    }

    highlightRadio(field) {
        return this.navigationController.highlightRadio(field);
    }

    startProgress() {
        return this.progressController.startProgress();
    }

    stopProgress(){
        return this.progressController.stopProgress();
    }

    resetProgress() {
        return this.progressController.resetProgress();
    }

    updateRenderProgress() {
        return this.progressController.updateRenderProgress();
    }

    updateProgress(progress, text, animated) {
        return this.progressController.updateProgress(progress, text, animated);
    }

    getBrowserZoom() {
        return window.devicePixelRatio || 1;
    }

    getApplicationLayers(config) {
        return this.layerController.getApplicationLayers(config);
    }

    resolveLayerName(stepNumber, requestedLayerName, layers) {
        return this.layerController.resolveLayerName(stepNumber, requestedLayerName, layers);
    }

    extractStableLayerStepId(layerName) {
        return this.layerController.extractStableLayerStepId(layerName);
    }

    async showLayerByStep(stepNumber, solo, layerName = null) {
        return this.layerController.showLayerByStep(stepNumber, solo, layerName);
    }

    updateLayerButtonsState() {
        return this.layerController.updateLayerButtonsState();
    }

    async showAllLayers() {
        return this.layerController.showAllLayers();
    }

    async toggleLayerByStep(stepNumber, solo, layerId = null) {
        return this.layerController.toggleLayerByStep(stepNumber, solo, layerId);
    }

    async highlightStep(stepNumber, layerId = null) {
        return this.layerController.highlightStep(stepNumber, layerId);
    }

    clearHighlight() {
        return this.layerController.clearHighlight();
    }
}
