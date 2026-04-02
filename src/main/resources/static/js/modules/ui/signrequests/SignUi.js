import {WorkspacePdf} from "./WorkspacePdf.js?version=@version@";
import {CsrfToken} from "../../../prototypes/CsrfToken.js?version=@version@";
import {Step} from "../../../prototypes/Step.js?version=@version@";
import {Nexu} from "./Nexu.js?version=@version@";
import {Recipient} from "../../../prototypes/Recipient.js?version=@version@";

export class SignUi {

    constructor(id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, comments, spots, isPdf, currentStepNumber, currentStepMultiSign, currentStepSingleSignWithAnnotation, currentStepMinSignLevel, workflow, signImages, userName, authUserName, csrf, fields, stepRepeatable, status, action, nbSignRequests, notSigned, attachmentAlert, attachmentRequire, isOtp, restore, phone, returnToHome) {
        console.info("Starting sign UI for " + id);
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.returnToHome = returnToHome;
        this.signRequestId = id;
        this.signable = signable;
        this.percent = 0;
        this.isOtp = isOtp;
        this.wait = $('#wait');
        this.signForm = document.getElementById("signForm");
        this.csrf = new CsrfToken(csrf);
        this.isPdf = isPdf;
        this.formId = formId;
        this.dataId = dataId;
        this.currentSignType = currentSignType;
        this.notSigned = notSigned;
        this.workspace = new WorkspacePdf(isPdf, id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, comments, spots, currentStepNumber, currentStepMultiSign, currentStepSingleSignWithAnnotation, workflow, signImages, userName, authUserName, fields, stepRepeatable, status, this.csrf, action, notSigned, attachmentAlert, attachmentRequire, isOtp, restore, phone);
        this.signRequestUrlParams = "";
        this.signComment = $('#signComment');
        this.signModal = $('#signModal');
        this.stepRepeatable = stepRepeatable;
        this.currentStepNumber = currentStepNumber;
        this.currentStepMinSignLevel = currentStepMinSignLevel;
        this.gotoNext = null;
        this.certTypeSelect = $("#certType");
        this.sealCertificatSelect = $("#sealCertificat");
        this.nbSignRequests = nbSignRequests;
        this.attachmentRequire = attachmentRequire;
        this.attachmentAlert = attachmentAlert;
        this.signLaunchButton = $("#signLaunchButton");
        this.saveOptionText =  $("#certType > option[value='imageStamp']").text();
        $("#password").hide();
        this.initListeners();
        if(status !== "archived" && status !== "cleaned" && currentSignType !== "form") {
            this.initReportModal();
        }
        // this.checkAfterChangeSignType();
        this.nexu = this.checkSignOptions();
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
                let modal = "<div class=\"modal fade\" data-bs-focus=\"false\" id=\"reportModal\" tabindex=\"-1\" role=\"dialog\" aria-hidden=\"true\">" +
                    "<div class=\"modal-dialog modal-lg\">" +
                    "<div class=\"modal-content\">" +
                    "<div class=\"modal-header\">" +
                    "<h5 class=\"modal-title\" id=\"exampleModalLabel\">Validation de la signature</h5>\n" +
                    "<button class=\"btn btn-sm btn-close text-dark float-end position-relative\" style='z-index: 2' onclick=\"$('#reportModal').modal('toggle');\"></button>" +
                    "</div>" +
                    "<div class=\"modal-body\">" +
                    data +
                    "</div></div></div></div>";
                $("body").append(modal);
                // $('#reportModal').on('hidden.bs.modal', function () {
                //     $("div[id^='report_']").each(function() {
                //         $(this).show();
                //     });
                // })
                $("#reportSpinner").hide();
                let reportModalBtn = $("#reportModalBtn");
                reportModalBtn.removeClass("d-none");
                // $("#reportModal .modal-content").addClass(reportModalBtn.attr("es-modal-style"));
            }
        });
    }

    launchSignModal() {
        console.info("launch sign modal");
        window.onbeforeunload = null;
        this.workspace.signPosition.lockSigns();
        let self = this;
        if (this.isPdf && this.currentSignType !== 'hiddenVisa') {
            this.workspace.saveData(true);
            this.workspace.pdfViewer.checkForm().then(function (result) {
                if (result === "ok") {
                    let signId = self.workspace.checkSignsPositions();
                    if (signId != null) {
                        $("#certType > option[value='imageStamp']").remove();
                        if(self.workspace.currentSignRequestParamses.length > 0 || self.stepRepeatable) {
                            bootbox.alert("Merci de placer la signature", function () {
                                let signSpace = $("#signSpace_" + signId);
                                if(signSpace.length) {
                                    self.workspace.pdfViewer.animateScrollToPosition(parseInt(signSpace.css('top').replace('px', ''), 10));
                                }
                            });
                        } else {
                            if(self.currentSignType === 'signature') {
                                bootbox.confirm({
                                    message: "<div class='alert alert-secondary'><h4>Attention, vous allez signer sans appliquer d’image de signature</h4>Vous pouvez continuer mais, dans ce cas, un certificat électronique sera nécessaire.</div>",
                                    buttons: {
                                        cancel: {
                                            label: '<i class="fa fa-undo"></i> Ajouter une signature',
                                            className: 'btn-primary'
                                        },
                                        confirm: {
                                            label: '<i class="fa fa-arrow-right"></i> Continuer sans visuel',
                                            className: 'btn-secondary'
                                        }
                                    },
                                    callback: function (result) {
                                        if (result) {
                                            self.checkAttachement();
                                        } else {
                                            $("#addSignButton").click();
                                        }
                                    }
                                });
                            } else {
                                bootbox.alert({
                                    message: "Pour cette étape de visa, vous devez obligatoirement insérer un visuel de signature",
                                    callback: function (result) {
                                        $("#addSignButton2").click();
                                    }
                                });
                            }
                            self.checkSignOptions();
                        }
                    } else {
                        if(self.notSigned && (self.currentSignType === "signature" || self.currentSignType === "visa") && (self.currentStepMinSignLevel === "simple")) {
                            $('#certType').prepend($('<option>', {
                                value: 'imageStamp',
                                text: self.saveOptionText
                            }));
                        }
                        self.checkSignOptions();
                        self.certTypeSelect.children().each(function(e) {
                            if($(this).val() === "imageStamp" && (self.currentSignType === "signature" || self.currentSignType === "visa")) {
                                $(this).removeAttr('disabled');
                                $("#no-options").hide();
                                $("#no-options-alert").hide();
                                $("#selectTypeDiv").show();
                                $("#checkValidateSignButtonEnd").show();
                                $("#checkValidateSignButtonNext").show();
                            }
                        });
                        if(self.currentSignType === "visa") {
                            $("#certType").val('imageStamp');
                        }
                        self.checkAttachement();
                    }
                }
            });
        } else {
            self.checkAttachement();
        }
    }

    checkSignOptions() {
        console.info("check sign options");
        if (this.signable) {
            let nexu = new Nexu(null, null, this.currentSignType, null, null);
            $("#certType").focus();
            return nexu;
        }
    }

    checkAttachement() {
        let self = this;
        if (this.attachmentRequire) {
            bootbox.dialog({
                message: "Vous devez joindre un document à cette étape avant de signer",
                buttons: {
                    close: {
                        label: 'Fermer'
                    }
                },
                callback: function (result) {
                }
            });
        } else if (this.attachmentAlert) {
            bootbox.confirm({
                message: "Attention, il est demandé de joindre un document à cette étape avant de signer",
                buttons: {
                    cancel: {
                        label: '<i class="fa fa-times"></i> Retour'
                    },
                    confirm: {
                        label: '<i class="fa fa-check"></i> Continuer sans pièce jointe'
                    }
                },
                callback: function (result) {
                    if (result) {
                        self.confirmLaunchSignModal();
                    }
                }
            });
        } else {
            this.confirmLaunchSignModal();
        }
    }

    confirmLaunchSignModal() {
        let enableInfinite = $("#enableInfinite");
        enableInfinite.unbind();
        enableInfinite.on("click", function () {
            $("#infiniteForm").toggleClass("d-none");
            $("#launchNoInfiniteSignButtonEnd").toggle();
            $("#launchNoInfiniteSignButtonNext").toggle();
            $("#signCommentNoInfinite").toggle();
        });
        let signModal = $("#signModal");
        signModal.on('shown.bs.modal', function () {
            $("#checkValidateSignButtonEnd").focus();
            let checkValidateSignButtonNext = $("#checkValidateSignButtonNext");
            if(checkValidateSignButtonNext != null) {
                checkValidateSignButtonNext.focus();
            }
        });
        // signModal.modal('show');
        this.launchSign();
    }

    checkAfterChangeSignType() {
        let self = this;
        if($("#certType").val() == null) {
            this.checkSignOptions();
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
                // $("#certType > option[value='nexuCert']").removeAttr('disabled');
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
        this.workspace.signPosition.goStep3();
    }

    launchNoInfiniteSign(next) {
        this.signComment = $("#signComment");
        this.launchSign(next);
    }

    launchSign(e) {
        $("#checkValidateSignButtonNext").attr("disabled", "disabled");
        $("#checkValidateSignButtonEnd").attr("disabled", "disabled");
        let signModal = $('#signModal');
        if(this.certTypeSelect.val() === '' || this.certTypeSelect.val() === null) {
            bootbox.alert("<div class='alert alert-danger'>Merci de choisir un type de signature dans la liste déroulante</div>", null);
            return;
        }
        if (this.isPdf && this.workspace.checkSignsPositions() != null && this.workspace.signType !== "hiddenVisa" && (this.certTypeSelect.val() === 'imageStamp')) {
            bootbox.alert("Merci de placer la signature", null);
            signModal.modal('hide');
            return;
        }
        $(window).unbind("beforeunload");
        if(e != null) {
            this.gotoNext = $(e.currentTarget).attr("data-es-next-url");
        }
        signModal.modal('hide');
        $('#stepRepeatableModal').modal('hide');
        this.percent = 0;
        let good = true;
        if(this.signForm) {
            let inputs = this.signForm.getElementsByTagName("input");
            for (let i = 0, len = inputs.length; i < len; i++) {
                let input = inputs[i];
                if (!input.checkValidity()) {
                    good = false;
                }
            }
        }
        if(good) {
            console.log('launch sign for : ' + this.signRequestId);
            this.wait.modal('show');
            this.wait.modal({backdrop: 'static', keyboard: false});
            if(this.isPdf) {
                this.workspace.pdfViewer.promiseSaveValues().then(e => this.submitSignRequest());
            } else {
                this.submitSignRequest();
            }
        } else {
            this.signModal.on('hidden.bs.modal', function () {
                $("#checkDataSubmit").click();
            })
        }
    }

    submitSignRequest() {
        let self = this;
        let signaturesCheck = true;
        let formData = { };
        if(this.isPdf) {
            $.each($('#signForm').serializeArray(), function () {
                if (!this.name.startsWith("comment")) {
                    formData[this.name] = this.value;
                }
            });
            if(this.formId != null) {
                this.workspace.pdfViewer.savedFields.forEach((value, key) => {
                    formData[key] = value;
                });
            }
        }
        if(this.workspace != null) {
            let signRequestParamses = Array.from(this.workspace.signPosition.signRequestParamses.values());
            let signRequestParamsesToSend = signRequestParamses.map(function (originalParams){
                let signScale = self.normalizeFloat(originalParams.signScale, 1, 0.01);
                let signPageNumber = self.normalizeInteger(originalParams.signPageNumber, 1, 1);
                let xPos = self.normalizeInteger(originalParams.xPos, 0, 0);
                let yPos = self.normalizeInteger(originalParams.yPos, 0, 0);
                // If the signature is dropped on a predefined slot, slot coordinates are the source of truth.
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
                    signPageNumber: signPageNumber,
                    signDocumentNumber: self.normalizeInteger(originalParams.signDocumentNumber, 0, 0),
                    signWidth: self.normalizeInteger(originalParams.signWidth / signScale, 200, 1),
                    signHeight: self.normalizeInteger(originalParams.signHeight / signScale, 100, 1),
                    xPos: xPos,
                    yPos: yPos,
                    rotate: self.normalizeInteger(self.workspace.pdfViewer.rotation, 0, 0),
                    signImageNumber: self.normalizeInteger(originalParams.signImageNumber, 0),
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
                    red: self.normalizeInteger(originalParams.red, 0, 0),
                    green: self.normalizeInteger(originalParams.green, 0, 0),
                    blue: self.normalizeInteger(originalParams.blue, 0, 0),
                    fontSize: self.normalizeInteger(originalParams.fontSize, self.globalProperties?.defaultFontSize ?? 16, 1),
                };
                if(originalParams.userSignaturePad != null) {
                    if(originalParams.userSignaturePad.signaturePad.isEmpty()) {
                        signaturesCheck = false;
                    } else {
                        originalParams.userSignaturePad.save();
                        paramToSend.imageBase64 = originalParams.userSignaturePad.signImageBase64Val;
                    }
                }
                return paramToSend;
            });
            this.signRequestUrlParams = {
                'password' : $("#password").val(),
                'certType' : this.certTypeSelect.val(),
                'signAll' : $("#sign-all").prop("checked"),
                'sealCertificat' : this.sealCertificatSelect.val(),
                'signRequestParams' : JSON.stringify(signRequestParamsesToSend),
                // 'visual' : visual,
                'comment' : this.signComment.val(),
                'formData' : JSON.stringify(formData)
            };
        } else {
            this.signRequestUrlParams = {
                "password": document.getElementById("password").value,
            }
        }
        if(signaturesCheck) {
            this.sendData(this.signRequestUrlParams);
        } else {
            bootbox.alert("Une signature est vide", null);
        }
    }

    sendData(signRequestUrlParams) {
        this.reset();
        let self = this;
        console.log("start sign");
        console.log(self.signRequestId);
        $.ajax({
            url: "/ws-secure/global/sign/" + this.signRequestId + "?" + self.csrf.parameterName + "=" + self.csrf.token,
            type: 'POST',
            data: signRequestUrlParams,
            success: function(data, textStatus, xhr) {
                if(data === "initNexu") {
                    document.location.href="/nexu-sign/start?ids=" + self.signRequestId;
                } else {
                    if (self.gotoNext != null) {
                        document.location.href = self.gotoNext;
                    } else {
                        if(self.isOtp== null || !self.isOtp) {
                            if(self.returnToHome == null) {
                                if (self.nbSignRequests > 1 || !self.globalProperties.returnToHomeAfterSign) {
                                    document.location.href = "/user/signrequests/" + self.signRequestId;
                                } else {
                                    document.location.href = "/user";
                                }
                            } else {
                                if(self.returnToHome) {
                                    document.location.href = "/user";
                                } else {
                                    document.location.href = "/user/signrequests/" + self.signRequestId;
                                }
                            }
                        } else {
                            document.location.href = "/otp/signrequests/" + self.signRequestId;
                        }
                    }
                }
            },
            error: function(data, textStatus, xhr) {
                $("#checkValidateSignButtonEnd").removeAttr("disabled");
                $("#checkValidateSignButtonNext").removeAttr("disabled");
                $("#signSpinner").hide();
                console.error("sign error : " + data.responseText);
                document.getElementById("signError").style.display = "block";
                document.getElementById("signError").innerHTML =
                    "<p>Une erreur s’est produite lors de la signature du document.</p>" +
                    "<small>Message retourné par le système de signature : " + data.responseText + "</small>";
                document.getElementById("closeModal").style.display = "block";
            }
        });
    }

    reset() {
        this.percent = 0;
        $("#signSpinner").show();
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
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
