import {WizUi} from "../ui/WizUi.js?version=@version@";
import NotificationCenter from "../ui/NotificationCenter.js?version=@version@";

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? "";
}

function parseNumber(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
}

function csrfToken() {
    return {
        headerName: metaContent("_csrf_header"),
        parameterName: metaContent("_csrf_parameter"),
        token: metaContent("_csrf")
    };
}

function maxUploadSize() {
    return parseNumber(metaContent("esup-max-upload-size"));
}

function filenameFromDisposition(disposition) {
    if (disposition == null || disposition === "") {
        return null;
    }
    const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
    if (utf8Match != null) {
        return decodeFilename(utf8Match[1].replace(/"/g, ""));
    }
    const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
    return filenameMatch != null ? decodeFilename(filenameMatch[1]) : null;
}

function decodeFilename(filename) {
    if (filename == null || filename === "") {
        return filename;
    }
    let decoded = filename.replace(/\+/g, " ");
    for (let i = 0; i < 2; i++) {
        try {
            const nextDecoded = decodeURIComponent(decoded);
            if (nextDecoded === decoded) {
                break;
            }
            decoded = nextDecoded;
        } catch (e) {
            break;
        }
    }
    return decoded;
}

async function fetchSignedFile(documentId, fallbackFilename) {
    const response = await fetch(`/ws-secure/global/get-file/${documentId}`, {
        credentials: "same-origin"
    });
    if (!response.ok) {
        throw new Error("Impossible de récupérer le dernier document signé.");
    }
    const blob = await response.blob();
    const filename = filenameFromDisposition(response.headers.get("Content-Disposition"))
        || decodeFilename(fallbackFilename)
        || "document-signe.pdf";
    return new File([blob], filename, {
        type: blob.type || "application/pdf",
        lastModified: Date.now()
    });
}

function setLoading(button, loading) {
    button.disabled = loading;
    button.querySelector(".js-reuse-last-signed-spinner")?.classList.toggle("d-none", !loading);
    button.querySelector(".js-reuse-last-signed-icon")?.classList.toggle("d-none", loading);
}

function addFileToFastSignWizard(wizUi, file) {
    if (wizUi?.input == null || !wizUi.input.length) {
        throw new Error("Le formulaire de demande de signature n'est pas disponible.");
    }
    wizUi.input.fileinput("clear");
    wizUi.input.fileinput("clearFileStack");
    wizUi.input.fileinput("readFiles", [file]);
    $("#title").val(decodeFilename(file.name).replace(/\.[^/.]+$/, ""));
}

function initReuseLastSignedButtons() {
    document.querySelectorAll(".js-reuse-last-signed-file").forEach(button => {
        button.addEventListener("click", event => {
            event.preventDefault();
            const documentId = button.dataset.documentId;
            if (documentId == null || documentId === "") {
                return;
            }
            setLoading(button, true);
            const wizUi = new WizUi("", $("#wiz-fast-sign-div"), csrfToken(), maxUploadSize());
            wizUi.fastStartSign(async loadedWizUi => {
                try {
                    const file = await fetchSignedFile(documentId, button.dataset.filename);
                    addFileToFastSignWizard(loadedWizUi, file);
                    loadedWizUi.requestModalFocus();
                } catch (error) {
                    NotificationCenter.showSnackbar(error.message || "Impossible de préparer la nouvelle demande.", "error");
                } finally {
                    setLoading(button, false);
                }
            });
        });
    });
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initReuseLastSignedButtons);
} else {
    initReuseLastSignedButtons();
}
