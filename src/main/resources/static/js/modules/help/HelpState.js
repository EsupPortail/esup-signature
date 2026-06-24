export function markIntroAsRead(helpKey, isOtp = false) {
    const previousUiMe = readUiMe();
    const previousValue = previousUiMe?.uiParams?.[helpKey];

    updateCachedIntroState(helpKey, 'true');

    return $.get(getMarkIntroUrl(helpKey, isOtp))
        .fail(() => restoreCachedIntroState(helpKey, previousValue));
}

function getMarkIntroUrl(helpKey, isOtp) {
    return (isOtp ? '/otp/users/mark-intro-as-read/' : '/user/users/mark-intro-as-read/') + helpKey;
}

function readUiMe() {
    try {
        const rawUiMe = sessionStorage.getItem('uiMe');
        return rawUiMe ? JSON.parse(rawUiMe) : null;
    } catch (e) {
        return null;
    }
}

function updateCachedIntroState(helpKey, value) {
    const uiMe = readUiMe();
    if (uiMe == null) {
        return;
    }

    uiMe.uiParams = uiMe.uiParams || {};
    uiMe.uiParams[helpKey] = value;
    sessionStorage.setItem('uiMe', JSON.stringify(uiMe));
}

function restoreCachedIntroState(helpKey, previousValue) {
    const uiMe = readUiMe();
    if (uiMe == null) {
        return;
    }

    uiMe.uiParams = uiMe.uiParams || {};
    if (previousValue == null) {
        delete uiMe.uiParams[helpKey];
    } else {
        uiMe.uiParams[helpKey] = previousValue;
    }
    sessionStorage.setItem('uiMe', JSON.stringify(uiMe));
}
