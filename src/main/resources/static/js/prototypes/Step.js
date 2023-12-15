export class Step {

    constructor() {
        this.title;
        this.workflowId;
        this.stepNumber;
        this.allSignToComplete;
        this.multiSign;
        this.autoSign;
        this.signType;
        this.forceAllSign;
        this.userSignFirst;
        this.recipientsCCEmails;
        this.changeable;
        this.recipients = [];
    }
}