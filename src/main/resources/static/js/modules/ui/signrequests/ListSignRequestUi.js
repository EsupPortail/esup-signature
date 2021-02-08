import {CsrfToken} from "../../../prototypes/CsrfToken.js";

export default class ListSignRequestUi {

    constructor(totalElementsToDisplay, csrf) {
        console.info("Starting list sign UI");
        this.totalElementsToDisplay = totalElementsToDisplay;
        this.csrf = new CsrfToken(csrf);
        this.signRequestTable = $("#signRequestTable")
        this.page = 1;
        this.initListeners();
    }

    initListeners() {
        $('#massSignButton').on('click', e => this.launchMassSign());
        $('#workflowFilter').on('change', e => this.buildUrlFilter());
        $('#recipientsFilter').on('change', e => this.buildUrlFilter());
        $('#docTitleFilter').on('change', e => this.buildUrlFilter());
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#listSignRequestTable').on('scroll', e => this.detectEndDiv(e));
        $('#selectAllButton').on("click", e => this.selectAllCheckboxes());
        $('#unSelectAllButton').on("click", e => this.unSelectAllCheckboxes());
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
            ids[i] = $(this).val();
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

    launchMassSign() {
        let csrf = this.csrf;
        let idDom = $('.idMassSign:checked');
        let ids = [];
        for (let i = 0; i < idDom.length ; i++) {
            ids.push(idDom.eq(i).val());
        }
        $.ajax({
            url: "/user/signrequests/mass-sign" + "?" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(ids),
            success:  function () {
                document.location.href = "/";
            }
        });
    }
}