import {SignWorkspaceController} from "./SignWorkspaceController.js?version=@version@";
import {CsrfToken} from "../../../prototypes/CsrfToken.js?version=@version@";
import {Step} from "../../../prototypes/Step.js?version=@version@";
import {Nexu} from "./Nexu.js?version=@version@";
import {Recipient} from "../../../prototypes/Recipient.js?version=@version@";
import {WorkspaceState} from "./WorkspaceState.js?version=@version@";
import {SignatureFlowController} from "./SignatureFlowController.js?version=@version@";
import {NexuProcessUi} from "./NexuProcessUi.js?version=@version@";

export class SignUi {

    constructor(showDataFlowInput, csrfToken, signatureUiConfig = null) {
        this.state = WorkspaceState.from(showDataFlowInput, csrfToken);
        const {signUiDto, csrfToken: csrf} = this.normalizeInput();
        console.info("Starting sign UI for " + signUiDto.signRequestId);
        this.signatureUiConfig = signatureUiConfig;
        this.wait = $('#wait');
        this.nexuProcessModal = $('#nexuProcessModal');
        this.nexuProcessLoading = $('#nexu-process-modal-loading');
        this.nexuProcessError = $('#nexu-process-modal-error');
        this.nexuProcessContent = $('#nexu-process-modal-content');
        this.signForm = document.getElementById("signForm");
        this.csrf = new CsrfToken(csrf);
        this.workspace = new SignWorkspaceController(this.state, this.csrf, this.signatureUiConfig);
        this.signComment = $('#signComment');
        this.certTypeSelect = $("#certType");
        this.sealCertificatSelect = $("#sealCertificat");
        this.signLaunchButton = $("#signLaunchButton");
        this.signAdvancedLaunchButton = $("#signAdvancedLaunchButton");
        this.signTypeLabel = $("#signTypeLabel");
        this.toolsBar = $("#tools");
        this.certTypeObserver = null;
        this.saveOptionText =  $("#certType > option[value='imageStamp']").text();
        this.signatureFlowController = new SignatureFlowController(this);
        this.initListeners();
        this.ensureSealCertificateSelection();
        this.initMobileCertTypeVisibility();
        if(signUiDto.status !== "archived" && signUiDto.status !== "cleaned" && signUiDto.currentSignType !== "form") {
            this.initReportModal();
        }
        this.nexu = this.checkSignOptions();
        this.applyInitialPreferredCertType();
    }

    normalizeInput() {
        return this.state.toSignUiContext();
    }

    get showDataFlow() {
        return this.state.showDataFlow;
    }

    get signUiDto() {
        return this.state.signUiDto;
    }

    get returnToHome() {
        return this.state.returnToHome;
    }

    get signRequestId() {
        return this.state.signRequestId;
    }

    get signable() {
        return this.state.signable;
    }

    get percent() {
        return this.state.percent;
    }

    set percent(value) {
        this.state.percent = value;
    }

    get isOtp() {
        return this.state.isOtp;
    }

    get isPdf() {
        return this.state.isPdf;
    }

    get formId() {
        return this.state.formId;
    }

    get dataId() {
        return this.state.dataId;
    }

    get currentSignType() {
        return this.state.currentSignType;
    }

    get notSigned() {
        return this.state.notSigned;
    }

    get stepRepeatable() {
        return this.state.stepRepeatable;
    }

    get currentStepNumber() {
        return this.state.currentStepNumber;
    }

    get currentStepMinSignLevel() {
        return this.state.currentStepMinSignLevel;
    }

    get gotoNext() {
        return this.state.gotoNext;
    }

    set gotoNext(value) {
        this.state.gotoNext = value;
    }

    get nbSignRequests() {
        return this.state.nbSignRequests;
    }

    get nbPendingSignRequests() {
        return this.state.nbPendingSignRequests;
    }

    get attachmentRequire() {
        return this.state.attachmentRequire;
    }

    get attachmentAlert() {
        return this.state.attachmentAlert;
    }

    get signRequestUrlParams() {
        return this.state.signRequestUrlParams;
    }

    set signRequestUrlParams(value) {
        this.state.signRequestUrlParams = value;
    }

