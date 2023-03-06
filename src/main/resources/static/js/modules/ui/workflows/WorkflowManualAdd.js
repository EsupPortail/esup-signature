
export class WorkflowManualAdd {

    constructor() {

        this.initListeners();

    }

    initListeners() {
        $("#description").on('input', e => this.computeName());
    }

    computeName() {
        $("#title").val($("#description").val().toLowerCase().replace(/[\W_]/g, "_"));
    }
}