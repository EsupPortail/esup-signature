import SelectUser from "../utils/SelectUser.js?version=@version@";

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? "";
}

const csrf = {
    headerName: metaContent("_csrf_header"),
    parameterName: metaContent("_csrf_parameter"),
    token: metaContent("_csrf")
};

if (document.getElementById("recipientsEmails2")) {
    new SelectUser("recipientsEmails2", null, null, csrf);
}

const addStepModal = document.getElementById("addStep");
const autoSignCheckbox = document.getElementById("autoSign");
const stepNumberInput = document.getElementById("stepNumber");
const positionLabel = document.getElementById("addStepPositionLabel");
const recipientsDiv = $("#recipients-div");
const recipientsSelect = $("#recipientsEmails2");
const allSignDiv = $("#all-sign-to-complete-div");
const signTypeDiv = $("#sign-type-div");

function updateAddStepPositionLabel(stepNumber) {
    if (!positionLabel) {
        return;
    }
    if (stepNumber === 0) {
        positionLabel.textContent = "L’étape sera ajoutée au début du circuit.";
    } else if (stepNumber === -1) {
        positionLabel.textContent = "L’étape sera ajoutée à la fin du circuit.";
    } else {
        positionLabel.textContent = "L’étape sera ajoutée après l’étape " + stepNumber + ".";
    }
}

function toggleAddStepAutoSign(isChecked) {
    allSignDiv.toggleClass("d-none", isChecked);
    signTypeDiv.toggleClass("d-none", isChecked);
    recipientsDiv.toggleClass("d-none", isChecked);
    if (isChecked) {
        recipientsSelect.removeAttr("required");
    } else {
        recipientsSelect.attr("required", "required");
    }
}

autoSignCheckbox?.addEventListener("change", function() {
    if (this.checked) {
        bootbox.confirm("Attention tous les paramètres de cette étape seront perdus après la validation", result => {
            if (result) {
                toggleAddStepAutoSign(true);
            } else {
                autoSignCheckbox.checked = false;
                toggleAddStepAutoSign(false);
            }
        });
    } else {
        toggleAddStepAutoSign(false);
    }
});

addStepModal?.addEventListener("show.bs.modal", event => {
    const trigger = event.relatedTarget;
    const requestedStepNumber = Number.parseInt(trigger?.getAttribute("data-step-number") ?? "-1", 10);
    const normalizedStepNumber = Number.isNaN(requestedStepNumber) ? -1 : requestedStepNumber;
    stepNumberInput.value = normalizedStepNumber;
    updateAddStepPositionLabel(normalizedStepNumber);
    toggleAddStepAutoSign(autoSignCheckbox?.checked === true);
});

updateAddStepPositionLabel(Number.parseInt(stepNumberInput?.value ?? "-1", 10));
toggleAddStepAutoSign(autoSignCheckbox?.checked === true);
