import {SignUi} from "../ui/signrequests/SignUi.js?version=@version@";
import {SignUiFrontDto} from "../ui/signrequests/dto/SignUiFrontDto.js?version=@version@";

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

const csrf = {
    headerName: metaContent("_csrf_header"),
    parameterName: metaContent("_csrf_parameter"),
    token: metaContent("_csrf")
};
const favoriteSignRequestParamsElement = document.getElementById("favorite-signrequest-params-json");

if (favoriteSignRequestParamsElement) {
const signRequestProfilePath = document.body.dataset.esupSignrequestProfilePath || "user";
const signRequestId = document.body.dataset.esupSignrequestId || "0";
const favoriteSignRequestParamsJson = favoriteSignRequestParamsElement.value || "{}";
const signatureUiConfig = parseJsonTextarea("signature-ui-config-json", null);
const signRequestFrontUrl = `/${signRequestProfilePath}/signrequests/${signRequestId}/front`;

sessionStorage.setItem("favoriteSignRequestParams", favoriteSignRequestParamsJson);

fetch(signRequestFrontUrl, {
    cache: "no-store",
    headers: {
        "Accept": "application/json"
    }
})
    .then(async response => {
        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }
        return response.json();
    })
    .then(data => new SignUi(SignUiFrontDto.from(data), csrf, signatureUiConfig))
    .catch(error => {
        console.error("Impossible de charger le bootstrap front de signature", error);
        bootbox.alert("Impossible de charger l’interface de signature.");
    });
}
