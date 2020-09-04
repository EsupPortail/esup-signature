export default class ShareUi {

    constructor() {
        console.info("Starting share UI");
        this.initListeners();
    }

    initListeners() {
        $('#selectType').on('change', e => this.toggleShareForm(e));
    }

    toggleShareForm(e) {
        let selectedTypeValue = $("#selectType :selected").val();
        console.info("toggle share form" + selectedTypeValue);
        if(selectedTypeValue === 'create') {
            $('#selectFormDiv').removeClass('d-none');
            $('#selectWorkflowDiv').removeClass('d-none');
        } else {
            $('#selectFormDiv').addClass('d-none');
            $('#selectWorkflowDiv').addClass('d-none');
        }
    }

}