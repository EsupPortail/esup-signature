export class Sign {

    percent = 0;
    getProgressTimer;
    wait = $('#wait');
    passwordError = document.getElementById("passwordError");

    constructor() {
    }

    launchSign(id) {
        this.percent = 0;
        console.log('launch sign for : ' + id);
        $('#signModal').modal('hide');
        this.wait.modal('show');
        this.wait.modal({backdrop: 'static', keyboard: false})
        submitSignRequest(id);
    }

    launchSignAll(id) {
        $('#signAllModal').modal('hide');
        this.wait.modal('show');
        this.wait.modal({backdrop: 'static', keyboard: false})
        var csrf = document.getElementsByName("_csrf")[0];
        var signRequestParams = "password=" + document.getElementById("passwordAll").value +
            "&" + csrf.name + "=" + csrf.value;
        var xmlHttp = new XMLHttpRequest();
        xmlHttp.open('POST', '/user/signbooks/sign/' + id, true);
        xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        xmlHttp.send(signRequestParams);
    }

    submitSignRequest(id) {
        var csrf = document.getElementsByName("_csrf")[0];
        var signPageNumber = document.getElementById("signPageNumber");
        var signRequestParams;
        if(signPageNumber != null) {
            signRequestParams = "password=" + document.getElementById("password").value +
                "&addDate=" + dateActive +
                "&visual=" + visualActive +
                "&xPos=" + document.getElementById("xPos").value +
                "&yPos=" + document.getElementById("yPos").value +
                "&signPageNumber=" + document.getElementById("signPageNumber").value +
                "&" + csrf.name + "=" + csrf.value
            ;
        } else {
            signRequestParams = "password=" + document.getElementById("password").value +
                "&" + csrf.name + "=" + csrf.value;
        }
        sendData(id, signRequestParams);
    }

    sendData(id, signRequestParams) {
        this.passwordError.style.display = "none";
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
        document.getElementById("bar").style.display = "none";
        document.getElementById("bar").classList.add("progress-bar-animated");
        var getStep = this.getStep();
        this.getProgressTimer = setInterval(function() { getStep(id, signRequestParams); }, 500);
        var xmlHttp = new XMLHttpRequest();
        xmlHttp.open('POST', '/user/signrequests/sign/' + id, true);
        xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        xmlHttp.send(signRequestParams);
    }

    getStep(id, signRequestParams) {
        this.percent = this.percent + 2;
        console.log("getStep");
        var xmlHttp = new XMLHttpRequest();
        xmlHttp.open("GET", "/user/signrequests/get-step", true);
        xmlHttp.on('readystatechange', e => this.updateWaitModal());
        xmlHttp.send(null);
    }

    updateWaitModal() {
        var result = xmlHttp.responseText;
        if (result === "security_bad_password") {
            this.passwordError.style.display = "block";
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            clearInterval(getProgressTimer);
        } else if(result === "sign_system_error" || result === "not_authorized") {
            clearInterval(getProgressTimer);
            document.getElementById("signError").style.display = "block";
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
        } else if(result === "initNexu") {
            clearInterval(getProgressTimer);
            document.location.href="/user/nexu-sign/" + id;
        } else if (result === "end") {
            clearInterval(getProgressTimer);
            document.getElementById("validModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            document.getElementById("bar-text").innerHTML = "Signature terminée";
            document.getElementById("bar").style.width = 100 + "%";
            document.location.href="/user/signrequests/" + id;
        } else {
            document.getElementById("bar").style.display = "block";
            document.getElementById("bar").style.width = percent + "%";
            document.getElementById("bar-text").innerHTML = result;
        }
    }
}