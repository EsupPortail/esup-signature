export class Step {

    constructor(step) {
        this.title = "";
        this.comment = "";
        this.workflowId = "";
        this.stepNumber = "";
        this.allSignToComplete = "";
        this.multiSign = "";
        this.autoSign = false;
        this.signType = "pdfImageStamp";
        this.forceAllSign = false;
        this.userSignFirst = false;
        this.changeable = "";
        this.recipients = [];
        this.recipientsCCEmails = [];
        this.repeatable = "";
        this.targetEmails = [];
        Object.assign(this, step);
    }
}