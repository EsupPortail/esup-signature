import {SignRequestParams} from "../../../prototypes/SignRequestParams.js?version=@version@";
import {EventFactory} from "../../utils/EventFactory.js?version=@version@";
import {UserUi} from '../users/UserUi.js?version=@version@';

export class SignPlacementController extends EventFactory {

    constructor(signType, currentSignRequestParamses, currentStepMultiSign, currentStepSingleSignWithAnnotation, signImageNumber, signImages, userName, authUserName, signable, forceResetSignPos, isOtp, phone, csrf, signatureUiConfig = null) {
        super();
        console.info("Starting sign positioning tools");
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
        this.currentScale;
        this.scrollTop = this.getCurrentScrollTop();
        this.signType = signType;
        this.forwardButton = $("#forward-btn");
        this.addSignButton = $("#addSignButton");
        $("#signLaunchButton").focus();
        $("#addSignButton2").focus();
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        // if(localStorage.getItem("scale") != null) {
        //     this.currentScale = localStorage.getItem("scale");
        // }
        this.initListeners();
        this.refreshSteps();
    }

    getScrollContainer() {
        return document.getElementById("workspace");
    }

    getCurrentScrollTop() {
        const workspace = this.getScrollContainer();
        return workspace ? workspace.scrollTop : window.scrollY;
    }

    initListeners() {
        let self = this;
        const workspace = this.getScrollContainer();
        if (workspace) {
            this.workspaceScrollHandler = () => {
                self.scrollTop = workspace.scrollTop;
            };
            workspace.removeEventListener('scroll', this.workspaceScrollHandler);
            workspace.addEventListener('scroll', this.workspaceScrollHandler);
        } else {
            $(window).off('scroll' + this.scrollNamespace).on('scroll' + this.scrollNamespace, function() {
                self.scrollTop = $(this).scrollTop();
            });
        }
        $(document).ready(function() {
            if(self.signImages != null && self.signImages.length === 1) {
                self.popUserUi();
            }
        });
        document.removeEventListener('userSignatureUpdated', this.userSignatureUpdatedHandler);
        document.removeEventListener('userSignatureDeleted', this.userSignatureDeletedHandler);
        document.addEventListener('userSignatureUpdated', this.userSignatureUpdatedHandler);
        document.addEventListener('userSignatureDeleted', this.userSignatureDeletedHandler);
    }

    getGeneratedSignImageNumber(userState) {
        const signImageIds = Array.isArray(userState?.signImageIds) ? userState.signImageIds : [];
        const signImages = Array.isArray(userState?.signImages) ? userState.signImages : [];
        return signImages.length > signImageIds.length ? signImageIds.length : null;
    }