    get status() {
        return this.state.status;
    }

    initListeners() {
        [
            ["#checkValidateSignButtonEnd", () => this.signatureFlowController.launchSign()],
            ["#checkValidateSignButtonNext", e => this.signatureFlowController.launchSign(e)],
            ["#checkValidateAdvancedSignButton", e => this.signatureFlowController.launchSign(e)],
            ["#launch-infinite-sign-button", e => this.insertStep(e)],
            ["#launchNoInfiniteSignButton", e => this.signatureFlowController.launchNoInfiniteSign(e)],
            ["#refresh-certType, #refresh-certType2", () => this.checkSignOptions()],
            ["#copyButton", () => this.copy()]
        ].forEach(([selector, handler]) => $(selector).on('click', handler));
        $("#certType").on("change", () => this.checkAfterChangeSignType());
        $("#send").on('submit', function (e) {
            e.preventDefault();
            bootbox.alert("Merci de saisir les participants", null);
        });
        this.initLaunchButtons();
        $("#refuseModal").on('shown.bs.modal', function () {
            $("#refuseComment").focus();
        });
        $("#signModal, #refuseModal")
            .on('shown.bs.modal', () => $(document.body).addClass('es-signrequest-sidepanel-open'))
            .on('hidden.bs.modal', () => $(document.body).removeClass('es-signrequest-sidepanel-open'));
        this.nexuProcessModal.on('hidden.bs.modal', () => {
            this.resetNexuProcessModal();
            this.signatureFlowController.resetLaunchUiState();
        });
        this.nexuProcessModal.get(0)?.addEventListener('nexuProcessReloadRequested', event => {
            const requestedIds = Array.isArray(event.detail?.ids) && event.detail.ids.length > 0
                ? event.detail.ids
                : [this.signRequestId];
            this.openNexuProcessModal(requestedIds);
        });
    }

    initLaunchButtons() {
        ["#visaLaunchButton", "#signAdvancedLaunchButton"].forEach(selector => {
            $(selector).on('click', () => this.signatureFlowController.prepareLaunchSign(true));
        });
        this.signLaunchButton.on('click', () => {
            const selectableCertTypeOption = this.getSingleSelectableCertTypeOption();
            if (selectableCertTypeOption.length && selectableCertTypeOption.val() !== "imageStamp") {
                this.certTypeSelect.val(selectableCertTypeOption.val()).trigger("change");
                $("#checkValidateAdvancedSignButton").trigger("click");
                return;
            }
            this.signatureFlowController.launchQuickSign();
        });
        $("#refuseLaunchButton").on('click', function () {
            window.onbeforeunload = null;
        });
    }

    initReportModal() {
        let self = this;
        $.ajax({
            url: "/ws-secure/validation/short/" + self.signRequestId,
            type: 'GET',
            success: function (data, textStatus, xhr) {
                const reportModal = document.createElement("div");
                reportModal.className = "alert collapse";
                reportModal.id = "reportModal";
                reportModal.tabIndex = -1;
                reportModal.setAttribute("data-bs-focus", "false");
                reportModal.setAttribute("role", "dialog");
                reportModal.setAttribute("aria-hidden", "true");

                const title = document.createElement("h5");
                title.textContent = "Validation de la signature";
                reportModal.appendChild(title);

                const content = document.createElement("div");
                $.parseHTML(data, document, false).forEach(node => content.appendChild(node));
                reportModal.appendChild(content);

                document.getElementById("alertSign")?.appendChild(reportModal);
                $("#reportSpinner").hide();
                $(".reportModalBtn").removeClass("d-none");
            }
        });
    }

    resetNexuProcessModal() {
        this.nexuProcessContent.empty();
        this.nexuProcessError.addClass('d-none').empty();
        this.nexuProcessLoading.removeClass('d-none');
    }

    buildNexuProcessFragmentUrl(ids = [this.signRequestId]) {
        const params = new URLSearchParams();
        ids.forEach(id => params.append('ids', id));
        return '/nexu-sign/start-fragment?' + params;
    }

