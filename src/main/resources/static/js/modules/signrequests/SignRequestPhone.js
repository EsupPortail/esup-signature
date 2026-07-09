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


const resendOtpPhoneInputs = new Map();

document.querySelectorAll(".js-resend-otp-phone").forEach(input => {
    if (typeof intlTelInput === "function") {
        resendOtpPhoneInputs.set(input.id, intlTelInput(input, {
            validationNumberTypes: "FIXED_LINE_OR_MOBILE",
            strictMode: true,
            separateDialCode: false,
            nationalMode: true,
            countryOrder: ["fr"],
            initialCountry: "auto",
            geoIpLookup: callback => callback(navigator.language.split("-")[0]),
            customPlaceholder: () => "Saisir un numéro",
            searchPlaceholder: "Rechercher"
        }));
    }
});

document.addEventListener("click", async event => {
    const button = event.target.closest(".js-resend-otp-confirm");
    if (!button) {
        return;
    }
    const phoneInput = document.getElementById(button.dataset.phoneTarget);
    if (!phoneInput) {
        return;
    }
    const iti = resendOtpPhoneInputs.get(phoneInput.id);
    const fullNumber = iti != null ? iti.getNumber() : "";
    const signBookId = phoneInput.dataset.signbookId;
    const recipientId = phoneInput.dataset.recipientId;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content") || "";
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content") || "X-CSRF-TOKEN";

    button.disabled = true;
    button.textContent = "Envoi en cours...";
    try {
        const response = await fetch(`/user/signrequests/send-otp/${signBookId}/${recipientId}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
                [csrfHeader]: csrfToken
            },
            body: new URLSearchParams({phone: fullNumber})
        });
        if (response.ok) {
            alert("Nouvelle demande OTP envoyée");
            window.location.reload();
        } else {
            alert("Une erreur est survenue lors de l'envoi du SMS");
            button.disabled = false;
            button.textContent = "Confirmer";
        }
    } catch (error) {
        console.error("Erreur:", error);
        alert("Erreur de connexion au serveur");
        button.disabled = false;
        button.textContent = "Confirmer";
    }
});
