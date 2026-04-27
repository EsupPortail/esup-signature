import {Message} from "../../../prototypes/Message.js?version=@version@";
import Toast from "../Toast.js?version=@version@";

export class FormFieldsUi {

    constructor(domain, formId, prefillTypes) {
        console.info("Starting Form UI for " + formId);
        this.toast = new Toast();
        this.formId = formId;
        this.btnSaveFields = $('#saveButton');
        this.prefillTypes = prefillTypes;
        this.domain = domain;
        this.initListeners();
    }

    initListeners() {
        this.btnSaveFields.on('click', () => this.saveFields());
        let self = this;
        $('[id^="valueServiceName_"]').each(function (){
            $(this).on('change', e => self.updateTypes(e));
        });

        $('[id^="prefillCheckbox_"]').each(function (){
            $(this).on('click', e => self.toggleSelect(e));
        });
        $('[id^="searchCheckbox_"]').each(function (){
            $(this).on('click', e => self.toggleSelect(e));
        });
        $("#multipartModel").on('change', function () {
            $("#submitModel").removeClass("d-none");
        });
    }

    toggleSelect(e) {
        let id = $(e.target).attr("id").split("_")[1];
        let valueServiceSlim = $("#valueServiceName_" + id)[0].slim;
        let valueTypeSlim = $("#valueType_" + id)[0].slim;
        if(!$("#prefillCheckbox_" + id).is(':checked') && !$("#searchCheckbox_" + id).is(':checked')) {
            valueServiceSlim.setSelected();
            valueServiceSlim.disable();
            valueTypeSlim.setSelected();
            valueTypeSlim.disable();
        } else {
            valueServiceSlim.enable();
        }

    }

    updateTypes(e) {
        console.info('update types');
        let selectedValue = $(e.target).find(":selected").attr("value");
        for(let i = 0; i < Object.keys(this.prefillTypes).length; i++) {
            if(selectedValue === Object.keys(this.prefillTypes)[i]) {
                let id =$(e.target).attr("id").split("_")[1];
                let slim = $("#valueType_" + id)[0].slim;
                console.log(slim);
                let typeValues = [];
                for(let j = 0; j < Object.entries(this.prefillTypes)[i][1].length; j++) {
                    let value = Object.entries(this.prefillTypes)[i][1][j];
                    typeValues[j] = {text : value};
                }
                console.log(slim);
                slim.setSelected();
                slim.setData([]);
                slim.setData(typeValues);
                slim.setSelected();
                slim.enable();
            }
        }
    }

    serializeFieldForm(form) {
        let fd = new FormData(form);

        return {
            id: Number($(form).attr('id')),
            description: fd.get('description') || '',
            fieldType: fd.get('fieldType') || 'text',
            required: fd.has('required'),
            favorisable: fd.has('favorisable'),
            readOnly: fd.has('readOnly'),
            prefill: fd.has('prefill'),
            search: fd.has('search'),
            valueServiceName: fd.get('valueServiceName') || '',
            valueType: fd.get('valueType') || '',
            valueReturn: fd.get('valueReturn') || '',
            stepZero: fd.has('stepZero'),
            workflowStepsIds: fd.getAll('workflowStepsIds')
                .map(value => Number(value))
                .filter(value => !Number.isNaN(value))
        };
    }

    async saveFields() {
        let message = new Message();
        message.type = "info";
        message.text = "Enregistrement en cours";
        message.object = null;

        let forms = $('form[name^="field-update"]').toArray();
        let payload = forms.map(form => this.serializeFieldForm(form));
        let csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
        let csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

        this.btnSaveFields.addClass('disabled');
        this.btnSaveFields.attr('aria-disabled', 'true');

        try {
            let response = await fetch(`/${this.domain}/forms/${this.formId}/fields/update-all`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(payload),
                credentials: 'same-origin'
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }

            let successMessage = new Message();
            successMessage.type = "success";
            successMessage.text = "Modifications enregistrées";
            successMessage.object = null;
            this.toast.launch(successMessage);
        } catch (error) {
            console.error('save fields error', error);
            let errorMessage = new Message();
            errorMessage.type = "error";
            errorMessage.text = "Problème lors de l'enregistrement";
            errorMessage.object = null;
            this.toast.launch(errorMessage);
        } finally {
            this.btnSaveFields.removeClass('disabled');
            this.btnSaveFields.removeAttr('aria-disabled');
        }
    }
}
