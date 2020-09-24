import {CsrfToken} from "../../../prototypes/CsrfToken.js";

export default class ListSignRequest {

    constructor(totalElementsToDisplay, csrf) {
        this.totalElementsToDisplay = totalElementsToDisplay;
        this.csrf = new CsrfToken(csrf);
        this.signRequestTable = $("#signRequestTable")
        this.page = 1;
        this.initListeners();
    }

    initListeners() {
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#listSignRequestTable').on('scroll', e => this.detectEndDiv(e));
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
            if(confirm("Voulez-vous supprimer définitivement les demandes sélectionnées ?")) {
                $.ajax({
                    url: "/user/signrequests/delete-multiple?" + this.csrf.parameterName + "=" + this.csrf.token,
                    type: 'POST',
                    dataType : 'json',
                    contentType: "application/json",
                    data: JSON.stringify(ids),
                    success: function(){
                        location.reload();
                    }
                });
            }
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
}