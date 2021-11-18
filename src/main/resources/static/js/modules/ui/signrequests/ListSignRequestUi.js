import {CsrfToken} from "../../../prototypes/CsrfToken.js?version=@version@";

export default class ListSignRequestUi {

    constructor(signRequests, statusFilter, recipientsFilter, workflowFilter, docTitleFilter, infiniteScrolling, csrf) {
        console.info("Starting list sign UI");
        this.signRequests = signRequests;
        this.infiniteScrolling = infiniteScrolling;
        this.totalElementsToDisplay = signRequests.totalElements - signRequests.numberOfElements;
        this.statusFilter = "";
        this.recipientsFilter = "";
        this.workflowFilter = "";
        this.docTitleFilter = "";
        if(statusFilter != null) {
            this.statusFilter = statusFilter;
        }
        if(recipientsFilter != null) {
            this.recipientsFilter = recipientsFilter;
        }
        if(workflowFilter != null) {
            this.workflowFilter = workflowFilter;
        }
        if(docTitleFilter != null) {
            this.docTitleFilter = docTitleFilter;
        }
        this.csrf = new CsrfToken(csrf);
        this.signRequestTable = $("#signRequestTable");
        this.page = 1;
        this.initListeners();
        this.massSignButtonHide = true;
        if(signRequests.totalElements > 10 && signRequests.numberOfElements === 10) {
            this.scaleList();
        }
    }

    initListeners() {
        $('#massSignButton').on('click', e => this.launchMassSign(false));
        $('#checkCertSignButton').on("click", e => this.checkCertSign());
        $('#workflowFilter').on('change', e => this.buildUrlFilter());
        $('#recipientsFilter').on('change', e => this.buildUrlFilter());
        $('#docTitleFilter').on('change', e => this.buildUrlFilter());
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#menuDeleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#downloadMultipleButton').on("click", e => this.downloadMultiple());
        $('#downloadMultipleButtonWithReport').on("click", e => this.downloadMultipleWithReport());
        $('#menuDownloadMultipleButton').on("click", e => this.downloadMultiple());
        $('#menuDownloadMultipleButtonWithReport').on("click", e => this.downloadMultipleWithReport());
        $('#listSignRequestTable').on('scroll', e => this.detectEndDiv(e));
        $('#selectAllButton').on("click", e => this.selectAllCheckboxes());
        $('#unSelectAllButton').on("click", e => this.unSelectAllCheckboxes());
        $('.sign-requests-ids').on("change", e => this.checkNbCheckboxes());
        document.addEventListener("massSign", e => this.updateWaitModal(e));
        document.addEventListener("sign", e => this.updateErrorWaitModal(e));
        if(this.signRequests.totalElements > 10 && this.signRequests.numberOfElements === 10) {
            $(window).resize(e => this.scaleList());
        }
    }

    scaleList() {
        let height = parseInt(this.signRequestTable.css("height"))
        this.signRequestTable.css("height", (height + ($(window ).height() - height)) + "px")
    }

    checkNbCheckboxes() {
        let idDom = $('.sign-requests-ids:checked');
        if (idDom.length > 0) {
            $('#deleteMultipleButton').removeClass('d-none');
            $('#downloadMultipleButton').removeClass('d-none');
            $('#downloadMultipleButtonWithReport').removeClass('d-none');
            $('#menuDeleteMultipleButton').removeClass('d-none');
            $('#menuDownloadMultipleButton').removeClass('d-none');
            $('#menuDownloadMultipleButtonWithReport').removeClass('d-none');
        } else {
            $('#deleteMultipleButton').addClass('d-none');
            $('#downloadMultipleButton').addClass('d-none');
            $('#downloadMultipleButtonWithReport').addClass('d-none');
            $('#menuDeleteMultipleButton').addClass('d-none');
            $('#menuDownloadMultipleButton').addClass('d-none');
            $('#menuDownloadMultipleButtonWithReport').addClass('d-none');
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
        if ($(e.target).scrollTop() + $(e.target).innerHeight() + 1 >= $(e.target)[0].scrollHeight && (this.infiniteScrolling != null && this.infiniteScrolling)) {
            if(this.totalElementsToDisplay >= (this.page - 1) * 5 ) {
                $("#listSignRequestTable").addClass("wait");
                $("#loader").show();
                this.addToPage();
            }
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
            let self = this;
            bootbox.confirm("Attention, les demandes au statut 'Supprimé' seront définitivement perdues. Les autres seront placées dans la corbeille.<br/>Confirmez vous l'opération ?", function(result) {
                if(result) {
                    $.ajax({
                        url: "/user/signrequests/delete-multiple?" + self.csrf.parameterName + "=" + self.csrf.token,
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

    downloadMultiple() {
        console.info("launch download multiple");
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-id-signbook");
            i++;
        });
        if (ids.length > 0) {
            window.open("/user/signrequests/download-multiple?ids=" + ids, "_blank");
        }
    }

    downloadMultipleWithReport() {
        console.info("launch download multiple");
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-id-signbook");
            i++;
        });
        if (ids.length > 0) {
            window.open("/user/signrequests/download-multiple-with-report?ids=" + ids, "_blank");
        }
    }

    addToPage() {
        console.info("Add to page");
        this.page++;
        let self = this;
        $.get("/user/signrequests/list-ws?statusFilter=" + this.statusFilter + "&recipientsFilter=" + this.recipientsFilter + "&workflowFilter=" + this.workflowFilter + "&docTitleFilter=" + this.docTitleFilter + "&" + this.csrf.parameterName + "=" + this.csrf.token + "&page=" + this.page, function (data) {
            self.signRequestTable.append(data);
            let clickableRow = $(".clickable-row");
            clickableRow.unbind();
            clickableRow.on('click',  function() {
                window.location = $(this).closest('tr').attr('data-href');
            });
            $(document).trigger("refreshClickableTd");
            $("#listSignRequestTable").removeClass("wait");
            $("#loader").hide();
        });
    }

    buildUrlFilter() {
        let currentParams = new URLSearchParams(window.location.search);
        let filters = $('.sign-request-filter');
        for (let i = 0 ; i < filters.length ; i++) {
            if (filters.eq(i).val() !== "") {
                currentParams.set(filters.eq(i).attr('id'), filters.eq(i).val());
            }
        }
        document.location.href = "/user/signrequests?" + currentParams.toString();
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
        let signRequestUrlParams;
        if (!comeFromDispatcher) {
            signRequestUrlParams = {
                "ids" : JSON.stringify(ids),
                "password" : $('#password').val()
            };
        } else {
            signRequestUrlParams = {
                "ids" : JSON.stringify(ids)
            };
        }
        this.reset();
        let self = this;
        $.ajax({
            url: "/user/signrequests/mass-sign/?" + self.csrf.parameterName + "=" + self.csrf.token,
            type: 'POST',
            data: signRequestUrlParams,
            success: function() {
                document.location.reload();
            },
            error: function(e) {
                bootbox.alert("La signature s'est terminée, d'une façon inattendue. La page va s'actualiser", function() {
                    location.href = "/user/reports";
                });
            }
        });

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
            console.debug("debug - " + "update bar");
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