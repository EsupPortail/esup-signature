import {SignRequestParams} from "../../../prototypes/SignRequestParams.js?version=@version@";
import {EventFactory} from "../../utils/EventFactory.js?version=@version@";
import {UserUi} from '../users/UserUi.js?version=@version@';
import {SignatureImageResolver, SPECIAL_SIGN_IMAGE_NUMBERS} from './SignatureImageResolver.js?version=@version@';

export class SignPlacementController extends EventFactory {

    constructor(signType, currentSignRequestParamses, currentStepMultiSign, currentStepSingleSignWithAnnotation, signImageNumber, signImages, userName, authUserName, signable, forceResetSignPos, isOtp, phone, csrf, signatureUiConfig = null, showPlacementStep = true, signRequestId = null) {
        super();
        console.info("Starting sign positioning tools");
        this.signRequestId = signRequestId;
        this.userName = userName;
        this.authUserName = authUserName;
        this.signImages = signImages;
        this.currentStepMultiSign = currentStepMultiSign;
        this.currentStepSingleSignWithAnnotation = currentStepSingleSignWithAnnotation;
        this.isOtp = isOtp;
        this.phone = phone;
        this.csrf = csrf;
        this.signatureUiConfig = signatureUiConfig;
        this.currentSignRequestParamses = currentSignRequestParamses;
        if(currentSignRequestParamses != null) {
            this.currentSignRequestParamses.sort((a, b) => (a.xPos > b.xPos) ? 1 : ((b.xPos > a.xPos) ? -1 : 0))
            this.currentSignRequestParamses.sort((a, b) => (a.yPos > b.yPos) ? 1 : ((b.yPos > a.yPos) ? -1 : 0))
            this.currentSignRequestParamses.sort((a, b) => (a.signPageNumber > b.signPageNumber) ? 1 : ((b.signPageNumber > a.signPageNumber) ? -1 : 0))
        }
        this.beforeUnloadNamespace = ".signPositionPendingChanges";
        this.scrollNamespace = ".signPositionScroll";
        this.workspaceScrollHandler = null;
        this.userSignatureUpdatedHandler = e => this.applyUserSignatureState(e.detail);
        this.userSignatureDeletedHandler = e => this.applyUserSignatureState(e.detail);
        this.signRequestParamses = new Map();
        this.id = 0;
        this.signsList = [];
        this.signatureStepRequested = false;
        this.showPlacementStep = showPlacementStep !== false;
        this.currentScale;
        this.scrollTop = this.getCurrentScrollTop();
        this.signType = signType;
        this.forwardButton = $("#forward-btn");
        this.addSignButton = $("#addSignButton");
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        // if(localStorage.getItem("scale") != null) {
        //     this.currentScale = localStorage.getItem("scale");
        // }
        this.initializeSpecialSignImageNumbers();
        this.initListeners();
        this.refreshSteps();
        this.syncAddSignButtonState();
    }

    initializeSpecialSignImageNumbers() {
        let uiMe = null;
        try {
            const rawUiMe = sessionStorage.getItem('uiMe');
            uiMe = rawUiMe ? JSON.parse(rawUiMe) : null;
        } catch (error) {
            console.debug('Unable to parse uiMe session payload', error);
        }
        const specialIndexes = SignatureImageResolver.getSpecialIndexes(this.signImages, uiMe);
        this.generatedSignImageNumber = specialIndexes.generatedSignImageNumber;
        this.parapheSignImageNumber = specialIndexes.parapheSignImageNumber;
    }

    getScrollContainer() {
        return document.getElementById("workspace");
    }

    getCurrentScrollTop() {
        const workspace = this.getScrollContainer();
        return workspace ? workspace.scrollTop : window.scrollY;
    }

    focusWhenVisible(selector, attempts = 10, delay = 60) {
        const tryFocus = remainingAttempts => {
            const element = document.querySelector(selector);
            if (element != null) {
                const style = window.getComputedStyle(element);
                const isVisible = element.getClientRects().length > 0
                    && style.display !== "none"
                    && style.visibility !== "hidden"
                    && !element.disabled;
                if (isVisible) {
                    element.focus({preventScroll: true});
                    return true;
                }
            }
            if (remainingAttempts <= 0) {
                return false;
            }
            window.setTimeout(() => tryFocus(remainingAttempts - 1), delay);
            return false;
        };
        return tryFocus(attempts);
    }

