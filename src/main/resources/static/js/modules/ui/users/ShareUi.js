export default class ShareUi {

    constructor() {
        console.info("Starting share UI");
        this.initListeners();
    }

    initListeners() {
        $('#selectTarget').on('change', e => this.toggleShareForm());
        $('#selectWorkflow').on('change', e => this.updateTypeCheckboxes(e));
        $('#selectForm').on('change', e => this.updateTypeCheckboxes(e));
        let checkSign = $("#check-sign");
        if(checkSign) {
            this.listenToCheckSign();
            if(checkSign.prop("checked") === true) {
                $('#sign-mod').removeClass("d-none");
            }
        }

        $("#submitShare").on('click', function (){
            if($('div.form-check.required :checkbox:checked').length > 0) {
                $("#sendShare").click();
            } else {
                bootbox.alert("Vous devez sélectionner un type de délégation", null);
            }
        });

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
            $('#selectFormDiv').addClass('d-none');
            $('#selectWorkflowDiv').addClass('d-none');
            let self = this;
            $("input[id^='check-']").each(function() {
                $(this).removeAttr("disabled");
                if($(this).attr("id").split("-")[1] === "sign") {
                    self.listenToCheckSign();
                }
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
            let self = this;
            $.each(authorizedSignTypes, function (id, value) {
                let checkBox = $("#check-" + value.trim());
                checkBox.removeAttr("disabled");
                if(value.trim() === "sign") {
                    self.listenToCheckSign();
                }
                console.log(value);
            });
        }
    }

    listenToCheckSign() {
        $("#check-sign").on('click', function () {
            if ($(this).prop("checked") === true) {
                $('#sign-mod').removeClass("d-none");
            } else {
                $('#sign-mod').addClass("d-none");
            }
        });
    }


}