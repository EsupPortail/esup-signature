export class WorkflowUi {

    sourceTypeSelect = document.getElementById("sourceTypeSelect");
    targetTypeSelect = document.getElementById("targetTypeSelect");
    sourceUri = document.getElementById("documentsSourceUriDiv");
    targetUri = document.getElementById("documentsTargetUriDiv");

    constructor() {
        console.info("Start workflowUI");
        console.log(this.sourceTypeSelect);
        if (this.sourceTypeSelect.value === "none") {
            this.sourceUri.style.display = "none";
        }
        if (this.targetTypeSelect.value === "none") {
            this.targetUri.style.display = "none";
        }
        this.initListeners();
    }

    initListeners() {
        this.sourceTypeSelect.addEventListener('change', e => this.toggleSourceSelector());
        this.targetTypeSelect.addEventListener('change', e => this.toggleTargetSelector());
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