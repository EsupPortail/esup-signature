import {CsrfToken} from "../../../prototypes/CsrfToken.js?version=@version@";
import {Nexu} from "../signrequests/Nexu.js?version=@version@";

export class ListSignBooksUi {

    constructor(signRequests, statusFilter, recipientsFilter, workflowFilter, creatorFilter, docTitleFilter, infiniteScrolling, csrf, mode) {
        console.info("Starting list sign UI");
        this.signRequests = signRequests;
        this.mode = mode;
        this.infiniteScrolling = infiniteScrolling;
        this.totalElementsToDisplay = this.signRequests.totalElements - this.signRequests.numberOfElements;
        this.statusFilter = "";
        this.recipientsFilter = "";
        this.workflowFilter = "";
        this.docTitleFilter = "";
        this.creatorFilter = null;
        if(statusFilter != null) {
            this.statusFilter = statusFilter;
        }
        if(recipientsFilter != null) {
            this.recipientsFilter = recipientsFilter;
        }
        if(workflowFilter != null) {
            this.workflowFilter = workflowFilter;
        }
        if(creatorFilter != null) {
            this.creatorFilter = creatorFilter;
        }
        if(docTitleFilter != null) {
            this.docTitleFilter = docTitleFilter;
        }
        this.csrf = new CsrfToken(csrf);
        this.signRequestTable = $("#signRequestTable");
        this.listSignRequestTable = $('#listSignRequestTable');
        this.page = 0;
        this.launchMassSignButtonHide = true;
        this.rowHeight = null;
        this.certTypeSelect = $("#certType");
        $("#password").hide();
        new Nexu(null, null, null, null, null);
        $(document).ready(e => this.initListeners());
    }

    initListeners() {
        $("#refresh-certType").on('click', e => this.checkSignOptions());
        $("#certType").on("change", e => this.checkAfterChangeSignType());
        $('#toggle-new-grid').on('click', e => this.toggleNewMenu());
        $('#launchMassSignButton').on('click', e => this.launchMassSign());
        //$('#massSignModalButton').on("click", e => this.checkCertSign());
        $('#workflowFilter').on('change', e => this.buildUrlFilter());
        let self = this;
        let creatorFilter = document.querySelector('#creatorFilter');
        if(creatorFilter != null) {
            creatorFilter.slim.settings.placeholderText = $(creatorFilter).attr("data-placeholder");
            creatorFilter.slim.open();
            creatorFilter.slim.close();
            creatorFilter.slim.events.afterChange = function () {
                self.buildUrlFilter();
            }
        }
        let recipientsFilter = document.querySelector('#recipientsFilter');
        if(recipientsFilter != null) {
            recipientsFilter.slim.settings.placeholderText = $(recipientsFilter).attr("data-placeholder");
            recipientsFilter.slim.open();
            recipientsFilter.slim.close();
            recipientsFilter.slim.events.afterChange = function () {
                self.buildUrlFilter();
            }
        }
        $('#docTitleFilter').on('change', e => this.buildUrlFilter());
        $('#statusFilter').on('change', e => this.buildUrlFilter());
        $('#dateFilter').on('change', e => this.buildUrlFilter());
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#menuDeleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#downloadMultipleButton').on("click", e => this.downloadMultiple());
        $('#downloadMultipleButtonWithReport').on("click", e => this.downloadMultipleWithReport());
        $('#menuDownloadMultipleButton').on("click", e => this.downloadMultiple());
        $('#menuDownloadMultipleButtonWithReport').on("click", e => this.downloadMultipleWithReport());
        this.listSignRequestTable.on('scroll', e => this.detectEndDiv(e));
        this.listSignRequestTable.focus();
        $(document).on('click', function(e){
            if (!$(e.target).closest('.modal').length) {
                self.listSignRequestTable.focus();
            }
        });
        $("#toggle-new-grid").css("top", "-55px");
        $(document).on('wheel', function(e){
            let delta = e.originalEvent.deltaY;
            let scrollAmount = delta > 0 ? 50 : -50;
            self.listSignRequestTable.scrollTop(self.listSignRequestTable.scrollTop() + scrollAmount);
        });
        $('#selectAllButton').on("click", e => this.selectAllCheckboxes());
        $('#unSelectAllButton').on("click", e => this.unSelectAllCheckboxes());
        this.refreshListeners();
        document.addEventListener("massSign", e => this.updateWaitModal(e));
        document.addEventListener("sign", e => this.updateErrorWaitModal(e));
        $("#more-sign-request").on("click", e => this.addToPage());
        $('#new-scroll').on('wheel', e => this.activeHorizontalScrolling(e));
    }

