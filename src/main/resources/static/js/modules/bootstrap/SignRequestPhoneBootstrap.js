function initPhoneInput(phoneInput) {
    if (!phoneInput || phoneInput.dataset.intlPhoneInitialized === "true" || typeof intlTelInput !== "function") {
        return;
    }
    phoneInput.dataset.intlPhoneInitialized = "true";
    intlTelInput(phoneInput, {
        validationNumberTypes: "FIXED_LINE_OR_MOBILE",
        strictMode: true,
        separateDialCode: false,
        nationalMode: true,
        countryOrder: ["fr"],
        initialCountry: "auto",
        geoIpLookup: callback => {
            callback(navigator.language.split("-")[0]);
        },
        customPlaceholder: () => "Saisir un numéro",
        searchPlaceholder: "Rechercher"
    });
}

function initPhoneInputs() {
    document.querySelectorAll(".js-intl-phone-input").forEach(initPhoneInput);
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initPhoneInputs, {once: true});
} else {
    initPhoneInputs();
}
