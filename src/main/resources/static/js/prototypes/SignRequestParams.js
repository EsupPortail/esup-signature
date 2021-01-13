export class SignRequestParams {

    constructor(signRequestParams) {
        this.pdSignatureFieldName;
        this.signImageNumber;
        this.signPageNumber;
        this.xPos;
        this.yPos;
        this.addExtra;
        this.addName;
        this.addDate;
        Object.assign(this, signRequestParams);
    }

    setxPos(x) {
        this.xPos = Math.round(x);
    }

    setyPos(y) {
        this.yPos = Math.round(y);
    }


}