    initListeners() {
        const workspace = this.getScrollContainer();
        if (workspace) {
            this.workspaceScrollHandler = () => {
                this.scrollTop = workspace.scrollTop;
            };
            workspace.removeEventListener('scroll', this.workspaceScrollHandler);
            workspace.addEventListener('scroll', this.workspaceScrollHandler);
        } else {
            $(window).off('scroll' + this.scrollNamespace).on('scroll' + this.scrollNamespace, () => {
                this.scrollTop = $(window).scrollTop();
            });
        }
        $(document).ready(() => {
            if (this.signImages?.length === 1) {
                this.popUserUi();
            }
        });
        [["userSignatureUpdated", this.userSignatureUpdatedHandler], ["userSignatureDeleted", this.userSignatureDeletedHandler]]
            .forEach(([event, handler]) => {
                document.removeEventListener(event, handler);
                document.addEventListener(event, handler);
            });
    }

    normalizeSignImageNumber(signImageNumber) {
        return SignatureImageResolver.normalizeSignImageNumber(signImageNumber);
    }

    async waitForOtpSelection() {
        this.popUserUi();
        return new Promise(resolve => {
            const modalElement = document.getElementById('add-sign-image');
            const selectionHandler = e => {
                cleanup();
                resolve(e.detail || null);
            };
            const hiddenHandler = () => {
                cleanup();
                resolve(null);
            };
            const cleanup = () => {
                document.removeEventListener('userSignatureSelected', selectionHandler);
                modalElement?.removeEventListener('hidden.bs.modal', hiddenHandler);
            };
            document.addEventListener('userSignatureSelected', selectionHandler);
            modalElement?.addEventListener('hidden.bs.modal', hiddenHandler, {once: true});
        });
    }

    prepareVisualPlacement() {
        this.disableForwardButton();
        $(window)
            .off("beforeunload" + this.beforeUnloadNamespace)
            .on("beforeunload" + this.beforeUnloadNamespace, event => {
                console.log("beforeunload déclenché");
                event.preventDefault();
                event.returnValue = "";
            });
        this.addSignButton.removeClass("pulse-success");
        $("#addSignButton2").removeClass("pulse-success");
    }

    getPendingCurrentSignRequestParams(forceSignNumber, signImageNumber, isParaph) {
        if (signImageNumber == null || signImageNumber < 0 || signImageNumber === SPECIAL_SIGN_IMAGE_NUMBERS.SPOT || isParaph) {
            return null;
        }
        if (forceSignNumber != null) {
            return this.currentSignRequestParamses[forceSignNumber];
        }
        return this.currentSignRequestParamses.find(signRequestParams => signRequestParams.ready == null || !signRequestParams.ready) ?? null;
    }

    canAddAnnotations() {
        return this.currentStepMultiSign !== false || this.currentStepSingleSignWithAnnotation !== false;
    }

    setSingleSignInsertionState(id, isParaph) {
        this.signsList.push(id);
        if (isParaph || this.currentStepMultiSign !== false || this.signRequestParamses.size === 0) {
            return;
        }
        if (this.currentStepSingleSignWithAnnotation === false) {
            $('#insert-btn').attr('disabled', 'disabled');
        } else {
            $('#addSignButton').attr('disabled', 'disabled');
        }
    }

    bindSignRequestParamsEvents(signRequestParams, id, signImageNumber, isParaph) {
        signRequestParams.addEventListener("delete", e => this.removeSign(e, id));
        signRequestParams.addEventListener("detachFromSlot", slotIndex => {
            if (Number.isFinite(parseInt(slotIndex, 10)) && this.currentSignRequestParamses?.[slotIndex] != null) {
                this.currentSignRequestParamses[slotIndex].ready = false;
            }
            this.refreshSteps?.();
            this.syncAddSignButtonState();
        });
        signRequestParams.addEventListener("placementStateChanged", () => {
            this.refreshSteps?.();
            this.syncAddSignButtonState();
        });
        signRequestParams.addEventListener("spotSaved", e => this.onSpotSaved(e));
        signRequestParams.addEventListener("spotDeleted", e => this.onSpotDeleted(e));
        if (signImageNumber != null && signImageNumber >= 0 && !isParaph) {
            signRequestParams.cross.addClass("drop-sign");
        }
        if (signImageNumber < 0) {
            $("#signImage_" + id).addClass("d-none");
        }
        if (!isParaph) {
            signRequestParams.addEventListener("sizeChanged", () => signRequestParams.simulateDrop());
        }
    }

