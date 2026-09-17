import {ListSignBooksUi} from "../ui/signbooks/ListSignBooksUi.js?version=@version@";

const preferenceStorageKey = "esup-user-signbooks-display-mode";

function readStoragePreference() {
    try {
        return window.localStorage.getItem(preferenceStorageKey);
    } catch (error) {
        console.debug("LocalStorage indisponible pour la préférence d’affichage des signbooks", error);
        return null;
    }
}

function applyDisplayModePreference() {
    if (window.location.pathname !== "/user/signbooks") {
        return;
    }

    const storedMode = readStoragePreference();
    const hasStoredPreference = storedMode === "infinite" || storedMode === "pagination";
    if (!hasStoredPreference) {
        return;
    }
    const resolvedInfiniteScrolling = storedMode === "infinite";
    const params = new URLSearchParams(window.location.search);
    const currentInfiniteScrolling = params.get("infiniteScrolling");
    if (currentInfiniteScrolling === String(resolvedInfiniteScrolling)) {
        return;
    }
    params.set("infiniteScrolling", String(resolvedInfiniteScrolling));
    params.delete("page");
    const queryString = params.toString();
    window.location.replace(window.location.pathname + (queryString !== "" ? "?" + queryString : ""));
}

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? "";
}

function nullableDataset(dataset, key) {
    const value = dataset[key];
    return value == null || value === "" ? null : value;
}

function initSignBooksList() {
    const dataset = document.body.dataset;
    const mode = nullableDataset(dataset, "signbooksMode") ?? "user";
    const csrf = {
        headerName: metaContent("_csrf_header"),
        parameterName: metaContent("_csrf_parameter"),
        token: metaContent("_csrf")
    };

    new ListSignBooksUi(
        nullableDataset(dataset, "statusFilter"),
        nullableDataset(dataset, "recipientsFilter"),
        nullableDataset(dataset, "workflowFilter"),
        nullableDataset(dataset, "creatorFilter"),
        nullableDataset(dataset, "docTitleFilter"),
        nullableDataset(dataset, "dateFilter"),
        dataset.infiniteScrolling === "true",
        csrf,
        mode,
        mode === "user" ? {
            preferenceStorageKey,
            toggleSelector: "#signbooksDisplayModeSwitch"
        } : {}
    );
}

applyDisplayModePreference();

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initSignBooksList, {once: true});
} else {
    initSignBooksList();
}

document.addEventListener("click", event => {
    if (event.target.closest(".js-signbooks-reload")) {
        window.location.reload();
        return;
    }
    if (event.target.closest(".js-signbooks-reports")) {
        window.location.href = "/user/reports";
    }
});
