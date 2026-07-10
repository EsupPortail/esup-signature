function initConfirmSubmit() {
    document.querySelectorAll(".es-confirm-submit[data-confirm-message]").forEach(form => {
        form.addEventListener("submit", event => {
            if (!window.confirm(form.dataset.confirmMessage)) {
                event.preventDefault();
            }
        });
    });
}

document.addEventListener("DOMContentLoaded", initConfirmSubmit, {once: true});
