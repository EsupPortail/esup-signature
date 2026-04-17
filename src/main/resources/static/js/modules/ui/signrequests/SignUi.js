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
        this.signModal = $('#signModal');
        this.certTypeSelect = $("#certType");
        this.sealCertificatSelect = $("#sealCertificat");
        this.signLaunchButton = $("#signLaunchButton");
        this.toolsBar = $("#tools");
        this.certTypeObserver = null;
        this.saveOptionText =  $("#certType > option[value='imageStamp']").text();
        this.signatureFlowController = new SignatureFlowController(this);
        $("#password").hide();
        this.initListeners();
        this.initMobileCertTypeVisibility();
        if(signUiDto.status !== "archived" && signUiDto.status !== "cleaned" && signUiDto.currentSignType !== "form") {
            this.initReportModal();
        }
        this.nexu = this.checkSignOptions();
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
        $("#launch-infinite-sign-button").on('click', e => this.insertStep());
        $("#launchNoInfiniteSignButtonEnd").on('click', e => this.launchNoInfiniteSign());
        $("#launchNoInfiniteSignButtonNext").on('click', e => this.launchNoInfiniteSign(e));
        $("#refresh-certType").on('click', e => this.checkSignOptions());
        $("#refresh-certType2").on('click', e => this.checkSignOptions());
        let self = this;
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                let checkValidateSignButtonNext = $("#checkValidateSignButtonNext");
                if (checkValidateSignButtonNext.length > 0) {
                    self.launchSign(true);
                } else {
                    self.launchSign(false);
                }
            }
        });
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
                $("#reportModalBtn").removeClass("d-none");
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
            this.updateMobileCertTypeVisibility();
            return nexu;
        }
    }

    getSelectableCertTypeCount() {
        return this.certTypeSelect.find("option:not(:disabled):not([unavailable])").length;
    }

    updateMobileCertTypeVisibility() {
        if (!this.toolsBar.length || !this.certTypeSelect.length) {
            return;
        }
        this.toolsBar.toggleClass("es-tools-single-cert-type-mobile", this.getSelectableCertTypeCount() === 1);
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
        return Array.from(signPlacementController.signRequestParamses.values()).some(signRequestParams =>
            signRequestParams != null
            && signRequestParams.isSign
            && signRequestParams.signImageNumber != null
            && signRequestParams.signImageNumber >= 0
            && signRequestParams.signImageNumber !== 999999
        );
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
            return;
        }
        if (typeof signPlacementController.refreshSteps === "function") {
            signPlacementController.refreshSteps();
            return;
        }
        if (this.hasValidSelectedCertType() && this.hasPendingSignaturePlacement()) {
            signPlacementController.goStep3();
            return;
        }
        if (this.hasValidSelectedCertType()) {
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
        if (value === "userCert") {
            $("#password").show();
        } else {
            $("#password").hide();
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

    insertStep() {
        console.info("check insert step");
        let signRequestId = this.signRequestId;
        let csrf = this.csrf;
        let step = new Step();
        let recipientsEmails = $('#recipientsEmails').find(`[data-es-check-cert='true']`).prevObject[0].slim.getSelected();
        if(recipientsEmails.length === 0 ) {
            $("#infiniteFormSubmit").click();
            return;
        }
        recipientsEmails.forEach(function(email) {
            let recipient = new Recipient();
            recipient.email = email;
            let id = email.replaceAll("@", "_").replaceAll(".", "_");
            let extInfos = $("div[id='recipient_" + id + "']");
            recipient.name = extInfos.find("#name_" + id).val();
            recipient.firstName = extInfos.find("#firstname_" + id).val();
            recipient.phone = extInfos.find("#phone_" + id).val();
            recipient.forceSms = extInfos.find("#forcesms_" + id).prop("checked");
            step.recipients.push(recipient);
        });

        let self = this;
        this.signComment = $("#signComment");
        step.stepNumber = this.currentStepNumber;
        step.allSignToComplete = $('#allSignToComplete').is(':checked');
        step.multiSign = $('#multiSign').is(':checked');
        step.autoSign = $('#autoSign').is(':checked');
        step.signType = $('#signType').val();
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
                $("#password").val($("#passwordInfinite").val());
                self.launchSign();
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
