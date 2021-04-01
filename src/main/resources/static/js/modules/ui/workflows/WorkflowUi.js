export class WorkflowUi {

    constructor() {
        console.info("Starting workflow UI");
        this.sourceTypeSelect = $("#sourceTypeSelect");
        this.targetTypeSelect = $("#targetTypeSelect");
        this.sourceUri = document.getElementById("documentsSourceUriDiv");
        if (this.sourceTypeSelect != null && this.sourceTypeSelect.value === "none") {
            this.sourceUri.style.display = "none";
        }
        this.initListeners();
    }

    initListeners() {
        this.sourceTypeSelect.on('change', e => this.toggleSourceSelector());
        $(document).ready(e => this.initDelListerner());
        ;
        $("#delete-button").on("click", e => this.confirmDelete());
    }

    confirmDelete() {
        bootbox.confirm("Voulez-vous vraiment supprimer ce circuit ?", function (result){
            if(result) {
                $("#delete").submit();
            }
        })
    }

    initDelListerner() {
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

    toggleSourceSelector() {
        console.log("toggle");
        if (this.sourceTypeSelect.value !== "none") {
            this.sourceUri.style.display = "block";
        } else {
            this.sourceUri.style.display = "none";
        }
    }

    toggleTargetSelector() {
        if (this.targetTypeSelect.value !== "none") {
            this.targetUri.style.display = "block";
        } else {
            this.targetUri.style.display = "none";
        }
    }
}