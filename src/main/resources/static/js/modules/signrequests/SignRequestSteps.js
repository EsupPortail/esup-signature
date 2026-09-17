document.addEventListener("click", event => {
    const showTrigger = event.target.closest(".js-step-menu-show");
    if (showTrigger) {
        const target = document.getElementById(showTrigger.dataset.menuTarget);
        if (target) {
            $(target).show();
        }
        return;
    }

    const hideTrigger = event.target.closest(".js-step-menu-hide");
    if (hideTrigger) {
        const target = document.getElementById(hideTrigger.dataset.menuTarget);
        if (target) {
            $(target).hide();
        }
        return;
    }

    const toggleTrigger = event.target.closest(".js-step-menu-toggle");
    if (toggleTrigger) {
        const target = document.getElementById(toggleTrigger.dataset.menuTarget);
        if (target) {
            $(target).toggle();
        }
        return;
    }

    const confirmSubmit = event.target.closest(".js-confirm-submit");
    if (confirmSubmit && !confirm(confirmSubmit.dataset.confirmMessage || "Confirmez-vous cette action ?")) {
        event.preventDefault();
    }
});

document.addEventListener("click", event => {
    if (event.target.closest(".js-stop-propagation")) {
        event.stopPropagation();
    }
});
