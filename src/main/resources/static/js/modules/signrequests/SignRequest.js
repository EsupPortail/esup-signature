import {SignRequestHelp} from "../help/SignRequestHelp.js?version=@version@";

class User {
    constructor(id, eppn, name, firstname, email) {
        this.id = id;
        this.eppn = eppn;
        this.login = eppn != null ? eppn.split("@")[0] : "";
        this.name = name;
        this.firstname = firstname;
        this.email = email;
    }
}

function readUiMe() {
    try {
        const rawUiMe = sessionStorage.getItem("uiMe");
        return rawUiMe ? JSON.parse(rawUiMe) : null;
    } catch (e) {
        return null;
    }
}

function waitForFreshUiMe(callback) {
    if (document.documentElement.dataset.globalUiReady === "true") {
        callback(readUiMe());
        return;
    }

    function onUiMeLoaded(event) {
        document.removeEventListener("globalUiReady", onGlobalUiReady);
        callback(event.detail ?? null);
    }

    function onGlobalUiReady() {
        document.removeEventListener("uiMeLoaded", onUiMeLoaded);
        callback(readUiMe());
    }

    document.addEventListener("uiMeLoaded", onUiMeLoaded, {once: true});
    document.addEventListener("globalUiReady", onGlobalUiReady, {once: true});
}

function parseNumber(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
}

function buildUser(prefix) {
    const dataset = document.body.dataset;
    return new User(
        parseNumber(dataset[`${prefix}Id`]),
        dataset[`${prefix}Eppn`] || "",
        dataset[`${prefix}Name`] || "",
        dataset[`${prefix}Firstname`] || "",
        dataset[`${prefix}Email`] || ""
    );
}

window.user = buildUser("esupUser");
window.creator = buildUser("esupCreator");
window.currentStepNumber = document.body.dataset.esupCurrentStepNumber || "";
window.supervisors = [];
window.isLastStep = document.body.dataset.esupIsLastStep === "true";

if (document.body.dataset.esupSignrequestHelpEnabled === "true") {
    waitForFreshUiMe(uiMe => {
        const isOtpHelp = document.body.dataset.esupSignrequestHelpOtp === "true";
        new SignRequestHelp(uiMe?.uiParams?.signRequestHelp ?? null, isOtpHelp).autoStart();
    });
}