    checkSignOptions() {
        console.info("check sign options");
        new Nexu(null, null, null, null, null);
    }

    checkAfterChangeSignType() {
        let value = this.certTypeSelect.val();
        $("#alert-sign-present").hide();
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
        if(value === "imageStamp") {
            $("#alert-sign-present").show();
        }
    }

    refreshListeners() {
        $('.sign-requests-ids').on("change", e => this.checkNbCheckboxes());
        $("button[id^='menu-toggle']").each(function() {
           $(this).on("click", function (){
               $("div[id^='menu-']").each(function() {
                   $(this).collapse('hide');
               });
           }) ;
        });
        $("div[id^='menu-']").each(function() {
            $(this).on('shown.bs.collapse', function (e) {
                let id = $(this).attr('id').split("-")[1];
                let menu = $("#menu-toggle_" + id);
                let div = $("#listSignRequestTable");
                let divHeight = div.height();
                let menuTop = menu.offset().top;
                if(divHeight < menuTop) {
                    div.scrollTop(div.scrollTop() + 150);
                }
            });
        });
    }

    toggleNewMenu() {
        console.info("toggle new menu");
        $('#new-scroll').toggleClass('text-nowrap').toggleClass('new-min-h');
        // $('#to-sign-list').toggleClass('d-flex d-none');
        // $('#new-fragment').toggleClass('position-fixed');
        $('#toggle-new-grid').children().toggleClass('fa-th fa-chevron-up');
        $('.newHr').toggleClass('d-none');
        $('#newContainer').toggleClass('d-inline').toggleClass("text-left");
        $('.newToggled').toggleClass('d-none');
        $('.noForm').toggleClass('d-none');
        $('.noWorkflow').toggleClass('d-none');
        // this.menuToggled = !this.menuToggled;
        // localStorage.setItem('menuToggled', this.menuToggled);
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

        if (idDom.length > 1 && this.launchMassSignButtonHide) {
            $('#massSignModalButton').removeClass('d-none');
            this.launchMassSignButtonHide = false;
        } else if (idDom.length < 2 && !this.launchMassSignButtonHide) {
            $('#massSignModalButton').addClass('d-none');
            this.launchMassSignButtonHide = true;
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
            if(this.totalElementsToDisplay > 0 ) {
                this.addToPage();
            } else {
                this.signRequestTable.parent().children('tfoot').remove();
            }
        }
    }

