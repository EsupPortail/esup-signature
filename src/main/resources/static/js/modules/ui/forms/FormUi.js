import {SseDispatcher} from "../../utils/SseDispatcher.js";
import {Message} from "../../../prototypes/Message.js";

export default class FormUi {

    constructor(formId) {
        console.info("Starting Form UI for " + formId);
        this.formId = formId;
        this.btnAddField = $('#btn-add-field');
        this.btnRemove = $('#btn-remove');
        this.btnSaveFields = $('#btn-save-fields');
        this.sseDispatcher = new SseDispatcher();
        this.initListeners();
    }

    initListeners() {
        this.btnAddField.on('click', e => this.addField(e));
        this.btnRemove.on('click', e => this.removeField(e));
        this.btnSaveFields.on('click', e => this.saveFields(e));
    }

    addField(e) {
        let controlForm = $('#repeatingInputFields:first'),
            currentEntry = this.btnAddField.parents('.entry:first'),
            newEntry = $(currentEntry.clone()).appendTo(controlForm);
        newEntry.find('input').val('');
        controlForm.find('.entry:not(:last) .btn-add-field')
            .removeClass('btn-add-field').addClass('btn-remove')
            .removeClass('btn-success').addClass('btn-danger')
            .html('<span class="fas fa-minus" aria-hidden="true"></span>');
    }

    removeField(e) {
        e.preventDefault();
        this.btnRemove.parents('.entry:first').remove();
        return false;
    }

    saveFields(e) {
        let formId = this.formId
        $('form[name^="field-update"]').each(function() {
            let fd = new FormData($(this)[0]);
            console.log(fd.get("_csrf"));
            $.ajax({
                type: "POST",
                url: "/admin/forms/" + formId + "/field/" + $(this).attr('id') + "/update?_csrf=" + fd.get("_csrf"),
                data: fd,
                processData: false,
                contentType: false,
                success: function(data,status) {
                },
                error: function(data, status) {
                },
            });
        });
        //location.reload();
        let message = new Message();
        message.type = "success";
        message.text = "Modifications enregistrées";
        message.object = null;
        this.sseDispatcher.dispatchEvent("user", message);
    }
}