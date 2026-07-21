export class SignatureFlowController {

    constructor(signUi) {
        this.signUi = signUi;
        this.state = signUi.state;
        this.contextualPassword = "";
        this.contextualSignAll = false;
        this.gotoNextStorageKey = "esup-signature.gotoNext";
        this.gotoNextToolbarToggleSelector = ".js-sign-goto-next-switch";
        this.removedImageStampOption = null;
        this.skipRestoreOnNextSignModalHide = false;
        this.initGotoNextToolbarPreferenceBinding();
        this.initSignModalLifecycle();
        this.initRefuseModalLifecycle();
    }

    getStoredGotoNextPreference() {
        try {
            return window.localStorage.getItem(this.gotoNextStorageKey) === "true";
        } catch (error) {
            console.debug("Unable to read gotoNext preference", error);
            return false;
        }
    }

    storeGotoNextPreference(value) {
        try {
            window.localStorage.setItem(this.gotoNextStorageKey, String(Boolean(value)));
        } catch (error) {
            console.debug("Unable to persist gotoNext preference", error);
        }
    }

    syncGotoNextPreference(checkboxSelector, value = this.getStoredGotoNextPreference()) {
        const gotoNextCheckbox = $(checkboxSelector);
        if (!gotoNextCheckbox.length) {
            return;
        }
        gotoNextCheckbox.prop("checked", value);
    }

    syncGotoNextToolbarButtons(value = this.getStoredGotoNextPreference()) {
        const gotoNextButtons = $(this.gotoNextToolbarToggleSelector);
        if (!gotoNextButtons.length) {
            return;
        }

        const title = value
            ? "Désactiver l'ouverture automatique de la demande suivante"
            : "Activer l'ouverture automatique de la demande suivante";

        gotoNextButtons
            .toggleClass("active", value)
            // .toggleClass("text-primary", value)
            .toggleClass("text-dark", !value)
            .attr("aria-pressed", String(value))
            .attr("title", title);
    }

    syncAllGotoNextPreferences(value = this.getStoredGotoNextPreference()) {
        this.syncGotoNextPreference("#signGotoNext", value);
        this.syncGotoNextToolbarButtons(value);
    }

    initGotoNextToolbarPreferenceBinding() {
        const gotoNextButtons = $(this.gotoNextToolbarToggleSelector);
        if (!gotoNextButtons.length) {
            return;
        }

        this.syncGotoNextToolbarButtons();
        gotoNextButtons.off("click.signatureFlowController");
        gotoNextButtons.on("click.signatureFlowController", event => {
            event.preventDefault();
            const nextValue = !this.getStoredGotoNextPreference();
            this.storeGotoNextPreference(nextValue);
            this.syncAllGotoNextPreferences(nextValue);
        });
    }

    initGotoNextPreferenceBinding(checkboxSelector, onChange = null) {
        const gotoNextCheckbox = $(checkboxSelector);
        if (!gotoNextCheckbox.length) {
            return;
        }

        this.syncGotoNextPreference(checkboxSelector);
        gotoNextCheckbox.off("change.signatureFlowController");
        gotoNextCheckbox.on("change.signatureFlowController", event => {
            const isChecked = $(event.currentTarget).is(':checked');
            this.storeGotoNextPreference(isChecked);
            this.syncAllGotoNextPreferences(isChecked);
            if (typeof onChange === "function") {
                onChange(isChecked, $(event.currentTarget));
            }
        });

        if (typeof onChange === "function") {
            onChange(gotoNextCheckbox.is(':checked'), gotoNextCheckbox);
        }
    }

    initSignModalLifecycle() {
        const signModal = $("#signModal");
        if (!signModal.length) {
            return;
        }

        signModal.off(".signatureFlowController");
        signModal.on('shown.bs.modal.signatureFlowController', () => {
            this.initGotoNextPreferenceBinding("#signGotoNext");
            const certType = $("#certType");
            const signComment = $("#signComment");
            const launchNoInfiniteSignButton = $("#launchNoInfiniteSignButton");
            const enableInfinite = $("#enableInfinite");
            const checkValidateAdvancedSignButton = $("#checkValidateAdvancedSignButton");
            const checkValidateSignButtonEnd = $("#checkValidateSignButtonEnd");
            const checkValidateSignButtonNext = $("#checkValidateSignButtonNext");

            if (certType.length && !certType.val()) {
                certType.trigger('focus');
                return;
            }
            if (signComment.length) {
                signComment.trigger('focus');
                return;
            }
            if (enableInfinite.length) {
                enableInfinite.trigger('focus');
                return;
            }
            if (launchNoInfiniteSignButton.length) {
                launchNoInfiniteSignButton.trigger('focus');
                return;
            }
            if (checkValidateAdvancedSignButton.length) {
                checkValidateAdvancedSignButton.trigger('focus');
                return;
            }
            if (checkValidateSignButtonNext.length) {
                checkValidateSignButtonNext.trigger('focus');
                return;
            }
            if (checkValidateSignButtonEnd.length) {
                checkValidateSignButtonEnd.trigger('focus');
            }
        });
        signModal.on('hidden.bs.modal.signatureFlowController', () => {
            if (this.skipRestoreOnNextSignModalHide) {
                this.skipRestoreOnNextSignModalHide = false;
                return;
            }
            this.restoreRemovedImageStampOption();
        });
    }

    initRefuseModalLifecycle() {
        const refuseModal = $("#refuseModal");
        if (!refuseModal.length) {
            return;
        }

        const syncRefuseRedirectValue = isChecked => {
            const refuseRedirectValue = $("#refuseRedirectValue");
            const refuseGotoNext = $("#refuseGotoNext");
            if (!refuseRedirectValue.length) {
                return;
            }

            const nextId = refuseGotoNext.attr("data-es-next-id");
            refuseRedirectValue.val(isChecked && nextId ? nextId : "end");
        };

        refuseModal.off("shown.bs.modal.signatureFlowController");
        refuseModal.on("shown.bs.modal.signatureFlowController", () => {
            this.initGotoNextPreferenceBinding("#refuseGotoNext", syncRefuseRedirectValue);
            syncRefuseRedirectValue($("#refuseGotoNext").is(':checked'));
        });

        refuseModal.find("form").off("submit.signatureFlowController");
        refuseModal.find("form").on("submit.signatureFlowController", () => {
            const refuseGotoNext = $("#refuseGotoNext");
            if (refuseGotoNext.length) {
                this.storeGotoNextPreference(refuseGotoNext.is(':checked'));
            }
            syncRefuseRedirectValue(refuseGotoNext.is(':checked'));
        });
    }

    launchQuickSign() {
        const requiresPanel = this.signUi.currentSignType !== 'hiddenVisa'
            && typeof this.signUi.hasValidSelectedCertType === 'function'
            && !this.signUi.hasValidSelectedCertType();
        this.signUi.signComment = $();
        return this.prepareLaunchSign(Boolean(requiresPanel));
    }

    removeImageStampOptionTemporarily() {
        const certTypeSelect = this.signUi.certTypeSelect;
        if (!certTypeSelect.length || this.removedImageStampOption != null) {
            return;
        }

        const imageStampOption = certTypeSelect.find("option[value='imageStamp']").first();
        if (!imageStampOption.length) {
            return;
        }

        this.removedImageStampOption = {
            text: imageStampOption.text() || this.signUi.saveOptionText,
            wasSelected: certTypeSelect.val() === 'imageStamp'
        };

        imageStampOption.remove();
        if (this.removedImageStampOption.wasSelected) {
            certTypeSelect.val("");
        }

        this.signUi.checkSignOptions();
        this.signUi.syncSignatureStepUi();
    }

    restoreRemovedImageStampOption() {
        const removedImageStampOption = this.removedImageStampOption;
        if (removedImageStampOption == null) {
            return;
        }

        const certTypeSelect = this.signUi.certTypeSelect;
        if (certTypeSelect.length && certTypeSelect.find("option[value='imageStamp']").length === 0) {
            certTypeSelect.prepend($('<option>', {
                value: 'imageStamp',
                text: removedImageStampOption.text
            }));
        }

        this.removedImageStampOption = null;
        this.signUi.checkSignOptions();

        if (removedImageStampOption.wasSelected) {
            certTypeSelect.val('imageStamp');
            this.signUi.checkAfterChangeSignType();
            return;
        }

        this.signUi.syncSignatureStepUi();
        this.signUi.updateMobileCertTypeVisibility();
    }

    prepareLaunchSign(forcePanel = false) {
        const signUi = this.signUi;
        const signPlacementController = signUi.workspace?.signPlacementController;
        if (signPlacementController?.hasInvalidSignaturePlacement?.()) {
            bootbox.alert("Merci de placer la signature entierement dans une page avant de signer");
            signPlacementController.refreshSteps?.();
            return;
        }
        this.setContextualSignAll(signUi.nbPendingSignRequests > 1 ? null : false);
        console.info("launch sign modal");
        window.onbeforeunload = null;
        signUi.workspace.signPlacementController.lockSigns();
        if (signUi.isPdf && signUi.currentSignType !== 'hiddenVisa') {
            signUi.workspace.saveData(true);
            const formCheck = signUi.workspace.workflow
                ? signUi.workspace.pdfViewer.checkForm()
                : Promise.resolve("ok");
            formCheck.then(result => {
                if (result === "ok") {
                    let signId = signUi.workspace.checkSignsPositions();
                    if (signId != null) {
                        if(signUi.workspace.currentSignRequestParamses.length > 0 || signUi.stepRepeatable) {
                            bootbox.alert("Merci de placer la signature", function () {
                                let signSpace = $("#signSpace_" + signId);
                                if(signSpace.length) {
                                    signUi.workspace.pdfViewer.animateScrollToPosition(parseInt(signSpace.css('top').replace('px', ''), 10));
                                }
                            });
                        } else {
                            if(signUi.currentSignType === 'signature') {
                                bootbox.confirm({
                                    message: "<div class='alert alert-secondary'><h4>Attention, vous allez signer sans appliquer d’image de signature</h4>Vous pouvez continuer mais, dans ce cas, un certificat électronique sera nécessaire.</div>",
                                    buttons: {
                                        cancel: {
                                            label: '<i class="fi fi-rr-undo-alt"></i> Ajouter une signature',
                                            className: 'btn-primary'
                                        },
                                        confirm: {
                                            label: '<i class="fi fi-rr-turn-right"></i> Continuer sans visuel',
                                            className: 'btn-secondary'
                                        }
                                    },
                                    callback: result => {
                                        if (result) {
                                            this.checkAttachement().then(canContinue => {
                                                if (canContinue) {
                                                    this.removeImageStampOptionTemporarily();
                                                    this.confirmLaunchSignModal(true);
                                                }
                                            });
                                        } else {
                                            $("#addSignButton").click();
                                        }
                                    }
                                });
                            } else {
                                bootbox.alert({
                                    message: "Pour cette étape de visa, vous devez obligatoirement insérer un visuel de signature",
                                    callback: function () {
                                        $("#addSignButton2").click();
                                    }
                                });
                            }
                            signUi.checkSignOptions();
                        }
                    } else {
                        if(signUi.notSigned && (signUi.currentSignType === "signature" || signUi.currentSignType === "visa") && (signUi.currentStepMinSignLevel === "simple") && $("#certType > option[value='imageStamp']").length === 0) {
                            $('#certType').prepend($('<option>', {
                                value: 'imageStamp',
                                text: signUi.saveOptionText
                            }));
                        }
                        signUi.checkSignOptions();
                        signUi.certTypeSelect.children().each(function() {
                            if($(this).val() === "imageStamp" && (signUi.currentSignType === "signature" || signUi.currentSignType === "visa")) {
                                $(this).removeAttr('disabled');
                                $("#no-options").hide();
                                $("#no-options-alert").hide();
                                $("#selectTypeDiv").show();
                                $("#checkValidateAdvancedSignButton").show();
                                $("#launchNoInfiniteSignButton").show();
                                $("#launch-infinite-sign-button").show();
                                $("#checkValidateSignButtonEnd").show();
                                $("#checkValidateSignButtonNext").show();
                            }
                        });
                        if(signUi.currentSignType === "visa") {
                            $("#certType").val('imageStamp').trigger('change');
                        }
                        this.checkAttachement().then(canContinue => {
                            if (canContinue) {
                                this.confirmLaunchSignModal(forcePanel);
                            }
                        });
                    }
                }
            });
        } else {
            this.checkAttachement().then(canContinue => {
                if (canContinue) {
                    this.confirmLaunchSignModal(forcePanel);
                }
            });
        }
    }

    checkAttachement() {
        const signUi = this.signUi;
        if (signUi.attachmentRequire) {
            return new Promise(resolve => {
                bootbox.alert({
                    message: "Vous devez joindre un document à cette étape avant de signer",
                    callback: () => resolve(false)
                });
            });
        } else if (signUi.attachmentAlert) {
            return new Promise(resolve => {
                bootbox.confirm({
                    message: "Attention, il est demandé de joindre un document à cette étape avant de signer",
                    buttons: {
                        cancel: {
                            label: '<i class="fi fi-rr-undo-alt"></i> Retour'
                        },
                        confirm: {
                            label: '<i class="fi fi-rr-turn-right"></i> Continuer sans pièce jointe'
                        }
                    },
                    callback: result => resolve(Boolean(result))
                });
            });
        } else {
            return Promise.resolve(true);
        }
    }

    confirmLaunchSignModal(forcePanel = false) {
        if (!Boolean(this.signUi.stepRepeatable) && !forcePanel) {
            this.signUi.signComment = $();
            this.launchSign();
            return;
        }

        const signModal = $("#signModal");
        const enableInfinite = $("#enableInfinite");
        const infiniteForm = $("#infiniteForm");
        const launchNoInfiniteSignButton = $("#launchNoInfiniteSignButton");
        const signCommentNoInfinite = $("#signCommentNoInfinite");

        infiniteForm.addClass("d-none");
        launchNoInfiniteSignButton.show();
        signCommentNoInfinite.show();

        enableInfinite.off("click.signatureFlowController");
        enableInfinite.on("click.signatureFlowController", function (e) {
            e.preventDefault();
            infiniteForm.toggleClass("d-none");
            launchNoInfiniteSignButton.toggle();
            signCommentNoInfinite.toggle();
        });

        if (signModal.length) {
            signModal.modal('show');
            return;
        }

        this.launchSign();
    }

    launchNoInfiniteSign(next) {
        this.signUi.signComment = $("#signComment");
        return this.launchSign(next);
    }

    setLaunchButtonsDisabled(disabled) {
        $("#signLaunchButton, #signAdvancedLaunchButton, #checkValidateAdvancedSignButton, #launchNoInfiniteSignButton, #launch-infinite-sign-button, #checkValidateSignButtonNext, #checkValidateSignButtonEnd").prop("disabled", disabled);
    }

    resetLaunchUiState() {
        this.reset();
        this.setLaunchButtonsDisabled(false);
        this.signUi.wait.modal('hide');
        $("#signActionButtons button").prop("disabled", false);
        $("#checkValidateAdvancedSignButton").prop("disabled", false);
    }

    resolveRequestedNextUrl(trigger = null) {
        const triggerElement = trigger?.currentTarget != null ? $(trigger.currentTarget) : $();
        const explicitNextUrl = triggerElement.attr("data-es-next-url");
        if (explicitNextUrl != null && explicitNextUrl !== "") {
            return explicitNextUrl;
        }

        if (triggerElement.closest("#signModal").length) {
            const signGotoNext = $("#signGotoNext");
            if (signGotoNext.length && signGotoNext.is(':checked')) {
                return this.getGotoNextUrl();
            }
            return null;
        }

        if (this.getStoredGotoNextPreference()) {
            return this.getGotoNextUrl();
        }

        return null;
    }

    shouldPersistGotoNextPreference(trigger = null) {
        const triggerElement = trigger?.currentTarget != null ? $(trigger.currentTarget) : $();
        return triggerElement.closest("#signModal").length > 0;
    }

    getGotoNextUrl() {
        const signGotoNext = $("#signGotoNext");
        const signGotoNextUrl = signGotoNext.attr("data-es-next-url");
        if (signGotoNextUrl != null && signGotoNextUrl !== "") {
            return signGotoNextUrl;
        }

        const nextSignButton = $("#nextSignBookButton");
        const nextSignButtonUrl = nextSignButton.attr("href");
        if (nextSignButtonUrl != null && nextSignButtonUrl !== "") {
            return nextSignButtonUrl;
        }

        return null;
    }

    setContextualPassword(password) {
        this.contextualPassword = typeof password === "string" ? password : "";
    }

    getContextualPassword() {
        return this.contextualPassword;
    }

    setContextualSignAll(signAll) {
        if (signAll == null) {
            this.contextualSignAll = null;
            return;
        }
        this.contextualSignAll = Boolean(signAll);
    }

    getContextualSignAll() {
        return Boolean(this.contextualSignAll);
    }

    requestContextualInfo() {
        const signUi = this.signUi;
        const needsPassword = signUi.certTypeSelect.val() === "userCert" && this.getContextualPassword().trim() === "";
        const needsSignAll = signUi.nbPendingSignRequests > 1 && this.contextualSignAll == null;

        if (signUi.certTypeSelect.val() !== "userCert") {
            this.setContextualPassword("");
        }

        if (!needsPassword && !needsSignAll) {
            return Promise.resolve(true);
        }

        return new Promise(resolve => {
            let resolved = false;
            const dialog = bootbox.dialog({
                title: needsPassword ? "Informations requises" : "Option de signature",
                message: `
                    <div>
                        ${needsPassword ? `
                            <p>La signature avec votre certificat utilisateur nécessite le mot de passe de votre magasin de certificats.</p>
                            <input type="password" id="bootbox-user-cert-password" class="form-control" autocomplete="current-password" placeholder="Saisir votre mot de passe">
                            <div id="bootbox-user-cert-password-error" class="text-danger mt-2 d-none">Merci de saisir votre mot de passe.</div>
                        ` : ''}
                        ${needsSignAll ? `
                            <div class="form-check form-switch form-switch-md d-flex flex-row align-items-center ${needsPassword ? 'mt-3' : ''}">
                                <input type="checkbox" class="form-check-input" id="bootbox-sign-all">
                                <label class="form-check-label" for="bootbox-sign-all">Signer tous les documents de la demande en utilisant ce même emplacement de signature.</label>
                            </div>
                        ` : ''}
                    </div>
                `,
                buttons: {
                    cancel: {
                        label: 'Annuler',
                        className: 'btn-secondary',
                        callback: () => {
                            if (!resolved) {
                                resolved = true;
                                resolve(false);
                            }
                        }
                    },
                    confirm: {
                        label: 'Continuer',
                        className: 'btn-primary',
                        callback: () => {
                            if (needsPassword) {
                                const passwordInput = dialog.find('#bootbox-user-cert-password');
                                const errorMessage = dialog.find('#bootbox-user-cert-password-error');
                                const password = passwordInput.val();
                                if (typeof password !== 'string' || password.trim() === '') {
                                    errorMessage.removeClass('d-none');
                                    passwordInput.trigger('focus');
                                    return false;
                                }
                                this.setContextualPassword(password);
                            }
                            if (needsSignAll) {
                                this.setContextualSignAll(dialog.find('#bootbox-sign-all').prop('checked'));
                            }
                            if (!resolved) {
                                resolved = true;
                                resolve(true);
                            }
                        }
                    }
                }
            });

            dialog.on('shown.bs.modal', () => {
                if (needsPassword) {
                    const passwordInput = dialog.find('#bootbox-user-cert-password');
                    passwordInput.trigger('focus');
                    passwordInput.on('keydown', e => {
                        if (e.key === 'Enter') {
                            e.preventDefault();
                            dialog.find('.btn-primary').trigger('click');
                        }
                    });
                }
            });

            dialog.on('hidden.bs.modal', () => {
                if (!resolved) {
                    resolved = true;
                    resolve(false);
                }
            });
        });
    }

    launchSign(e) {
        const signUi = this.signUi;
        this.setLaunchButtonsDisabled(true);
        let signModal = $('#signModal');
        if(signUi.currentSignType !== 'hiddenVisa' && (signUi.certTypeSelect.val() === '' || signUi.certTypeSelect.val() === null)) {
            bootbox.alert("<div class='alert alert-danger'>Merci de choisir un type de signature dans la liste déroulante</div>", null);
            this.setLaunchButtonsDisabled(false);
            return;
        }
        if (signUi.isPdf && signUi.workspace.checkSignsPositions() != null && signUi.workspace.signType !== "hiddenVisa" && (signUi.certTypeSelect.val() === 'imageStamp')) {
            bootbox.alert("Merci de placer la signature", null);
            signModal.modal('hide');
            this.setLaunchButtonsDisabled(false);
            return;
        }
        $(window).unbind("beforeunload");
        this.state.gotoNext = this.resolveRequestedNextUrl(e);
        const signGotoNext = $("#signGotoNext");
        if (signGotoNext.length && this.shouldPersistGotoNextPreference(e)) {
            this.storeGotoNextPreference(signGotoNext.is(':checked'));
        }
        signUi.gotoNext = this.state.gotoNext;
        this.skipRestoreOnNextSignModalHide = true;
        signModal.modal('hide');
        this.state.percent = 0;
        signUi.percent = 0;
        let good = true;
        if(signUi.signForm && signUi.workspace.workflow) {
            let inputs = signUi.signForm.getElementsByTagName("input");
            for (let i = 0, len = inputs.length; i < len; i++) {
                let input = inputs[i];
                if (!input.checkValidity()) {
                    good = false;
                }
            }
        }
        if(good) {
            this.requestContextualInfo().then(canContinue => {
                if (!canContinue) {
                    this.restoreRemovedImageStampOption();
                    this.setLaunchButtonsDisabled(false);
                    return;
                }
                console.log('launch sign for : ' + signUi.signRequestId);
                signUi.wait.modal('show');
                signUi.wait.modal({backdrop: 'static', keyboard: false});
                if(signUi.isPdf) {
                    signUi.workspace.pdfViewer.promiseSaveValues().then(() => this.submitSignRequest());
                } else {
                    this.submitSignRequest();
                }
            });
        } else {
            $("#checkDataSubmit").click();
        }
    }

    async submitSignRequest() {
        const signUi = this.signUi;
        let signaturesCheck = true;
        let formData = { };
        try {
            await signUi.workspace?.signPlacementController?.persistMobileSignaturePreviews?.();
        } catch (error) {
            console.error("Unable to persist mobile signature preview", error);
            signUi.wait?.modal?.('hide');
            this.setLaunchButtonsDisabled(false);
            bootbox.alert("La signature mobile n'a pas pu etre enregistree. Veuillez reessayer avant de valider.");
            return;
        }
        if(signUi.isPdf) {
            $.each($('#signForm').serializeArray(), function () {
                if (!this.name.startsWith("comment")) {
                    formData[this.name] = this.value;
                }
            });
            if(signUi.formId != null) {
                signUi.workspace.pdfViewer.savedFields.forEach((value, key) => {
                    formData[key] = value;
                });
            }
        }
        if(signUi.workspace != null) {
            let signRequestParamses = Array.from(signUi.workspace.signPlacementController.signRequestParamses.values());
            let signRequestParamsesToSend = signRequestParamses.map(originalParams => {
                if (typeof originalParams?.synchronizePositionWithRenderedCross === 'function') {
                    originalParams.synchronizePositionWithRenderedCross();
                }
                let signScale = signUi.normalizeFloat(originalParams.signScale, 1, 0.01);
                let signPageNumber = signUi.normalizeInteger(originalParams.signPageNumber, 1, 1);
                let xPos = signUi.normalizeInteger(originalParams.xPos, 0, 0);
                let yPos = signUi.normalizeInteger(originalParams.yPos, 0, 0);
                if (originalParams.signSpace != null && originalParams.signSpace.attr) {
                    const slotPage = Number.parseInt(originalParams.signSpace.attr("data-es-pos-page"), 10);
                    const slotX = Number.parseInt(originalParams.signSpace.attr("data-es-pos-x"), 10);
                    const slotY = Number.parseInt(originalParams.signSpace.attr("data-es-pos-y"), 10);
                    if (Number.isFinite(slotPage)) {
                        signPageNumber = slotPage;
                    }
                    if (Number.isFinite(slotX)) {
                        xPos = slotX;
                    }
                    if (Number.isFinite(slotY)) {
                        yPos = slotY;
                    }
                }
                let paramToSend = {
                    id: signUi.normalizeInteger(originalParams.id, null),
                    signPageNumber: signPageNumber,
                    signDocumentNumber: signUi.normalizeInteger(originalParams.signDocumentNumber, 0, 0),
                    signWidth: signUi.normalizeInteger(originalParams.signWidth / signScale, 200, 1),
                    signHeight: signUi.normalizeInteger(originalParams.signHeight / signScale, 100, 1),
                    xPos: xPos,
                    yPos: yPos,
                    rotate: signUi.normalizeInteger(signUi.workspace.pdfViewer.rotation, 0, 0),
                    signImageNumber: signUi.normalizeInteger(originalParams.signImageNumber, 0),
                    pdSignatureFieldName: originalParams.pdSignatureFieldName ?? null,
                    signScale: signScale,
                    extraText: originalParams.extraText ?? "",
                    isExtraText: Boolean(originalParams.isExtraText),
                    addWatermark: Boolean(originalParams.addWatermark),
                    allPages: Boolean(originalParams.allPages),
                    addImage: Boolean(originalParams.addImage),
                    addExtra: Boolean(originalParams.addExtra),
                    extraType: Boolean(originalParams.extraType),
                    extraName: Boolean(originalParams.extraName),
                    extraDate: Boolean(originalParams.extraDate),
                    extraOnTop: originalParams.extraOnTop == null ? true : Boolean(originalParams.extraOnTop),
                    textPart: originalParams.textPart ?? null,
                    red: signUi.normalizeInteger(originalParams.red, 0, 0),
                    green: signUi.normalizeInteger(originalParams.green, 0, 0),
                    blue: signUi.normalizeInteger(originalParams.blue, 0, 0),
                    fontSize: signUi.normalizeInteger(originalParams.fontSize, signUi.signatureUiConfig?.defaultFontSize ?? 16, 1),
                    recipientId: signUi.normalizeInteger(originalParams.recipientId, null),
                };
                if(originalParams.userSignaturePad != null) {
                    const hasDrawnSignature = !originalParams.userSignaturePad.signaturePad.isEmpty();
                    const hasLoadedSignatureImage = Boolean(originalParams.userSignaturePad.signImageBase64Val) && !originalParams.mobileSignatureSaved;
                    if(!hasDrawnSignature && !hasLoadedSignatureImage) {
                        signaturesCheck = false;
                    } else if (hasDrawnSignature) {
                        originalParams.userSignaturePad.save();
                        paramToSend.imageBase64 = originalParams.userSignaturePad.signImageBase64Val;
                    } else {
                        paramToSend.imageBase64 = originalParams.userSignaturePad.signImageBase64Val;
                    }
                }
                return paramToSend;
            });
            this.state.signRequestUrlParams = {
                'password' : this.getContextualPassword(),
                'signAll' : this.getContextualSignAll(),
                'sealCertificat' : signUi.sealCertificatSelect.val(),
                'signRequestParams' : JSON.stringify(signRequestParamsesToSend),
                'comment' : signUi.signComment?.val?.() ?? "",
                'formData' : JSON.stringify(formData)
            };
            if (signUi.currentSignType !== 'hiddenVisa') {
                this.state.signRequestUrlParams.certType = signUi.certTypeSelect.val();
            }
            signUi.signRequestUrlParams = this.state.signRequestUrlParams;
        } else {
            this.state.signRequestUrlParams = {
                "password": this.getContextualPassword(),
            };
            signUi.signRequestUrlParams = this.state.signRequestUrlParams;
        }
        if(signaturesCheck) {
            this.sendData(this.state.signRequestUrlParams);
        } else {
            bootbox.alert("Une signature est vide", null);
        }
    }

    sendData(signRequestUrlParams) {
        const signUi = this.signUi;
        this.reset();
        console.log("start sign");
        console.log(signUi.signRequestId);
        $.ajax({
            url: "/ws-secure/global/sign/" + signUi.signRequestId + "?" + signUi.csrf.parameterName + "=" + signUi.csrf.token,
            type: 'POST',
            data: signRequestUrlParams,
            success: (data) => {
                if(data === "initNexu") {
                    signUi.openNexuProcessModal([signUi.signRequestId]).then(opened => {
                        if (!opened) {
                            document.location.href = "/nexu-sign/start?ids=" + signUi.signRequestId;
                        }
                    });
                } else {
                    if (this.state.gotoNext != null) {
                        document.location.href = this.state.gotoNext;
                    } else {
                        if(signUi.isOtp== null || !signUi.isOtp) {
                            if(signUi.returnToHome == null) {
                                if (signUi.nbSignRequests > 1 || !signUi.signatureUiConfig?.returnToHomeAfterSign) {
                                    document.location.href = "/user/signrequests/" + signUi.signRequestId;
                                } else {
                                    document.location.href = "/user";
                                }
                            } else {
                                if(signUi.returnToHome) {
                                    document.location.href = "/user";
                                } else {
                                    document.location.href = "/user/signrequests/" + signUi.signRequestId;
                                }
                            }
                        } else {
                            document.location.href = "/otp/signrequests/" + signUi.signRequestId;
                        }
                    }
                }
            },
            error: function(data) {
                this.setLaunchButtonsDisabled(false);
                $("#signSpinner").hide();
                console.error("sign error : " + data.responseText);
                document.getElementById("signError").style.display = "block";
                document.getElementById("signError").innerHTML =
                    "<p>Une erreur s’est produite lors de la signature du document.</p>" +
                    "<small>Message retourné par le système de signature : " + data.responseText + "</small>";
                document.getElementById("closeModal").style.display = "block";
            }.bind(this)
        });
    }

    reset() {
        this.state.percent = 0;
        this.signUi.percent = 0;
        $("#signSpinner").show();
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
    }

}
