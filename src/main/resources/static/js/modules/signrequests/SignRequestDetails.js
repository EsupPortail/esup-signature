function updatePdfAlertsIndicator() {
    const hasPdfAlerts = document.querySelector(".es-pdf-alert-marker") != null;
    if (!hasPdfAlerts) {
        return;
    }
    document.getElementById("display-pdf-alerts-dropdown")?.classList.remove("d-none");
    document.getElementById("infos-btn")?.classList.add("form-dirty-indicator");
}

document.getElementById("infos-btn")?.addEventListener("click", event => {
    event.currentTarget.classList.remove("form-dirty-indicator");
});

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", updatePdfAlertsIndicator, {once: true});
} else {
    updatePdfAlertsIndicator();
}
