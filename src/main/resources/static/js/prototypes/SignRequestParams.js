export class SignRequestParams {

    constructor(signRequestParams) {
        this.cross;
        this.pdSignatureFieldName;
        this.signImageNumber = 0;
        this.signPageNumber = 1;
        this.signWidth;
        this.signHeight;
        this.xPos = -1;
        this.yPos = -1;
        this.visual = true;
        this.addWatermark = false;
        this.addExtra = true;
        this.extraOnTop = true;
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.extraText = "";
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