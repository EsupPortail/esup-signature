import {Message} from "../../prototypes/Message.js?version=@version@";

export default class NotificationCenter {

    constructor(message) {
        if (NotificationCenter.instance != null) {
            const normalizedMessage = NotificationCenter.instance.normalizeMessage(message);
            if (normalizedMessage != null) {
                NotificationCenter.instance.launch(normalizedMessage);
            }
            return NotificationCenter.instance;
        }

        this.notificationBackdrop = $("#toast-backdrop, #notification-backdrop").first();
        this.inlineSnackbar = this.getInlineSnackbar();
        this.activeNotification = null;
        this.activeTimer = null;
        this.progressTimer = null;
        this.isBound = false;
        this.initialMessage = this.normalizeMessage(message);
        NotificationCenter.instance = this;
        this.init();
    }

    static getInstance() {
        if (NotificationCenter.instance == null) {
            new NotificationCenter();
        }
        return NotificationCenter.instance;
    }

    static show(message) {
        return NotificationCenter.getInstance().launch(message);
    }

    static showSnackbar(text, type = "info", options = {}) {
        return NotificationCenter.show({type, text, ...options});
    }

    init() {
        $(document).ready(() => {
            this.inlineSnackbar = this.getInlineSnackbar();
            this.bindListeners();
            this.displayNewsMessages();
            if (this.initialMessage != null) {
                const message = this.initialMessage;
                this.initialMessage = null;
                this.launch(message);
            }
        });
    }

    bindListeners() {
        if (this.isBound) {
            return;
        }

        this.isBound = true;
        $(document)
            .off("click.esupToastDismiss click.esupNotificationDismiss")
            .on("click.esupNotificationDismiss", "[data-es-dismiss='notification'], [data-bs-dismiss='toast']", event => {
                const notification = $(event.currentTarget).closest(".es-notification, .toast-es");
                if (notification.length > 0) {
                    this.hideElement(notification);
                } else {
                    this.hideInlineSnackbar();
                }
            });

        document.addEventListener("esup:notify", event => {
            this.launch(event.detail);
        });

        window.esupNotification = this;
        window.esupToast = this;
        window.showNotification = message => NotificationCenter.show(message);
        window.showSnackbar = (text, type = "info", options = {}) => NotificationCenter.showSnackbar(text, type, options);
    }

    getInlineSnackbar() {
        return document.querySelector("#snackbar, .es-notification--snackbar");
    }

    getPanelSelector() {
        return ".es-notification--panel, .toast-es";
    }

    getNewsSelector() {
        return "[data-es-notification-kind='news'], div[id^='toast-message-']";
    }

    normalizeMessage(message) {
        if (message == null || message === "" || message === "null") {
            return null;
        }

        if (message instanceof Message) {
            return message;
        }

        if (typeof message === "string") {
            return new Message({
                type: "info",
                text: message
            });
        }

        return new Message(message);
    }

    displayNewsMessages() {
        $(this.getNewsSelector()).each((index, element) => {
            $(element)
                .addClass("show")
                .attr("aria-hidden", "false")
                .css("z-index", 10000 + index);
        });
    }

    launch(message) {
        const normalizedMessage = this.normalizeMessage(message);
        if (normalizedMessage == null || normalizedMessage.text == null || normalizedMessage.text === "") {
            return null;
        }

        if ((this.inlineSnackbar ?? this.getInlineSnackbar()) == null && document.readyState === "loading") {
            this.initialMessage = normalizedMessage;
            return null;
        }

        console.info("display notification : " + normalizedMessage.type + " " + normalizedMessage.text);
        this.hideAll();

        if (["custom", "help"].includes(normalizedMessage.type)) {
            return this.launchPanel(normalizedMessage);
        }

        return this.launchInlineSnackbar(normalizedMessage);
    }

    launchPanel(message) {
        const panel = $("[data-es-notification-kind='" + message.type + "'], #toast-" + message.type).first();
        const panelMessage = panel.find("#message-" + message.type).first();
        if (panel.length === 0 || panelMessage.length === 0) {
            return this.launchInlineSnackbar(message);
        }

        if (message.type === "custom") {
            panelMessage.html("<pre>" + message.text + "</pre>");
        } else {
            panelMessage.html(message.text);
        }

        this.showElement(panel);
        return panel;
    }

    launchInlineSnackbar(message) {
        const snackbar = this.inlineSnackbar ?? this.getInlineSnackbar();
        if (snackbar == null) {
            return null;
        }

        snackbar.innerHTML = message.text;
        snackbar.classList.remove(...this.getSnackbarStateClasses());
        snackbar.classList.add("show", ...this.getSnackbarVariantClasses(message.type));
        snackbar.setAttribute("aria-hidden", "false");

        this.activeNotification = snackbar;

        const explicitDelay = Number.parseInt(message.delay, 10);
        const delay = Number.isFinite(explicitDelay)
            ? explicitDelay
            : this.getDelay($(snackbar), this.getDefaultDelay(message.type));
        if (delay > 0) {
            this.activeTimer = window.setTimeout(() => {
                this.hideInlineSnackbar();
            }, delay);
        }

        return snackbar;
    }

