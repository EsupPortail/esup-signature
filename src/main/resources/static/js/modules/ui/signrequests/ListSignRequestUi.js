import {CsrfToken} from "../../../prototypes/CsrfToken.js";

export default class ListSignRequestUi {

    constructor(signRequets, statusFilter, csrf) {
        console.info("Starting list sign UI");
        this.totalElementsToDisplay = signRequets.totalElements - signRequets.numberOfElements;
        this.statusFilter = "";
        if(statusFilter != null) {
            this.statusFilter = statusFilter;
        }
        this.csrf = new CsrfToken(csrf);
        this.signRequestTable = $("#signRequestTable")
        this.page = 1;
        this.initListeners();
        this.massSignButtonHide = true;
    }

    initListeners() {
        $('#massSignButton').on('click', e => this.launchMassSign(false));
        $('#checkCertSignButton').on("click", e => this.checkCertSign());
        $('#workflowFilter').on('change', e => this.buildUrlFilter());
        $('#recipientsFilter').on('change', e => this.buildUrlFilter());
        $('#docTitleFilter').on('change', e => this.buildUrlFilter());
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#listSignRequestTable').on('scroll', e => this.detectEndDiv(e));
        $('#selectAllButton').on("click", e => this.selectAllCheckboxes());
        $('#unSelectAllButton').on("click", e => this.unSelectAllCheckboxes());
        $('.sign-requests-ids').on("change", e => this.checkNbCheckboxes());
        document.addEventListener("massSign", e => this.updateWaitModal(e));
        document.addEventListener("sign", e => this.updateErrorWaitModal(e));
    }

    checkNbCheckboxes() {
        let idDom = $('.sign-requests-ids:checked');
        if (idDom.length > 0) {
            $('#deleteMultipleButton').removeClass('d-none');
        } else {
            $('#deleteMultipleButton').addClass('d-none');
        }

        if (idDom.length > 1 && this.massSignButtonHide) {
            $('#checkCertSignButton').removeClass('d-none');
            this.massSignButtonHide = false;
        } else if (idDom.length < 2 && !this.massSignButtonHide) {
            $('#checkCertSignButton').addClass('d-none');
            this.massSignButtonHide = true;
        }
    }

    selectAllCheckboxes() {
        $("input[name^='ids']").each(function() {
            $(this).prop("checked", true).change();
        });
    }

    unSelectAllCheckboxes() {
        $("input[name^='ids']").each(function() {
            $(this).prop("checked", false).change();
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
            let self = this;
            $.get("/user/signrequests/list-ws?statusFilter=" + this.statusFilter + "&" + this.csrf.parameterName + "=" + this.csrf.token + "&page=" + this.page, function (data) {
                self.signRequestTable.append(data);
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
            let passwordInput = $('#password');
            if(passwordInput.val() == null || passwordInput.val() === "") {
                $("#passwordSubmit").click();
                return;
            }
            $('#checkCertSignModal').modal('hide');
        }
        let signRequestIds = $('.sign-requests-ids:checked');
        let ids = [];
        for (let i = 0; i < signRequestIds.length ; i++) {
            let checkbox = signRequestIds.eq(i);
            if(checkbox.attr("data-status") === 'pending') {
                ids.push(signRequestIds.eq(i).val());
            }
        }
        if(ids.length > 0) {
            let self = this;
            bootbox.confirm("Vous êtes sur le point de signer " + ids.length + " documents sans consultation préalable.<br/>Cette action est irrevocable !<br/>Voulez-vous continuer ?", function (result) {
                if (result) {
                    self.massSign(ids, comeFromDispatcher);
                }
            });
        } else {
            bootbox.alert("Aucune demande à signer dans la selection", function (){});
        }
    }

    massSign(ids, comeFromDispatcher) {
        let waitModal = $("#wait");
        waitModal.modal('show');
        waitModal.modal({backdrop: 'static', keyboard: false});
        let signRequestUrlParams = "sseId=" + sessionStorage.getItem("sseId") +
            "&ids=" + JSON.stringify(ids) +
            "&" + this.csrf.parameterName + "=" + this.csrf.token;
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
        let bar = $("#bar");
        let barText = $("#bar-text");
        if(message.type === "end") {
            console.info("mass-sign end");
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            barText.after(cloneBarText);
            cloneBarText.before("<br>");
            bar.removeClass("progress-bar-animated");
            barText.html(message.text);
            bar.css("width", 100 + "%");
            $("#closeModal").show();
            $("#validModal").show();
        } else if(message.type === "nextSuccess") {
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            cloneBarText.css("color", "green");
            barText.after(cloneBarText);
            cloneBarText.before("<br>");
            // barText.html("");
        } else if(message.type === "nextError") {
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            cloneBarText.css("color", "red");
            barText.after(cloneBarText);
            barText.css("color", "red");
            cloneBarText.before("<br>");
            if(message.text !== "") {
                cloneBarText.html(message.text);
            }
        } else {
            console.debug("update bar");
            bar.css("display", "block");
            bar.css("width", this.percent + "%");
            barText.html(message.text);
        }
    }

    updateErrorWaitModal(e) {
        console.info("update wait modal");
        let message = e.detail
        console.info(message);
        this.percent = this.percent + 1;
        let barText = $("#bar-text");
        if(message.type === "sign_system_error" || message.type === "not_authorized") {
            console.error("sign error : system error");
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            cloneBarText.css("color", "red");
            barText.after(cloneBarText);
            cloneBarText.before("<br>");
            cloneBarText.html(message.text);
        }
    }

    reset() {
        this.percent = 0;
        $("#passwordError").hide();
        $("#signError").hide();
        $("#closeModal").hide();
        $("#validModal").hide();
        let bar = $("#bar");
        bar.hide();
        bar.addClass("progress-bar-animated");
    }

    checkCertSign() {
        let signRequestIds = $('.sign-requests-ids:checked');
        let ids = [];
        let isCertSign = false;
        for (let i = 0; i < signRequestIds.length ; i++) {
            let checkbox = signRequestIds.eq(i);
            ids.push(checkbox.val());
            if(checkbox.attr("data-sign-type") === 'certSign' && checkbox.attr("data-status") === 'pending') {
                isCertSign = true;
            }
        }
        if (isCertSign) {
            $("#passwordForm").on("submit", function (e){
               e.preventDefault();
            });
            $('#checkCertSignModal').modal('show');
        } else {
            this.launchMassSign(true)
        }
    }
}