export class SignRequestParams {
    pdSignatureFieldName;
    signPageNumber;
    xPos;
    yPos;

    constructor(currentSignRequestParams) {
        Object.assign(this, currentSignRequestParams);
    }
}