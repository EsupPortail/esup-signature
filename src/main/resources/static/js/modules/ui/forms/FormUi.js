import {attachDirtyIndicator} from '../DirtyIndicator.js?version=@version@';

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

        attachDirtyIndicator({
            form: this.form,
            saveButton,
            extraInputs: actionText ? [actionText] : []
        });
    }
}