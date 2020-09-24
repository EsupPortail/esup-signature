export class SignRequest {

    _id;
    _title;
    _signPageNumber;
    _xPos;
    _yPos;
    _addName;
    _addDate;
    
    constructor(signRequest) {
        Object.assign(this, signRequest);
    }


    get id() {
        return this._id;
    }

    set id(value) {
        this._id = value;
    }

    get title() {
        return this._title;
    }

    set title(value) {
        this._title = value;
    }

    get signPageNumber() {
        return this._signPageNumber;
    }

    set signPageNumber(value) {
        this._signPageNumber = value;
    }

    get xPos() {
        return this._xPos;
    }

    set xPos(value) {
        this._xPos = value;
    }

    get yPos() {
        return this._yPos;
    }

    set yPos(value) {
        this._yPos = value;
    }

    get addName() {
        return this._addName;
    }

    set addName(value) {
        this._addName = value;
    }

    get addDate() {
        return this._addDate;
    }

    set addDate(value) {
        this._addDate = value;
    }
}