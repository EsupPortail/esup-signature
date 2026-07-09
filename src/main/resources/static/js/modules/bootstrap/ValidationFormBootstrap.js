document.addEventListener("click", event => {
    const clearButton = event.target.closest(".js-clear-file-input");
    if (!clearButton) {
        return;
    }
    const input = document.getElementById(clearButton.dataset.clearTarget);
    if (input) {
        input.value = "";
    }
});
