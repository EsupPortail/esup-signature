export class SignRequestParams {

    constructor(signRequestParams) {
        this.pdSignatureFieldName;
        this.signImageNumber = 0;
        this.signPageNumber;
        this.signWidth;
        this.signHeight;
        this.xPos = -1;
        this.yPos = -1;
        this.addExtra = true;
        this.extraOnTop = true;
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.addName;
        this.addDate;
        this.signScale = 1;
        this.red = 0;
        this.green = 0;
        this.blue = 0;
        Object.assign(this, signRequestParams);
    }

    setxPos(x) {
        this.xPos = Math.round(x);
    }

    setyPos(y) {
        this.yPos = Math.round(y);
    }


}