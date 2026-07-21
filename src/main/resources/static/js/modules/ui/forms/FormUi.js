import {attachDirtyIndicator} from "../DirtyIndicator.js?version=@version@";

export class FormUi {

    constructor() {
        this.form = null;
        this.isInitialized = false;

        if (document.documentElement.dataset.globalUiReady === 'true') {
            queueMicrotask(() => this.initListeners());
            return;
        }

        document.addEventListener('globalUiReady', () => this.initListeners(), { once: true });
    }

    initListeners() {
        if (this.isInitialized) {
            return;
        }

        this.form = document.getElementById('formUpdate');
        const saveButton = document.getElementById('saveButton');

        if (!this.form || !saveButton) {
            return;
        }

        this.isInitialized = true;

        const actionText = document.getElementById('actionText');
        this.initActionEditor(actionText);
        this.initSaveButton(saveButton);

        attachDirtyIndicator({
            form: this.form,
            saveButton,
            extraInputs: actionText ? [actionText] : []
        });
    }

    initActionEditor(actionText) {
        const actionEditor = document.getElementById('actionDiv');
        if (!actionText || !actionEditor || typeof ace === 'undefined') {
            return;
        }

        ace.require("ace/ext/language_tools");
        const editor = ace.edit(actionEditor, {
            mode: "ace/mode/javascript",
            autoScrollEditorIntoView: true,
            enableBasicAutocompletion: true,
            selectionStyle: "text"
        });

        actionText.hidden = true;
        editor.getSession().setValue(actionText.value);
        editor.getSession().on('change', () => {
            actionText.value = editor.getSession().getValue();
            actionText.dispatchEvent(new Event('change', {bubbles: true}));
        });
    }

    initSaveButton(saveButton) {
        saveButton.addEventListener('click', () => this.form.requestSubmit());
    }
}
