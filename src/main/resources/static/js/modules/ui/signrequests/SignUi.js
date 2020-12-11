import {WorkspacePdf} from "./WorkspacePdf.js";
import {CsrfToken} from "../../../prototypes/CsrfToken.js";
import {PrintDocument} from "../../utils/PrintDocument.js";

export class SignUi {

    constructor(id, currentSignRequestParams, currentSignType, signable, postits, isPdf, currentStepNumber, signImages, userName, csrf, fields) {
        console.info("Starting sign UI");
        this.signRequestId = id;
        this.percent = 0;
        this.getProgressTimer = null;
        this.wait = $('#wait');
        this.passwordError = document.getElementById("passwordError");
        this.workspace = null;
        this.signForm = document.getElementById("signForm");
        this.workspace = new WorkspacePdf(isPdf, id, currentSignRequestParams, currentSignType, signable, postits, currentStepNumber, signImages, userName, currentSignType);
        this.csrf = new CsrfToken(csrf);
        this.xmlHttpMain = new XMLHttpRequest();
        this.signRequestUrlParams = "";
        this.signComment = $('#signComment');
        this.signModal = $('#signModal');
        this.printDocument = new PrintDocument();
        this.initListeners();
        this.initDataFileds(fields);
    }

    initListeners() {
        $("#launchSignButton").on('click', e => this.launchSign());
        //$("#launchAllSignButton").on('click', e => this.launchAllSign());
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                $("#launchSignButton").click();
            }
        });
        $("#copyButton").on('click', e => this.copy());
        $("#print").on('click', e => this.launchPrint());
        document.addEventListener("sign", e => this.updateWaitModal(e));
    }

    initDataFileds(fields) {
        if(this.workspace.pdfViewer) {
            this.workspace.pdfViewer.setDataFields(fields);
            if (this.workspace.pdfViewer.dataFields.length > 0 && this.workspace.pdfViewer.dataFields[0].defaultValue != null) {
                for (let i = 0 ; i < signUi.workspace.pdfViewer.dataFields.length ; i++) {
                    this.workspace.pdfViewer.savedFields.set(this.workspace.pdfViewer.dataFields[i].name, this.workspace.pdfViewer.dataFields[i].defaultValue);
                }
            }
        }
    }

    launchPrint() {
        this.printDocument.launchPrint("/user/signrequests/get-last-file-base-64/" + this.signRequestId)
    }

    launchSign() {
        this.signModal.modal('hide');
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
        if(this.workspace != null) {
            this.signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&sseId=" + sessionStorage.getItem("sseId") +
                "&signRequestParams=" + JSON.stringify(this.workspace.signPosition.signRequestParamses) +
                "&visual=" + this.workspace.signPosition.visualActive +
                "&comment=" + this.signComment.val() +
                // "&formData=" + JSON.stringify(formData) +
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
        //this.xmlHttpMain.addEventListener('readystatechange', e => this.end());
        this.xmlHttpMain.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        // this.xmlHttpMain.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        this.xmlHttpMain.send(signRequestUrlParams);
        //this.getProgressTimer = setInterval(e => this.getStep(), 500);
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
            document.location.href="/user/signrequests/" + this.signRequestId;
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
}