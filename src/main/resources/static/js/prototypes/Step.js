export class Step {

    constructor(signRequestParams) {
        this.workflowId;
        this.recipientsEmails;
        this.stepNumber;
        this.allSignToComplete;
        this.signType;
        Object.assign(this, signRequestParams);
    }


}