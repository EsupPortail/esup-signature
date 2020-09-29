export default class ShareUi {

    constructor() {
        console.info("Starting share UI");
        this.initListeners();
    }

    initListeners() {
        $('#selectTarget').on('change', e => this.toggleShareForm());
    }

    toggleShareForm() {
        let selectedTargetValue = $("#selectTarget :selected").val();
        console.info("toggle share form" + selectedTargetValue);
        if(selectedTargetValue === 'form') {
            $('#selectFormDiv').removeClass('d-none');
            $('#selectWorkflowDiv').addClass('d-none');
        } else {
            $('#selectFormDiv').addClass('d-none');
            $('#selectWorkflowDiv').removeClass('d-none');
        }
    }

}