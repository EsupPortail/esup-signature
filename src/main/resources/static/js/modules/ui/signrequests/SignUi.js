import {WorkspacePdf} from "./WorkspacePdf.js";
import {CsrfToken} from "../../../prototypes/CsrfToken.js";
import {PrintDocument} from "../../utils/PrintDocument.js";
import {Step} from "../../../prototypes/Step.js";

export class SignUi {

    constructor(id, currentSignRequestParams, currentSignType, signable, postits, isPdf, currentStepNumber, signImages, userName, csrf, fields, stepRepeatable) {
        console.info("Starting sign UI");
        this.signRequestId = id;
        this.percent = 0;
        this.getProgressTimer = null;
        this.wait = $('#wait');
        this.passwordError = document.getElementById("passwordError");
        this.workspace = null;
        this.signForm = document.getElementById("signForm");
        this.workspace = new WorkspacePdf(isPdf, id, currentSignRequestParams, currentSignType, signable, postits, currentStepNumber, signImages, userName, currentSignType, fields);
        this.csrf = new CsrfToken(csrf);
        this.xmlHttpMain = new XMLHttpRequest();
        this.signRequestUrlParams = "";
        this.signComment = $('#signComment');
        this.signModal = $('#signModal');
        this.stepRepeatable = stepRepeatable;
        this.currentStepNumber = currentStepNumber;
        this.printDocument = new PrintDocument();
        this.gotoNext = false;
        this.initListeners();
    }

    initListeners() {
        $("#checkRepeatableButtonEnd").on('click', e => this.checkRepeatable(false));
        $("#checkRepeatableButtonNext").on('click', e => this.checkRepeatable(true));
        $("#launchSignButton").on('click', e => this.insertStep());
        $("#launchAllSignButton").on('click', e => this.launchSign());
        //$("#launchAllSignButton").on('click', e => this.launchAllSign());
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                $("#launchSignButton").click();
            }
        });
        $("#copyButton").on('click', e => this.copy());
        document.addEventListener("sign", e => this.updateWaitModal(e));
    }

    launchSign() {
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
            this.submitSignRequest();
        } else {
            this.signModal.on('hidden.bs.modal', function () {
                $("#checkDataSubmit").click();
            })
        }
    }

    // launchAllSign() {
    //     $('#signAllModal').modal('hide');
    //     this.wait.modal('show');
    //     this.wait.modal({backdrop: 'static', keyboard: false});
    //     let csrf = document.getElementsByName("_csrf")[0];
    //     let signRequestParams = "password=" + document.getElementById("passwordAll").value +
    //         "&" + csrf.name + "=" + csrf.value;
    //     let xmlHttp = new XMLHttpRequest();
    //     xmlHttp.open('POST', '/user/signbooks/sign/' + this.signRequestId, true);
    //     xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
    //     xmlHttp.send(signRequestParams);
    // }

    submitSignRequest() {
        let formData = { };
        $.each($('#signForm').serializeArray(), function() {
            if(!this.name.startsWith("comment")) {
                formData[this.name] = this.value;
            }
        });
        this.workspace.pdfViewer.savedFields.forEach((value, key)=>{
            formData[key] = value;
        });
        if(this.workspace != null) {
            this.signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&sseId=" + sessionStorage.getItem("sseId") +
                "&signRequestParams=" + JSON.stringify(Array.from(this.workspace.signPosition.signRequestParamses.values())) +
                "&visual=" + this.workspace.signPosition.visualActive +
                "&comment=" + this.signComment.val() +
                "&formData=" + JSON.stringify(formData) +
                "&" + this.csrf.parameterName + "=" + this.csrf.token
            ;
        } else {
            this.signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&sseId=" + sessionStorage.getItem("sseId") +
                "&" + this.csrf.name + "=" + this.csrf.value;
        }
        console.info("params to send : " + this.signRequestUrlParams);
        this.sendData(this.signRequestUrlParams);
    }

    sendData(signRequestUrlParams) {
        this.reset();
        this.xmlHttpMain.open('POST', '/user/signrequests/sign/' + this.signRequestId, true);
        this.xmlHttpMain.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        this.xmlHttpMain.send(signRequestUrlParams);
    }

    updateWaitModal(e) {
        console.info("update wait modal");
        let message = e.detail
        console.info(message);
        this.percent = this.percent + 5;
        if(message.type === "sign_system_error" || message.type === "not_authorized") {
            console.error("sign error : system error");
            document.getElementById("signError").style.display = "block";
            document.getElementById("signError").innerHTML =" Erreur du système de signature : <br>" + message.text;
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
        } else if(message.type === "initNexu") {
            console.info("redirect to NexU sign proccess");
            document.location.href="/user/nexu-sign/" + this.signRequestId;
        }else if(message.type === "end") {
            console.info("sign end");
            document.getElementById("bar-text").innerHTML = "";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            document.getElementById("bar-text").innerHTML = message.text;
            document.getElementById("bar").style.width = 100 + "%";
            if(this.gotoNext) {
                document.location.href = $("#nextSignRequestButton").attr('href');
            } else {
                document.location.href = "/user/signrequests";
            }
        } else {
            console.debug("update bar");
            document.getElementById("bar").style.display = "block";
            document.getElementById("bar").style.width = this.percent + "%";
            document.getElementById("bar-text").innerHTML = message.text;
        }
    }

    reset() {
        this.percent = 0;
        this.passwordError.style.display = "none";
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
        document.getElementById("bar").style.display = "none";
        document.getElementById("bar").classList.add("progress-bar-animated");
    }

    end() {
        if(this.xmlHttpMain.status === 200) {
            console.info("sign end");
            document.getElementById("validModal").style.display = "block";
            setTimeout(e => this.redirect(),500);
        }
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

    checkRepeatable(gotoNext) {
        this.gotoNext = gotoNext;
        if (this.stepRepeatable) {
            this.signModal.modal('hide');
            $('#stepRepeatableModal').modal('show');
        } else {
            this.signModal.modal('hide');
            this.launchSign();
        }
    }

    insertStep() {
        let signRequestId = this.signRequestId;
        let csrf = this.csrf;
        let step = new Step();
        step.recipientsEmails = $('#recipientsEmailsInfinite').find(`[data-check='true']`).prevObject[0].slim.selected();
        step.stepNumber = this.currentStepNumber + 1;
        step.allSignToComplete = $('#allSignToCompleteInfinite').is(':checked');
        step.signType = $('#signTypeInfinite').val();
        $.ajax({
            url: "/user/signbooks/add-repeatable-step/" + signRequestId + "/?" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            contentType: "application/json",
            dataType: 'json',
            data: JSON.stringify(step),
            success: response => this.launchSign()
        });
    }
}