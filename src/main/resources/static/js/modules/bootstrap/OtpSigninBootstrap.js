let iti = null;

function initPhoneInput() {
    const phoneInput = document.getElementById("phone");
    if (phoneInput == null || typeof intlTelInput !== "function") {
        return;
    }

    phoneInput.addEventListener("focus", () => {
        const v = phoneInput.value;
        const digits = v.replace(/\D/g, "");
        if (digits.length >= 4) {
            phoneInput.value = digits.slice(-4);
        }
        setTimeout(() => phoneInput.setSelectionRange(0, 0), 0);
    });

    iti = intlTelInput(phoneInput, {
        validationNumberTypes: "FIXED_LINE_OR_MOBILE",
        strictMode: true,
        separateDialCode: false,
        initialCountry: "auto",
        geoIpLookup: callback => {
            callback(navigator.language.split("-")[0]);
        },
        nationalMode: true,
        countryOrder: ["fr"],
        customPlaceholder: () => "Saisir un numéro",
        searchPlaceholder: "Rechercher",
    });
}

function initPhoneForm() {
    $("#phoneForm").on("submit", function(e) {
        e.preventDefault();
        $("#phone").attr("disabled", "disabled");
        $("#submitPhone").attr("disabled", "disabled");
        const form = e.target;
        const formData = new FormData();

        if (iti != null) {
            formData.append("phone", iti.getNumber());
        } else {
            formData.append("phone", "");
        }
        formData.append("urlId", form.querySelector("[name=\"urlId\"]").value);
        formData.append("_csrf", form.querySelector("[name=\"_csrf\"]").value);
        let smsPassword = $("#smsPassword");
        fetch("/otp-access/phone", {
            method: "POST",
            headers: {
                "X-CSRF-TOKEN": formData.get("_csrf")
            },
            body: formData
        })
            .then(async response => {
                if (!response.ok) throw new Error(await response.text());
                return response.text();
            })
            .then(data => {
                if (data !== "") {
                    bootbox.alert(data, function() {
                        smsPassword.removeAttr("disabled");
                        $("#smsSubmit").removeAttr("disabled");
                        setTimeout(() => {
                            smsPassword.trigger("focus");
                        }, 50);
                    });
                }
            })
            .catch(error => {
                console.error("Erreur :", error);
                bootbox.alert(error.message, function() {
                    $("#phone").removeAttr("disabled");
                    $("#submitPhone").removeAttr("disabled");
                });
            });
    });
}

document.addEventListener("DOMContentLoaded", () => {
    initPhoneInput();
    initPhoneForm();
}, {once: true});
