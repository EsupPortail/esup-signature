import {Message} from "../../prototypes/Message.js?version=@version@";

export default class Toast {

    constructor(message) {
        this.resetToasts();
        this.launchMsg = (message != null && message !== "");
        this.message = new Message(message);
        this.toastBackdrop = $("#toast-backdrop");
        this.toast = null;
        // this.initListener();
        this.init();
    }

    // initListener() {
    //     document.addEventListener("global", e => this.launch(e.detail));
    //     document.addEventListener("user", e => this.launch(e.detail));
    // }

    init() {
        let self = this;
        $(document).ready(function() {
            $("div[id^='toast-message-']").each(function() {
                let idToast = $(this).attr("id");
                $(this).css('z-index', 10000);
                $("#" + idToast).toast('show');
            });
            if(self.launchMsg) {
                self.launch(self.message);
            }
        });
    }

    resetToasts() {
        $(".toast-es").each(function() {
            $(this).css('z-index', -1);
            $(this).on('hidden.bs.toast', function (){
                $(this).css('z-index', -1);
            })
        });
    }

    launch(message) {
        console.info("display toast : " + message.type + " " + message.text);
        $(".toast-es").each(function(e) {
            $(this).toast('hide');
        });
        let toast = $("#toast-" + message.type);
        let toastMessage = $("#message-" + message.type);
        if(message.type === "custom") {
            toastMessage.html("<pre>" + message.text + "</pre>");
        } else {
            toastMessage.html(message.text);
        }
        toast.css('z-index', 10000);
        toast.toast('show');
        let toastBackdrop = this.toastBackdrop;
        toast.on('hidden.bs.toast', function () {
            toastBackdrop.removeClass("backdrop");
            toastBackdrop.unbind();
        })
        toastBackdrop.addClass("backdrop");
        toastBackdrop.on("click", function () {
            toast.toast('hide');
        });
        this.toast = toast;
        new Notification(message.text);
        let start = new Date();
        let end = new Date();
        end.setSeconds(start.getSeconds() + (toast.attr('data-bs-delay') / 1000));
        this.setUpProgressBar("#progress-" + message.type, start.getTime(), end.getTime(), 50)
    }

    setUpProgressBar(selector, startTime, endTime, update) {
        let timer;
        let elem = $(selector);
        let max = endTime - startTime;
        elem.attr('max', max);
        elem.attr('value', max);
        let toastBackdrop = this.toastBackdrop;
        let toast = this.toast;
        let setValue = function() {
            let currentTime = new Date().getTime();
            let ellasped = endTime - currentTime;
            if (ellasped < 0) {
                ellasped = 0
                window.clearTimeout(timer)
                toastBackdrop.removeClass("backdrop");
                toastBackdrop.unbind();
                toast.toast('hide');
            }
            elem.attr('value', ellasped);
        }
        setValue();
        timer = window.setInterval(setValue, update);
    }

}