    deleteMultiple() {
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-es-signbook-id");
            i++;
        });

        if(ids.length > 0) {
            let self = this;
            bootbox.confirm("Attention, les demandes au statut 'Supprimé' seront définitivement perdues. Les autres seront placées dans la corbeille.<br/>Confirmez vous l'opération ?",
                function(result) {
                    if(result) {
                        bootbox.dialog({
                            closeButton : false,
                            message : "<h5>Suppression en cours</h5>" +
                                "<div class=\"text-center\">" +
                                "<div id=\"signSpinner\" class=\"justify-content-center mx-auto\">\n" +
                                "   <div class=\"spinner-border mx-auto\" role=\"status\" style=\"width: 3rem; height: 3rem;\">\n" +
                                "       <span class=\"sr-only\">En cours...</span>\n" +
                                "   </div>\n" +
                                "</div> " +
                                "</div> "
                        });
                        $.ajax({
                            url: "/" + self.mode + "/signbooks/delete-multiple?" + self.csrf.parameterName + "=" + self.csrf.token,
                            type: 'POST',
                            dataType: 'json',
                            contentType: "application/json",
                            data: JSON.stringify(ids),
                            success: function () {
                                location.reload();
                            }
                        });
                    }
                }
            );
        }
    }

    downloadMultiple() {
        console.info("launch download multiple");
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-es-signbook-id");
            i++;
        });
        if (ids.length > 0) {
            window.open("/" + this.mode + "/signbooks/download-multiple?ids=" + ids, "_blank");
        }
    }

    downloadMultipleWithReport() {
        console.info("launch download multiple");
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-es-signbook-id");
            i++;
        });
        if (ids.length > 0) {
            window.open("/" + this.mode + "/signbooks/download-multiple-with-report?ids=" + ids, "_blank");
        }
    }

    addToPage() {
        console.info("Add to page");
        this.listSignRequestTable.unbind('scroll');
        this.listSignRequestTable.addClass("wait");
        $("#loader").show();
        this.page++;
        let self = this;
        const urlParams = new URLSearchParams(window.location.search);
        let sort = "";
        if(urlParams.get("sort") != null) {
            sort = urlParams.get("sort");
        }
        $.get("/" + this.mode + "/signbooks/list-ws?statusFilter=" + this.statusFilter + "&sort=" + sort + "&recipientsFilter=" + this.recipientsFilter + "&workflowFilter=" + this.workflowFilter + "&docTitleFilter=" + this.docTitleFilter + "&" + this.csrf.parameterName + "=" + this.csrf.token + "&page=" + this.page + "&size=15", function (data) {
            self.signRequestTable.append(data);
            let clickableRows = $(".clickable-row");
            clickableRows.off('click').on('click', function(e) {
                let url = $(this).closest('tr').attr('data-href');
                if (e.ctrlKey || e.metaKey) {
                    window.open(url, '_blank');
                } else {
                    window.location = url;
                }
            });
            $(document).trigger("refreshClickableTd");
            self.listSignRequestTable.removeClass("wait");
            $("#loader").hide();
            self.refreshListeners();
            self.listSignRequestTable.on('scroll', e => self.detectEndDiv(e));
            let displayedElements = $("#signRequestTable tr").length;
            self.totalElementsToDisplay = self.signRequests.totalElements - displayedElements;
        });
    }

    buildUrlFilter() {
        let currentParams = new URLSearchParams(window.location.search);
        let filters = $('select.sign-request-filter');
        for (let i = 0 ; i < filters.length ; i++) {
            currentParams.set(filters.eq(i).attr('id'), filters.eq(i).val());
        }
        filters = $('input.sign-request-filter');
        for (let i = 0 ; i < filters.length ; i++) {
            currentParams.set(filters.eq(i).attr('id'), filters.eq(i).val());
        }
        document.location.href = "/" + this.mode + "/signbooks?" + currentParams.toString();
    }

    launchMassSign() {
        $('#massSignModal').modal('hide');
        let signRequestIds = $('.sign-requests-ids:checked');
        let ids = [];
        let nbNotViewed = 0;
        for (let i = 0; i < signRequestIds.length ; i++) {
            let checkbox = signRequestIds.eq(i);
            if(checkbox.attr("data-es-signrequest-status") === 'pending') {
                ids.push(signRequestIds.eq(i).val());
            }
            if(checkbox.attr("data-es-viewed") === 'false') {
                nbNotViewed++;
            }
        }
        let self = this;
        if(ids.length > 0) {
            if(nbNotViewed > 0) {
                bootbox.confirm({
                    message: "Vous êtes sur le point de signer " + nbNotViewed + " documents sans consultation préalable.<br/>Cette action est irrevocable !<br/>Voulez-vous continuer ?",
                    buttons: {
                        cancel: {
                            label: 'Annuler',
                        },
                        confirm: {
                            label: 'Confirmer la signature',
                            className: 'btn-success'
                        }
                    },
                    callback: function (result) {
                        if (result) {
                            self.massSign(ids);
                        }
                    }
                });
            } else {
                self.massSign(ids);
            }
        } else {
            bootbox.alert("Aucune demande à signer dans la selection", function (){});
        }
    }

    massSign(ids) {
        let waitModal = $("#wait");
        waitModal.modal('show');
        waitModal.modal({backdrop: 'static', keyboard: false});
        let signRequestUrlParams;
        signRequestUrlParams = {
            "ids" : JSON.stringify(ids),
            "signWith" : $("#certType").val(),
            "password" : $("#password").val()
        };
        this.reset();
        let self = this;
        $.ajax({
            url: "/" + this.mode + "/signbooks/mass-sign?" + self.csrf.parameterName + "=" + self.csrf.token,
            type: 'POST',
            data: signRequestUrlParams,
            success: function(e) {
                document.location.reload();
            },
            error: function(e) {
                if(e.responseText === "initNexu") {
                    document.location.href="/nexu-sign/start?ids=" + ids;
                } else {
                    bootbox.alert("La signature s'est terminée, d'une façon inattendue. La page va s'actualiser", function () {
                        location.href = "/" + self.mode + "/reports";
                    });
                }
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
            if(checkbox.attr("data-es-sign-type") === 'certSign' && checkbox.attr("data-es-signrequest-status") === 'pending') {
                isCertSign = true;
            }
        }
        if (isCertSign) {
            $("#passwordForm").on("submit", function (e){
               e.preventDefault();
            });
            $('#checkCertSignModal').modal('show');
        } else {
            this.launchMassSign()
        }
    }

    activeHorizontalScrolling(e){
        if(!this.menuToggled) {
            let delta = Math.max(-1, Math.min(1, (e.originalEvent.wheelDelta || -e.originalEvent.detail)));
            $(e.currentTarget).scrollLeft($(e.currentTarget).scrollLeft() - ( delta * 40 ) );
            e.preventDefault();
        }
    }
}
