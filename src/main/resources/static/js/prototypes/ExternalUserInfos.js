export class ExternalUserInfos {

    constructor(externalUserInfos) {
        this.email;
        this.name;
        this.firstname;
        this.phone;
        Object.assign(this, externalUserInfos);
    }


}