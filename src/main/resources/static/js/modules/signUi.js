import {WorkspacePdf} from "./workspacePdf.js";

export class SignUi {

    constructor(id, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits, isPdf) {
        console.info("Starting sign UI");
        this.signRequestId = id;
        this.percent = 0;
        this.getProgressTimer = null;
        this.wait = $('#wait');
        this.passwordError = document.getElementById("passwordError");
        this.launchSignButton = $("#launchSignButton");
        this.launchAllSignButton = $("#launchAllSignButton");
        this.workspace = null;
        if(isPdf) {
            this.workspace = new WorkspacePdf('/user/signrequests/get-last-file/' + id, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits);
        }
        this.initListeners();
    }

    initListeners() {
        this.launchSignButton.on('click', e => this.launchSign());
        this.launchAllSignButton.on('click', e => this.launchAllSign());
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                $("#launchSignButton").click();
            }
        });
    }

    launchSign() {
        this.percent = 0;
        console.log('launch sign for : ' + this.signRequestId);
        $('#signModal').modal('hide');
        this.wait.modal('show');
        this.wait.modal({backdrop: 'static', keyboard: false});
        this.submitSignRequest(this.signRequestId);
    }

    launchAllSign() {
        $('#signAllModal').modal('hide');
        this.wait.modal('show');
        this.wait.modal({backdrop: 'static', keyboard: false});
        let csrf = document.getElementsByName("_csrf")[0];
        let signRequestParams = "password=" + document.getElementById("passwordAll").value +
            "&" + csrf.name + "=" + csrf.value;
        let xmlHttp = new XMLHttpRequest();
        xmlHttp.open('POST', '/user/signbooks/sign/' + this.signRequestId, true);
        xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        xmlHttp.send(signRequestParams);
    }

    submitSignRequest() {
        let csrf = document.getElementsByName("_csrf")[0];
        let signPageNumber = document.getElementById("signPageNumber");
        let signRequestUrlParams;
        if(signPageNumber != null) {
            signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&addDate=" + this.workspace.signPosition.dateActive +
                "&visual=" + this.workspace.signPosition.visualActive +
                "&xPos=" + this.workspace.signPosition.posX +
                "&yPos=" + this.workspace.signPosition.posY +
                "&signPageNumber=" + this.workspace.pdfViewer.pageNum +
                "&" + csrf.name + "=" + csrf.value
            ;
        } else {
            signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&" + csrf.name + "=" + csrf.value;
        }
        this.sendData(signRequestUrlParams);
    }

    sendData(signRequestUrlParams) {
        console.log("params : " + signRequestUrlParams);
        this.passwordError.style.display = "none";
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
        document.getElementById("bar").style.display = "none";
        document.getElementById("bar").classList.add("progress-bar-animated");
        this.getProgressTimer = setInterval(e => this.getStep(), 500);
        let xmlHttp = new XMLHttpRequest();
        xmlHttp.open('POST', '/user/signrequests/sign/' + this.signRequestId, true);
        xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        xmlHttp.send(signRequestUrlParams);
    }

    getStep() {
        this.percent = this.percent + 2;
        console.log("getStep");
        let xmlHttp = new XMLHttpRequest();
        xmlHttp.open("GET", "/user/signrequests/get-step", true);
        xmlHttp.addEventListener('readystatechange', e => this.updateWaitModal(xmlHttp));
        xmlHttp.send(null);
    }

    updateWaitModal(xmlHttp) {
        let result = xmlHttp.responseText;
        if (result === "security_bad_password") {
            this.passwordError.style.display = "block";
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            clearInterval(this.getProgressTimer);
        } else if(result === "sign_system_error" || result === "not_authorized") {
            clearInterval(this.getProgressTimer);
            document.getElementById("signError").style.display = "block";
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
        } else if(result === "initNexu") {
            console.info("redirect to NexU sign proccess");
            clearInterval(this.getProgressTimer);
            document.location.href="/user/nexu-sign/" + this.signRequestId;
        } else if (result === "end") {
            clearInterval(this.getProgressTimer);
            document.getElementById("validModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            document.getElementById("bar-text").innerHTML = "Signature terminée";
            document.getElementById("bar").style.width = 100 + "%";
            document.location.href="/user/signrequests/" + id;
        } else {
            document.getElementById("bar").style.display = "block";
            document.getElementById("bar").style.width = this.percent + "%";
            document.getElementById("bar-text").innerHTML = result;
        }
    }
}