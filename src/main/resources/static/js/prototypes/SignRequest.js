export class SignRequest {

    constructor(signRequest) {
        this.id;
        this.title;
        this.signPageNumber;
        this.xPos;
        this.yPos;
        this.addName;
        this.addDate;
        Object.assign(this, signRequest);
    }
}