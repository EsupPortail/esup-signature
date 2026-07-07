export class PdfProgressController {

    constructor(viewer) {
        this.viewer = viewer;
    }

    startProgress() {
        this.updateProgress(5, "Préparation du rendu…", true);
    }

    stopProgress(){
        this.updateProgress(100, "Chargement terminé", false);
        clearInterval(this.viewer.interval);
    }

    resetProgress() {
        $("#pdf-progress-bar").removeClass("es-progress-visible");
        this.updateProgress(0, "", false);
        clearInterval(this.viewer.interval);
    }

    updateRenderProgress() {
        const progress = this.viewer.numPages > 0 ? Math.round(this.viewer.renderedPages / this.viewer.numPages * 100) : 0;
        this.updateProgress(progress, "Chargement de la page " + this.viewer.renderedPages + "/" + this.viewer.numPages, this.viewer.renderedPages < this.viewer.numPages);
    }

    updateProgress(progress, text, animated) {
        $("#pdf-progress-bar .progress-bar")
            .toggleClass("progress-bar-striped progress-bar-animated", animated)
            .css("width", progress + "%")
            .attr("aria-valuenow", progress)
            .text(text);
    }
}
