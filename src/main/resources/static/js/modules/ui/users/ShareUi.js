import {attachDirtyIndicator} from "../DirtyIndicator.js?version=@version@";

export default class ShareUi {

    constructor() {
        console.info("Starting share UI");
        this.dirtyIndicator = null;
        this.initListeners();
    }

    initListeners() {
        $('#selectTarget').on('change', () => this.toggleShareForm());
        $('#selectWorkflow').on('change', e => this.updateTypeCheckboxes(e));
        $('#selectForm').on('change', e => this.updateTypeCheckboxes(e));
        this.initDirtyIndicatorWhenReady();
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

    initDirtyIndicatorWhenReady() {
        if (document.documentElement.dataset.globalUiReady === 'true') {
            queueMicrotask(() => this.initDirtyIndicator());
            return;
        }

        document.addEventListener('globalUiReady', () => this.initDirtyIndicator(), {once: true});
    }

    initDirtyIndicator() {
        if (this.dirtyIndicator != null) {
            return;
        }

        const form = document.getElementById('shareForm');
        const saveButton = document.getElementById('saveButton');

        if (!form || !saveButton) {
            return;
        }

        this.dirtyIndicator = attachDirtyIndicator({
            form,
            saveButton
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