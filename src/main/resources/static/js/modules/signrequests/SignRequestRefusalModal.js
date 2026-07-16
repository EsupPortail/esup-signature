import FilesInput from "../utils/FilesInput.js?version=@version@";

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? "";
}

function parseNumber(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
}

function toggleSource() {
    const useExisting = $("#docSourceExisting").is(":checked");
    $("#existingBlock").toggle(useExisting);
    $("#uploadBlock").toggle(!useExisting);
    $("#multipartFiles1").prop("required", !useExisting);
}

$('input[name="documentSource"]').on("change", toggleSource);
toggleSource();

const csrf = {
    headerName: metaContent("_csrf_header"),
    parameterName: metaContent("_csrf_parameter"),
    token: metaContent("_csrf")
};
const maxUploadSize = parseNumber(metaContent("esup-max-upload-size"));
const multipartFilesInput = $("#multipartFiles1");
if (multipartFilesInput.length) {
    new FilesInput(multipartFilesInput, maxUploadSize, csrf, null, false, null);
}
