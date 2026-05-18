import {SignWorkspaceController} from "./SignWorkspaceController.js?version=@version@";
import {CsrfToken} from "../../../prototypes/CsrfToken.js?version=@version@";
import {Step} from "../../../prototypes/Step.js?version=@version@";
import {Nexu} from "./Nexu.js?version=@version@";
import {Recipient} from "../../../prototypes/Recipient.js?version=@version@";
import {WorkspaceState} from "./WorkspaceState.js?version=@version@";
import {SignatureFlowController} from "./SignatureFlowController.js?version=@version@";

export class SignUi {

    constructor(showDataFlowInput, csrfToken, signatureUiConfig = null) {
        this.state = WorkspaceState.from(showDataFlowInput, csrfToken);
        const {signUiDto, csrfToken: csrf} = this.normalizeInput();
        console.info("Starting sign UI for " + signUiDto.signRequestId);
        this.signatureUiConfig = signatureUiConfig;
        this.wait = $('#wait');
        this.signForm = document.getElementById("signForm");
        this.csrf = new CsrfToken(csrf);
        this.workspace = new SignWorkspaceController(this.state, this.csrf, this.signatureUiConfig);
        this.signComment = $('#signComment');
        this.certTypeSelect = $("#certType");
        this.sealCertificatSelect = $("#sealCertificat");
        this.signLaunchButton = $("#signLaunchButton");
        this.toolsBar = $("#tools");
        this.certTypeObserver = null;
        this.lastResponsiveActiveStepId = null;
        this.responsiveStepChangeHandler = null;
        this.saveOptionText =  $("#certType > option[value='imageStamp']").text();
        this.signatureFlowController = new SignatureFlowController(this);
        this.initListeners();
        this.ensureSealCertificateSelection();
        this.initMobileCertTypeVisibility();
        this.initResponsiveStepNavigation();
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
        $("#checkValidateSignButtonEnd").on('click', e => this.launchSign());
        $("#checkValidateSignButtonNext").on('click', e => this.launchSign(e));
        $("#launch-infinite-sign-button").on('click', e => this.insertStep(e));
        $("#launchNoInfiniteSignButtonEnd").on('click', e => this.launchNoInfiniteSign());
        $("#launchNoInfiniteSignButtonNext").on('click', e => this.launchNoInfiniteSign(e));
        $("#refresh-certType").on('click', e => this.checkSignOptions());
        $("#refresh-certType2").on('click', e => this.checkSignOptions());
        $("#certType").on("change", e => this.checkAfterChangeSignType());
        $("#copyButton").on('click', e => this.copy());
        $("#send").on('submit', function (e) {
            e.preventDefault();
            bootbox.alert("Merci de saisir les participants", null);
            if ($(e.target).is(':invalid')) {
                bootbox.alert("Merci de saisir les participants", null);
            }
        });
        this.initLaunchButtons();
        $("#refuseModal").on('shown.bs.modal', function () {
            $("#refuseComment").focus();
        });
    }

