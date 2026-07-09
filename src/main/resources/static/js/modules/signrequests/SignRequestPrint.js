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

document.addEventListener("click", event => {
    if (!event.target.closest(".js-print-download-warning")) {
        return;
    }
    alert("Le document ainsi téléchargé est destiné exclusivement à l`impression. Les signatures électroniques ne sont présentes que dans la version numérique");
});
