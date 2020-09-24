export class CsrfToken {

    headerName;
    parameterName;
    token;

    constructor(csrfToken) {
        Object.assign(this, csrfToken);
    }
}