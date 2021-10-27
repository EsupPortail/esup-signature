export class WorkflowUi {

    constructor() {
        console.info("Starting workflow UI");
        this.visibility = $("#visibility");
        this.initListeners();
    }

    initListeners() {
        $(document).ready(e => this.initDeleteListener());
        $("#delete-button").on("click", e => this.confirmDelete());
        this.visibility.on("change", function(e){
            if(this.checked) {
                $("#roles").next('.ss-main').removeClass("d-none");
            } else {
                $("#roles").next('.ss-main').addClass("d-none");
            }
        });
        this.visibility.on("load", function(e){
            if(this.checked) {
                $("#roles").next('.ss-main').removeClass("d-none");
            } else {
                $("#roles").next('.ss-main').addClass("d-none");
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