    getSnackbarStateClasses() {
        return [
            "snackbar-error",
            "snackbar-warning",
            "snackbar-success",
            "snackbar-info",
            "snackbar-custom",
            "is-error",
            "is-warning",
            "is-success",
            "is-info",
            "is-custom"
        ];
    }

    getSnackbarVariantClasses(type) {
        switch (type) {
            case "error":
                return ["snackbar-error", "is-error"];
            case "warn":
                return ["snackbar-warning", "is-warning"];
            case "success":
                return ["snackbar-success", "is-success"];
            case "custom":
                return ["snackbar-custom", "is-custom"];
            case "info":
            default:
                return ["snackbar-info", "is-info"];
        }
    }

    getDefaultDelay(type) {
        switch (type) {
            case "error":
                return 4000;
            case "warn":
                return 10000;
            case "success":
                return 2000;
            case "info":
            default:
                return 3000;
        }
    }

    getDelay(element, fallback = 0) {
        const rawDelay = element.attr("data-es-delay") ?? element.attr("data-bs-delay");
        const delay = Number.parseInt(rawDelay, 10);
        return Number.isFinite(delay) ? delay : fallback;
    }

    showElement(element) {
        if (element == null || element.length === 0) {
            return;
        }

        const delay = this.getDelay(element);
        const withBackdrop = element.attr("data-es-backdrop") === "true";
        element
            .addClass("show")
            .attr("aria-hidden", "false")
            .css("z-index", 10000);

        this.activeNotification = element.get(0);

        if (withBackdrop) {
            this.showBackdrop();
        }

        this.setUpProgressBar(element, delay);

        if (delay > 0 && delay < 999999999) {
            this.activeTimer = window.setTimeout(() => {
                this.hideElement(element);
            }, delay);
        }
    }

    showBackdrop() {
        this.notificationBackdrop
            .addClass("backdrop")
            .off("click.esupToastBackdrop click.esupNotificationBackdrop")
            .on("click.esupNotificationBackdrop", () => {
                this.hideCurrentNotification();
            });
    }

    hideBackdrop() {
        this.notificationBackdrop
            .removeClass("backdrop")
            .off("click.esupToastBackdrop click.esupNotificationBackdrop");
    }

    isInlineSnackbarElement(element) {
        const snackbar = this.inlineSnackbar ?? this.getInlineSnackbar();
        return element != null && snackbar != null && (element === snackbar || element.classList?.contains("es-notification--snackbar"));
    }

    hideCurrentNotification() {
        if (this.activeNotification == null) {
            return;
        }

        if (this.isInlineSnackbarElement(this.activeNotification)) {
            this.hideInlineSnackbar();
            return;
        }

        this.hideElement($(this.activeNotification));
    }

    hideAll() {
        this.clearTimers();
        this.hideInlineSnackbar(false);
        $(this.getPanelSelector())
            .removeClass("show")
            .attr("aria-hidden", "true")
            .css("z-index", -1);
        $(this.getPanelSelector()).find("progress").each((_, progress) => {
            $(progress).attr("value", 0);
        });
        this.hideBackdrop();
        this.activeNotification = null;
    }

    hideElement(element) {
        if (element == null || element.length === 0) {
            return;
        }

        this.clearTimers();
        element
            .removeClass("show")
            .attr("aria-hidden", "true")
            .css("z-index", -1);
        element.find("progress").attr("value", 0);
        this.hideBackdrop();
        if (this.activeNotification === element.get(0)) {
            this.activeNotification = null;
        }
    }

    hideInlineSnackbar(clearState = true) {
        const snackbar = this.inlineSnackbar ?? this.getInlineSnackbar();
        if (snackbar == null) {
            return;
        }

        snackbar.classList.remove("show", ...this.getSnackbarStateClasses());
        snackbar.setAttribute("aria-hidden", "true");

        if (clearState && this.activeNotification === snackbar) {
            this.clearTimers();
            this.activeNotification = null;
        }
    }

    clearTimers() {
        if (this.activeTimer != null) {
            window.clearTimeout(this.activeTimer);
            this.activeTimer = null;
        }
        if (this.progressTimer != null) {
            window.clearInterval(this.progressTimer);
            this.progressTimer = null;
        }
    }

    setUpProgressBar(element, delay) {
        const progressBar = element.find("progress").first();
        if (progressBar.length === 0) {
            return;
        }

        if (!Number.isFinite(delay) || delay <= 0 || delay >= 999999999) {
            progressBar.attr("max", 1);
            progressBar.attr("value", 0);
            return;
        }

        const startedAt = Date.now();
        progressBar.attr("max", delay);
        progressBar.attr("value", delay);

        this.progressTimer = window.setInterval(() => {
            const remaining = Math.max(delay - (Date.now() - startedAt), 0);
            progressBar.attr("value", remaining);
            if (remaining === 0 && this.progressTimer != null) {
                window.clearInterval(this.progressTimer);
                this.progressTimer = null;
            }
        }, 50);
    }

}

NotificationCenter.instance = null;


