import {CsrfToken} from "../../../prototypes/CsrfToken.js";

export default class ListSignRequestUi {

    constructor(totalElementsToDisplay, csrf) {
        console.info("Starting list sign UI");
        this.totalElementsToDisplay = totalElementsToDisplay;
        this.csrf = new CsrfToken(csrf);
        this.signRequestTable = $("#signRequestTable")
        this.page = 1;
        this.initListeners();
        this.massSignButtonHide = true;
    }

    initListeners() {
        $('#massSignButton').on('click', e => this.checkCertSign());
        $('#workflowFilter').on('change', e => this.buildUrlFilter());
        $('#recipientsFilter').on('change', e => this.buildUrlFilter());
        $('#docTitleFilter').on('change', e => this.buildUrlFilter());
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#listSignRequestTable').on('scroll', e => this.detectEndDiv(e));
        $('#selectAllButton').on("click", e => this.selectAllCheckboxes());
        $('#unSelectAllButton').on("click", e => this.unSelectAllCheckboxes());
        $('.idMassSign').on("click", e => this.checkNbCheckboxes());
        document.addEventListener("massSign", e => this.updateWaitModal(e));
        $('#checkCertSignButton').on("click", e => this.launchMassSign(false));
    }

    checkNbCheckboxes() {
        let idDom = $('.idMassSign:checked');
        if (idDom.length > 1 && this.massSignButtonHide) {
            $('#massSignButton').removeClass('d-none');
            this.massSignButtonHide = false;
        } else if (idDom.length < 2 && !this.massSignButtonHide) {
            $('#massSignButton').addClass('d-none');
            this.massSignButtonHide = true;
        }
    }

    selectAllCheckboxes() {
        $("input[name^='ids']").each(function() {
            $(this).prop("checked", true);
        });
    }

    unSelectAllCheckboxes() {
        $("input[name^='ids']").each(function() {
            $(this).prop("checked", false);
        });
    }

    detectEndDiv(e) {
        if ($(e.target).scrollTop() + $(e.target).innerHeight() >= $(e.target)[0].scrollHeight) {
            this.addToPage();
        }
    }

    deleteMultiple() {
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-id-signbook");
            i++;
        });

        if(ids.length > 0) {
            let csrf = this.csrf;
            bootbox.confirm("Voulez-vous supprimer définitivement les demandes sélectionnées ?", function(result) {
                if(result) {
                    $.ajax({
                        url: "/user/signrequests/delete-multiple?" + csrf.parameterName + "=" + csrf.token,
                        type: 'POST',
                        dataType: 'json',
                        contentType: "application/json",
                        data: JSON.stringify(ids),
                        success: function () {
                            location.reload();
                        }
                    });
                }
            });
        }
    }

    addToPage() {
        if(this.totalElementsToDisplay >= (this.page - 1) * 5 ) {
            console.info("Add to page");
            this.page++;
            $.get("/user/signrequests/list-ws?" + this.csrf.parameterName + "=" + this.csrf.token + "&page=" + this.page, function (data) {
                $('#signRequestTable').append(data);
            });
        }
    }

    buildUrlFilter() {
        let url = '/user/signrequests'
        let filters = $('.sign-request-filter');
        let firstParameter = true;
        for (let i = 0 ; i < filters.length ; i++) {
            if (filters.eq(i).val() !== "") {
                if (firstParameter) {
                    url = url + '?' + filters.eq(i).attr('id') + '=' + filters.eq(i).val();
                    firstParameter = false;
                } else {
                    url = url + '&' + filters.eq(i).attr('id') + '=' + filters.eq(i).val();
                }
            }
        }
        document.location.href = url;
    }

    launchMassSign(comeFromDispatcher) {
        if (!comeFromDispatcher) {
            $('#checkCertSignModal').modal('hide');
        }
        $('#wait').modal('show');
        $('#wait').modal({backdrop: 'static', keyboard: false});
        let idDom = $('.idMassSign:checked');
        let ids = [];
        for (let i = 0; i < idDom.length ; i++) {
            ids.push(idDom.eq(i).val());
        }
        let signRequestUrlParams = "sseId=" + sessionStorage.getItem("sseId") +
            "&ids=" + JSON.stringify(ids) +
            "&" + this.csrf.parameterName + "=" + this.csrf.token
        ;
        if (!comeFromDispatcher) {
            signRequestUrlParams += "&password=" + $('#password').val();
        }
        this.reset();
        let xmlHttp = new XMLHttpRequest();
        xmlHttp.open('POST', '/user/signrequests/mass-sign', true);
        xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        xmlHttp.send(signRequestUrlParams);
    }

    updateWaitModal(e) {
        console.info("update wait modal");
        let message = e.detail
        console.info(message);
        this.percent = this.percent + 1;
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
            console.info("mass-sign end");
            document.getElementById("bar-text").innerHTML = "";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            document.getElementById("bar-text").innerHTML = message.text;
            document.getElementById("bar").style.width = 100 + "%";
            document.location.href = "/user/reports";
        } else {
            console.debug("update bar");
            document.getElementById("bar").style.display = "block";
            document.getElementById("bar").style.width = this.percent + "%";
            document.getElementById("bar-text").innerHTML = message.text;
        }
    }

    reset() {
        this.percent = 0;
        document.getElementById("passwordError").style.display = "none";
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
        document.getElementById("bar").style.display = "none";
        document.getElementById("bar").classList.add("progress-bar-animated");
    }

    checkCertSign() {
        let idDom = $('.idMassSign:checked');
        let ids = [];
        for (let i = 0; i < idDom.length ; i++) {
            ids.push(idDom.eq(i).val());
        }
        let csrf = this.csrf
        $.ajax({
            url: "/user/signrequests/check-cert-sign?" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            dataType: 'json',
            contentType: "application/json",
            data: JSON.stringify(ids),
            success: response => this.dispatcher(response)
        });
    }

    dispatcher(response) {
        if (response) {
            $('#checkCertSignModal').modal('show');
        } else {
            this.launchMassSign(true)
        }
    }
}