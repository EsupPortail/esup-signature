export class Step {

    constructor(signRequestParams) {
        this.workflowId;
        this.recipientsEmails;
        this.stepNumber;
        this.allSignToComplete;
        this.signType;
        this.names = [];
        this.firstnames = [];
        this.phones= [];
        Object.assign(this, signRequestParams);
    }


}