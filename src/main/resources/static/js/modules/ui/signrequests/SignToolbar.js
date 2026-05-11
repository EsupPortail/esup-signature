export class SignToolbar {

    constructor(options = {}) {
        this.eventNamespace = options.eventNamespace ?? ".esupSignToolbar";
        this.rootSelector = options.rootSelector ?? "#tools";
        this.callbacks = {
            onAddComment: options.onAddComment ?? (() => {}),
            onAddSpot: options.onAddSpot ?? (() => {}),
            onAddSign: options.onAddSign ?? (() => {}),
            onAddParaph: options.onAddParaph ?? (() => {}),
            onAddCheck: options.onAddCheck ?? (() => {}),
            onAddTimes: options.onAddTimes ?? (() => {}),
            onAddCircle: options.onAddCircle ?? (() => {}),
            onAddMinus: options.onAddMinus ?? (() => {}),
            onAddText: options.onAddText ?? (() => {})
        };
        this.bindings = [
            { selector: "#addCommentButton", event: "click", handler: () => this.callbacks.onAddComment() },
            { selector: "#addCommentButton2", event: "click", handler: () => this.callbacks.onAddComment() },
            { selector: "#addSpotButton", event: "click", handler: () => this.callbacks.onAddSpot() },
            { selector: "#addSpotButton2", event: "click", handler: () => this.callbacks.onAddSpot() },
            { selector: "#addSignButton", event: "click", handler: () => this.callbacks.onAddSign() },
            { selector: "#addSignButton2", event: "click", handler: () => this.callbacks.onAddSign() },
            { selector: "#addSignButton3", event: "click", handler: () => this.callbacks.onAddSign() },
            { selector: "#addParaphButton", event: "click", handler: () => this.callbacks.onAddParaph() },
            { selector: "#addParaphButton2", event: "click", handler: () => this.callbacks.onAddParaph() },
            { selector: "#addCheck", event: "click", handler: () => this.callbacks.onAddCheck() },
            { selector: "#addTimes", event: "click", handler: () => this.callbacks.onAddTimes() },
            { selector: "#addCircle", event: "click", handler: () => this.callbacks.onAddCircle() },
            { selector: "#addMinus", event: "click", handler: () => this.callbacks.onAddMinus() },
            { selector: "#addText", event: "click", handler: () => this.callbacks.onAddText() }
        ];
    }

    bind() {
        this.bindings.forEach(binding => {
            $(binding.selector)
                .off(binding.event + this.eventNamespace)
                .on(binding.event + this.eventNamespace, binding.handler);
        });
    }

    setToolsDisabled(disabled) {
        const toolsBar = $(this.rootSelector);
        if (!toolsBar.length) {
            return;
        }
        toolsBar.toggleClass("tools-disabled", disabled);
        toolsBar.attr("aria-disabled", disabled ? "true" : "false");
    }


    setCommentAddActive(enabled) {
        $("#addSpotButton").attr("disabled", enabled);
        $("#addSpotButton2").attr("disabled", enabled);
        $("#addCommentButton").toggleClass("border-dark", enabled);
        const addCommentButton2 = $("#addCommentButton2");
        addCommentButton2.toggleClass("bg-danger", enabled);
        addCommentButton2.children().toggleClass("text-white", enabled);
        addCommentButton2.attr("title", enabled ? "Annuler l'ajout d'annotation" : "Ajouter une annotation");
    }

    setSpotActionButtonsDisabled(disabled) {
        $("#addSpotButton").attr("disabled", disabled);
        $("#addCommentButton").attr("disabled", disabled);
        $("#addSpotButton2").attr("disabled", disabled);
        $("#addCommentButton2").attr("disabled", disabled);
    }

    destroy() {
        this.bindings.forEach(binding => {
            $(binding.selector).off(binding.event + this.eventNamespace);
        });
    }

}

