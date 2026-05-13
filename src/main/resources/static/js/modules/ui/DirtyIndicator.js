export function attachDirtyIndicator({ form, saveButton, extraInputs = [], extraStateProviders = [] }) {
    if (!form || !saveButton) {
        return null;
    }

    const trackedRoot = form;

    const setDirty = isDirty => {
        saveButton.classList.toggle('form-dirty-indicator', isDirty);
    };

    const getTrackedElements = () => {
        if (trackedRoot.elements != null) {
            return Array.from(trackedRoot.elements);
        }

        return Array.from(trackedRoot.querySelectorAll('input, select, textarea, button'));
    };

    const getFormState = () => {
        const fields = getTrackedElements()
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
    };

    const getState = () => JSON.stringify({
        form: JSON.parse(getFormState()),
        extra: extraStateProviders.map(provider => provider ? provider() : null)
    });

    let initialState = getState();
    const refreshDirtyState = () => {
        setDirty(getState() !== initialState);
    };

    const markClean = () => {
        initialState = getState();
        setDirty(false);
    };

    trackedRoot.addEventListener('input', refreshDirtyState, true);
    trackedRoot.addEventListener('change', refreshDirtyState, true);
    trackedRoot.addEventListener('click', event => {
        if (event.target.closest('.btn-add-field, .btn-remove')) {
            window.setTimeout(refreshDirtyState, 0);
        }
    }, true);

    if (trackedRoot.tagName === 'FORM') {
        trackedRoot.addEventListener('submit', () => setDirty(false));
    }

    extraInputs.forEach(input => {
        if (!input) {
            return;
        }
        input.addEventListener('change', refreshDirtyState);
        input.addEventListener('input', refreshDirtyState);
    });

    if (window.jQuery) {
        const $message = window.jQuery('#message');
        if ($message.length) {
            $message.on('summernote.change summernote.blur summernote.keyup summernote.paste', refreshDirtyState);
        }
    }

    setDirty(false);

    return {
        refreshDirtyState,
        setDirty,
        markClean,
        getFormState,
        getState,
        initialState
    };
}

