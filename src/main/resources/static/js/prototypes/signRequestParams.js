export class SignRequestParams {
    pdSignatureFieldName;
    signPageNumber;
    xPos;
    yPos;

    constructor(pdSignatureFieldName, signPageNumber, xPos, yPos) {
        this.pdSignatureFieldName = pdSignatureFieldName;
        this.signPageNumber = signPageNumber;
        this.xPos = xPos;
        this.yPos = yPos;
    }
}