import FilesInput from "../utils/FilesInput.js?version=@version@";

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? "";
}

function parseJsonTextarea(id, fallback) {
    const value = document.getElementById(id)?.value;
    if (value == null || value === "") {
        return fallback;
    }
    try {
        return JSON.parse(value);
    } catch (error) {
        console.error("Unable to parse JSON from " + id, error);
        return fallback;
    }
}

function parseNumber(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
}

const csrf = {
    headerName: metaContent("_csrf_header"),
    parameterName: metaContent("_csrf_parameter"),
    token: metaContent("_csrf")
};
const maxUploadSize = parseNumber(metaContent("esup-max-upload-size"));
const originalDocuments = parseJsonTextarea("signrequest-original-documents-json", []);
const signedDocuments = parseJsonTextarea("signrequest-signed-documents-json", []);

[
    ["#multipartFiles", originalDocuments],
    ["#signedFiles", signedDocuments]
].forEach(([selector, documents]) => {
    const input = $(selector);
    if (input.length) {
        new FilesInput(input, maxUploadSize, csrf, documents, true);
    }
});
