export function attachDirtyIndicator({ form, saveButton, extraInputs = [], extraStateProviders = [] }) {
    if (!form || !saveButton) {
        return null;
    }

    const setDirty = isDirty => {
        saveButton.classList.toggle('form-dirty-indicator', isDirty);
    };

    const getFormState = () => {
        const fields = Array.from(form.elements)
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

    const initialState = getState();
    const refreshDirtyState = () => {
        setDirty(getState() !== initialState);
    };

    form.addEventListener('input', refreshDirtyState, true);
    form.addEventListener('change', refreshDirtyState, true);
    form.addEventListener('click', event => {
        if (event.target.closest('.btn-add-field, .btn-remove')) {
            window.setTimeout(refreshDirtyState, 0);
        }
    }, true);
    form.addEventListener('submit', () => setDirty(false));

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
        getFormState,
        getState,
        initialState
    };
}

