export class FormUi {

    btnAddField = document.querySelector('btn-add-field');
    btnRemove = document.querySelector('btn-remove');

    constructor() {
        this.initListeners();
    }

    initListeners() {
        this.btnAddField.on('click', e => this.addField(e));
        this.btnRemove.on('click', e => this.removeField(e));
    }

    addField(e)
    {
        e.preventDefault();
        var controlForm = $('#repeatingInputFields:first'),
            currentEntry = this.btnAddField.parents('.entry:first'),
            newEntry = $(currentEntry.clone()).appendTo(controlForm);
        newEntry.find('input').val('');
        controlForm.find('.entry:not(:last) .btn-add-field')
            .removeClass('btn-add-field').addClass('btn-remove')
            .removeClass('btn-success').addClass('btn-danger')
            .html('<span class="fas fa-minus" aria-hidden="true"></span>');
    }

    removeField(e)
    {
        e.preventDefault();
        this.btnRemove.parents('.entry:first').remove();
        return false;
    }
}