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
        this.btnSaveFields.on('click', e => this.saveFields());
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

    saveFields() {
        let message = new Message();
        message.type = "info";
        message.text = "Enregistrement en cours";
        message.object = null;
        let self = this;
        let fieldsUpdates = $('form[name^="field-update"]');
        let i = 1;
        fieldsUpdates.each(function() {
            let fd = new FormData($(this)[0]);
            console.log(fd.get("_csrf"));
            $.ajax({
                type: "PUT",
                url: "/" + self.domain + "/forms/" + self.formId + "/fields/" + $(this).attr('id') + "/update?_csrf=" + fd.get("_csrf"),
                data: fd,
                processData: false,
                contentType: false,
                success: function(data,status) {
                    if(i === fieldsUpdates.length) {
                        let message = new Message();
                        message.type = "success";
                        message.text = "Modifications enregistrées";
                        message.object = null;
                        self.toast.launch(message);
                    }
                    i++;
                },
                error: function(data, status) {
                    let message = new Message();
                    message.type = "error";
                    message.text = "Problème lors de l'enregistrement";
                    message.object = null;
                    self.toast.launch(message);
                },
            });
        });
    }
}