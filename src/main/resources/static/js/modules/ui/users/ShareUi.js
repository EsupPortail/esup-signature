export default class ShareUi {

    constructor() {
        console.info("Starting share UI");
        this.initListeners();
    }

    initListeners() {
        $('#selectTarget').on('change', e => this.toggleShareForm());
        $('#selectWorkflow').on('change', e => this.updateTypeCheckboxes(e));
        $('#selectForm').on('change', e => this.updateTypeCheckboxes(e));
    }

    toggleShareForm() {
        let selectedTargetValue = $("#selectTarget :selected").val();
        console.info("toggle share form" + selectedTargetValue);
        if(selectedTargetValue === 'form') {
            $('#selectFormDiv').removeClass('d-none');
            $('#selectWorkflowDiv').addClass('d-none');
        } else if(selectedTargetValue === 'workflow') {
            $('#selectFormDiv').addClass('d-none');
            $('#selectWorkflowDiv').removeClass('d-none');
        } else {
            $("input[id^='check-']").each(function() {
                $(this).removeAttr("disabled");
            });
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
            $.each(authorizedSignTypes, function (id, value) {
                $("#check-" + value.trim()).removeAttr("disabled");
                console.log(value);
            });
        }
    }



}