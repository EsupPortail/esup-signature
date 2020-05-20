export class SignRequestParams {

    constructor(currentSignRequestParams) {
        this.pdSignatureFieldName;
        this.signPageNumber;
        this.xPos;
        this.yPos;
        Object.assign(this, currentSignRequestParams);
    }
}