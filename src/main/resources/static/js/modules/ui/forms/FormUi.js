export class FormUi {

    constructor() {
        this.form = null;
        this.saveButton = null;
        this.initialState = null;
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
        this.saveButton = document.getElementById('saveButton');

        if (!this.form || !this.saveButton) {
            return;
        }

        this.isInitialized = true;
        this.initialState = this.getFormState();

        const refreshDirtyState = () => {
            this.setDirty(this.getFormState() !== this.initialState);
        };

        this.form.addEventListener('input', refreshDirtyState, true);
        this.form.addEventListener('change', refreshDirtyState, true);
        this.form.addEventListener('click', event => {
            if (event.target.closest('.btn-add-field, .btn-remove')) {
                window.setTimeout(refreshDirtyState, 0);
            }
        }, true);
        this.form.addEventListener('submit', () => this.setDirty(false));

        const actionText = document.getElementById('actionText');
        if (actionText) {
            actionText.addEventListener('change', refreshDirtyState);
            actionText.addEventListener('input', refreshDirtyState);
        }

        if (window.jQuery) {
            const $message = window.jQuery('#message');
            if ($message.length) {
                $message.on('summernote.change summernote.blur summernote.keyup summernote.paste', refreshDirtyState);
            }
        }

        this.setDirty(false);
    }

    getFormState() {
        const fields = Array.from(this.form.elements)
            .filter(element => element && element.name && !element.disabled && element.tagName !== 'BUTTON' && !['button', 'submit', 'reset', 'image'].includes(element.type));

        return JSON.stringify(fields.map(element => {
            if (element.type === 'checkbox' || element.type === 'radio') {
                return [element.name, element.type, element.value, element.checked];
            }

            if (element.tagName === 'SELECT' && element.multiple) {
                return [element.name, element.type, Array.from(element.selectedOptions).map(option => option.value)];
            }

            return [element.name, element.type, element.value];
        }));
    }

    setDirty(isDirty) {
        this.saveButton.classList.toggle('form-dirty-indicator', isDirty);
    }
}