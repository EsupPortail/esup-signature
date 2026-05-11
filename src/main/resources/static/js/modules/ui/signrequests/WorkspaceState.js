import {ShowSignRequestDataFlowDto} from "./dto/ShowSignRequestDataFlowDto.js?version=@version@";
import {SignUiFrontDto} from "./dto/SignUiFrontDto.js?version=@version@";

export class WorkspaceState {

    constructor(showDataFlowInput, csrfToken) {
        const {showDataFlow, backDto, frontDto, signUiDto} = this.normalizeInput(showDataFlowInput);

        this.showDataFlow = showDataFlow;
        this.backDto = backDto;
        this.frontDto = frontDto;
        this.signUiDto = signUiDto;
        this.csrfToken = csrfToken;

        this.returnToHome = signUiDto.returnToHomeAfterSign;
        this.signRequestId = signUiDto.signRequestId;
        this.signable = signUiDto.signable;
        this.isOtp = signUiDto.otp;
        this.isPdf = signUiDto.pdf;
        this.formId = signUiDto.formId;
        this.dataId = signUiDto.dataId;
        this.currentSignType = signUiDto.currentSignType;
        this.notSigned = signUiDto.notSigned;
        this.stepRepeatable = signUiDto.stepRepeatable;
        this.currentStepNumber = signUiDto.currentStepNumber;
        this.currentStepMinSignLevel = signUiDto.currentStepMinSignLevel;
        this.nbSignRequests = signUiDto.nbSignRequests;
        this.attachmentRequire = signUiDto.attachmentRequire;
        this.attachmentAlert = signUiDto.attachmentAlert;
        this.status = signUiDto.status;

        this.percent = 0;
        this.gotoNext = null;
        this.signRequestUrlParams = "";
    }

    static from(showDataFlowInput, csrfToken) {
        return new WorkspaceState(showDataFlowInput, csrfToken);
    }

    normalizeInput(input) {
        if (this.isSignUiFrontInput(input)) {
            return {
                showDataFlow: null,
                backDto: null,
                frontDto: null,
                signUiDto: SignUiFrontDto.from(input)
            };
        }

        const showDataFlow = ShowSignRequestDataFlowDto.from(input);
        return {
            showDataFlow,
            backDto: showDataFlow.back,
            frontDto: showDataFlow.front,
            signUiDto: showDataFlow.front.signUi
        };
    }

    isSignUiFrontInput(input) {
        if (input instanceof SignUiFrontDto) {
            return true;
        }
        return input != null
            && typeof input === "object"
            && !Object.prototype.hasOwnProperty.call(input, "back")
            && !Object.prototype.hasOwnProperty.call(input, "front")
            && Object.prototype.hasOwnProperty.call(input, "signRequestId")
            && Object.prototype.hasOwnProperty.call(input, "currentSignRequestParamses");
    }

    toSignUiContext() {
        return {
            showDataFlow: this.showDataFlow,
            backDto: this.backDto,
            frontDto: this.frontDto,
            signUiDto: this.signUiDto,
            csrfToken: this.csrfToken
        };
    }

    toWorkspaceContext() {
        return {
            showDataFlow: this.showDataFlow,
            backDto: this.backDto,
            frontDto: this.frontDto,
            signUiDto: this.signUiDto,
            csrfToken: this.csrfToken
        };
    }

}