    getGeneratedSignImageNumber(userState) {
        return SignatureImageResolver.getSpecialIndexes(userState?.signImages, userState).generatedSignImageNumber;
    }

    getParapheSignImageNumber(userState) {
        return SignatureImageResolver.getSpecialIndexes(userState?.signImages, userState).parapheSignImageNumber;
    }

    applySpecialSignImageNumbers(signRequestParams) {
        if (signRequestParams == null) {
            return;
        }
        signRequestParams.generatedSignImageNumber = this.generatedSignImageNumber;
        signRequestParams.parapheSignImageNumber = this.parapheSignImageNumber;
    }

    applyUserSignatureState(userState) {
        if (!userState) {
            return;
        }
        const previousGeneratedSignImageNumber = this.generatedSignImageNumber;
        const previousParapheSignImageNumber = this.parapheSignImageNumber;
        const displayName = [userState.firstname, userState.name].filter(Boolean).join(' ').trim();
        if (displayName) {
            this.userName = displayName;
            this.authUserName = displayName;
        }
        this.generatedSignImageNumber = this.getGeneratedSignImageNumber(userState);
        this.parapheSignImageNumber = this.getParapheSignImageNumber(userState);
        if (Array.isArray(userState.signImages)) {
            this.signImages = userState.signImages;
            if (this.userUI != null) {
                this.userUI.signImages = userState.signImages;
                this.userUI.userName = displayName || this.userUI.userName;
            }
            this.signRequestParamses.forEach(signRequestParams => {
                const currentSignImageNumber = Number.parseInt(signRequestParams.signImageNumber, 10);
                const mobilePersistedSignImageNumber = Number.parseInt(signRequestParams.mobilePersistedSignImageNumber, 10);
                const isPersistedMobileSignature = Number.isFinite(mobilePersistedSignImageNumber)
                    && currentSignImageNumber === mobilePersistedSignImageNumber;
                if (!isPersistedMobileSignature && Number.isFinite(currentSignImageNumber)) {
                    if (previousGeneratedSignImageNumber != null
                        && currentSignImageNumber === previousGeneratedSignImageNumber
                        && this.generatedSignImageNumber != null) {
                        signRequestParams.signImageNumber = this.generatedSignImageNumber;
                    } else if (previousParapheSignImageNumber != null
                        && currentSignImageNumber === previousParapheSignImageNumber
                        && this.parapheSignImageNumber != null) {
                        signRequestParams.signImageNumber = this.parapheSignImageNumber;
                    }
                }
                signRequestParams.signImages = userState.signImages;
                this.applySpecialSignImageNumbers(signRequestParams);
                if (signRequestParams.signImageNumber != null && signRequestParams.signImageNumber >= 0 && signRequestParams.signImageNumber !== SPECIAL_SIGN_IMAGE_NUMBERS.SPOT) {
                    signRequestParams.changeSignImage(signRequestParams.signImageNumber);
                }
            });
        }
    }

    async persistMobileSignaturePreviews() {
        const persistPromises = [];
        this.signRequestParamses.forEach(signRequestParams => {
            if (typeof signRequestParams.persistMobileSignaturePreviewIfNeeded === "function") {
                persistPromises.push(signRequestParams.persistMobileSignaturePreviewIfNeeded());
            }
        });
        if (persistPromises.length === 0) {
            return null;
        }

        const results = await Promise.all(persistPromises);
        let lastSavedSignImageNumber = null;
        results.forEach(userState => {
            const savedSignImageNumber = Number.parseInt(userState?.savedSignImageNumber, 10);
            if (Number.isFinite(savedSignImageNumber)) {
                lastSavedSignImageNumber = savedSignImageNumber;
            }
        });
        return lastSavedSignImageNumber;
    }

