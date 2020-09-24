export class CsrfToken {

    _headerName;
    _parameterName;
    _token;

    constructor(csrfToken) {
        Object.assign(this, csrfToken);
        this._csrfToken = csrfToken;
    }


    get headerName() {
        return this._headerName;
    }

    set headerName(value) {
        this._headerName = value;
    }

    get parameterName() {
        return this._parameterName;
    }

    set parameterName(value) {
        this._parameterName = value;
    }

    get token() {
        return this._token;
    }

    set token(value) {
        this._token = value;
    }

    get csrfToken() {
        return this._csrfToken;
    }

    set csrfToken(value) {
        this._csrfToken = value;
    }
}