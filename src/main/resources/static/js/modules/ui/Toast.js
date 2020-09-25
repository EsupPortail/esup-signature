import {Message} from "../../prototypes/Message.js";

export default class Toast {

    constructor(message) {
        this.message = new Message(message);
        $('.toast').each(function() {
            $(this).css('z-index', -1);
        });
        if(message) {
            this.launch(this.message);
            $(document).ready(e =>this.launch(this.message));
        }
        this.initListener();
    }

    initListener() {
        let eventSource = new EventSource('/user/sse');
        eventSource.addEventListener('global', response => {
            console.log(response.data);
            let message = new Message(JSON.parse(response.data));
            console.info("new global event : ");
            console.info(message);
            this.launch(message);
        }, false);

        eventSource.addEventListener('user', response => {
            let message = new Message(JSON.parse(response.data));
            console.info("new user event : " + message);
            this.launch(message);
        }, false);
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
        toast.on('hidden.bs.toast', function (){
            toast.css('z-index', -1);
        })
        new Notification(message.text);
    }

}