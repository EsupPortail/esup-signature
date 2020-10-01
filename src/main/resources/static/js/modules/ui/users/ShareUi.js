export default class ShareUi {

    constructor() {
        console.info("Starting share UI");
        this.initListeners();
    }

    initListeners() {
        $('#selectTarget').on('change', e => this.toggleShareForm());
        $('#selectWorkflow').on('change', e => this.updateTypeCheckboxes(e));
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

    updateTypeCheckboxes(e) {
        $("input[id^='check-']").each(function() {
            $(this).attr("disabled", "disabled");
        });
        let optionSelected = $(e.target).find("option:selected");
        let authorizedSignTypesData = optionSelected.attr("data");
        if(authorizedSignTypesData) {
            let authorizedSignTypes = authorizedSignTypesData.replace("[", "").replace("]", "").split(",");
            $.each(authorizedSignTypes, function (e, value) {
                $("#check-" + value.trim()).removeAttr("disabled");
                console.log(value);
            });
        }
    }



}