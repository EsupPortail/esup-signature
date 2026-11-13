document.getElementById("print-last-file-button")?.addEventListener("click", event => {
    const printUrl = event.currentTarget.dataset.printUrl;
    if (!printUrl) {
        return;
    }
    const printWindow = window.open(printUrl, "", "");
    $(printWindow).ready(() => {
        setTimeout(() => {
            printWindow.window.print();
        }, 500);
    });
});
