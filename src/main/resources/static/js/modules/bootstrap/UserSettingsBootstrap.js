import {UserParamsHelp} from '/js/modules/help/UserParamsHelp.js';
import {UserUi} from '/js/modules/ui/users/UserUi.js';
import '/js/modules/bootstrap/UserMobileSignBootstrap.js';

const readJson = (id, fallback = null) => {
    const element = document.getElementById(id);
    if (!element || element.value.trim() === '') {
        return fallback;
    }

    try {
        return JSON.parse(element.value);
    } catch (e) {
        console.debug('Unable to parse user settings JSON payload', id, e);
        return fallback;
    }
};

function readUiMe() {
    try {
        const rawUiMe = sessionStorage.getItem('uiMe');
        return rawUiMe ? JSON.parse(rawUiMe) : null;
    } catch (e) {
        return null;
    }
}

function waitForFreshUiMe(callback) {
    if (document.documentElement.dataset.globalUiReady === 'true') {
        callback(readUiMe());
        return;
    }

    function onUiMeLoaded(event) {
        document.removeEventListener('globalUiReady', onGlobalUiReady);
        callback(event.detail ?? null);
    }

    function onGlobalUiReady() {
        document.removeEventListener('uiMeLoaded', onUiMeLoaded);
        callback(readUiMe());
    }

    document.addEventListener('uiMeLoaded', onUiMeLoaded, {once: true});
    document.addEventListener('globalUiReady', onGlobalUiReady, {once: true});
}

const bootstrapData = document.getElementById('user-settings-bootstrap-data');

window.userUi = new UserUi(
    bootstrapData?.dataset.userName || '',
    readJson('user-settings-favorite-signrequest-params-json'),
    readJson('user-settings-sign-images-json', []),
    bootstrapData?.dataset.userType || '',
    Number(bootstrapData?.dataset.defaultSignImageNumber || 0),
    readJson('user-settings-signature-ui-config-json')
);

if (bootstrapData?.dataset.enableHelp === 'true') {
    waitForFreshUiMe(uiMe => {
        const help = new UserParamsHelp(uiMe?.uiParams?.userParamsHelp ?? null, bootstrapData.dataset.isOtpUserHelp === 'true');
        help.autoStart();
    });
}

document.querySelectorAll('.js-stop-propagation').forEach(element => {
    element.addEventListener('click', event => event.stopPropagation());
});
