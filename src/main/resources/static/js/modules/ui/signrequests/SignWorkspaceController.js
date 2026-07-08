import {PdfViewer} from "../../utils/PdfViewer.js?version=@version@";
import {SignPlacementController} from "./SignPlacementController.js?version=@version@";
import {WheelDetector} from "../../utils/WheelDetector.js?version=@version@";
import {SignToolbar} from "./SignToolbar.js?version=@version@";
import {CommentManager} from "./CommentManager.js?version=@version@";
import {SpotManager} from "./SpotManager.js?version=@version@";
import {SignSpaceManager} from "./SignSpaceManager.js?version=@version@";
import {PostitManager} from "./PostitManager.js?version=@version@";
import {WorkspaceState} from "./WorkspaceState.js?version=@version@";

export class SignWorkspaceController {

    constructor(workspaceStateInput, csrf, signatureUiConfig = null) {
        const workspaceState = workspaceStateInput instanceof WorkspaceState
            ? workspaceStateInput
            : WorkspaceState.from(workspaceStateInput, null);
        const {showDataFlow, signUiDto} = workspaceState.toWorkspaceContext();
        const isPdf = signUiDto.pdf;
        const id = signUiDto.signRequestId;
        const dataId = signUiDto.dataId;
        const formId = signUiDto.formId;
        const currentSignRequestParamses = signUiDto.currentSignRequestParamses;
        const signImageNumber = signUiDto.signImageNumber;
        const currentSignType = signUiDto.currentSignType;
        const signable = signUiDto.signable;
        const editable = signUiDto.editable;
        const comments = signUiDto.comments;
        const spots = signUiDto.spots;
        const currentStepNumber = signUiDto.currentStepNumber;
        const currentStepMultiSign = signUiDto.currentStepMultiSign;
        const currentStepSingleSignWithAnnotation = signUiDto.currentStepSingleSignWithAnnotation;
        const workflowAvailable = signUiDto.workflowAvailable;
        const signImages = signUiDto.signImages;
        const userName = signUiDto.userName;
        const authUserName = signUiDto.authUserName;
        const fields = signUiDto.fields;
        const stepRepeatable = signUiDto.stepRepeatable;
        const status = signUiDto.status;
        const action = signUiDto.action;
        const notSigned = signUiDto.notSigned;
        const isOtp = signUiDto.otp;
        const restore = signUiDto.restore;
        const phone = signUiDto.phone;
        const isManager = signUiDto.manager;
        console.info("Starting workspace UI");
        this.ready = false;
        this.formInitialized = false;
        this.isPdf = isPdf;
        this.pdfRenderComplete = !isPdf;
        this.pdfRenderScope = $();
        this.isOtp = isOtp;
        this.phone = phone;
        this.changeModeSelector = null;
        this.action = action;
        this.dataId = dataId;
        this.formId = formId;
        this.userName = userName;
        this.workflow = workflowAvailable;
        this.signImageNumber = signImageNumber;
        this.restore = restore;
        this.comments = comments;
        this.spots = spots;
        this.notSigned = notSigned;
        this.signable = signable;
        this.editable = editable;
        this.isManager = isManager;
        this.signRequestId = id;
        this.currentSignType = currentSignType;
        this.currentStepNumber = currentStepNumber;
        this.stepRepeatable = stepRepeatable;
        this.status = status;
        this.csrf = csrf;
        this.signatureUiConfig = signatureUiConfig;
        this.currentStepMultiSign = currentStepMultiSign;
        this.forcePageNum = null;
        this.first = true;
        this.actionInitialyzed = false;
        this.saveAlert = false;
        this.missingCertTypeAlertShown = false;
        this.scrollTop = 0;
        this.refreshWorkspaceTimer = null;
        this.nextCommand = "none";
        this.toolsLoadingStateReleased = false;
        this.state = workspaceState;
        this.showDataFlow = showDataFlow;
        this.eventNamespace = ".workspacePdf";
        this.postitNamespace = ".workspacePdfPostit";
        this.commentDialogNamespace = ".workspacePdfCommentAdd";
        this.spotAddNamespace = ".workspacePdfSpotAdd";
        this.layoutNamespace = ".workspacePdfLayout";
        this.signSpaceNamespace = ".workspacePdfSignSpace";
        this.postitManager = new PostitManager(this.state, {
            eventNamespace: this.eventNamespace,
            postitNamespace: this.postitNamespace,
            getComments: () => this.comments
        });
        this.displayComments = this.shouldDisplayCommentsOnLoad(comments);
        if(fields != null) {
            for (let i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.workflowSteps != null && field.workflowSteps.includes(currentStepNumber) && field.required) {
                    this.forcePageNum = field.page;
                    break;
                }
            }
        }
        if (this.isPdf) {
            if(currentSignType === "form") {
                this.pdfViewer = new PdfViewer('/' + userName + '/forms/get-file/' + id, signable, editable, currentStepNumber, this.forcePageNum, fields, true);
            } else {
                this.pdfViewer = new PdfViewer('/ws-secure/global/get-last-file-pdf/' + id, signable, editable, currentStepNumber, this.forcePageNum, fields, false);
            }
        }
        this.signPlacementController = new SignPlacementController(
            currentSignType,
            currentSignRequestParamses,
            currentStepMultiSign,
            currentStepSingleSignWithAnnotation,
            signImageNumber,
            signImages,
            userName, authUserName, signable, this.forcePageNum, this.isOtp, this.phone, this.csrf, this.signatureUiConfig, this.isPdf, this.signRequestId);
        this.signPlacementController.addEventListener("spotSaved", spotData => this.onSpotSaved(spotData));
        this.signPlacementController.addEventListener("spotDeleted", spotId => this.onSpotDeleted(spotId));
        this.signPlacementController.addEventListener("signPlacementChanged", () => this.updateAnnotationActionButtonsAvailability());
        this.currentSignRequestParamses = currentSignRequestParamses;
        // Mode system deprecated: UI is now driven by rights (signable/editable).
        this.wheelDetector = new WheelDetector();
        this.addSpotEnabled = false;
        this.addCommentEnabled = false;
        this.nextCommand = "none";
        this.toolbar = new SignToolbar({
            eventNamespace: ".workspacePdfToolbar",
            priorityContainers: ["#tools", ".es-nav-tools"],
            onAddComment: () => this.enableCommentAdd(),
            onAddSpot: () => this.enableSpotAdd(),
            onRequestSignatureStep: () => this.signPlacementController.requestSignatureStep(),
            onAddSign: () => this.addSign(),
            onAddParaph: () => this.addParaph(),
            onAddCheck: () => this.signPlacementController.addCheckImage(this.pdfViewer.pageNum),
            onAddTimes: () => this.signPlacementController.addTimesImage(this.pdfViewer.pageNum),
            onAddCircle: () => this.signPlacementController.addCircleImage(this.pdfViewer.pageNum),
            onAddMinus: () => this.signPlacementController.addMinusImage(this.pdfViewer.pageNum),
            onAddText: () => this.signPlacementController.addText(this.pdfViewer.pageNum)
        });
        this.commentManager = new CommentManager(this.state, {
            eventNamespace: this.eventNamespace,
            postitNamespace: this.postitNamespace,
            commentDialogNamespace: this.commentDialogNamespace,
            getPdfViewer: () => this.pdfViewer,
            getComments: () => this.comments,
            isEditable: () => this.editable,
            isDisplayComments: () => this.displayComments,
            setDisplayComments: value => { this.displayComments = value; },
            isAddSpotEnabled: () => this.addSpotEnabled,
            setAddSpotEnabled: value => { this.addSpotEnabled = value; },
            isAddCommentEnabled: () => this.addCommentEnabled,
            setAddCommentEnabled: value => { this.addCommentEnabled = value; },
            setToolsDisabled: disabled => this.setToolsBarDisabled(disabled),
            setInsertActionsDisabled: disabled => this.toolbar.setInsertActionsDisabled(disabled),
            setSignSpacesDroppableEnabled: enabled => this.setSignSpacesDroppableEnabled(enabled),
            setCommentAddButtonsState: enabled => this.setCommentAddButtonsState(enabled),
            lockSigns: () => this.signPlacementController.lockSigns(),
            setPointItEnabled: enabled => { this.signPlacementController.pointItEnable = enabled; },
            selectChangeMode: value => {
                if (this.changeModeSelector != null) {
                    this.changeModeSelector.setSelected(value);
                }
            },
            showAllPostits: () => this.showAllPostits(),
            hideAllPostits: () => this.hideAllPostits(),
            reloadPage: () => document.location.reload(),
            restoreAddSpotButton: () => this.updateAnnotationActionButtonsAvailability(),
            signRequestId: () => this.signRequestId,
            isOtp: () => this.isOtp,
            csrf: () => this.csrf,
            status: () => this.status
        });
        this.spotManager = new SpotManager(this.state, {
            signSpaceNamespace: this.signSpaceNamespace,
            spotAddNamespace: this.spotAddNamespace,
            getSpots: () => this.spots,
            setSpots: spots => { this.spots = spots; },
            getCurrentSignRequestParamses: () => this.currentSignRequestParamses,
            setCurrentSignRequestParamses: params => {
                this.currentSignRequestParamses = params;
                this.signPlacementController.currentSignRequestParamses = params;
            },
            getCurrentStepNumber: () => this.currentStepNumber,
            isSignable: () => this.signable,
            getCurrentSignType: () => this.currentSignType,
            getUserName: () => this.userName,
            getFormId: () => this.formId,
            getSignRequestId: () => this.signRequestId,
            getCsrf: () => this.csrf,
            getPdfViewer: () => this.pdfViewer,
            setToolsDisabled: disabled => this.setToolsBarDisabled(disabled),
            setInsertActionsDisabled: disabled => this.toolbar.setInsertActionsDisabled(disabled),
            setSignSpacesDroppableEnabled: enabled => this.setSignSpacesDroppableEnabled(enabled),
            setSpotActionButtonsDisabled: disabled => this.setSpotActionButtonsDisabled(disabled),
            exitCommentAddMode: () => this.exitCommentAddMode(),
            startSpotPlacement: () => this.signPlacementController.addSign(this.pdfViewer.pageNum, false, 999999, null),
            refreshSignFields: () => this.initSignFields(),
            removeSignSpaceBySpotId: spotId => $("#signSpace_spot_" + spotId).remove()
        });
        this.signSpaceManager = new SignSpaceManager(this.state, {
            signSpaceNamespace: this.signSpaceNamespace,
            getPdfViewer: () => this.pdfViewer,
            getSignPlacementController: () => this.signPlacementController,
            getCurrentSignRequestParamses: () => this.currentSignRequestParamses,
            getSpots: () => this.spots,
            getCurrentStepNumber: () => this.currentStepNumber,
            isSignable: () => this.signable,
            isEditable: () => this.editable,
            isManager: () => this.isManager,
            isCreator: () => this.isCurrentUserCreator(),
            getBrowserZoom: () => this.getBrowserZoom(),
            requestAddSign: signIndex => this.addSign(signIndex),
            findSpotIdForSignParams: signParams => this.findSpotIdForSignParams(signParams),
            filterSpotsNotCurrentStep: spotsToFilter => this.filterSpotsNotCurrentStep(spotsToFilter),
            bindSignSpaceDelete: signSpaceDiv => this.bindSignSpaceDelete(signSpaceDiv)
        });
        this.initChangeModeSelector();
        this.initDataFields(fields);
        this.wsTabs = $("#ws-tabs");
        this.addSignButton = $("#addSignButton");
        this.lastWidth = window.innerWidth;
        this.lastHeight = window.innerHeight;
        if (currentSignType === "form" || (formId == null && !workflowAvailable) || currentSignRequestParamses.length === 0) {
            if(this.wsTabs.length) {
                this.autocollapse();
                let self = this;
                const THRESHOLD = 100;
                const DEBOUNCE_DELAY = 100;
                let resizeTimer = null;
                $(window)
                    .off("resize" + this.layoutNamespace)
                    .on("resize" + this.layoutNamespace, () => {
                    clearTimeout(resizeTimer);
                    resizeTimer = setTimeout(() => {
                        const w = window.innerWidth;
                        const h = window.innerHeight;
                        const deltaW = Math.abs(w - self.lastWidth);
                        const deltaH = Math.abs(h - self.lastHeight);
                        if (w === self.lastWidth || (deltaW < THRESHOLD && deltaH < THRESHOLD)) return;
                        self.autocollapse();
                        self.lastWidth = w;
                        self.lastHeight = h;
                    }, DEBOUNCE_DELAY);
                });
            }
        }
        let root = document.querySelector(':root');
        root.setAttribute("style", "scroll-behavior: auto;");
        this.initListeners();
        this.postitManager.applyVisibility(this.displayComments);
        if (this.isPdf) {
            this.signPlacementController.updateScales(this.pdfViewer.scale);
        } else {
            this.initWorkspace();
        }
    }

    initListeners() {
        const eventNamespace = this.eventNamespace;
        if (this.isPdf) {
            [
                ['#prev', 'click', () => {
                    this.pdfViewer.prevPage();
                    this.updateAnnotationActionButtonsAvailability();
                }],
                ['#next', 'click', () => {
                    this.pdfViewer.nextPage();
                    this.updateAnnotationActionButtonsAvailability();
                }],
                ['#end-button', 'click', () => {
                    this.pdfViewer.nextPage();
                    this.updateAnnotationActionButtonsAvailability();
                }],
                [`[name='spotStepNumber']`, 'change', () => this.changeSpotStep()]
            ].forEach(([selector, event, handler]) => $(selector).off(event + eventNamespace).on(event + eventNamespace, handler));
            [
                ['renderStarted', () => this.beginPdfRender()],
                ['renderFinished', () => {
                    this.initSignWorkspace();
                    this.setPdfRenderComplete(true);
                    this.releaseToolsLoadingState();
                }],
                ['renderComplete', () => this.completePdfRender()],
                ['scaleChange', () => this.refreshWorkspace()],
                ['change', () => this.saveData(localStorage.getItem('disableFormAlert') === "true")]
            ].forEach(([event, handler]) => this.pdfViewer.addEventListener(event, handler));
            this.setPdfRenderComplete(Boolean(this.pdfViewer.renderComplete));
            if (this.currentSignType !== "form") {
                this.pdfViewer.addEventListener('reachEnd', () => this.markAsViewed());
            }
            this.commentManager.bind(eventNamespace);
        }
        this.toolbar.bind();
        this.refreshToolbarAccessibility();
        this.updateAnnotationActionButtonsAvailability();
        this.postitManager.bind();

        $("#signImageBtn").off('click' + eventNamespace).on('click' + eventNamespace, () => this.signPlacementController.popUserUi());
        this.notviewedAnim();
    }

    toggleDNone(selectors, hidden) {
        $(selectors.join(', ')).toggleClass('d-none', hidden);
    }

    ensureWorkspaceReady(onReady = null) {
        if (this.ready) {
            return;
        }
        this.ready = true;
        this.enableSignMode();
        onReady?.();
    }

    bindWheelEvents() {
        [
            ["down", e => {
                this.pdfViewer.checkCurrentPage(e);
                this.updateAnnotationActionButtonsAvailability();
            }],
            ["up", e => {
                this.pdfViewer.checkCurrentPage(e);
                this.updateAnnotationActionButtonsAvailability();
            }],
            ["zoomin", e => this.pdfViewer.zoomOut(e)],
            ["zoomout", e => this.pdfViewer.zoomIn(e)],
            ["zoominit", e => this.pdfViewer.zoomInit(e)]
        ].forEach(([event, handler]) => this.wheelDetector.addEventListener(event, handler));
    }

    resetRequestedSignatureStep() {
        this.signPlacementController?.clearRequestedSignatureStep?.();
    }

    hasCertifiedVisualSignature() {
        return !this.notSigned && this.signPlacementController.signsList.length > 0;
    }

    hasValidSelectedCertType(certTypeSelect = $("#certType")) {
        return !certTypeSelect.length
            || (typeof this.signPlacementController?.hasValidSelectedCertType === "function"
                ? this.signPlacementController.hasValidSelectedCertType()
                : certTypeSelect.val() != null && certTypeSelect.val() !== "");
    }

    resolveTargetSign(forceSignNumber) {
        let signNum = forceSignNumber;
        if (signNum == null) {
            signNum = this.currentSignRequestParamses.findIndex(signRequestParams => !signRequestParams.ready);
            if (signNum >= 0) {
                this.signPlacementController.currentSignRequestParamsNum = signNum;
            } else {
                signNum = null;
            }
        }
        return {
            signNum,
            targetPageNumber: this.currentSignRequestParamses?.[signNum]?.signPageNumber ?? this.pdfViewer.pageNum
        };
    }

    activateSignPlacement(signRequestParams, signNum) {
        if (signRequestParams == null) {
            this.signPlacementController?.clearRequestedSignatureStep?.();
            return;
        }
        if (Number.isFinite(signNum)) {
            this.signSpaceManager.placeSignOnSlot(signNum, signRequestParams);
            this.updateAnnotationActionButtonsAvailability();
            return;
        }
        if (typeof signRequestParams.activatePlacement === "function") {
            const activatePlacement = () => signRequestParams.activatePlacement();
            activatePlacement();
            window.requestAnimationFrame?.(activatePlacement);
        }
        this.updateAnnotationActionButtonsAvailability();
    }

    shouldDisplayCommentsOnLoad(comments = this.comments) {
        if (this.postitManager != null && typeof this.postitManager.shouldDisplayOnLoad === 'function') {
            return this.postitManager.shouldDisplayOnLoad(comments);
        }
        if (Array.isArray(comments) && comments.length > 0) {
            return true;
        }
        return document.querySelector('.postit-global') != null;
    }

    notviewedAnim() {
        let div = document.querySelector('.jumping');
        if(div == null || div.dataset.esNotviewedAnimBound === "true") return;
        div.dataset.esNotviewedAnimBound = "true";
        let isPaused = false;
        function togglePause() {
            isPaused = !isPaused;
            if (!isPaused) {
                div.style.animationPlayState = 'running';
            } else {
                div.style.animationPlayState = 'paused';
                setTimeout(togglePause, 3000);
            }
        }

        div.addEventListener('animationiteration', () => {
            togglePause();
        });
    }

    markAsViewed() {
        $.ajax({
            url: "/ws-secure/global/viewed/" + this.signRequestId + "?" + this.csrf.parameterName + "=" + this.csrf.token,
            type: 'POST',
            success: function () {
                $(".not-viewed").remove();
            }
        });
    }

    initSignFields() {
        return this.signSpaceManager.initSignFields();
    }

    filterSpotsNotCurrentStep(spots) {
        return this.spotManager.filterSpotsNotCurrentStep(spots);
    }

    isCurrentUserCreator() {
        const creator = this.state?.frontDto?.creator ?? this.showDataFlow?.front?.creator ?? null;
        const candidates = [
            this.state?.frontDto?.user ?? null,
            this.state?.frontDto?.authUser ?? null,
            this.showDataFlow?.front?.user ?? null,
            this.showDataFlow?.front?.authUser ?? null,
            (typeof window !== "undefined" && window.user != null) ? window.user : null
        ].filter(candidate => candidate != null);

        const creatorId = Number.parseInt(creator?.id, 10);
        const creatorEppn = creator?.eppn ?? null;
        for (let i = 0; i < candidates.length; i++) {
            const candidate = candidates[i];
            const candidateId = Number.parseInt(candidate?.id, 10);
            if (Number.isFinite(creatorId) && Number.isFinite(candidateId) && creatorId === candidateId) {
                return true;
            }
            if (creatorEppn != null && candidate?.eppn != null && creatorEppn === candidate.eppn) {
                return true;
            }
        }
        return false;
    }

    findSpotIdForSignParams(signParams) {
        return this.spotManager.findSpotIdForSignParams(signParams);
    }

    onSpotSaved(spotData) {
        return this.spotManager.onSpotSaved(spotData);
    }

    onSpotDeleted(spotId) {
        return this.spotManager.onSpotDeleted(spotId);
    }

    bindSignSpaceDelete(signSpaceDiv) {
        return this.spotManager.bindSignSpaceDelete(signSpaceDiv);
    }

    refreshSignFields() {
        return this.signSpaceManager.refreshSignFields();
    }

    resolvePreferredSignImageNumber() {
        const storedSignNumber = Number.parseInt(localStorage.getItem('signNumber'), 10);
        const candidates = [
            this.state?.frontDto?.user?.defaultSignImageNumber,
            this.showDataFlow?.front?.user?.defaultSignImageNumber,
            this.signImageNumber
        ];
        try {
            const rawUiMe = sessionStorage.getItem('uiMe');
            const uiMe = rawUiMe ? JSON.parse(rawUiMe) : null;
            candidates.unshift(uiMe?.user?.defaultSignImageNumber ?? null);
        } catch (error) {
            console.debug('Unable to read default sign image from session UI payload', error);
        }
        for (let i = 0; i < candidates.length; i++) {
            const parsedSignImageNumber = Number.parseInt(candidates[i], 10);
            if (Number.isFinite(parsedSignImageNumber)) {
                return parsedSignImageNumber;
            }
        }
        if (this.restore && Number.isFinite(storedSignNumber)) {
            return storedSignNumber;
        }
        return null;
    }

    async addSign(forceSignNumber) {
        if (this.hasCertifiedVisualSignature()) {
            this.signPlacementController?.clearRequestedSignatureStep?.();
            bootbox.alert("Ce document contient déjà une signature électronique certifiée, il n’est donc pas possible d’ajouter d'autre visuel de signature.");
            return;
        }
        // const certTypeSelect = $("#certType");
        // if (!this.hasValidSelectedCertType(certTypeSelect)) {
        //     this.signPlacementController?.clearRequestedSignatureStep?.();
        //     if (!this.missingCertTypeAlertShown) {
        //         this.missingCertTypeAlertShown = true;
        //         bootbox.alert("<div class='alert alert-info mb-0'>Merci de choisir un type de signature dans la liste déroulante avant de cliquer sur un emplacement de signature.</div>", () => {
        //             setTimeout(() => $("#certType").focus(), 50);
        //         });
        //     }
        //     if (typeof this.signPlacementController?.refreshSteps !== "function") {
        //         certTypeSelect.trigger("focus");
        //     }
        //     return;
        // }
        this.pdfViewer.annotationLinkRemove();
        const {signNum, targetPageNumber} = this.resolveTargetSign(forceSignNumber);
        const persistedSignImageNumber = await this.signPlacementController?.persistMobileSignaturePreviews?.();
        const resolvedSignImageNumber = Number.isFinite(persistedSignImageNumber)
            ? persistedSignImageNumber
            : this.resolvePreferredSignImageNumber();
        if (Number.isFinite(resolvedSignImageNumber)) {
            this.signImageNumber = resolvedSignImageNumber;
        }
        const signRequestParams = await this.signPlacementController.addSign(targetPageNumber, this.restore, this.signImageNumber, signNum);
        this.activateSignPlacement(signRequestParams, signNum);
    }


    async addParaph() {
        if (this.hasCertifiedVisualSignature()) {
            bootbox.alert("Ce document contient déjà une signature électronique certifiée, il n’est donc pas possible d’ajouter d'autre visuel de signature.");
            return;
        }
        const srp = await this.signPlacementController.addSign(this.pdfViewer.pageNum, false, 999997);
        if (srp != null && typeof srp.initParaph === 'function') {
            srp.initParaph();
        }
    }

    initWorkspace() {
        console.info("init workspace");
        this.ensureWorkspaceReady();
        this.releaseToolsLoadingState();
    }

    initSignWorkspace() {
        console.info("init sign workspace");
        this.ensureWorkspaceReady(() => this.bindWheelEvents());
        this.refreshAfterPageChange();
        this.initForm();
        $("#content")
            .off('mousedown' + this.eventNamespace)
            .on('mousedown' + this.eventNamespace, () => this.signPlacementController.lockSigns());
        this.signPlacementController.updateScales(this.pdfViewer.scale);
    }

    releaseToolsLoadingState() {
        if (this.toolsLoadingStateReleased) {
            return;
        }
        this.toolsLoadingStateReleased = true;
        this.setPdfRenderMode(false);
        this.setToolsBarDisabled(false);
        this.updateAnnotationActionButtonsAvailability();
        this.refreshToolbarAccessibility();
        this.focusPrimaryToolbarAction();
    }

    getPrimaryToolbarFocusSelectors() {
        if (this.currentSignType === 'hiddenVisa') {
            return ['#signLaunchButton', '#signAdvancedLaunchButton', '#refuseLaunchButton', '#insert-btn'];
        }
        return ['#addSignButton2', '#signLaunchButton', '#signAdvancedLaunchButton', '#addParaphButton2', '#refuseLaunchButton'];
    }

    refreshToolbarAccessibility() {
        if (this.toolbar != null && typeof this.toolbar.refreshAccessibility === 'function') {
            this.toolbar.refreshAccessibility();
        }
    }

    setPdfRenderComplete(complete) {
        this.pdfRenderComplete = Boolean(complete);
        this.updateAnnotationActionButtonsAvailability();
    }

    beginPdfRender() {
        this.toolsLoadingStateReleased = false;
        this.setPdfRenderMode(true);
        this.setToolsBarDisabled(true);
        this.setPdfRenderComplete(false);
    }

    setPdfRenderMode(enabled) {
        const active = Boolean(enabled);
        $("body").toggleClass("es-pdf-render-mode", active);
        if (active) {
            const workspace = $("#workspace");
            const mainContent = workspace.closest(".es-main-content");
            const workspaceElement = workspace.get(0);
            this.pdfRenderScope = mainContent.length && workspaceElement != null
                ? mainContent.children().has(workspaceElement)
                : $();
            this.pdfRenderScope.addClass("es-pdf-render-scope");
            return;
        }
        this.pdfRenderScope.removeClass("es-pdf-render-scope");
        this.pdfRenderScope = $();
    }

    completePdfRender() {
        this.setPdfRenderComplete(true);
        this.releaseToolsLoadingState();
    }

    canUseAnnotationActions() {
        return this.editable
            && (!this.isPdf || this.pdfRenderComplete)
            && !this.addCommentEnabled
            && !this.addSpotEnabled
            && !$("body").hasClass("es-spot-add-mode")
            && !this.hasActiveSignRequestParamsOnCurrentPage();
    }

    hasActiveSignRequestParamsOnCurrentPage() {
        const currentPage = parseInt(this.pdfViewer?.pageNum, 10);
        if (!Number.isFinite(currentPage) || this.signPlacementController?.signRequestParamses == null) {
            return false;
        }
        return Array.from(this.signPlacementController.signRequestParamses.values()).some(signRequestParams => {
            const signPageNumber = parseInt(signRequestParams?.signPageNumber, 10);
            const signImageNumber = parseInt(signRequestParams?.signImageNumber, 10);
            return signRequestParams?.isSign === true
                && signImageNumber !== 999999
                && Number.isFinite(signPageNumber)
                && signPageNumber === currentPage;
        });
    }

    updateAnnotationActionButtonsAvailability() {
        if (this.toolbar == null) {
            return;
        }
        if (this.addCommentEnabled) {
            this.toolbar.setCommentAddActive(true);
            return;
        }
        this.toolbar.setSpotActionButtonsDisabled(!this.canUseAnnotationActions());
    }

    focusPrimaryToolbarAction() {
        if (this.toolbar == null || typeof this.toolbar.focusPrimaryAction !== 'function') {
            return;
        }
        this.toolbar.focusPrimaryAction(this.getPrimaryToolbarFocusSelectors());
    }

    initForm() {
        console.info("init form");
        if(!this.formInitialized) {
            this.formInitialized = true;
            if (!this.editable) {
                this.disableForm();
            }
        }
    }

    async saveData(disableAlert) {
        try {
            let promises = [];
            for(let i = 1; i < this.pdfViewer.pdfDoc.numPages + 1; i++) {
                let promise = this.pdfViewer.pdfDoc.getPage(i)
                    .then(page => page.getAnnotations())
                    .then(items => this.pdfViewer.saveValues(items));
                promises.push(promise);
            }
            await Promise.all(promises);
            this.pushData(false, disableAlert);
        } catch(error) {
            console.error('Erreur lors de la sauvegarde:', error);
        }
    }

    pushData(redirect, disableAlert) {
        console.debug("debug - " + "push data");
        let formData = {};
        this.pdfViewer.dataFields.forEach(dataField => {
            formData[dataField.name] = this.pdfViewer.savedFields.get(dataField.name);
        });

        if (redirect || this.dataId != null) {
            let json = JSON.stringify(formData);
            let dataId = $('#dataId');
            $.ajax({
                data: {'formData': json},
                type: 'POST',
                url: '/user/datas/form/' + this.formId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&dataId=' + this.dataId,
                success: function (response) {
                    dataId.val(response);
                    if (redirect) {
                        location.href = "/user/datas/" + response + "/update";
                    }
                }
            });
        } else {
            if(!this.saveAlert && !disableAlert) {
                this.saveAlert = true;
                bootbox.confirm({
                    message : "Attention, <p>Vous modifier les champs d’un PDF en dehors d’une procédure de formulaire esup-signature.<br>Dans ce cas, vos modifications seront prises en compte seulement si vous allez jusqu’à la signature du document. <br>Dans le cas contraire, si vous abandonnez, votre saisie sera perdue.</p>",
                    buttons: {
                        cancel: {
                            label: 'Ok',
                            className: 'btn-primary'
                        },
                        confirm: {
                            label: 'Ne plus me montrer ce message',
                            className: 'btn-secondary'
                        }
                    },
                    callback: function (result) {
                        if (result) {
                            localStorage.setItem('disableFormAlert', true)
                        }
                    }
                });
            }
        }
        this.executeNextCommand();
    }

    executeNextCommand() {
        if (this.nextCommand === "next") {
            this.pdfViewer.nextPage();
        } else if (this.nextCommand === "prev") {
            this.pdfViewer.prevPage()
        }
        this.nextCommand = "none";
    }

    initDataFields() {
        if (this.pdfViewer?.dataFields.length > 0 && this.pdfViewer.dataFields[0].defaultValue != null) {
            this.pdfViewer.dataFields.forEach(dataField => this.pdfViewer.savedFields.set(dataField.name, dataField.defaultValue));
        }
    }

    checkSignsPositions() {
        let testSign = Array.from(this.signPlacementController.signRequestParamses.values());
        if(testSign.filter(s => s.signImageNumber >= 0 && s.signImageNumber !== 999999 && s.isSign).length > 0) {
            for (let i = 0; i < this.currentSignRequestParamses.length; i++) {
                if ((this.currentSignRequestParamses[i].ready == null || !this.currentSignRequestParamses[i].ready)) {
                    return i;
                }
            }
            return null;
        } else {
            return 0;
        }
    }

    disableForm() {
        $("#signForm :input").not(':input[type=button], :input[type=submit], :input[type=reset]').each(function (i, e) {
            console.debug("debug - " + "disable ");
            console.debug("debug - " + e);
            e.disabled = true;
        });
    }

    refreshWorkspace() {
        console.info("refresh workspace");
        clearTimeout(this.refreshWorkspaceTimer);
        this.refreshWorkspaceTimer = setTimeout(() => {
            if (!this.pdfViewer.applyScaleWithoutRerender()) {
                this.pdfViewer.startRender();
            } else {
                this.signPlacementController.updateScales(this.pdfViewer.scale);
                this.refreshSignFields();
                this.commentManager.refresh();
            }
            localStorage.setItem("scale", this.pdfViewer.scale);
        }, 75);
    }

    refreshAfterPageChange() {
        console.debug("debug - " + "refresh comments and sign pos" + this.pdfViewer.pageNum);
        this.commentManager.refresh();
        if(this.status === "pending") {
            this.initFormAction();
        }
        if(this.currentSignType !== "hiddenVisa") {
            if(this.first) {
                this.initSignFields();
            } else {
                this.refreshSignFields();
            }
        }
        // $("div[id^='cross_']").each((index, e) => this.toggleSign(e));
        this.pdfViewer.pdfDiv.css('opacity', 1);
        this.first = false;
    }

    enableSignMode() {
        console.info("apply unified workspace ui");
        this.disableAllModes();
        this.setToolsBarDisabled(this.isPdf && !this.pdfRenderComplete);
        this.refreshToolbarAccessibility();
        this.setSignSpacesDroppableEnabled(true);
        this.signPlacementController.pointItEnable = false;
        if (this.status === 'deleted') {
            $('#workspace').addClass('alert-danger');
        }

        if (this.signable) {
            this.toggleDNone(['#sign-tools', '#signTools', '#signLaunchButton', '#signAdvancedLaunchButton', '#addSignButton2', '#addParaphButton', '#visaLaunchButton', '#signButtons', '#forward-btn', '#refuseLaunchButton', '#trashLaunchButton'], false);
        }

        if (this.editable) {
            $('#commentsTools, #insert-btn-div, #insert-btn').show();
            this.toggleDNone(['#addCommentButton2', '#addSpotButton2', '#postit', '#commentHelp'], false);
        }

        if (this.displayComments) {
            $(".circle").show().css('width', '0px');
            this.showAllPostits();
        } else {
            $(".circle").hide();
            this.hideAllPostits();
        }

        $(".sign-space").toggle(this.signable || this.editable);

        $('#infos, #insert-btn-div, #insert-btn').show();
        $('#insert-btn').removeClass("btn-warning");
        this.refreshToolbarAccessibility();
        if (this.isPdf) {
            this.pdfViewer.scrollToPage(this.forcePageNum && this.currentSignRequestParamses?.[0] != null ? this.forcePageNum : 1);
        }
        $("#cross_999999").remove();
        this.updateAnnotationActionButtonsAvailability();
        if (this.isPdf) {
            this.refreshAfterPageChange();
        }
    }

    disableAllModes() {
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        this.toggleDNone(['#addCommentButton2', '#addSpotButton2', '#signLaunchButton', '#signAdvancedLaunchButton', '#forward-btn', '#addSignButton2', '#addParaphButton', '#visaLaunchButton', '#refuseLaunchButton', '#commentHelp', '#sign-tools', '#signTools'], true);
        $('#commentsTools, #infos, #postit, #refusetools, #insert-btn-div').hide();
        $('#pdf').css('cursor', 'default');
        $('#hideCommentButton').off('click' + this.commentDialogNamespace);
        $(".spot, .circle").hide();
        this.hideAllPostits();
    }

    hideAllPostits() {
        return this.postitManager.hideAll();
    }

    showAllPostits() {
        return this.postitManager.showAll();
    }

    enableCommentAdd(e) {
        if (!this.canUseAnnotationActions()) {
            return;
        }
        return this.commentManager.enableCommentAdd(e);
    }

    setCommentAddButtonsState(enabled) {
        this.toolbar.setCommentAddActive(enabled);
        if (!enabled) {
            this.updateAnnotationActionButtonsAvailability();
        }
    }

    exitCommentAddMode() {
        return this.commentManager.exitCommentAddMode();
    }

    enableSpotAdd() {
        if (!this.canUseAnnotationActions()) {
            return;
        }
        return this.spotManager.enableSpotAdd();
    }

    setSpotActionButtonsDisabled(disabled) {
        this.toolbar.setSpotActionButtonsDisabled(disabled || !this.canUseAnnotationActions());
    }

    shouldKeepToolsEnabledDuringPdfRender() {
        return ['completed', 'exported', 'refused'].includes(this.status);
    }

    setToolsBarDisabled(disabled) {
        this.toolbar.setToolsDisabled(disabled && !this.shouldKeepToolsEnabledDuringPdfRender());
    }


    setSignSpacesDroppableEnabled(enabled) {
        return this.signSpaceManager.setSignSpacesDroppableEnabled(enabled);
    }

    changeSpotStep() {
        return this.spotManager.changeSpotStep();
    }

    initChangeModeSelector() {
        $("#changeMode1").off("click" + this.eventNamespace).on("click" + this.eventNamespace, () => {
            this.displayComments = !this.displayComments;
            this.enableSignMode();
        });
        $("#changeMode2").off("click" + this.eventNamespace).on("click" + this.eventNamespace, () => this.enableSignMode());
    }

    initFormAction() {
        if(!this.actionInitialyzed) {
            console.debug("debug - " + "eval : " + this.action);
            jQuery.globalEval(this.action);
            this.actionInitialyzed = true;
        }
    }

    autocollapse() {
        let menu = "#ws-tabs";
        let maxWidth = $("#workspace").innerWidth() - 50;
        const calculateTotalWidth = () => {
            let total = 0;
            const listItems = document.querySelectorAll('#ws-tabs > li');
            listItems.forEach(li => {
                const rect = li.getBoundingClientRect();
                total += rect.width;
                const style = window.getComputedStyle(li);
                total += parseFloat(style.marginLeft) + parseFloat(style.marginRight);
            });
            return total;
        };
        let totalWidth = calculateTotalWidth();
        if (totalWidth >= maxWidth) {
            $(menu + ' .dropdown').removeClass('d-none');
            totalWidth = calculateTotalWidth();
            while (totalWidth > maxWidth) {
                let children = $(menu + ' > li.file-tab');
                let count = children.length;
                if (count === 0) break; // Sécurité
                $(children[count - 1]).prependTo(menu + ' .dropdown-menu');
                totalWidth = calculateTotalWidth();
                console.warn("Nouvelle largeur : " + totalWidth);
            }
        }
        else {
            let collapsed = $(menu + ' .dropdown-menu > li');

            if (collapsed.length === 0) {
                $(menu + ' .dropdown').addClass('d-none');
                return;
            }
            const dropdownWidth = $(menu + ' .dropdown')[0].getBoundingClientRect().width;
            const safeMaxWidth = maxWidth - dropdownWidth - 50; // marge de sécurité

            let i = 0;
            while (i < collapsed.length && totalWidth < safeMaxWidth) {
                const itemWidth = collapsed[i].getBoundingClientRect().width;
                const style = window.getComputedStyle(collapsed[i]);
                const margins = parseFloat(style.marginLeft) + parseFloat(style.marginRight);
                const estimatedWidth = totalWidth + itemWidth + margins;
                if (estimatedWidth >= safeMaxWidth) break;
                $(collapsed[i]).insertBefore($(menu + ' > li.dropdown'));
                totalWidth = calculateTotalWidth();
                i++;
            }
            if ($(menu + ' .dropdown-menu > li').length === 0) {
                $(menu + ' .dropdown').addClass('d-none');
            }
        }
    }

    getBrowserZoom() {
        return 1 || 1;
    }

    destroy() {
        $(window).off(this.layoutNamespace);
        $(document).off('keydown' + this.commentDialogNamespace);
        $(document).off('click' + this.spotAddNamespace);
        $('#pdf').off(this.commentDialogNamespace).css('cursor', 'default');
        $('#content').off('mousedown' + this.eventNamespace);
        $('#saveCommentButton').off('click' + this.commentDialogNamespace);
        $('#hideCommentButton').off('click' + this.commentDialogNamespace);

        [
            '#prev',
            '#next',
            '#end-button',
            '#addCommentButton',
            '#addSpotButton',
            '#addCommentButton2',
            '#addSpotButton2',
            '#spotStepNumber',
            '#addSignButton2',
            '#addSignButton3',
            '#addParaphButton',
            '#addParaphButton2',
            '#addCheck',
            '#addTimes',
            '#addCircle',
            '#addMinus',
            '#addText',
            '#signImageBtn',
            '#changeMode1',
            '#changeMode2'
        ].forEach(selector => $(selector).off(this.eventNamespace));
        this.addSignButton && this.addSignButton.off('click' + this.eventNamespace);
        $('.toggleComments').off(this.eventNamespace);
        $('.sign-space').each((_, element) => {
            const signSpace = $(element);
            signSpace.off(this.signSpaceNamespace);
            signSpace.find('.slot-delete-btn').off(this.signSpaceNamespace);
            try {
                if (signSpace.hasClass('ui-droppable')) {
                    signSpace.droppable('destroy');
                }
            } catch (error) {
                // Ignore partially initialized droppable widgets.
            }
        });
        if (this.signPlacementController != null && typeof this.signPlacementController.destroy === 'function') {
            this.signPlacementController.destroy();
        }
        if (this.toolbar != null && typeof this.toolbar.destroy === 'function') {
            this.toolbar.destroy();
        }
        if (this.postitManager != null && typeof this.postitManager.destroy === 'function') {
            this.postitManager.destroy();
        }
        if (this.commentManager != null && typeof this.commentManager.destroy === 'function') {
            this.commentManager.destroy();
        }
        if (this.spotManager != null && typeof this.spotManager.destroy === 'function') {
            this.spotManager.destroy();
        }
        if (this.signSpaceManager != null && typeof this.signSpaceManager.destroy === 'function') {
            this.signSpaceManager.destroy();
        }
    }

}
