export class WorkflowUi {

    constructor() {
        console.info("Starting workflow UI");
        this.sourceTypeSelect = document.getElementById("sourceTypeSelect");
        this.targetTypeSelect = document.getElementById("targetTypeSelect");
        this.sourceUri = document.getElementById("documentsSourceUriDiv");
        if (this.sourceTypeSelect.value === "none") {
            this.sourceUri.style.display = "none";
        }
        this.initListeners();
    }

    initListeners() {
        this.sourceTypeSelect.addEventListener('change', e => this.toggleSourceSelector());
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