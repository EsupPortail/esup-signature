export class Step {
    externalUsersInfos;

    constructor(signRequestParams) {
        this.workflowId;
        this.recipientsEmails;
        this.stepNumber;
        this.allSignToComplete;
        this.multiSign;
        this.signType;
        this.externalUsersInfos = [];
        Object.assign(this, signRequestParams);
    }


}