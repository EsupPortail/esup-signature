import {WorkspacePdf} from "./WorkspacePdf.js?version=@version@";
import {CsrfToken} from "../../../prototypes/CsrfToken.js?version=@version@";
import {Step} from "../../../prototypes/Step.js?version=@version@";

export class SignUi {

    constructor(id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, postits, isPdf, currentStepNumber, currentStepId, currentStepMultiSign, workflow, signImages, userName, authUserName, csrf, fields, stepRepeatable, status, action, nbSignRequests, notSigned, attachmentAlert, attachmentRequire, isOtp, restore, phone) {
        console.info("Starting sign UI");
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.signRequestId = id;
        this.percent = 0;
        this.isOtp = isOtp;
        this.wait = $('#wait');
        this.signForm = document.getElementById("signForm");
        this.csrf = new CsrfToken(csrf);
        this.isPdf = isPdf;
        this.currentSignType = currentSignType;
        this.workspace = new WorkspacePdf(isPdf, id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, postits, currentStepNumber, currentStepId, currentStepMultiSign, workflow, signImages, userName, authUserName, currentSignType, fields, stepRepeatable, status, this.csrf, action, notSigned, attachmentAlert, attachmentRequire, isOtp, restore, phone);
        this.signRequestUrlParams = "";
        this.signComment = $('#signComment');
        this.signModal = $('#signModal');
        this.stepRepeatable = stepRepeatable;
        this.currentStepNumber = currentStepNumber;
        this.gotoNext = false;
        this.certTypeSelect = $("#certType");
        this.nbSignRequests = nbSignRequests;
        this.attachmentRequire = attachmentRequire;
        this.attachmentAlert = attachmentAlert;
        this.signLaunchButton = $("#signLaunchButton");
        this.saveOptionText =  $("#certType > option[value='imageStamp']").text();
        $("#password").hide();
        this.initListeners();
        if(status !== "exported") {
            this.initReportModal();
        }
        this.togglePasswordField();
    }

