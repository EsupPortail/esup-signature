document.addEventListener("submit", event => {
    const form = event.target.closest("form.es-confirm-submit[data-confirm-message]");
    if (form != null && !window.confirm(form.dataset.confirmMessage)) {
        event.preventDefault();
    }
});
