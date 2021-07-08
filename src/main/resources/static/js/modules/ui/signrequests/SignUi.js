import {WorkspacePdf} from "./WorkspacePdf.js";
import {CsrfToken} from "../../../prototypes/CsrfToken.js";
import {Step} from "../../../prototypes/Step.js";

export class SignUi {

    constructor(id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, postits, isPdf, currentStepNumber, currentStepId, currentStepMultiSign, workflow, signImages, userName, csrf, fields, stepRepeatable, status, action, nbSignRequests, notSigned, attachmentRequire) {
        console.info("Starting sign UI");
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.signRequestId = id;
        this.percent = 0;
        this.getProgressTimer = null;
        this.wait = $('#wait');
        this.workspace = null;
        this.signForm = document.getElementById("signForm");
        this.csrf = new CsrfToken(csrf);
        this.isPdf = isPdf;
        this.workspace = new WorkspacePdf(isPdf, id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, postits, currentStepNumber, currentStepId, currentStepMultiSign, workflow, signImages, userName, currentSignType, fields, stepRepeatable, status, this.csrf, action, notSigned, attachmentRequire);
        this.signRequestUrlParams = "";
        this.signComment = $('#signComment');
        this.signModal = $('#signModal');
        this.stepRepeatable = stepRepeatable;
        this.currentStepNumber = currentStepNumber;
        this.gotoNext = false;
        this.certTypeSelect = $("#certType");
        this.nbSignRequests = nbSignRequests;
        this.initListeners();
        if(status !== "exported") {
            this.initReportModal();
        }

    }

    initListeners() {
        $("#checkValidateSignButtonNexu").on('click', e => this.launchSign(false));
        $("#checkValidateSignButtonEnd").on('click', e => this.launchSign(false));
        $("#checkValidateSignButtonNext").on('click', e => this.launchSign(true));
        $("#launchInfiniteSignButton").on('click', e => this.insertStep());
        $("#launchNoInfiniteSignButton").on('click', e => this.launchNoInfiniteSign());
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                $("#launchNoInfiniteSignButton").click();
            }
        });
        if(this.certTypeSelect) {
            this.certTypeSelect.on("change", e => this.togglePasswordField());
        }

        $("#copyButton").on('click', e => this.copy());
        // document.addEventListener("sign", e => this.updateWaitModal(e));
    }

    initReportModal() {
        let self = this;
        $.ajax({
            url: "/user/validation/short/" + self.signRequestId,
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

    togglePasswordField(){
        let value = $("#certType").val();
        if(value === "etab") {
            $("#password").hide();
        } else {
            $("#password").show();
        }
    }

    launchNoInfiniteSign() {
        this.signComment = $("#signCommentNoInfinite");
        this.launchSign(false);
    }

    launchSign(gotoNext) {
        this.gotoNext = gotoNext;
        $('#signModal').modal('hide');
        $('#stepRepeatableModal').modal('hide');
        this.percent = 0;
        let good = true;
        if(this.signForm) {
            let inputs = this.signForm.getElementsByTagName("input");
            for (var i = 0, len = inputs.length; i < len; i++) {
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
                this.workspace.pdfViewer.promizeSaveValues().then(e => this.submitSignRequest());
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
            for(let i = 0 ; i < signRequestParamses.length; i++) {
                signRequestParamses[i].cross = null;
                signRequestParamses[i].border = null;
                signRequestParamses[i].tools = null;
                signRequestParamses[i].textareaExtra = null;
                signRequestParamses[i].defaultTools = null;
                signRequestParamses[i].divExtra = null;
                signRequestParamses[i].signColorPicker = null;
            }
            this.signRequestUrlParams = {
                'password' : $("#password").val(),
                'certType' : $("#certType").val(),
                'signRequestParams' : JSON.stringify(Array.from(this.workspace.signPosition.signRequestParamses.values())),
                'visual' : this.workspace.signPosition.visualActive,
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
            url: "/user/signrequests/sign/" + this.signRequestId + "/?" + self.csrf.parameterName + "=" + self.csrf.token,
            type: 'POST',
            data: signRequestUrlParams,
            success: function(data, textStatus, xhr) {
                if(data === "initNexu") {
                    document.location.href="/user/nexu-sign/" + self.signRequestId;
                } else {
                    if (self.gotoNext) {
                        document.location.href = $("#nextSignRequestButton").attr('href');
                    } else {
                        if (self.nbSignRequests > 1 || !self.globalProperties.returnToHomeAfterSign) {
                            document.location.href = "/user/signrequests/" + self.signRequestId;
                        } else {
                            document.location.href = "/user/";
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
        this.signComment = $("#signCommentInfinite");
        step.recipientsEmails = selectedRecipients;
        step.stepNumber = this.currentStepNumber;
        step.allSignToComplete = $('#allSignToCompleteInfinite').is(':checked');
        step.multiSign = $('#multiSign').is(':checked');
        step.signType = $('#signTypeInfinite').val();
        $.ajax({
            url: "/user/signbooks/add-repeatable-step/" + signRequestId + "/?" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(step),
            success: response => this.launchSign()
        });
    }

}
