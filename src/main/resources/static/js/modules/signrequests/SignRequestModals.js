function initSidebarModals() {
    const sidebar = document.getElementById("sidebar");
    if (!sidebar) {
        return;
    }
    // Keep dialogs outside the offcanvas stacking context and hidden content.
    sidebar.querySelectorAll(".modal").forEach(modal => document.body.appendChild(modal));
    document.addEventListener("show.bs.modal", event => {
        if (!sidebar.classList.contains("show")) {
            return;
        }
        event.preventDefault();
        sidebar.addEventListener("hidden.bs.offcanvas", () => {
            bootstrap.Modal.getOrCreateInstance(event.target).show(event.relatedTarget);
        }, {once: true});
        bootstrap.Offcanvas.getOrCreateInstance(sidebar).hide();
    });
}

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
    document.addEventListener("DOMContentLoaded", initSidebarModals, {once: true});
    document.addEventListener("DOMContentLoaded", openAttachmentModalIfRequested, {once: true});
} else {
    initSidebarModals();
    openAttachmentModalIfRequested();
}