    async openNexuProcessModal(ids = [this.signRequestId]) {
        if (!this.nexuProcessModal.length || !this.nexuProcessContent.length) {
            return false;
        }
        this.signatureFlowController.resetLaunchUiState();
        this.resetNexuProcessModal();
        const modalElement = this.nexuProcessModal.get(0);
        const modalInstance = bootstrap.Modal.getOrCreateInstance(modalElement, {
            backdrop: 'static',
            keyboard: false
        });
        modalInstance.show();
        try {
            const response = await fetch(this.buildNexuProcessFragmentUrl(ids), {
                headers: {
                    'Accept': 'text/html',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            const html = await response.text();
            const fragment = document.createElement('template');
            fragment.innerHTML = html;
            fragment.content.querySelectorAll('script').forEach(script => script.remove());
            this.nexuProcessContent.empty().append(fragment.content.cloneNode(true));
            this.nexuProcessLoading.addClass('d-none');
            NexuProcessUi.initWithin(modalElement);
            return true;
        } catch (error) {
            console.error('Impossible de charger l’interface Nexu', error);
            this.nexuProcessLoading.addClass('d-none');
            this.nexuProcessError
                .removeClass('d-none')
                .text("Impossible de charger l’interface de signature eIDAS.");
            return false;
        }
    }

    checkSignOptions() {
        console.info("check sign options");
        if (this.signable) {
            let nexu = new Nexu(null, null, this.currentSignType, null, null);
            $("#certType").focus();
            this.ensureSealCertificateSelection();
            this.updateMobileCertTypeVisibility();
            return nexu;
        }
    }

    ensureSealCertificateSelection() {
        if (!this.sealCertificatSelect.length) {
            return;
        }

        const currentValue = this.sealCertificatSelect.val();
        if (currentValue != null && currentValue !== "") {
            return;
        }

        const firstOptionValue = this.sealCertificatSelect.find("option:first").val();
        if (firstOptionValue != null && firstOptionValue !== "") {
            this.sealCertificatSelect.val(firstOptionValue);
        }
    }

    getSelectableCertTypeCount() {
        return this.certTypeSelect.find("option:not(:disabled):not([unavailable])").length;
    }

    getSingleSelectableCertTypeOption() {
        if (!this.certTypeSelect.length) {
            return $();
        }
        const selectableCertTypeOptions = this.certTypeSelect.find("option:not(:disabled):not([unavailable])");
        return selectableCertTypeOptions.length === 1 ? selectableCertTypeOptions.first() : $();
    }

    getActiveImageStampOption() {
        if (!this.certTypeSelect.length) {
            return $();
        }
        return this.certTypeSelect.find("option[value='imageStamp']:not(:disabled):not([unavailable])");
    }

    hasActiveImageStampOption() {
        return this.getActiveImageStampOption().length > 0;
    }

    applyInitialPreferredCertType() {
        if (!this.hasActiveImageStampOption()) {
            return;
        }

        this.certTypeSelect.val("imageStamp");
        this.checkAfterChangeSignType();
    }

    updateSelectableSignAlerts() {
        if (!this.certTypeSelect.length) {
            return;
        }

        const hasSelectableSignWith = this.getSelectableCertTypeCount() > 0;
        const warningSelectDivs = $(".warning-select-signwith-unavailable");
        const noOptionsAlert = $("#no-options-alert");
        const pdfAlertsDropdown = $("#display-pdf-alerts-dropdown");

        warningSelectDivs.toggle(!hasSelectableSignWith);
        noOptionsAlert.toggle(!hasSelectableSignWith);

        if (!hasSelectableSignWith) {
            pdfAlertsDropdown.removeClass("d-none");
        }
    }

    updateMobileCertTypeVisibility() {
        if (!this.toolsBar.length || !this.certTypeSelect.length) {
            return;
        }
        this.toolsBar.toggleClass("es-tools-single-cert-type-mobile", this.getSelectableCertTypeCount() === 1);
        this.updateSelectableSignAlerts();
    }

    hasPendingSignaturePlacement() {
        const signPlacementController = this.workspace?.signPlacementController;
        if (signPlacementController == null) {
            return false;
        }
        if (typeof signPlacementController.hasPendingSignaturePlacement === "function") {
            return signPlacementController.hasPendingSignaturePlacement();
        }
        if (signPlacementController.signRequestParamses == null) {
            return false;
        }
        return Array.from(signPlacementController.signRequestParamses.values()).some(signRequestParams => {
            const signImageNumber = signRequestParams?.signImageNumber == null
                ? null
                : Number.parseInt(signRequestParams.signImageNumber, 10);
            return signRequestParams != null
                && signRequestParams.isSign
                && signImageNumber != null
                && signImageNumber >= 0
                && signImageNumber !== 999999;
        });
    }

    hasValidSelectedCertType() {
        if (!this.certTypeSelect.length) {
            return false;
        }
        const selectedOption = this.certTypeSelect.find("option:selected");
        const value = this.certTypeSelect.val();
        return value != null
            && value !== ""
            && selectedOption.length > 0
            && !selectedOption.is(":disabled")
            && !selectedOption.is("[unavailable]");
    }

    getSelectedCertTypeLabel() {
        if (!this.hasValidSelectedCertType()) {
            return "";
        }

        return this.certTypeSelect.find("option:selected").text().trim();
    }

    updateSignTypeLabel() {
        if (!this.signTypeLabel.length) {
            return;
        }

        const selectedCertTypeLabel = this.getSelectedCertTypeLabel();
        this.signTypeLabel.text(selectedCertTypeLabel);
        this.signTypeLabel.toggleClass("d-none", selectedCertTypeLabel === "");
    }

    syncSignatureStepUi() {
        const signPlacementController = this.workspace?.signPlacementController;
        if (signPlacementController == null) {
            return;
        }
        if (this.currentSignType === 'hiddenVisa') {
            signPlacementController.goStep2();
            return;
        }
        if (typeof signPlacementController.refreshSteps === "function") {
            signPlacementController.refreshSteps();
            return;
        }
        if (this.hasPendingSignaturePlacement()) {
            signPlacementController.goStep2();
            return;
        }
        signPlacementController.goStep1();
    }

    initMobileCertTypeVisibility() {
        if (!this.certTypeSelect.length || !this.toolsBar.length) {
            return;
        }

        this.updateMobileCertTypeVisibility();

        this.certTypeObserver?.disconnect?.();
        this.certTypeObserver = new MutationObserver(() => {
            this.updateMobileCertTypeVisibility();
            this.updateSignTypeLabel();
        });
        this.certTypeObserver.observe(this.certTypeSelect.get(0), {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ["disabled", "unavailable", "selected", "hidden", "style", "class", "value"]
        });

        $(window)
            .off("resize.signUiCertTypeVisibility")
            .on("resize.signUiCertTypeVisibility", () => this.updateMobileCertTypeVisibility());

        this.updateSelectableSignAlerts();
        this.updateSignTypeLabel();
    }

    checkAfterChangeSignType() {
        let self = this;
        const value = this.certTypeSelect.val();
        if(value == null) {
            this.checkSignOptions();
            this.syncSignatureStepUi();
            this.updateMobileCertTypeVisibility();
            this.updateSignTypeLabel();
            return;
        }
        if(value === "nexuCert") {
            this.nexu.checkNexuClient().then(function (e) {
                console.info("Esup-DSS-Client est lancé !");
                $("#certType > option[value='nexuCert']").remove('unavailable');
                $("#nexu_missing_alert").hide();
                $("#no-options").hide();
                $("#no-options-alert").hide();
                $("#selectTypeDiv").show();
                if (self.currentSignType === 'nexuSign') {
                    $("#certType").val("nexuCert");
                    $("#nexu_ready_alert").show();
                    $("#alertNexu").hide();
                    $("#signLaunchButton").show();
                }
            }).catch(function (e) {
                console.info("Esup-DSS-Client non lancé !");
                console.info(e);
                $("#nexu_ready_alert").hide();
                // $("#certType > option[value='nexuCert']").attr('disabled', 'disabled');
                $("#alertNexu").show();
                bootbox.alert(`
                <div id="nexu_missing_alert" class="alert alert-warning">
                    <p>L'application Esup-DSS-Client n'a pas été détectée</p>
                    <p class="text-left">
                        Si vous devez signer à l'aide d'un certificat présent sur votre poste ou sur clé USB,
                        merci de lancer l'application Esup-DSS-Client sur votre poste puis de cliquer sur
                        le bouton "Actualiser".<br/>
                        Pour plus d'informations :
                        <a target="_blank"
                            href="https://www.esup-portail.org/wiki/display/SIGN/Esup-DSS-Client">
                            Documentation Esup-DSS-Client
                        </a>
                    </p>
                </div>
            `, function () {
                    $("#certType").val("").trigger("change");
                });
            });
        }
        if (value !== "userCert") {
            this.signatureFlowController.setContextualPassword("");
        }
        $("#nexuCheck").toggleClass('d-none', value !== "nexuCert");
        $("#alert-sign-present").toggle(value === "imageStamp");
        if (value === "sealCert") {
            this.ensureSealCertificateSelection();
        }
        $("#sealChoose").toggleClass('d-none', value !== "sealCert");
        this.syncSignatureStepUi();
        this.updateMobileCertTypeVisibility();
        this.updateSignTypeLabel();
    }


    copy() {
        let copyText = document.getElementById("exportUrl");
        let textArea = document.createElement("textarea");
        textArea.value = copyText.textContent;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand("Copy");
        textArea.remove();
    }

    insertStep(next = null) {
        console.info("check insert step");
        let signRequestId = this.signRequestId;
        let csrf = this.csrf;
        let step = new Step();
        let recipientsEmails = $('#recipientsEmails')[0]?.slim?.getSelected?.() ?? [];
        if(recipientsEmails.length === 0 ) {
            $("#infiniteFormSubmit").click();
            return;
        }
        recipientsEmails.forEach(function(email) {
            let recipient = new Recipient();
            recipient.email = email;
            let id = email.replaceAll("@", "_").replaceAll(".", "_");
            let extInfos = $("#recipient_" + id);
            recipient.name = extInfos.find("#name_" + id).val() ?? "";
            recipient.firstName = extInfos.find("#firstname_" + id).val() ?? "";
            recipient.phone = extInfos.find("#phone_" + id).val() ?? "";
            recipient.forceSms = extInfos.find("#forcesms_" + id).prop("checked") ?? false;
            step.recipients.push(recipient);
        });

        let self = this;
        this.signComment = $("#signComment");
        step.stepNumber = this.currentStepNumber;
        step.allSignToComplete = $('#allSignToComplete').is(':checked');
        step.multiSign = $('#multiSign').is(':checked');
        step.autoSign = $('#autoSign').is(':checked');
        step.signType = $('#repeatableSignType').val() ?? $('#signTypeHidden').val();
        step.repeatable = true;
        let url;
        if(self.isOtp== null || !self.isOtp) {
            url = "/user/signrequests/add-repeatable-step/" + signRequestId + "?" + csrf.parameterName + "=" + csrf.token
        } else {
            url = "/otp/signrequests/add-repeatable-step/" + signRequestId + "?" + csrf.parameterName + "=" + csrf.token
        }
        $.ajax({
            url: url,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(step),
            success: function() {
                self.signatureFlowController.setContextualPassword("");
                self.signatureFlowController.launchSign(next);
            },
            error: function() {
                bootbox.alert("Une erreur s’est produite lors de l’ajout de l’étape supplémentaire.");
            }
        });
    }

    normalizeInteger(value, fallback = 0, min = null) {
        let normalized = Number.parseInt(value, 10);
        if (Number.isNaN(normalized)) {
            normalized = fallback;
        }
        if (min != null && normalized < min) {
            normalized = min;
        }
        return normalized;
    }

    normalizeFloat(value, fallback = 1, min = null) {
        let normalized = Number.parseFloat(value);
        if (!Number.isFinite(normalized)) {
            normalized = fallback;
        }
        if (min != null && normalized < min) {
            normalized = min;
        }
        return Math.round(normalized * 1000) / 1000;
    }

    getBrowserZoom() {
        return 1 || 1;
    }

}
