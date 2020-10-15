import {Message} from "../../prototypes/Message.js";
import {EventFactory} from "./EventFactory.js";

export class SseSubscribe extends EventFactory {

    constructor() {
        super();
        this.eventSource = new EventSource('/user/sse');
        this.listenToEvent();
    }

    listenToEvent() {
        console.info("subscribe to events");
        this.eventSource.addEventListener("global", response => {
            let message = new Message(JSON.parse(response.data));
            console.info("new global event : ");
            console.info(message);
            let event = new CustomEvent("global");
            event.initCustomEvent("global", false, false, message);
            document.dispatchEvent(event);
        }, false);
        this.eventSource.addEventListener("user", response => {
            let message = new Message(JSON.parse(response.data));
            console.info("new user event : ");
            console.info(message);
            let event = new CustomEvent("user");
            event.initCustomEvent("user", false, false, message);
            document.dispatchEvent(event);
        }, false);
        this.eventSource.addEventListener("sign", response => {
            console.info("new sign event : ");
            console.info(response.data);
            let message = new Message(JSON.parse(response.data));
            console.info("new sign event : ");
            console.info(message);
            let event = new CustomEvent("sign");
            event.initCustomEvent("sign", false, false, message);
            document.dispatchEvent(event);
        }, false);
    }

}