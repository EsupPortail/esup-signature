export class SignRequestParams {

    pdSignatureFieldName;
    signImageNumber;
    signPageNumber;
    xPos;
    yPos;
    addName;
    addDate;

    constructor(signRequestParams) {
        Object.assign(this, signRequestParams);
    }

    setxPos(x) {
        this.xPos = Math.round(x);
    }

    setyPos(y) {
        this.yPos = Math.round(y);
    }


}