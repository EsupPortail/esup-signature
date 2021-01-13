export class SignRequestParams {

    constructor(signRequestParams) {
        this.pdSignatureFieldName;
        this.signImageNumber = 0;
        this.signPageNumber;
        this.signWidth;
        this.signHeight;
        this.xPos;
        this.yPos;
        this.addExtra = false;
        this.extraWidth = 0;
        this.addName;
        this.addDate;
        this.signScale = 1;
        Object.assign(this, signRequestParams);
    }

    setxPos(x) {
        this.xPos = Math.round(x);
    }

    setyPos(y) {
        this.yPos = Math.round(y);
    }


}