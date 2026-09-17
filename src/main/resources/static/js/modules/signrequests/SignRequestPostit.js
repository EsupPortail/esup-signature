const unevaluatedInlineMarker = "[[";

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? "";
}

function showPostitSnackbar(message, type) {
    if (typeof window.showSnackbar === "function") {
        window.showSnackbar(message, type, {delay: type === "dark" ? 2000 : 4000});
    } else {
        console[type === "error" ? "error" : "info"](message);
    }
}

function isInvalidPostitId(postitId) {
    return !postitId || /\$\{/.test(postitId) || postitId.includes(unevaluatedInlineMarker);
}

function postitUrl(action, postitId) {
    const profilePath = document.body.dataset.esupSignrequestProfilePath || "user";
    const signRequestId = document.body.dataset.esupSignrequestId || "0";
    return `/${profilePath}/signrequests/comment-ajax/${signRequestId}/${action}/${postitId}`;
}

function csrfHeaders() {
    return {
        [metaContent("_csrf_header") || "X-CSRF-TOKEN"]: metaContent("_csrf"),
        "Accept": "application/json",
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
        "X-Requested-With": "XMLHttpRequest"
    };
}

window.postitSaveAjax = async function(event, button) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    const saveBtn = button || (event?.target ? event.target.closest(".postit-save-ajax") : null);
    if (!saveBtn) return false;
    const container = saveBtn.closest(".es-postit-edit-form");
    if (!container) return false;
    const textarea = container.querySelector('textarea[name="comment"]') || container.querySelector("textarea");
    const postitId = saveBtn.dataset.postitId || container.dataset.postitId || (container.id || "").replace("postit-", "") || "";
    if (isInvalidPostitId(postitId)) return false;
    saveBtn.disabled = true;
    try {
        const resp = await fetch(postitUrl("update", postitId), {
            method: "POST",
            redirect: "error",
            headers: csrfHeaders(),
            body: new URLSearchParams({comment: textarea ? textarea.value : ""}).toString()
        });
        if (!resp.ok) throw resp;
        const payload = await resp.json();
        saveBtn.classList.add("disabled");
        setTimeout(() => saveBtn.classList.remove("disabled"), 800);
        showPostitSnackbar(payload.message || "Post-it modifié", "success");
    } catch (err) {
        try {
            const payload = await err.json();
            showPostitSnackbar(payload.message || "Erreur lors de la mise à jour du postit", "error");
        } catch (e) {
            console.error(e);
            showPostitSnackbar("Erreur lors de la mise à jour du postit", "error");
        }
    } finally {
        saveBtn.disabled = false;
    }
    return false;
};

window.postitDeleteAjax = async function(event, button) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    const delBtn = button || (event?.target ? event.target.closest(".postit-delete-ajax") : null);
    if (!delBtn) return false;
    if (!confirm("Confirmez-vous la suppression ?")) return false;
    const postitId = delBtn.dataset.postitId || "";
    if (isInvalidPostitId(postitId)) return false;
    delBtn.disabled = true;
    try {
        const resp = await fetch(postitUrl("delete", postitId), {
            method: "POST",
            redirect: "error",
            headers: csrfHeaders(),
            body: ""
        });
        if (!resp.ok) throw resp;
        const payload = await resp.json();
        delBtn.closest(".postit-global")?.remove();
        showPostitSnackbar(payload.message || "Post-it supprimé", "dark");
    } catch (err) {
        try {
            const payload = await err.json();
            showPostitSnackbar(payload.message || "Erreur lors de la suppression du postit", "error");
        } catch (e) {
            console.error(e);
            showPostitSnackbar("Erreur lors de la suppression du postit", "error");
        }
    } finally {
        delBtn.disabled = false;
    }
    return false;
};

document.addEventListener("click", event => {
    const saveBtn = event.target.closest(".postit-save-ajax");
    if (saveBtn) {
        window.postitSaveAjax(event, saveBtn);
        return;
    }
    const delBtn = event.target.closest(".postit-delete-ajax");
    if (delBtn) {
        window.postitDeleteAjax(event, delBtn);
    }
});
