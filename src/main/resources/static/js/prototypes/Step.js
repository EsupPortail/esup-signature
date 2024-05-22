export class Step {

    constructor(step) {
        this.title = "";
        this.comment = "";
        this.workflowId = "";
        this.stepNumber = "";
        this.allSignToComplete = false;
        this.multiSign = true;
        this.autoSign = false;
        this.signType = "pdfImageStamp";
        this.forceAllSign = false;
        this.userSignFirst = false;
        this.changeable = false;
        this.recipients = [];
        this.recipientsCCEmails = [];
        this.targetEmails = [];
        Object.assign(this, step);
    }
}