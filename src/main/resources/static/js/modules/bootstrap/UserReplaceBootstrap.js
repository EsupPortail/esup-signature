document.querySelectorAll('.js-confirm-transfer').forEach(link => {
    link.addEventListener('click', event => {
        if (!window.confirm(link.dataset.confirmMessage || 'Merci de confirmer cette action')) {
            event.preventDefault();
        }
    });
});