    buildInitialSignRequestParamsModel(currentSignRequestParams, signImageNumber, isParaph) {
        let initialModel = currentSignRequestParams;
        let favoriteSignRequestParams = null;
        if (!isParaph) {
            try {
                const rawFavorite = sessionStorage.getItem("favoriteSignRequestParams");
                favoriteSignRequestParams = rawFavorite ? JSON.parse(rawFavorite) : null;
            } catch (error) {
                console.debug("Unable to parse favorite sign request params", error);
            }
        }
        if (favoriteSignRequestParams != null) {
            initialModel = { ...favoriteSignRequestParams };
            if (currentSignRequestParams != null) {
                initialModel.xPos = currentSignRequestParams.xPos;
                initialModel.yPos = currentSignRequestParams.yPos;
            }
        }
        const normalizedSignImageNumber = Number.parseInt(signImageNumber, 10);
        if (Number.isFinite(normalizedSignImageNumber) && normalizedSignImageNumber >= 0) {
            initialModel = {
                ...(initialModel ?? {}),
                signImageNumber: normalizedSignImageNumber
            };
        }
        return initialModel;
    }

    removeSign(srpId, id) {
        if(srpId != null) {
            this.currentSignRequestParamses[srpId].ready = false;
        }
        this.signRequestParamses.delete(id);
        if(this.signsList.includes(id)) {
            this.signsList.splice(this.signsList.indexOf(id), 1);
        }
        if(this.signsList.length === 0) {
            $('#addSignButton').removeAttr('disabled');
        }
        // Ne pas modifier l'état des étapes si c'est un spot qui est supprimé.
        if(id === SPECIAL_SIGN_IMAGE_NUMBERS.SPOT) {
            return;
        }
        if(this.signRequestParamses.size === 0) {
            let addSignButton2 = $("#addSignButton2");
            addSignButton2.removeClass("d-none");
            addSignButton2.addClass("pulse-success");
            $("#signLaunchButton").removeClass("pulse-success");
            this.focusWhenVisible("#addSignButton2");
            $("#addSignButton").removeAttr("disabled");
            $(window).off("beforeunload" + this.beforeUnloadNamespace);
            this.enableForwardButton();
        }
        this.refreshSteps();
        this.syncAddSignButtonState();
    }

    onSpotSaved(spotData) {
        this.fireEvent("spotSaved", [spotData]);
    }

    onSpotDeleted(spotId) {
        this.fireEvent("spotDeleted", [spotId]);
    }

    updateScales(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        this.currentScale = scale;
        this.signRequestParamses.forEach(function (signRequestParams){
            signRequestParams.updateScale(scale);
        });
    }

    lockSigns() {
        this.signRequestParamses.forEach(function (signRequestParams){
            signRequestParams.lock();
        });
        this.syncAddSignButtonState();
    }

    disableForwardButton() {
        if(this.forwardButton.length) {
            this.forwardButton.addClass("disabled");
        }
    }

    enableForwardButton() {
        if(this.forwardButton.length) {
            this.forwardButton.removeClass("disabled");
        }
    }

    popUserUi() {
        if (this.userUI == null) {
            this.userUI = new UserUi(undefined, undefined, undefined, undefined, undefined, this.signatureUiConfig);
        }
        window.userUi = this.userUI;
        $("#add-sign-image").modal("show");
    }

