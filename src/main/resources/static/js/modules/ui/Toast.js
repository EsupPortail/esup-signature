import {Message} from "../../prototypes/Message.js";

export default class Toast {

    constructor(message) {
        this.resetToasts();
        this.message = new Message(message);
        if(message) {
            $(document).ready(e => this.launch(this.message));
        }
        $("div[id^='toast-message']").each(function() {
            $(this).css('z-index', 10000);
            $(this).toast('show');
        });
        this.initListener();
    }

    initListener() {
        document.addEventListener("global", e => this.launch(e.detail));
        document.addEventListener("user", e => this.launch(e.detail));
    }

    resetToasts() {
        $('.toast').each(function() {
            $(this).css('z-index', -1);
            $(this).on('hidden.bs.toast', function (){
                toast.css('z-index', -1);
            })
        });
    }

    launch(message) {
        console.info("display toast : " + message.type + " " + message.text);
        let toast = $("#toast-" + message.type);
        let toastMessage = $("#message-" + message.type);
        if(message.type === "custom") {
            toastMessage.html("<pre>" + message.text + "</pre>");
        } else {
            toastMessage.html(message.text);
        }
        toast.css('z-index', 10000);
        toast.toast('show');
        new Notification(message.text);
    }

}