    getParapheSignImageNumber(userState) {
        const generatedSignImageNumber = this.getGeneratedSignImageNumber(userState);
        const signImages = Array.isArray(userState?.signImages) ? userState.signImages : [];
        return generatedSignImageNumber != null && signImages.length > generatedSignImageNumber + 1
            ? generatedSignImageNumber + 1
            : null;
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
                signRequestParams.signImages = userState.signImages;
                this.applySpecialSignImageNumbers(signRequestParams);
                if (signRequestParams.signImageNumber != null && signRequestParams.signImageNumber >= 0 && signRequestParams.signImageNumber !== 999999) {
                    signRequestParams.changeSignImage(signRequestParams.signImageNumber);
                }
            });
        }
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
        // Ne pas modifier l'état des étapes si c'est un spot (999999) qui est supprimé.
        if(id === 999999) {
            return;
        }
        if(this.signRequestParamses.size === 0) {
            let addSignButton2 = $("#addSignButton2");
            addSignButton2.removeClass("d-none");
            addSignButton2.addClass("pulse-success");
            $("#signLaunchButton").removeClass("pulse-success");
            addSignButton2.focus();
            $("#addSignButton").removeAttr("disabled");
            $(window).off("beforeunload" + this.beforeUnloadNamespace);
            this.enableForwardButton();
        }
        this.refreshSteps();
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
        $("#add-sign-image").modal("show");
    }

    async addSign(page, restore, signImageNumber, forceSignNumber) {
        const normalizedSignImageNumber = signImageNumber == null ? null : Number.parseInt(signImageNumber, 10);
        if (signImageNumber != null && Number.isFinite(normalizedSignImageNumber)) {
            signImageNumber = normalizedSignImageNumber;
        }
        if (this.isOtp) {
            this.popUserUi();
            const selection = await new Promise((resolve) => {
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
                    if (modalElement) {
                        modalElement.removeEventListener('hidden.bs.modal', hiddenHandler);
                    }
                };
                document.addEventListener('userSignatureSelected', selectionHandler);
                if (modalElement) {
                    modalElement.addEventListener('hidden.bs.modal', hiddenHandler, { once: true });
                }
            });
            const selectedSignImageNumber = selection?.selectedSignImageNumber != null ? parseInt(selection.selectedSignImageNumber, 10) : null;
            if (selectedSignImageNumber == null || Number.isNaN(selectedSignImageNumber)) {
                return;
            }
            this.applyUserSignatureState(selection);
            signImageNumber = selectedSignImageNumber;
        }
        const isSpot = signImageNumber === 999999;
        const isParaph = signImageNumber === 999997;
        if (!isSpot && !isParaph) {
            this.disableForwardButton();
            $(window)
                .off("beforeunload" + this.beforeUnloadNamespace)
                .on("beforeunload" + this.beforeUnloadNamespace, function (event) {
                console.log("beforeunload déclenché");
                event.preventDefault();
                event.returnValue = "";
            });
            this.addSignButton.removeClass("pulse-success");
            $("#addSignButton2").removeClass("pulse-success");
        }
        let id = this.id;
        let currentSignRequestParams = null;
        if(signImageNumber != null && signImageNumber >= 0 && signImageNumber !== 999999 && !isParaph) {
            if(forceSignNumber != null) {
                currentSignRequestParams = this.currentSignRequestParamses[forceSignNumber];
            } else {
                for (let i = 0; i < this.currentSignRequestParamses.length; i++) {
                    if (this.currentSignRequestParamses[i].ready == null || !this.currentSignRequestParamses[i].ready) {
                        currentSignRequestParams = this.currentSignRequestParamses[i];
                        break;
                    }
                }
            }
        }
        if(signImageNumber != null) {
            let favoriteSignRequestParams = currentSignRequestParams;
            if (signImageNumber === 999999) {
                id = 999999;
                this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, null, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, false, null, false, signImageNumber, this.scrollTop, this.csrf, this.signType, this.signatureUiConfig));
                    this.applySpecialSignImageNumbers(this.signRequestParamses.get(id));
                this.signRequestParamses.get(id).addEventListener("sizeChanged", e => this.signRequestParamses.get(id).simulateDrop());
                this.signRequestParamses.get(id).changeSignSize(null);

            } else if(signImageNumber >= 0) {
                if(!isParaph && this.currentStepMultiSign === false && this.signsList.length > 0) {
                    alert("Impossible d'ajouter plusieurs signatures sur cette étape");
                    return;
                }
                if(!isParaph && JSON.parse(sessionStorage.getItem("favoriteSignRequestParams")) != null) {
                    favoriteSignRequestParams = JSON.parse(sessionStorage.getItem("favoriteSignRequestParams"));
                    if(currentSignRequestParams != null) {
                        favoriteSignRequestParams.xPos = currentSignRequestParams.xPos;
                        favoriteSignRequestParams.yPos = currentSignRequestParams.yPos;
                    }
                }
                this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, isParaph ? null : favoriteSignRequestParams, id, this.currentScale, isParaph ? 1 : page, this.userName, this.authUserName, restore, true, this.signType === "visa", this.isOtp, this.phone, false, this.signImages, this.scrollTop, this.csrf, this.signType, this.signatureUiConfig));
                this.applySpecialSignImageNumbers(this.signRequestParamses.get(id));
                if(!isParaph) {
                    this.signsList.push(id);
                }
                if(!isParaph && this.currentStepMultiSign === false && this.signRequestParamses.size > 0) {
                    if(this.currentStepSingleSignWithAnnotation === false) {
                        $('#insert-btn').attr('disabled', 'disabled');
                    } else {
                        $('#addSignButton').attr('disabled', 'disabled');
                    }
                }
            } else {
                if(this.currentStepMultiSign === false && this.currentStepSingleSignWithAnnotation === false) {
                    alert("Impossible d'ajouter des annotations sur cette étape");
                    return;
                }
                this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, favoriteSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, this.isOtp, this.phone, false, null, this.scrollTop, this.csrf, this.signType, this.signatureUiConfig));
                this.applySpecialSignImageNumbers(this.signRequestParamses.get(id));
            }
            if(signImageNumber !== 999999) {
                if(this.signType !== "visa") {
                    this.signRequestParamses.get(id).changeSignImage(signImageNumber);
                }
            }
        } else {
            if(this.currentStepMultiSign === false && this.currentStepSingleSignWithAnnotation === false) {
                alert("Impossible d'ajouter des annotations sur cette étape");
                return;
            }
            this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, null, id, this.currentScale, page, this.userName, this.authUserName, restore, signImageNumber != null && signImageNumber >= 0, false, this.isOtp, this.phone, false, null, this.scrollTop, this.csrf, this.signType, this.signatureUiConfig));
            this.applySpecialSignImageNumbers(this.signRequestParamses.get(id));
        }
        this.signRequestParamses.get(id).addEventListener("delete", e => this.removeSign(e, id));
        this.signRequestParamses.get(id).addEventListener("spotSaved", e => this.onSpotSaved(e));
        this.signRequestParamses.get(id).addEventListener("spotDeleted", e => this.onSpotDeleted(e));
        if (signImageNumber != null && signImageNumber >= 0 && !isParaph) {
            this.signRequestParamses.get(id).cross.addClass("drop-sign");
        }
        if (signImageNumber < 0) {
            $("#signImage_" + id).addClass("d-none");
        }
        if (!isParaph) {
            this.signRequestParamses.get(id).addEventListener("sizeChanged", e => this.signRequestParamses.get(id).simulateDrop());
        }
        let srp = this.signRequestParamses.get(id);
        this.id++;
        if (!isSpot && !isParaph) {
            this.refreshSteps();
        }
        return srp;
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

    setCertTypeHighlight(isHighlighted) {
        let selectCertType = $("#certType");
        selectCertType.toggleClass("border-success", isHighlighted);
        selectCertType.toggleClass("border-light-subtle", !isHighlighted);
        if(!isHighlighted) {
            selectCertType.trigger("blur");
        }
    }

    getSelectableCertTypeOptions() {
        return $("#certType").find("option:not(:disabled):not([unavailable])");
    }

    hasValidSelectedCertType() {
        const selectCertType = $("#certType");
        const selectedOption = selectCertType.find("option:selected");
        const value = selectCertType.val();
        return value != null
            && value !== ""
            && selectedOption.length > 0
            && !selectedOption.is(":disabled")
            && !selectedOption.is("[unavailable]");
    }

    hasPendingSignaturePlacement() {
        const activeSigns = Array.from(this.signRequestParamses.values()).filter(signRequestParams =>
            {
                const signImageNumber = signRequestParams?.signImageNumber == null
                    ? null
                    : Number.parseInt(signRequestParams.signImageNumber, 10);
                return signRequestParams != null
                    && signRequestParams.isSign
                    && signImageNumber != null
                    && signImageNumber >= 0
                    && signImageNumber !== 999999;
            }
        );

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

    refreshSteps() {
        const selectCertType = $("#certType");
        if (!selectCertType.length) {
            return;
        }

        const selectableOptions = this.getSelectableCertTypeOptions();
        if (selectableOptions.length === 1 && !this.hasValidSelectedCertType()) {
            selectableOptions.prop("selected", true);
            selectCertType.trigger("change");
        }

        const countVisible = selectCertType.find("option:not([unavailable])").length;
        if (countVisible > 0) {
            selectCertType.removeAttr("disabled");
        }

        if (!this.hasValidSelectedCertType()) {
            this.goStep1();
            selectCertType.trigger("focus");
            return;
        }

        if (this.hasPendingSignaturePlacement()) {
            this.goStep3();
            return;
        }

        this.goStep2();
    }

    goStep1() {
        let step1 = $("#step-1");
        let step2 = $("#step-2");
        let step3 = $("#step-3");
        let addSignButton = $("#addSignButton");
        let addSignButton2 = $("#addSignButton2");
        let addParaphButton2 = $("#addParaphButton2");
        let insertBtn = $("#insert-btn");
        let refuseLaunchButton = $("#refuseLaunchButton");
        let signLaunchButton = $("#signLaunchButton");
        let refuseLaunchDiv = $("#refuseLaunchDiv");
        let selectCertType = $("#certType");

        selectCertType.removeAttr("disabled");
        addSignButton.attr("disabled", "disabled");
        addSignButton2.attr("disabled", "disabled");
        addParaphButton2.attr("disabled", "disabled");
        insertBtn.removeAttr("disabled");
        refuseLaunchButton.removeAttr("disabled");
        signLaunchButton.attr("disabled", "disabled");
        refuseLaunchDiv.removeClass("d-none");

        this.setButtonVariant(addSignButton2, "btn-secondary");
        this.setButtonVariant(insertBtn, "btn-success");
        this.setButtonVariant(refuseLaunchButton, "btn-danger");
        this.setButtonVariant(signLaunchButton, "btn-secondary");
        this.setCertTypeHighlight(true);

        this.setStepState(step1, true, false, false);
        this.setStepState(step2, false, false, true);
        this.setStepState(step3, false, false, true);

        step1.find(".step-horizontal-v2-icon").html("1");
        step2.find(".step-horizontal-v2-icon").html("2");
        step3.find(".step-horizontal-v2-icon").html("3");
    }

    goStep2() {
        let step1 = $("#step-1");
        let step2 = $("#step-2");
        let step3 = $("#step-3");
        let addSignButton = $("#addSignButton");
        let addSignButton2 = $("#addSignButton2");
        let addParaphButton2 = $("#addParaphButton2");
        let insertBtn = $("#insert-btn");
        let refuseLaunchButton = $("#refuseLaunchButton");
        let signLaunchButton = $("#signLaunchButton");
        let refuseLaunchDiv = $("#refuseLaunchDiv");
        let selectCertType = $("#certType");

        selectCertType.removeAttr("disabled");
        addSignButton.removeAttr("disabled");
        addSignButton2.removeAttr("disabled");
        addParaphButton2.removeAttr("disabled");
        refuseLaunchButton.removeAttr("disabled");
        insertBtn.removeAttr("disabled");
        refuseLaunchDiv.removeClass("d-none");

        this.setButtonVariant(addSignButton2, "btn-success");
        this.setButtonVariant(insertBtn, "btn-success");
        this.setButtonVariant(refuseLaunchButton, "btn-danger");
        this.setButtonVariant(signLaunchButton, "btn-secondary");

        this.setStepState(step1, false, true, false);
        this.setStepState(step2, true, false, false);
        this.setStepState(step3, false, false, true);

        this.setCertTypeHighlight(false);
        step1.find(".step-horizontal-v2-icon").html("<i class='fi fi-rr-check'></i>");
        step2.find(".step-horizontal-v2-icon").html("2");
        step3.find(".step-horizontal-v2-icon").html("3");

    }

    goStep3() {
        let step1 = $("#step-1");
        let step2 = $("#step-2");
        let step3 = $("#step-3");
        let addSignButton = $("#addSignButton2");
        let insertBtn = $("#insert-btn");
        let refuseLaunchButton = $("#refuseLaunchButton");
        let signLaunchButton = $("#signLaunchButton");
        let refuseLaunchDiv = $("#refuseLaunchDiv");
        let selectCertType = $("#certType");

        this.setStepState(step1, false, true, false);
        this.setStepState(step2, false, true, false);
        this.setStepState(step3, true, false, false);

        selectCertType.removeAttr("disabled");
        addSignButton.attr("disabled", "disabled");
        insertBtn.removeAttr("disabled");
        refuseLaunchButton.attr("disabled", "disabled");
        signLaunchButton.removeAttr("disabled");
        refuseLaunchDiv.addClass("d-none");

        this.setButtonVariant(addSignButton, "btn-secondary");
        this.setButtonVariant(insertBtn, "btn-success");
        this.setButtonVariant(refuseLaunchButton, "btn-secondary");
        this.setButtonVariant(signLaunchButton, "btn-success");
        this.setCertTypeHighlight(false);

        step1.find(".step-horizontal-v2-icon").html("<i class='fi fi-rr-check'></i>");
        step2.find(".step-horizontal-v2-icon").html("<i class='fi fi-rr-check'></i>");
        step3.find(".step-horizontal-v2-icon").html("3");
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
