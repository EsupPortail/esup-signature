export class SignRequest {

    id;
    title;
    signPageNumber;
    xPos;
    yPos;
    addName;
    addDate;
    
    constructor(signRequest) {
        Object.assign(this, signRequest);
    }
}