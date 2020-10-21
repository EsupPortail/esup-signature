import {Message} from "../../prototypes/Message.js";
import {EventFactory} from "./EventFactory.js";
import {SseDispatcher} from "./SseDispatcher.js";

export class SseSubscribe extends EventFactory {

    constructor() {
        super();
        console.log(window.EventSource);
        // if (!!window.EventSource) {
            this.eventSource = new EventSource('/sse');
        // }
        this.listenToEvent();
        this.sseDispatcher = new SseDispatcher();
    }

    listenToEvent() {
        console.info("subscribe to events");
        this.eventSource.addEventListener("global", response => {
            console.info("receive global event");
            let message = new Message(JSON.parse(response.data));
            this.sseDispatcher.dispatchEvent("global", message);
        }, false);
        this.eventSource.addEventListener("user", response => {
            console.info("receive user event");
            let message = new Message(JSON.parse(response.data));
            this.sseDispatcher.dispatchEvent("user", message);
        }, false);
        this.eventSource.addEventListener("sign", response => {
            console.info("receive sign event");
            let message = new Message(JSON.parse(response.data));
            this.sseDispatcher.dispatchEvent("sign", message);
        }, false);
    }
}