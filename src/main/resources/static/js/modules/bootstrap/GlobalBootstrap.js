import {GlobalUi} from "../ui/GlobalUi.js?version=@version@";

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? null;
}

function parseNumber(value) {
    if (value == null || value === "") {
        return null;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
}

function parseBoolean(value) {
    if (value == null || value === "") {
        return null;
    }
    return value === "true";
}

const csrf = {
    headerName: metaContent("_csrf_header"),
    parameterName: metaContent("_csrf_parameter"),
    token: metaContent("_csrf")
};
const enableSms = metaContent("esup-enable-sms");
const smsRequired = parseBoolean(metaContent("esup-sms-required"));

sessionStorage.setItem("enableSms", JSON.stringify(enableSms));
sessionStorage.setItem("smsRequired", JSON.stringify(smsRequired));

new GlobalUi(
    metaContent("esup-auth-user-eppn"),
    csrf,
    metaContent("esup-application-email"),
    parseNumber(metaContent("esup-max-upload-size")),
    parseNumber(metaContent("esup-max-inactive-interval"))
);

document.addEventListener("change", event => {
    const selector = event.target.closest(".page-size-selector");
    if (selector == null) {
        return;
    }

    const selectedOption = selector.options[selector.selectedIndex];
    const pageSizeUrl = selectedOption?.getAttribute("data-page-size-url");
    if (pageSizeUrl != null && pageSizeUrl !== "") {
        window.location.href = pageSizeUrl;
    }
});
