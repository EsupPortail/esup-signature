export class SignToolbar {

    constructor(options = {}) {
        this.eventNamespace = options.eventNamespace ?? ".esupSignToolbar";
        this.rootSelector = options.rootSelector ?? "#tools";
        this.priorityContainers = options.priorityContainers ?? ["#tools", ".es-nav-tools"];
        this.primaryFocusSelectors = options.primaryFocusSelectors ?? ["#addSignButton2", "#signLaunchButton", "#signAdvancedLaunchButton", "#refuseLaunchButton"];
        this.focusRetryDelay = options.focusRetryDelay ?? 80;
        this.focusableSelector = options.focusableSelector ?? [
            "a[href]",
            "button",
            "input:not([type='hidden'])",
            "select",
            "textarea",
            "[tabindex]"
        ].join(", ");
        this.callbacks = {
            onAddComment: options.onAddComment ?? (() => {}),
            onAddSpot: options.onAddSpot ?? (() => {}),
            onRequestSignatureStep: options.onRequestSignatureStep ?? (() => {}),
            onAddSign: options.onAddSign ?? (() => {}),
            onAddParaph: options.onAddParaph ?? (() => {}),
            onAddCheck: options.onAddCheck ?? (() => {}),
            onAddTimes: options.onAddTimes ?? (() => {}),
            onAddCircle: options.onAddCircle ?? (() => {}),
            onAddMinus: options.onAddMinus ?? (() => {}),
            onAddText: options.onAddText ?? (() => {})
        };
        this.bindings = [
            {
                selector: "#addCommentButton",
                event: "click",
                handler: () => {
                    this.callbacks.onAddComment();
                }
            },
            {
                selector: "#addCommentButton2",
                event: "click",
                handler: () => {
                    this.callbacks.onAddComment();
                }
            },
            {
                selector: "#addSpotButton",
                event: "click",
                handler: () => {
                    this.callbacks.onAddSpot();
                }
            },
            {
                selector: "#addSpotButton2",
                event: "click",
                handler: () => {
                    this.callbacks.onAddSpot();
                }
            },
            {
                selector: "#addSignButton",
                event: "click",
                handler: () => {
                    this.callbacks.onAddSign();
                }
            },
            {
                selector: "#drawSignButton",
                event: "click",
                handler: () => {
                    this.callbacks.onRequestSignatureStep();
                    this.callbacks.onAddSign();
                }
            },
            {
                selector: "#addSignButton2",
                event: "click",
                handler: () => {
                    this.callbacks.onAddSign();
                }
            },
            {
                selector: "#addSignButton3",
                event: "click",
                handler: () => {
                    this.callbacks.onAddSign();
                }
            },
            {
                selector: "#addParaphButton",
                event: "click",
                handler: () => {
                    this.callbacks.onAddParaph();
                }
            },
            {
                selector: "#addParaphButton2",
                event: "click",
                handler: () => {
                    this.callbacks.onAddParaph();
                }
            },
            {
                selector: "#addCheck",
                event: "click",
                handler: () => {
                    this.callbacks.onAddCheck();
                }
            },
            {
                selector: "#addTimes",
                event: "click",
                handler: () => {
                    this.callbacks.onAddTimes();
                }
            },
            {
                selector: "#addCircle",
                event: "click",
                handler: () => {
                    this.callbacks.onAddCircle();
                }
            },
            {
                selector: "#addMinus",
                event: "click",
                handler: () => {
                    this.callbacks.onAddMinus();
                }
            },
            {
                selector: "#addText",
                event: "click",
                handler: () => {
                    this.callbacks.onAddText();
                }
            }
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

    getPriorityContainers() {
        return this.priorityContainers
            .map(selector => document.querySelector(selector))
            .filter(container => container != null);
    }

    getFocusableCandidates(container) {
        if (container == null) {
            return [];
        }
        return Array.from(container.querySelectorAll(this.focusableSelector)).filter(element => {
            if (element == null) {
                return false;
            }
            if (element.matches("[tabindex='-1']")) {
                return false;
            }
            if (element.closest(".modal") != null) {
                return false;
            }
            return true;
        });
    }

    getPriorityElements() {
        return this.getPriorityContainers().flatMap(container => this.getFocusableCandidates(container));
    }

    normalizeNonToolbarTabIndexes(priorityElements) {
        const prioritySet = new Set(priorityElements);
        document.querySelectorAll("body.es-signrequest-page [tabindex]").forEach(element => {
            if (prioritySet.has(element) || element.closest(".modal") != null) {
                return;
            }
            const tabIndex = Number.parseInt(element.getAttribute("tabindex"), 10);
            if (Number.isFinite(tabIndex) && tabIndex > 0) {
                element.setAttribute("tabindex", "0");
            }
        });
    }

    applyPriorityTabOrder() {
        const priorityElements = this.getPriorityElements();
        this.normalizeNonToolbarTabIndexes(priorityElements);
        let tabIndex = 1;
        priorityElements.forEach(element => {
            element.setAttribute("tabindex", String(tabIndex));
            tabIndex += 1;
        });
    }

    isElementVisible(element) {
        if (element == null || element.getClientRects().length === 0) {
            return false;
        }
        const style = window.getComputedStyle(element);
        return style.display !== "none"
            && style.visibility !== "hidden"
            && !element.hasAttribute("hidden")
            && element.getAttribute("aria-hidden") !== "true";
    }

    isElementEnabled(element) {
        return element != null
            && !element.disabled
            && element.getAttribute("aria-disabled") !== "true";
    }

    findPrimaryFocusableElement(selectors = this.primaryFocusSelectors) {
        for (const selector of selectors) {
            const element = document.querySelector(selector);
            if (this.isElementVisible(element) && this.isElementEnabled(element)) {
                return element;
            }
        }
        return this.getPriorityElements().find(element => this.isElementVisible(element) && this.isElementEnabled(element)) ?? null;
    }

    focusPrimaryAction(selectors = this.primaryFocusSelectors, options = {}) {
        const attempts = options.attempts ?? 10;
        const delay = options.delay ?? this.focusRetryDelay;
        const tryFocus = remainingAttempts => {
            const element = this.findPrimaryFocusableElement(selectors);
            if (element != null) {
                element.focus({preventScroll: true});
                return true;
            }
            if (remainingAttempts <= 0) {
                return false;
            }
            window.setTimeout(() => tryFocus(remainingAttempts - 1), delay);
            return false;
        };
        return tryFocus(attempts);
    }

    refreshAccessibility() {
        this.applyPriorityTabOrder();
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
        $("#commentsTools")
            .toggleClass("disabled", disabled)
            .attr("aria-disabled", disabled ? "true" : "false").css("opacity", disabled ? "0.5" : "1");
        $("#addSpotButton").prop("disabled", disabled);
        $("#addCommentButton").prop("disabled", disabled);
        $("#addSpotButton2").prop("disabled", disabled);
        $("#addCommentButton2").prop("disabled", disabled);
    }

    setInsertActionsDisabled(disabled) {
        const insertTools = $("#insert-btn-div");
        insertTools
            .toggleClass("opacity-50", disabled)
            .attr("aria-disabled", disabled ? "true" : "false");
        insertTools.find("button").prop("disabled", disabled);
    }

    destroy() {
        this.bindings.forEach(binding => {
            $(binding.selector).off(binding.event + this.eventNamespace);
        });
    }

}