    initListeners() {
        $("#checkValidateSignButtonEnd").on('click', e => this.launchSign(false));
        $("#checkValidateSignButtonNext").on('click', e => this.launchSign(true));
        $("#launchInfiniteSignButton").on('click', e => this.insertStep());
        $("#launchNoInfiniteSignButton").on('click', e => this.launchNoInfiniteSign());
        let self = this;
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                self.launchSign(true);
            }
        });
        $("#certType").on("change", e => this.togglePasswordField());
        $("#copyButton").on('click', e => this.copy());
        $("#send").on('submit', function (e) {
            e.preventDefault();
            alert("Merci de saisir les participants");
            if ($(e.target).is(':invalid')) {
                alert("Merci de saisir les participants");
            }
        });
        this.initLaunchButtons();
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
                let modal = "<div class=\"modal fade\" id=\"reportModal\" tabindex=\"-1\" role=\"dialog\" aria-hidden=\"true\">" +
                    "<div class=\"modal-dialog modal-lg\">" +
                    "<div class=\"modal-content\">" +
                    "<div class=\"modal-body\">" +
                    data +
                    "</div></div></div></div>";
                $("body").append(modal);
                $('#reportModal').on('hidden.bs.modal', function () {
                    $("div[id^='report_']").each(function() {
                        $(this).show();
                    });
                })
                $("#reportModalBtn").removeClass("d-none");
                $("#reportModalBtn").on('click', function (){
                    $("#alertSign").remove();
                });
            }
        });
    }

    launchSignModal() {
        console.info("launch sign modal");
        window.onbeforeunload = null;
        let self = this;
        if (this.isPdf && this.currentSignType !== 'hiddenVisa') {
            this.workspace.pdfViewer.checkForm().then(function (result) {
                if (result === "ok") {
                    if (self.workspace.signPosition.signRequestParamses.size === 0) {
                        $("#certType > option[value='imageStamp']").remove();
                        if(self.workspace.currentSignRequestParamses.length > 0 || self.stepRepeatable) {
                            bootbox.alert("Merci de placer la signature", null);
                        } else {
                            bootbox.confirm({
                                message: "<h3>Attention vous allez signer ce document sans visuel</h3>" +
                                    "<div class='alert alert-danger'>Dans ce cas seules les signatures avec certificat sont possibles</div>",
                                buttons: {
                                    cancel: {
                                        label: '<i class="fa fa-undo"></i> Modifier ma signature',
                                        className: 'btn-success'
                                    },
                                    confirm: {
                                        label: '<i class="fa fa-check"></i> Continuer sans visuel'
                                    }
                                },
                                callback: function (result) {
                                    if (result) {
                                        self.checkAttachement();
                                    }
                                }
                            });
                            self.checkSignOptions();
                        }
                    } else {
                        $("#certType > option[value='imageStamp']").remove();
                        if(self.currentSignType === "pdfImageStamp" || self.currentSignType === "visa") {
                            $('#certType').prepend($('<option>', {
                                value: 'imageStamp',
                                text: self.saveOptionText
                            }));
                        }
                        self.checkSignOptions();
                        self.certTypeSelect.children().each(function(e) {
                            if($(this).val() === "imageStamp" && (self.currentSignType === "pdfImageStamp" || self.currentSignType === "visa")) {
                                $(this).removeAttr('disabled');
                                $("#noOptions").hide();
                                $("#selectTypeDiv").show();
                                $("#checkValidateSignButtonEnd").show();
                                $("#checkValidateSignButtonNext").show();
                            }
                        });
                        if(self.currentSignType === "imageStamp" || self.currentSignType === "visa") {
                            $("#certType > option[value='imageStamp']").attr('selected', 'selected');
                            $("#certType").val('imageStamp');
                        }
                        self.checkAttachement();
                    }
                    self.certTypeSelect.children().each(function(e) {
                        if(!$(this).attr('disabled')) {
                            $(this).attr('selected', 'selected');
                            $("#certType").val($(this).attr('value')).trigger('change');
                            return false;
                        }
                    });
                }
            });
        } else {
            let signModal;
            if (self.stepRepeatable) {
                signModal = $('#stepRepeatableModal');
                // $('#launchNoInfiniteSignButton').hide();
            } else {
                signModal = $("#signModal");
            }
            signModal.modal('show');
        }
    }

    checkSignOptions() {
        this.certTypeSelect.children().each(function (e) {
            let nbOptions = $("#certType option:not([disabled])").length;
            if (nbOptions === 0) {
                $("#nexuCheck").removeClass("d-none");
                $("#noOptions").show();
                $("#selectTypeDiv").hide();
                $("#checkValidateSignButtonEnd").hide();
                $("#checkValidateSignButtonNext").hide();
            } else {
                $("#nexuCheck").addClass("d-none");
                $("#noOptions").hide();
                $("#selectTypeDiv").show();
                $("#checkValidateSignButtonEnd").show();
                $("#checkValidateSignButtonNext").show();
            }
        });
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
                        label: '<i class="fa fa-times"></i> Annuler'
                    },
                    confirm: {
                        label: '<i class="fa fa-check"></i> Continuer'
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
            $("#launchNoInfiniteSignButton").toggle();
            $("#signCommentNoInfinite").toggle();
        });
        let signModal;
        if (this.stepRepeatable) {
            signModal = $('#stepRepeatableModal');
            // $('#launchNoInfiniteSignButton').hide();
        } else {
            signModal = $("#signModal");
        }
        signModal.on('shown.bs.modal', function () {
            $("#checkValidateSignButtonEnd").focus();
            $("#checkValidateSignButtonNext").focus();
        });
        signModal.modal('show');
    }

    togglePasswordField() {
        let value = this.certTypeSelect.val();
        if(value === "userCert") {
            $("#password").show();
        } else {
            $("#password").hide();
        }
        if(value === "nexuCert") {
            $("#nexuCheck").removeClass('d-none');
        } else {
            $("#nexuCheck").addClass('d-none');
        }
    }

    launchNoInfiniteSign() {
        this.signComment = $("#signCommentNoInfinite");
        $("#password").val($("#passwordInfinite").val());
        this.launchSign(false);
    }

    launchSign(gotoNext) {
        let signModal = $('#signModal');
        if(this.certTypeSelect.val() === '' || this.certTypeSelect.val() === null) {
            bootbox.alert("<div class='alert alert-danger'>Merci de choisir un type de signature dans la liste déroulante</div>", null);
            return;
        }
        if (this.isPdf && !this.workspace.checkSignsPositions() && this.workspace.signType !== "hiddenVisa" && (this.certTypeSelect.val() === 'imageStamp')) {
            bootbox.alert("Merci de placer la signature", null);
            signModal.modal('hide');
            return;
        }
        this.gotoNext = gotoNext;
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
        let formData = { };
        if(this.isPdf) {
            $.each($('#signForm').serializeArray(), function () {
                if (!this.name.startsWith("comment")) {
                    formData[this.name] = this.value;
                }
            });
            this.workspace.pdfViewer.savedFields.forEach((value, key) => {
                formData[key] = value;
            });
        }
        if(this.workspace != null) {
            let signRequestParamses = Array.from(this.workspace.signPosition.signRequestParamses.values());
            this.signRequestUrlParams = {
                'password' : $("#password").val(),
                'certType' : this.certTypeSelect.val(),
                'signRequestParams' : JSON.stringify(signRequestParamses, function replacer(key, value) {
                    if (this &&
                        (key === "events"
                        || key === "cross"
                        || key === "defaultTools"
                        || key === "tools"
                        || key === "signColorPicker"
                        || key === "textareaExtra"
                        || key === "divExtra"
                        || key === "border"
                        || key === "textareaPart")) {
                        return undefined;
                    }
                    return value;
                }),
                // 'visual' : visual,
                'comment' : this.signComment.val(),
                'formData' : JSON.stringify(formData)
            };
        } else {
            this.signRequestUrlParams = {
                "password": document.getElementById("password").value,
            }
        }
        console.info("params to send : " + this.signRequestUrlParams);
        this.sendData(this.signRequestUrlParams);
    }

    sendData(signRequestUrlParams) {
        this.reset();
        let self = this;
        $.ajax({
            url: "/ws-secure/signrequests/sign/" + this.signRequestId + "/?" + self.csrf.parameterName + "=" + self.csrf.token,
            type: 'POST',
            data: signRequestUrlParams,
            success: function(data, textStatus, xhr) {
                if(data === "initNexu") {
                    document.location.href="/user/nexu-sign/" + self.signRequestId;
                } else {
                    if (self.gotoNext) {
                        document.location.href = $("#nextSignRequestButton").attr('href');
                    } else {
                        if(self.isOtp== null || !self.isOtp) {
                            if(self.nbSignRequests > 1 || !self.globalProperties.returnToHomeAfterSign) {
                                document.location.href = "/user/signrequests/" + self.signRequestId;
                            } else {
                                document.location.href = "/user/";
                            }
                        } else {
                            document.location.href = "/otp/signrequests/" + self.signRequestId;
                        }
                    }
                }
            },
            error: function(data, textStatus, xhr) {
                $("#signSpinner").hide();
                console.error("sign error : " + data.responseText);
                document.getElementById("signError").style.display = "block";
                document.getElementById("signError").innerHTML = " Erreur du système de signature : <br>" + data.responseText;
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
        let selectedRecipients = $('#recipientsEmailsInfinite').find(`[data-check='true']`).prevObject[0].slim.selected();
        if(selectedRecipients.length === 0 ) {
            $("#infiniteFormSubmit").click();
            return;
        }
        let self = this;
        this.signComment = $("#signCommentInfinite");
        step.recipientsEmails = selectedRecipients;
        step.stepNumber = this.currentStepNumber;
        step.allSignToComplete = $('#allSignToCompleteInfinite').is(':checked');
        step.multiSign = $('#multiSign').is(':checked');
        step.autoSign = $('#autoSign').is(':checked');
        step.signType = $('#signTypeInfinite').val();
        $.ajax({
            url: "/user/signbooks/add-repeatable-step/" + signRequestId + "/?" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(step),
            success: function() {
                $("#password").val($("#passwordInfinite").val());
                self.launchSign();
            }
        });
    }

}
