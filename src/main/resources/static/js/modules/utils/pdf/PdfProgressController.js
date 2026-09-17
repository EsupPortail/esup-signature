export class PdfProgressController {

    constructor(viewer) {
        this.viewer = viewer;
    }

    startProgress() {
        this.setProgressBarState("bg-dark");
        this.updateProgress(5, "Préparation du rendu…", true);
    }

    stopProgress(){
        this.setProgressBarState("bg-dark");
        this.updateProgress(100, "Chargement terminé", false);
    }

    resetProgress() {
        $("#pdf-progress-bar").removeClass("es-progress-visible");
        this.setProgressBarState("bg-dark");
        this.updateProgress(0, "", false);
    }

    failProgress(text = "Impossible d’afficher le document") {
        $("#pdf-progress-bar").addClass("es-progress-visible");
        this.setProgressBarState("bg-danger");
        this.updateProgress(100, text, false);
    }

    setProgressBarState(stateClass) {
        $("#pdf-progress-bar .progress-bar")
            .removeClass("bg-dark bg-danger bg-warning bg-success")
            .addClass(stateClass);
    }

    updateRenderProgress() {
        const progress = this.viewer.numPages > 0 ? Math.round(this.viewer.renderedPages / this.viewer.numPages * 100) : 0;
        this.updateProgress(progress, "Chargement de la page " + this.viewer.renderedPages + "/" + this.viewer.numPages, this.viewer.renderedPages < this.viewer.numPages);
    }

    updateProgress(progress, text, animated) {
        const progressBar = $("#pdf-progress-bar .progress-bar");
        progressBar
            .toggleClass("progress-bar-striped progress-bar-animated", animated)
            .css("width", progress + "%")
            .attr("aria-valuenow", progress);
        $("#pdf-progress-bar .progress-label").text(text);
    }
}
