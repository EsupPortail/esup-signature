import {Home2Help} from "../help/Home2Help.js?version=@version@";
import {HomeUi} from "../ui/HomeUi.js?version=@version@";

function readUiMe() {
    try {
        const rawUiMe = sessionStorage.getItem("uiMe");
        return rawUiMe ? JSON.parse(rawUiMe) : null;
    } catch (e) {
        return null;
    }
}

function startHelp(setting) {
    const help2 = new Home2Help(setting);
    help2.autoStart();
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

window.toggleSignLevel = function() {
    const signType = document.getElementById("signType-1")?.value;
    const minSignLevelDiv = $("#minSignLevel");
    const sealOptions = $("#seal-options");
    if (signType === "visa") {
        minSignLevelDiv.hide();
        sealOptions.show();
    } else if (signType === "signature") {
        minSignLevelDiv.show();
        sealOptions.hide();
    } else {
        minSignLevelDiv.hide();
        sealOptions.hide();
    }
};

new HomeUi();

if (document.body?.dataset.esupHomeHelpEnabled === "true") {
    waitForFreshUiMe(uiMe => startHelp(uiMe?.uiParams?.home2Help ?? null));
}
