export class Message {

    constructor(message) {
        this.type;
        this.text;
        this.object;
        Object.assign(this, message);
    }
}