export class SseDispatcher {

    constructor() {
    }

    dispatchEvent(type, message) {
        console.info("dispatch event : " + type + " " + message);
        let event = new CustomEvent(type);
        event.initCustomEvent(type, false, false, message);
        document.dispatchEvent(event);
    }

}