
export class FormManualAdd {

    constructor() {
        this.btnAddField = $('#btn-add-field');
        this.btnRemove = $('#btn-remove');
        this.initListeners();

    }

    initListeners() {
        this.btnAddField.on('click', e => this.addField(e));
        this.btnRemove.on('click', e => this.removeField(e));
        $("#title").on('input', e => this.computeName());
        $("#titleManual").on('change', e => this.computeNameManual());
    }

    addField(e) {
        let controlForm = $('#repeatingInputFields:first'),
            currentEntry = this.btnAddField.parents('.entry:first'),
            newEntry = $(currentEntry.clone()).appendTo(controlForm);
        newEntry.find('input').val('');
        controlForm.find('.entry:not(:last) .btn-add-field')
            .removeClass('btn-add-field').addClass('btn-remove')
            .removeClass('btn-success').addClass('btn-danger')
            .html('<span class="fa-solid fa-minus" aria-hidden="true"></span>');
    }

    removeField(e) {
        e.preventDefault();
        this.btnRemove.parents('.entry:first').remove();
        return false;
    }

    computeName() {
        $("#name").val($("#title").val().toLowerCase().replace(/[\W_]/g, "_"));
    }

    computeNameManual() {
        $("#nameManual").val($("#titleManual").val().replace(/[^a-zA-Z0-9 ]/g, ''));
    }
}