    async addSign(page, restore, signImageNumber, forceSignNumber) {
        signImageNumber = this.normalizeSignImageNumber(signImageNumber);
        if (this.isOtp || this.signatureStepRequested) {
            const selection = await this.waitForOtpSelection();
            if (this.signatureStepRequested) {
                this.signatureStepRequested = false;
            }
            const selectedSignImageNumber = selection?.selectedSignImageNumber != null ? parseInt(selection.selectedSignImageNumber, 10) : null;
            if (selectedSignImageNumber == null || Number.isNaN(selectedSignImageNumber)) {
                this.refreshSteps?.();
                return;
            }
            this.applyUserSignatureState(selection);
            signImageNumber = selectedSignImageNumber;
        }
        const isSpot = signImageNumber === SPECIAL_SIGN_IMAGE_NUMBERS.SPOT;
        const isParaph = signImageNumber === SPECIAL_SIGN_IMAGE_NUMBERS.PARAPHE;
        const isVisaPlacement = this.signType === "visa" && !isParaph;
        if (!isSpot && !isParaph) {
            this.prepareVisualPlacement();
        }
        const id = signImageNumber === SPECIAL_SIGN_IMAGE_NUMBERS.SPOT ? SPECIAL_SIGN_IMAGE_NUMBERS.SPOT : this.id;
        const currentSignRequestParams = this.getPendingCurrentSignRequestParams(forceSignNumber, signImageNumber, isParaph);
        const initialSignRequestParamsModel = signImageNumber == null ? null : this.buildInitialSignRequestParamsModel(currentSignRequestParams, signImageNumber, isParaph);
        let signRequestParams = null;

        if (signImageNumber === SPECIAL_SIGN_IMAGE_NUMBERS.SPOT) {
            signRequestParams = new SignRequestParams(this.isOtp, null, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, false, null, false, signImageNumber, this.scrollTop, this.csrf, this.signType, this.signatureUiConfig, this.signRequestId);
            signRequestParams.changeSignSize({
                w: signRequestParams.originalWidth,
                h: signRequestParams.originalHeight
            });
        } else if (signImageNumber != null && signImageNumber >= 0) {
            if (!isParaph && this.currentStepMultiSign === false && this.signsList.length > 0) {
                alert("Impossible d'ajouter plusieurs signatures sur cette étape");
                return;
            }
            signRequestParams = new SignRequestParams(this.isOtp, isParaph ? null : initialSignRequestParamsModel, id, this.currentScale, isParaph ? 1 : page, this.userName, this.authUserName, restore, true, isVisaPlacement, this.isOtp, this.phone, false, this.signImages, this.scrollTop, this.csrf, this.signType, this.signatureUiConfig, this.signRequestId);
            if (!isParaph) {
                this.setSingleSignInsertionState(id, isParaph);
            }
        } else {
            if (!this.canAddAnnotations()) {
                alert("Impossible d'ajouter des annotations sur cette étape");
                return;
            }
            signRequestParams = new SignRequestParams(this.isOtp, initialSignRequestParamsModel, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, this.isOtp, this.phone, false, null, this.scrollTop, this.csrf, this.signType, this.signatureUiConfig, this.signRequestId);
        }

        this.signRequestParamses.set(id, signRequestParams);
        this.applySpecialSignImageNumbers(signRequestParams);
        this.bindSignRequestParamsEvents(signRequestParams, id, signImageNumber, isParaph);
        this.syncAddSignButtonState();

        if (signImageNumber != null && signImageNumber !== SPECIAL_SIGN_IMAGE_NUMBERS.SPOT && (!isVisaPlacement || isParaph)) {
            await signRequestParams.changeSignImage(signImageNumber);
            if (!restore && typeof signRequestParams.syncExtraLayoutFromState === "function") {
                signRequestParams.syncExtraLayoutFromState();
            }
            if (currentSignRequestParams == null && typeof signRequestParams.centerOnCurrentViewport === "function") {
                signRequestParams.centerOnCurrentViewport();
            }
        }

        this.id++;
        if (!isSpot && !isParaph) {
            this.refreshSteps();
            this.syncAddSignButtonState();
        }
        return signRequestParams;
    }

    addCheckImage(page) {
        this.addSign(page, false, -1);
    }

    addTimesImage(page) {
        this.addSign(page, false, -2);
    }

    addCircleImage(page) {
        this.addSign(page, false, -3);
    }

    addMinusImage(page) {
        this.addSign(page, false, -4);
    }

    async addText(page) {
        let signRequestParams = await this.addSign(page, false, null);
        if(signRequestParams != null) {
            signRequestParams.turnToText();
            signRequestParams.cross.css("background-image", "");
            signRequestParams.changeSignSize(null);
            signRequestParams.textareaPart.focus();
        }
    }

