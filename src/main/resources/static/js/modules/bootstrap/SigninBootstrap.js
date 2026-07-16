document.addEventListener("click", event => {
    const button = event.target.closest(".es-auth-button[data-url]");
    if (!button) {
        return;
    }

    window.location.href = button.dataset.url;
});

const jquery = window.jQuery ?? window.$;
const Captcha = window.jCaptcha;
const captchaForm = jquery?.("#captchaForm");

if (captchaForm?.length && typeof Captcha === "function") {
    const captchaFillStyle = getComputedStyle(document.documentElement).getPropertyValue("--bs-dark").trim();
    const myCaptcha = new Captcha({
        el: ".jCaptcha",
        canvasClass: "jCaptchaCanvas",
        canvasStyle: {
            width: 150,
            height: 15,
            textBaseline: "top",
            font: "15px Arial",
            textAlign: "left",
            fillStyle: captchaFillStyle
        },
        callback: (response, $captchaInputElement, numberOfTries) => {
            if (response === "success") {
                jquery("#loginBlock").removeClass("d-none");
                captchaForm.addClass("d-none");
            }
            if (response === "error") {
                const captchaInput = jquery("#captchaInput");
                captchaInput.removeClass("success");
                captchaInput.addClass("error");
                captchaInput.attr("placeholder", "Please try again!");

                if (numberOfTries === 3) {
                    captchaForm.off("submit", formSubmit);
                    captchaInput.attr("placeholder", "Maximum attempts reached!");
                    captchaInput.attr("disabled", "true");
                    jquery("#captchaButton").attr("disabled", "true");
                }
            }
        }
    });

    function formSubmit(event) {
        event.preventDefault();
        myCaptcha.validate();
    }

    captchaForm.on("submit", formSubmit);
}
