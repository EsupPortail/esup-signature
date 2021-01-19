export class Step {

    constructor(signRequestParams) {
        this.recipientsEmails;
        this.stepNumber;
        this.allSignToComplete;
        this.signType;
        Object.assign(this, signRequestParams);
    }


}