    getBrowserZoom() {
        return window.devicePixelRatio || 1;
    }

    setStepState(step, active, complete, disabled) {
        step.toggleClass("active", active);
        step.toggleClass("complete", complete);
        step.toggleClass("disable", disabled);
    }

    setButtonVariant(button, activeClass) {
        button.removeClass("btn-secondary btn-success btn-danger");
        button.addClass(activeClass);
    }

    getStepUiElements() {
        return {
            step1: $("#step-1"),
            step2: $("#step-2"),
            addSignButton2: $("#addSignButton2, #drawSignButton"),
            insertBtn: $("#insert-btn"),
            refuseLaunchButton: $("#refuseLaunchButton"),
            signLaunchButton: $("#signLaunchButton"),
            signAdvancedLaunchButton: $("#signAdvancedLaunchButton"),
            refuseLaunchDiv: $("#refuseLaunchDiv"),
            refuseLaunchDivResponsive: $("#refuseLaunchDivResponsive")
        };
    }

    getActiveSigns() {
        return Array.from(this.signRequestParamses.values()).filter(signRequestParams => {
            const signImageNumber = signRequestParams?.signImageNumber == null
                ? null
                : Number.parseInt(signRequestParams.signImageNumber, 10);
            return signRequestParams != null
                && signRequestParams.isSign
                && signImageNumber != null
                && signImageNumber >= 0
                && signImageNumber !== SPECIAL_SIGN_IMAGE_NUMBERS.SPOT;
        });
    }

    hasStartedSignaturePlacement() {
        if (Array.isArray(this.signsList) && this.signsList.length > 0) {
            return true;
        }
        return this.getActiveSigns().length > 0;
    }

    getPlacedSignatureCount() {
        return this.getActiveSigns().length;
    }

    syncAddSignButtonState() {
        const addSignButton2 = $("#addSignButton2");
        if (!addSignButton2.length) {
            return;
        }
        const count = this.getPlacedSignatureCount();
        const hasSignature = count > 0;
        const label = hasSignature ? "Ajouter une autre signature" : "Insérer une signature";
        addSignButton2.find(".es-add-sign-button-label").text(label);
        addSignButton2
            .attr("aria-label", hasSignature
                ? `${label}. ${count} signature${count > 1 ? "s" : ""} en place.`
                : label);
        const countBadge = addSignButton2.find("#addSignButton2Count");
        countBadge.toggleClass("d-none", !hasSignature);
        countBadge.find(".es-add-sign-count").text(count);
        countBadge.find(".visually-hidden").text(` signature${count > 1 ? "s" : ""} en place`);
    }

    hasPendingSignaturePlacement() {
        const activeSigns = this.getActiveSigns();

        if (activeSigns.length === 0) {
            return false;
        }

        const hasInvalidPlacement = activeSigns.some(signRequestParams => signRequestParams.inside === false);
        if (hasInvalidPlacement) {
            return false;
        }

        const currentSignRequestParamses = Array.isArray(this.currentSignRequestParamses)
            ? this.currentSignRequestParamses
            : [];

        if (currentSignRequestParamses.length > 0) {
            return currentSignRequestParamses.every(signRequestParams => signRequestParams?.ready === true);
        }

        return true;
    }

    isSignatureActionReady() {
        return this.isHiddenVisa() || !this.showPlacementStep || this.hasPendingSignaturePlacement();
    }

    syncSignatureActionButtons(forceEnabled = null, skipFocus = false) {
        const {
            signLaunchButton,
            signAdvancedLaunchButton
        } = this.getStepUiElements();

        const enabled = forceEnabled == null
            ? this.isSignatureActionReady()
            : forceEnabled;

        signLaunchButton.prop("disabled", false);
        signAdvancedLaunchButton.prop("disabled", false);
        this.setButtonVariant(signLaunchButton, enabled ? "btn-success" : "btn-secondary");
        this.setButtonVariant(signAdvancedLaunchButton, enabled ? "btn-success" : "btn-secondary");
        if(enabled && !skipFocus) {
            signLaunchButton.focus();
        }
    }

    isHiddenVisa() {
        return this.signType === "hiddenVisa";
    }

