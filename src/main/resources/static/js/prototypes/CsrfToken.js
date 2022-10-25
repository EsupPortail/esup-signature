export class CsrfToken {

    constructor(csrfToken) {
        this.headerName = "";
        this.parameterName = "";
        this.token = "";
        Object.assign(this, csrfToken);
    }
}