export class SignRequestParams {

    constructor(currentSignRequestParams) {
        this.pdSignatureFieldName;
        this.signImageNumber;
        this.signPageNumber;
        this.xPos;
        this.yPos;
        this.addName;
        this.addDate;
        Object.assign(this, currentSignRequestParams);
    }

    setxPos(x) {
        this.xPos = Math.round(x);
    }

    setyPos(y) {
        this.yPos = Math.round(y);
    }


}