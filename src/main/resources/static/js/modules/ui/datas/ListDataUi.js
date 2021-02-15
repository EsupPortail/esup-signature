import {CsrfToken} from "../../../prototypes/CsrfToken.js";

export default class ListDataUi {

    constructor(csrf) {
        this.csrf = new CsrfToken(csrf);
        this.initListeners();
    }

    initListeners() {
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#selectAllButton').on("click", e => this.selectAllCheckboxes());
        $('#unSelectAllButton').on("click", e => this.unSelectAllCheckboxes());
        $('.datas-ids').on("change", e => this.checkNbCheckboxes());
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
            bootbox.confirm("Voulez-vous supprimer définitivement les brouillons sélectionnées ?", function(result) {
                if(result) {
                    $.ajax({
                        url: "/user/datas/delete-multiple?" + csrf.parameterName + "=" + csrf.token,
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

    checkNbCheckboxes() {
        let idDom = $('.datas-ids:checked');
        if (idDom.length > 0) {
            $('#deleteMultipleButton').removeClass('d-none');
        } else {
            $('#deleteMultipleButton').addClass('d-none');
        }
    }

}