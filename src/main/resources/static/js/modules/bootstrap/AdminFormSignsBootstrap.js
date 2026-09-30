import FormSignsUi from "../ui/forms/FormSignsUi.js?version=@version@";
import {SignUi} from "../ui/signrequests/SignUi.js?version=@version@";
import {SignUiFrontDto} from "../ui/signrequests/dto/SignUiFrontDto.js?version=@version@";

function metaContent(name) {
    return document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") ?? "";
}

function parseJsonTextarea(id, fallback) {
    const value = document.getElementById(id)?.value;
    if (value == null || value === "") {
        return fallback;
    }
    try {
        return JSON.parse(value);
    } catch (error) {
        console.error("Unable to parse JSON from " + id, error);
        return fallback;
    }
}

const csrf = {
    headerName: metaContent("_csrf_header"),
    parameterName: metaContent("_csrf_parameter"),
    token: metaContent("_csrf")
};
const bodyData = document.body.dataset;
const workflowRole = bodyData.esupWorkflowRole;
const formId = Number.parseInt(bodyData.esupFormId, 10);

new FormSignsUi(workflowRole, formId, csrf);

if (bodyData.esupWorkflowAvailable === "true") {
    const rawSpots = parseJsonTextarea("admin-form-signs-spots-json", []);
    const rawSrpMap = parseJsonTextarea("admin-form-signs-srp-map-json", {});
    const signatureUiConfig = parseJsonTextarea("admin-form-signs-signature-ui-config-json", null);
    const idToStepMap = {};

    Object.entries(rawSrpMap).forEach(([step, srpId]) => {
        const parsedStep = Number.parseInt(step, 10);
        const parsedSrpId = Number.parseInt(srpId, 10);
        if (Number.isFinite(parsedStep) && Number.isFinite(parsedSrpId)) {
            idToStepMap[parsedSrpId] = parsedStep;
        }
    });

    const spotsWithStepNumber = rawSpots.map(spot => {
        const parsedId = Number.parseInt(spot?.id, 10);
        const mappedStep = Number.isFinite(parsedId) ? idToStepMap[parsedId] : null;
        return {
            ...spot,
            stepNumber: Number.isFinite(mappedStep) ? mappedStep : (spot?.stepNumber ?? 1)
        };
    });
    const defaultSignImageNumber = Number.parseInt(bodyData.esupDefaultSignImageNumber, 10);
    const formSignsFrontDto = SignUiFrontDto.from({
        signRequestId: formId,
        dataId: null,
        formId,
        currentSignRequestParamses: spotsWithStepNumber,
        signImageNumber: Number.isFinite(defaultSignImageNumber) ? defaultSignImageNumber : null,
        currentSignType: "form",
        signable: false,
        editable: true,
        comments: [],
        spots: spotsWithStepNumber,
        pdf: bodyData.esupPdf === "true",
        currentStepNumber: 1,
        currentStepMultiSign: false,
        currentStepSingleSignWithAnnotation: false,
        currentStepMinSignLevel: null,
        workflowAvailable: false,
        signImages: [],
        userName: workflowRole,
        authUserName: "forms",
        fields: [],
        stepRepeatable: null,
        status: "pending",
        action: null,
        nbSignRequests: 1,
        notSigned: true,
        attachmentAlert: false,
        attachmentRequire: false,
        otp: false,
        restore: true,
        phone: null,
        returnToHomeAfterSign: false,
        manager: true
    });

    new SignUi(formSignsFrontDto, csrf, signatureUiConfig);
}
