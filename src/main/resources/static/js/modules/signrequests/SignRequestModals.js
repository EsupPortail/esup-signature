function cleanUrlParameter() {
    const urlParams = new URLSearchParams(window.location.search);
    if (!urlParams.has("attachment")) {
        return;
    }
    urlParams.delete("attachment");
    const newUrl = window.location.pathname + (urlParams.toString() ? "?" + urlParams.toString() : "");
    window.history.replaceState({}, document.title, newUrl);
}

function openAttachmentModalIfRequested() {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get("attachment") !== "true") {
        return;
    }
    const attachmentModal = document.getElementById("attachment");
    if (!attachmentModal) {
        return;
    }
    const bsModal = new bootstrap.Modal(attachmentModal, {
        backdrop: "static",
        keyboard: false
    });
    bsModal.show();
    cleanUrlParameter();
    attachmentModal.addEventListener("hidden.bs.modal", cleanUrlParameter);
}

document.addEventListener("click", event => {
    if (event.target.closest(".js-reload-page")) {
        window.location.reload();
    }
});

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", openAttachmentModalIfRequested, {once: true});
} else {
    openAttachmentModalIfRequested();
}
