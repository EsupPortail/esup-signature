export class WorkflowUi {

    constructor() {
        console.info("Starting workflow UI");
        this.initListeners();
    }

    initListeners() {
        $(document).ready(e => this.initDeleteListener());
        $("#delete-button").on("click", e => this.confirmDelete());
        let self = this;
        $("#autoSign").on('change', function(){
           if($(this).is(":checked")) {
               let id = $(this).attr("data-es-step-id");
               bootbox.confirm('Attention tous les paramètres de cette étape seront perdus après la validation', function (result){
                   if(result) {
                       $("#cert_" + id).removeClass("d-none");
                       $("#conf_" + id).addClass("d-none");
                       $("#table_" + id).addClass("d-none");

                   } else {
                       $("#autoSign").click();
                   }
               });
           } else {
               let id = $(this).attr("data-es-step-id");
               $("#cert_" + id).addClass("d-none");
               $("#conf_" + id).removeClass("d-none");
               $("#table_" + id).removeClass("d-none");
           }
        });
    }

    confirmDelete() {
        bootbox.confirm("Voulez-vous vraiment supprimer ce circuit ?", function (result){
            if(result) {
                $("#delete").submit();
            }
        })
    }

    initDeleteListener() {
        let self = this;
        $(".del-step-btn").each(function(){
            $(this).on("click", e => self.launchDelete(e));
        });
    }

    launchDelete(e) {
        bootbox.confirm('Voulez-vous vraiment supprimer cette étape ?<br>Tous les liens avec les champs de formulaires seront supprimés', function(result){
            if(result) {
                $('#del_' + $(e.currentTarget).attr("id")).submit();
            }
        });
    }

}