    requestSignatureStep() {
        if (this.isHiddenVisa() || !this.showPlacementStep) {
            this.goStep2({singleVisibleStep: true});
            return;
        }
        this.signatureStepRequested = true;
        this.goStep2();
    }

    clearRequestedSignatureStep() {
        this.signatureStepRequested = false;
    }

    refreshSteps() {
        if (this.isHiddenVisa() || !this.showPlacementStep) {
            this.goStep2({singleVisibleStep: true});
            return;
        }

        if (this.hasStartedSignaturePlacement()) {
            this.signatureStepRequested = false;
            this.goStep2();
            return;
        }

        if (this.signatureStepRequested) {
            this.goStep2();
            return;
        }

        this.goStep1();
    }

    goStep1() {
        if (!this.showPlacementStep) {
            this.goStep2({singleVisibleStep: true});
            return;
        }
        const {
            step1,
            step2,
            addSignButton2,
            insertBtn,
            refuseLaunchButton,
            refuseLaunchDiv,
            refuseLaunchDivResponsive
        } = this.getStepUiElements();

        addSignButton2.removeAttr("disabled");
        insertBtn.removeAttr("disabled");
        refuseLaunchButton.removeAttr("disabled");
        refuseLaunchDiv.removeClass("d-none es-refuse-slot-hidden");
        refuseLaunchDivResponsive.removeClass("d-none");

        this.setButtonVariant(addSignButton2, "btn-success");
        addSignButton2.addClass("pulse-success");
        this.setButtonVariant(insertBtn, "btn-success");
        this.setButtonVariant(refuseLaunchButton, "btn-danger");
        this.syncSignatureActionButtons(false, true);

        this.setStepState(step1, true, false, false);
        this.setStepState(step2, false, false, true);
        this.syncAddSignButtonState();

        step1.find(".step-horizontal-v2-icon").html("1");
        step2.find(".step-horizontal-v2-icon").html("2");
    }

    goStep2({singleVisibleStep = false} = {}) {
        const {
            step1,
            step2,
            addSignButton2,
            insertBtn,
            refuseLaunchButton,
            refuseLaunchDiv,
            refuseLaunchDivResponsive
        } = this.getStepUiElements();

        addSignButton2.removeAttr("disabled");
        refuseLaunchButton.removeAttr("disabled");
        insertBtn.removeAttr("disabled");
        refuseLaunchDiv.removeClass("d-none");
        refuseLaunchDiv.addClass("es-refuse-slot-hidden");
        refuseLaunchDivResponsive.addClass("d-none");

        this.setButtonVariant(addSignButton2, "btn-success");
        addSignButton2.removeClass("pulse-success");
        this.syncAddSignButtonState();
        this.setButtonVariant(insertBtn, "btn-success");
        this.setButtonVariant(refuseLaunchButton, "btn-secondary");
        this.syncSignatureActionButtons(null, singleVisibleStep);

        if (this.isHiddenVisa() || singleVisibleStep) {
            this.setStepState(step1, false, false, true);
            refuseLaunchDiv.removeClass("es-refuse-slot-hidden");
            refuseLaunchDivResponsive.removeClass("d-none");
            this.setButtonVariant(refuseLaunchButton, "btn-danger");
            this.setStepState(step2, true, false, false);
            step2.find(".step-horizontal-v2-icon").html("1");
            return;
        }

        this.setStepState(step1, false, true, false);
        this.setStepState(step2, true, false, false);
        step1.find(".step-horizontal-v2-icon").html("<i class='fi fi-rr-check'></i>");
        step2.find(".step-horizontal-v2-icon").html("2");
    }

    destroy() {
        const workspace = this.getScrollContainer();
        if (workspace != null && this.workspaceScrollHandler != null) {
            workspace.removeEventListener('scroll', this.workspaceScrollHandler);
        }
        $(window).off('scroll' + this.scrollNamespace);
        $(window).off('beforeunload' + this.beforeUnloadNamespace);
        document.removeEventListener('userSignatureUpdated', this.userSignatureUpdatedHandler);
        document.removeEventListener('userSignatureDeleted', this.userSignatureDeletedHandler);
    }

}