    initLaunchButtons() {
        $("#visaLaunchButton").on('click', e => this.launchSignModal());
        this.signLaunchButton.on('click', e => this.launchSignModal());
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
                let modal = "<div class=\"alert collapse\" data-bs-focus=\"false\" id=\"reportModal\" tabindex=\"-1\" role=\"dialog\" aria-hidden=\"true\">" +
                    "<h5>Validation de la signature</h5>\n" +
                    "<div>" +
                    data +
                    "</div></div>";
                $("#alertSign").append(modal);
                $("#reportSpinner").hide();
                $(".reportModalBtn").removeClass("d-none");
            }
        });
    }

    launchSignModal() {
        return this.signatureFlowController.launchSignModal();
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
        this.syncResponsiveStepNavigationState();
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

    syncSignatureStepUi() {
        const signPlacementController = this.workspace?.signPlacementController;
        if (signPlacementController == null) {
            this.syncResponsiveStepNavigationState();
            return;
        }
        if (this.currentSignType === 'hiddenVisa') {
            signPlacementController.goStep3();
            this.syncResponsiveStepNavigationState();
            return;
        }
        if (typeof signPlacementController.refreshSteps === "function") {
            signPlacementController.refreshSteps();
            this.syncResponsiveStepNavigationState();
            return;
        }
        if (this.hasValidSelectedCertType() && this.hasPendingSignaturePlacement()) {
            signPlacementController.goStep3();
            this.syncResponsiveStepNavigationState();
            return;
        }
        if (this.hasValidSelectedCertType()) {
            signPlacementController.goStep2();
            this.syncResponsiveStepNavigationState();
            return;
        }
        signPlacementController.goStep1();
        this.syncResponsiveStepNavigationState();
    }

    initResponsiveStepNavigation() {
        this.responsiveStepPrevButton = $("#step-nav-prev");
        this.responsiveStepNextButton = $("#step-nav-next");
        this.responsiveStepsContainer = this.toolsBar.find(".steps-horizontal-v2").first();

        if (!this.responsiveStepsContainer.length
            || !this.responsiveStepPrevButton.length
            || !this.responsiveStepNextButton.length) {
            return;
        }

        this.responsiveStepPrevButton
            .off("click.signUiResponsiveSteps")
            .on("click.signUiResponsiveSteps", e => {
                e.preventDefault();
                this.navigateResponsiveStep(-1);
            });

        this.responsiveStepNextButton
            .off("click.signUiResponsiveSteps")
            .on("click.signUiResponsiveSteps", e => {
                e.preventDefault();
                this.navigateResponsiveStep(1);
            });

        if (this.responsiveStepChangeHandler != null) {
            document.removeEventListener("es-signrequest-step-change", this.responsiveStepChangeHandler);
        }
        this.responsiveStepChangeHandler = event => this.handleResponsiveStepChange(event);
        document.addEventListener("es-signrequest-step-change", this.responsiveStepChangeHandler);

        $(window)
            .off("resize.signUiResponsiveSteps")
            .on("resize.signUiResponsiveSteps", () => this.syncResponsiveStepNavigationState());

        this.syncResponsiveStepNavigationState();
    }

    getResponsiveStepElements() {
        if (!this.responsiveStepsContainer?.length) {
            return [];
        }

        const hideSingleCertTypeStep1 = this.toolsBar.hasClass("es-tools-single-cert-type-mobile")
            && window.matchMedia("(max-width: 991.98px)").matches;

        return this.responsiveStepsContainer
            .children('[id^="step-"]')
            .toArray()
            .map(element => $(element))
            .filter(step => !(hideSingleCertTypeStep1 && step.attr("id") === "step-1"));
    }

    isResponsiveStepNavigationEnabled() {
        return window.matchMedia("(max-width: 1600px)").matches && this.getResponsiveStepElements().length > 1;
    }

    getResponsiveVisibleStepIndex(steps = this.getResponsiveStepElements()) {
        let visibleIndex = steps.findIndex(step => step.hasClass("es-mobile-visible"));
        if (visibleIndex >= 0) {
            return visibleIndex;
        }

        visibleIndex = steps.findIndex(step => step.hasClass("active"));
        if (visibleIndex >= 0) {
            return visibleIndex;
        }

        return steps.length > 0 ? 0 : -1;
    }

    applyResponsiveVisibleStep(targetStep, steps = this.getResponsiveStepElements()) {
        if (targetStep == null || !steps.length) {
            this.updateResponsiveStepNavigationButtons(steps);
            return;
        }

        steps.forEach(step => step.removeClass("es-mobile-visible"));
        targetStep.addClass("es-mobile-visible");
        this.updateResponsiveStepNavigationButtons(steps);
    }

    updateResponsiveStepNavigationButtons(steps = this.getResponsiveStepElements()) {
        if (!this.responsiveStepPrevButton?.length || !this.responsiveStepNextButton?.length) {
            return;
        }

        const enabled = this.isResponsiveStepNavigationEnabled();
        const visibleIndex = this.getResponsiveVisibleStepIndex(steps);
        const hasPrevious = enabled && visibleIndex > 0;
        const hasNext = enabled && visibleIndex >= 0 && visibleIndex < steps.length - 1;

        this.responsiveStepPrevButton.toggleClass("d-none", !enabled);
        this.responsiveStepNextButton.toggleClass("d-none", !enabled);
        this.responsiveStepPrevButton.prop("disabled", !hasPrevious);
        this.responsiveStepNextButton.prop("disabled", !hasNext);
    }

    syncResponsiveStepNavigationState() {
        if (!this.responsiveStepsContainer?.length) {
            return;
        }

        const steps = this.getResponsiveStepElements();
        const enabled = window.matchMedia("(max-width: 1600px)").matches && steps.length > 1;

        if (!enabled) {
            this.responsiveStepsContainer.removeClass("es-mobile-step-view");
            this.lastResponsiveActiveStepId = null;
            steps.forEach(step => step.removeClass("es-mobile-visible"));
            this.updateResponsiveStepNavigationButtons(steps);
            return;
        }

        this.responsiveStepsContainer.addClass("es-mobile-step-view");

        const activeStep = steps.find(step => step.hasClass("active")) ?? null;
        const currentVisibleStep = steps.find(step => step.hasClass("es-mobile-visible")) ?? null;
        const targetStep = currentVisibleStep ?? activeStep ?? steps[0] ?? null;

        this.lastResponsiveActiveStepId = activeStep?.attr("id") ?? null;
        this.applyResponsiveVisibleStep(targetStep, steps);
    }

    handleResponsiveStepChange(event) {
        if (!this.responsiveStepsContainer?.length) {
            return;
        }

        const stepId = event?.detail?.stepId ?? null;
        const steps = this.getResponsiveStepElements();
        const targetStep = stepId == null
            ? null
            : steps.find(step => step.attr("id") === stepId) ?? null;

        if (!this.isResponsiveStepNavigationEnabled() || targetStep == null) {
            this.syncResponsiveStepNavigationState();
            return;
        }

        this.lastResponsiveActiveStepId = stepId;
        this.responsiveStepsContainer.addClass("es-mobile-step-view");
        this.applyResponsiveVisibleStep(targetStep, steps);
    }

    navigateResponsiveStep(direction) {
        const steps = this.getResponsiveStepElements();
        const currentIndex = this.getResponsiveVisibleStepIndex(steps);
        const targetIndex = currentIndex + direction;

        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= steps.length) {
            this.updateResponsiveStepNavigationButtons(steps);
            return;
        }

        this.applyResponsiveVisibleStep(steps[targetIndex], steps);
    }

    initMobileCertTypeVisibility() {
        if (!this.certTypeSelect.length || !this.toolsBar.length) {
            return;
        }

        this.updateMobileCertTypeVisibility();

        this.certTypeObserver?.disconnect?.();
        this.certTypeObserver = new MutationObserver(() => this.updateMobileCertTypeVisibility());
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
    }

    checkAfterChangeSignType() {
        let self = this;
        if($("#certType").val() == null) {
            this.checkSignOptions();
            this.syncSignatureStepUi();
            this.updateMobileCertTypeVisibility();
            return;
        }
        if($("#certType").val() === "nexuCert") {
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
        let value = this.certTypeSelect.val();
        $("#alert-sign-present").hide();
        if (value !== "userCert") {
            this.signatureFlowController.setContextualPassword("");
        }
        if (value === "nexuCert") {
            $("#nexuCheck").removeClass('d-none');
        } else {
            $("#nexuCheck").addClass('d-none');
        }
        if (value === "imageStamp") {
            $("#alert-sign-present").show();
        }
        if (value === "sealCert") {
            this.ensureSealCertificateSelection();
            $("#sealChoose").removeClass('d-none');
        } else {
            $("#sealChoose").addClass('d-none');
        }
        this.syncSignatureStepUi();
        this.updateMobileCertTypeVisibility();
    }

    launchNoInfiniteSign(next) {
        return this.signatureFlowController.launchNoInfiniteSign(next);
    }

    launchSign(e) {
        return this.signatureFlowController.launchSign(e);
    }

    reset() {
        return this.signatureFlowController.reset();
    }

    redirect() {
        document.location.href="/user/signrequests/" + this.signRequestId;
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
