export class SignRequestParams {

    _pdSignatureFieldName;
    _signImageNumber;
    _signPageNumber;
    _xPos;
    _yPos;
    _addName;
    _addDate;

    constructor(signRequestParams) {
        Object.assign(this, signRequestParams);
    }

    setxPos(x) {
        this._xPos = Math.round(x);
    }

    setyPos(y) {
        this._yPos = Math.round(y);
    }


    get pdSignatureFieldName() {
        return this._pdSignatureFieldName;
    }

    set pdSignatureFieldName(value) {
        this._pdSignatureFieldName = value;
    }

    get signImageNumber() {
        return this._signImageNumber;
    }

    set signImageNumber(value) {
        this._signImageNumber = value;
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