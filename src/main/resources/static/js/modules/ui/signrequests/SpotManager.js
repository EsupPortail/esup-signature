export class SpotManager {

    constructor(state, options = {}) {
        this.state = state;
        this.spotAddEnabled = false;
        this.stepDefinitions = this.buildStepDefinitions();
        this.activeWorkspaceScope = $();
        this.preservedWorkspaceVisualScope = $();
        this.preservedSidebarScope = $();
        this.options = {
            signSpaceNamespace: options.signSpaceNamespace ?? ".spotManagerSignSpace",
            spotAddNamespace: options.spotAddNamespace ?? ".spotManagerAdd",
            getSpots: options.getSpots ?? (() => []),
            setSpots: options.setSpots ?? (() => {}),
            getCurrentSignRequestParamses: options.getCurrentSignRequestParamses ?? (() => []),
            setCurrentSignRequestParamses: options.setCurrentSignRequestParamses ?? (() => {}),
            getCurrentStepNumber: options.getCurrentStepNumber ?? (() => null),
            isSignable: options.isSignable ?? (() => false),
            getCurrentSignType: options.getCurrentSignType ?? (() => null),
            getUserName: options.getUserName ?? (() => null),
            getFormId: options.getFormId ?? (() => null),
            getSignRequestId: options.getSignRequestId ?? (() => null),
            getCsrf: options.getCsrf ?? (() => null),
            getPdfViewer: options.getPdfViewer ?? (() => null),
            setToolsDisabled: options.setToolsDisabled ?? (() => {}),
            setInsertActionsDisabled: options.setInsertActionsDisabled ?? (() => {}),
            setSignSpacesDroppableEnabled: options.setSignSpacesDroppableEnabled ?? (() => {}),
            setSpotActionButtonsDisabled: options.setSpotActionButtonsDisabled ?? (() => {}),
            exitCommentAddMode: options.exitCommentAddMode ?? (() => {}),
            startSpotPlacement: options.startSpotPlacement ?? (() => {}),
            refreshSignFields: options.refreshSignFields ?? (() => {}),
            removeSignSpaceBySpotId: options.removeSignSpaceBySpotId ?? (() => {}),
            lockSigns: options.lockSigns ?? (() => {}),
            getEditable: options.getEditable ?? (() => false)
        };
    }

    activateSpotAddMode() {
        this.spotAddEnabled = true;
        const pdfViewer = this.options.getPdfViewer();
        const workspace = $("#workspace");
        const signrequestScope = $(".es-signrequest-main-content *");
        const navigationToolsSelector = "#fullheight, #fullheight *, #fullwidth, #fullwidth *, #zoomout, #zoomout *, #zoomin, #zoomin *, #prev, #prev *, #next, #next *, #end-button, #end-button *, #page_num";
        const sidebarRoots = $(".es-signrequest-main-content #sidebar, .es-signrequest-main-content > .es-sidebar");
        this.preservedSidebarScope = sidebarRoots.length
            ? sidebarRoots.parentsUntil(".es-signrequest-main-content").addBack()
            : $();
        const mainContent = workspace.closest(".es-main-content");
        const workspaceElement = workspace.get(0);
        this.activeWorkspaceScope = mainContent.length && workspaceElement != null ? mainContent.children().has(workspaceElement) : $();
        this.preservedWorkspaceVisualScope = workspace.length
            ? workspace.parentsUntil(".es-main-content").addBack()
            : $();
        this.activeWorkspaceScope.addClass("es-spot-add-scope");
        $("body").addClass("es-spot-add-mode");
        signrequestScope.css('pointer-events', 'none');
        $(navigationToolsSelector).css('pointer-events', 'auto');
        $('#workspace, #workspace *').css('pointer-events', 'auto');
        this.preservedSidebarScope.css({
            opacity: 1,
            filter: 'none',
            'pointer-events': 'auto'
        });
        this.preservedSidebarScope.find('*').css('pointer-events', 'auto');
        this.preservedWorkspaceVisualScope.css({
            opacity: 1,
            filter: 'none'
        });
        $("#cross_999999, #cross_999999 *").css('pointer-events', 'auto');
        $("#spot-modal, #spot-modal *").css('pointer-events', 'auto');
        $("#spot-modal .ss-main, #spot-modal .ss-main *, .ss-content, .ss-content *").css('pointer-events', 'auto');
        $(".textLayer").each(function () {
            $(this).addClass("text-disable-selection");
        });
    }

    deactivateSpotAddMode() {
        this.spotAddEnabled = false;
        const pdfViewer = this.options.getPdfViewer();
        const signrequestScope = $(".es-signrequest-main-content *");
        $("body").removeClass("es-spot-add-mode");
        this.activeWorkspaceScope.removeClass("es-spot-add-scope");
        this.activeWorkspaceScope = $();
        this.preservedWorkspaceVisualScope.css({
            opacity: '',
            filter: ''
        });
        this.preservedWorkspaceVisualScope = $();
        this.preservedSidebarScope.css({
            opacity: '',
            filter: '',
            'pointer-events': ''
        });
        this.preservedSidebarScope.find('*').css('pointer-events', '');
        this.preservedSidebarScope = $();
        if (pdfViewer?.pdfDiv != null) {
            pdfViewer.pdfDiv.css('cursor', 'default');
        }
        signrequestScope.css('pointer-events', '');
        $(".textLayer").each(function () {
            $(this).removeClass("text-disable-selection");
        });
    }

    exitSpotAddMode() {
        $(document).off("click" + this.options.spotAddNamespace);
        $(document).off("keydown" + this.options.spotAddNamespace);
        $("#spot-modal").off("hidden.bs.modal" + this.options.spotAddNamespace);
        this.options.setToolsDisabled(false);
        this.options.setInsertActionsDisabled(false);
        this.options.setSignSpacesDroppableEnabled(true);
        this.deactivateSpotAddMode();
        this.options.setSpotActionButtonsDisabled(false);
    }

    cancelSpotAddMode() {
        const deleteBtn = $("#delete-add-spot");
        if (deleteBtn.length) {
            deleteBtn.trigger("click");
            return;
        }
        this.exitSpotAddMode();
    }

    changeSpotStep() {
        let stepNumber = this.getSpotStepField().val();
        $('[id^="liveStep-"]').each(function () {
            $(this).find(".step-vertical-content")
                .removeClass("bg-success bg-secondary-subtle")
                .addClass("bg-light");
        });
        let liveStep = $("#liveStep-" + stepNumber);
        liveStep.find(".step-vertical-content")
            .removeClass("bg-light bg-secondary-subtle")
            .addClass("bg-secondary");
        this.refreshSpotRecipientOptions();
    }

    getSpotStepField() {
        return $("[name='spotStepNumber']").first();
    }

    getSpotRecipientField() {
        return $("[name='recipientId']").first();
    }

    buildStepDefinitions() {
        const rawSteps = Array.isArray(this.state?.signUiDto?.steps)
            ? this.state.signUiDto.steps
            : Array.isArray(this.state?.backDto?.steps)
                ? this.state.backDto.steps
                : [];
        if (rawSteps.length > 0) {
            return rawSteps.map((step, index) => ({
                stepNumber: String(index + 1),
                allSignToComplete: step?.allSignToComplete === true,
                recipients: Array.isArray(step?.recipients)
                    ? step.recipients
                        .filter(recipient => recipient?.id != null)
                        .map(recipient => ({
                            id: String(recipient.id),
                            userId: recipient?.user?.id != null ? String(recipient.user.id) : null,
                            label: recipient?.user?.email ?? `Destinataire #${recipient.id}`
                        }))
                    : []
            }));
        }

        const recipientOptions = this.getSpotRecipientField().find("option[data-step-number]");
        const recipientsByStep = new Map();
        recipientOptions.each((_, optionElement) => {
            const option = $(optionElement);
            const stepNumber = option.attr("data-step-number");
            const value = option.attr("value");
            if (stepNumber == null || value == null || value === "") {
                return;
            }
            if (!recipientsByStep.has(stepNumber)) {
                recipientsByStep.set(stepNumber, []);
            }
            recipientsByStep.get(stepNumber).push({
                id: String(value),
                label: option.text()
            });
        });

        const stepField = this.getSpotStepField();
        if (!stepField.length) {
            return [];
        }

        const definitions = [];
        if (stepField.is("select")) {
            stepField.find("option[value]").each((_, optionElement) => {
                const option = $(optionElement);
                const value = option.attr("value") ?? "";
                if (value === "") {
                    return;
                }
                definitions.push({
                    stepNumber: String(value),
                    allSignToComplete: option.attr("data-es-all-sign-to-complete") === "true",
                    recipients: recipientsByStep.get(String(value)) ?? []
                });
            });
            return definitions;
        }

        return [{
            stepNumber: String(stepField.val() ?? stepField.attr("value") ?? "1"),
            allSignToComplete: stepField.attr("data-es-all-sign-to-complete") === "true",
            recipients: recipientsByStep.get(String(stepField.val() ?? stepField.attr("value") ?? "1")) ?? []
        }];
    }

    getStepDefinition(stepNumber) {
        return this.stepDefinitions.find(step => step.stepNumber === String(stepNumber)) ?? null;
    }

    getStepRecipients(stepNumber) {
        return this.getStepDefinition(stepNumber)?.recipients ?? [];
    }

    getCurrentFrontUserId() {
        const candidates = [];
        candidates.push(this.state?.frontDto?.user?.id ?? null);
        candidates.push(this.state?.showDataFlow?.front?.user?.id ?? null);
        try {
            const rawUiMe = sessionStorage.getItem('uiMe');
            const uiMe = rawUiMe ? JSON.parse(rawUiMe) : null;
            candidates.push(uiMe?.user?.id ?? null);
            candidates.push(uiMe?.authUser?.id ?? null);
        } catch (error) {
            // Ignore malformed session data.
        }
        if (typeof window !== "undefined" && window.user != null) {
            candidates.push(window.user.id ?? null);
        }
        for (let i = 0; i < candidates.length; i++) {
            const parsedUserId = parseInt(candidates[i], 10);
            if (Number.isFinite(parsedUserId)) {
                return parsedUserId;
            }
        }
        return null;
    }

    getCurrentUserRecipientIds(stepNumber) {
        const currentUserId = this.getCurrentFrontUserId();
        if (!Number.isFinite(currentUserId)) {
            return new Set();
        }
        return new Set(
            this.getStepRecipients(stepNumber)
                .filter(recipient => parseInt(recipient?.userId, 10) === currentUserId)
                .map(recipient => parseInt(recipient?.id, 10))
                .filter(recipientId => Number.isFinite(recipientId))
        );
    }

    getSoleCurrentStepRecipientId(stepNumber) {
        const recipients = this.getStepRecipients(stepNumber)
            .map(recipient => parseInt(recipient?.id, 10))
            .filter(recipientId => Number.isFinite(recipientId));
        return recipients.length === 1 ? recipients[0] : null;
    }

    canCurrentUserUseSpot(spot) {
        const spotStepNumber = parseInt(spot?.stepNumber, 10);
        const currentStepNumber = parseInt(this.options.getCurrentStepNumber(), 10);
        if (!Number.isFinite(spotStepNumber) || !Number.isFinite(currentStepNumber) || spotStepNumber !== currentStepNumber) {
            return false;
        }

        const spotRecipientId = parseInt(spot?.recipientId, 10);
        if (!Number.isFinite(spotRecipientId)) {
            return true;
        }

        const currentUserRecipientIds = this.getCurrentUserRecipientIds(spotStepNumber);
        if (currentUserRecipientIds.has(spotRecipientId)) {
            return true;
        }

        const currentParams = Array.isArray(this.options.getCurrentSignRequestParamses())
            ? this.options.getCurrentSignRequestParamses()
            : [];
        if (currentParams.some(param => parseInt(param?.recipientId, 10) === spotRecipientId)) {
            return true;
        }

        const soleRecipientId = this.getSoleCurrentStepRecipientId(spotStepNumber);
        return Number.isFinite(soleRecipientId)
            && soleRecipientId === spotRecipientId
            && this.options.isSignable();
    }

    getStepMetadata(stepNumber) {
        const stepDefinition = this.getStepDefinition(stepNumber);
        if (stepDefinition != null) {
            return {
                allSignToComplete: stepDefinition.allSignToComplete === true,
                recipientCount: stepDefinition.recipients.length
            };
        }

        const spotStepNumber = this.getSpotStepField();
        if (!spotStepNumber.length) {
            return {allSignToComplete: false, recipientCount: 0};
        }

        let source = spotStepNumber;
        if (spotStepNumber.is("select")) {
            source = spotStepNumber.find(`option[value='${stepNumber}']`).first();
            if (!source.length) {
                return {allSignToComplete: false, recipientCount: 0};
            }
        }

        const fallbackRecipients = this.getSpotRecipientField()
            .find(`option[data-step-number='${stepNumber}']`)
            .filter((_, optionElement) => ($(optionElement).attr("value") ?? "") !== "")
            .length;
        const recipientCount = parseInt(source.attr("data-es-recipient-count"), 10);
        return {
            allSignToComplete: source.attr("data-es-all-sign-to-complete") === "true",
            recipientCount: Number.isFinite(recipientCount) ? recipientCount : fallbackRecipients
        };
    }

    stepRequiresRecipientSelection(stepNumber) {
        const metadata = this.getStepMetadata(stepNumber);
        return metadata.recipientCount > 1 && metadata.allSignToComplete === true;
    }

    syncSlimSelect(selectField, items, selectedValue) {
        const slim = selectField.get(0)?.slim;
        if (slim != null && typeof slim.setData === "function") {
            items.forEach(item => {
                item.selected = item.value === selectedValue;
            });
            slim.setData(items);
            if (typeof slim.setSelected === "function") {
                slim.setSelected(selectedValue === "" ? "" : selectedValue);
            }
        }
    }

    escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    rebuildRecipientFieldOptions(recipientField, recipients, selectedValue, occupiedRecipientIds, hasGenericSpot) {
        const optionHtml = ["<option data-placeholder='true' readonly='' value=''>Choisir un destinataire</option>"];
        const slimData = [{
            text: "Choisir un destinataire",
            value: "",
            disabled: false,
            placeholder: true,
            selected: false
        }];

        recipients.forEach(recipient => {
            const value = String(recipient.id);
            const disabled = hasGenericSpot || occupiedRecipientIds.has(value);
            optionHtml.push(`<option value='${this.escapeHtml(value)}'${disabled ? " disabled='disabled'" : ""}>${this.escapeHtml(recipient.label)}</option>`);
            slimData.push({
                text: recipient.label,
                value,
                disabled,
                placeholder: false,
                selected: false
            });
        });

        recipientField.html(optionHtml.join(""));
        const nextValue = slimData.some(item => item.value === selectedValue && item.disabled !== true) ? selectedValue : "";
        recipientField.val(nextValue);
        this.syncSlimSelect(recipientField, slimData, nextValue);
        return nextValue;
    }

    refreshSpotRecipientOptions() {
        const recipientField = this.getSpotRecipientField();
        const recipientWrapper = $("#divSpotRecipientId");
        const spotStepNumber = this.getSpotStepField();
        if (!recipientField.length || !recipientWrapper.length || !spotStepNumber.length) {
            return;
        }

        const stepValue = spotStepNumber.val() ?? "";
        const requiresRecipient = stepValue !== "" && this.stepRequiresRecipientSelection(stepValue);
        let nextValue = recipientField.val() ?? "";
        const spots = Array.isArray(this.options.getSpots()) ? this.options.getSpots() : [];
        const stepSpots = spots.filter(spot => String(spot?.stepNumber ?? "") === String(stepValue));
        const hasGenericSpot = stepSpots.some(spot => spot?.recipientId == null);
        const occupiedRecipientIds = new Set(
            stepSpots
                .map(spot => spot?.recipientId)
                .filter(recipientId => recipientId != null)
                .map(recipientId => String(recipientId))
        );
        const recipients = requiresRecipient ? this.getStepRecipients(stepValue) : [];
        recipientField.prop("required", requiresRecipient);
        recipientField.prop("disabled", !requiresRecipient);
        recipientWrapper.toggleClass("d-none", !requiresRecipient);
        if (!requiresRecipient) {
            nextValue = "";
            recipientField.html("<option data-placeholder='true' readonly='' value=''>Choisir un destinataire</option>");
            recipientField.val("");
            this.syncSlimSelect(recipientField, [{
                text: "Choisir un destinataire",
                value: "",
                disabled: false,
                placeholder: true,
                selected: false
            }], "");
            return;
        }

        nextValue = this.rebuildRecipientFieldOptions(recipientField, recipients, nextValue, occupiedRecipientIds, hasGenericSpot);
        recipientField.val(nextValue);
    }

    refreshSpotStepOptions() {
        const spotStepNumber = this.getSpotStepField();
        if (!spotStepNumber.length || spotStepNumber.attr("type") === "hidden") {
            this.refreshSpotRecipientOptions();
            return;
        }

        const spots = Array.isArray(this.options.getSpots()) ? this.options.getSpots() : [];

        let nextValue = spotStepNumber.val() ?? "";
        const slimData = [];

        spotStepNumber.find("option").each((_, optionElement) => {
            const option = $(optionElement);
            const value = option.attr("value") ?? "";
            const isPlaceholder = option.is("[data-placeholder='true']");
            const stepSpots = spots.filter(spot => String(spot?.stepNumber ?? "") === value);
            let shouldDisable = false;
            if (!isPlaceholder && value !== "") {
                if (this.stepRequiresRecipientSelection(value)) {
                    const metadata = this.getStepMetadata(value);
                    const hasGenericSpot = stepSpots.some(spot => spot?.recipientId == null);
                    const occupiedRecipientCount = new Set(
                        stepSpots
                            .map(spot => spot?.recipientId)
                            .filter(recipientId => recipientId != null)
                            .map(recipientId => String(recipientId))
                    ).size;
                    shouldDisable = hasGenericSpot || (metadata.recipientCount > 0 && occupiedRecipientCount >= metadata.recipientCount);
                } else {
                    shouldDisable = stepSpots.length > 0;
                }
            }
            option.prop("disabled", shouldDisable);
            slimData.push({
                text: option.text(),
                value: value,
                disabled: shouldDisable,
                placeholder: isPlaceholder,
                selected: false
            });
            if (value === nextValue && shouldDisable) {
                nextValue = "";
            }
        });

        spotStepNumber.val(nextValue);
        this.syncSlimSelect(spotStepNumber, slimData, nextValue);
        this.refreshSpotRecipientOptions();
    }

    filterSpotsNotCurrentStep(spots) {
        const currentStep = parseInt(this.options.getCurrentStepNumber(), 10);
        if (!Number.isFinite(currentStep)) {
            return spots;
        }
        return spots.filter(spot => {
            const step = parseInt(spot?.stepNumber, 10);
            if (!Number.isFinite(step)) {
                return true;
            }
            return step > currentStep;
        });
    }

    findSpotIdForSignParams(signParams) {
        if (signParams != null && signParams.id != null && Number.isFinite(parseInt(signParams.id, 10))) {
            return parseInt(signParams.id, 10);
        }
        const spots = this.options.getSpots();
        if (!Array.isArray(spots)) {
            return null;
        }
        const page = parseInt(signParams.signPageNumber, 10);
        const x = parseInt(signParams.xPos, 10);
        const y = parseInt(signParams.yPos, 10);
        const width = parseInt(signParams.signWidth, 10);
        const height = parseInt(signParams.signHeight, 10);
        for (let i = 0; i < spots.length; i++) {
            const spot = spots[i];
            const samePosition = parseInt(spot.signPageNumber, 10) === page && parseInt(spot.xPos, 10) === x && parseInt(spot.yPos, 10) === y;
            const sameSize = !Number.isFinite(width) || !Number.isFinite(height)
                || (parseInt(spot.signWidth, 10) === width && parseInt(spot.signHeight, 10) === height);
            if (samePosition && sameSize) {
                return spot.id;
            }
        }
        return null;
    }

    onSpotSaved(spotData) {
        if (spotData == null) {
            return;
        }
        const spots = Array.isArray(this.options.getSpots()) ? [...this.options.getSpots()] : [];
        $(".step-vertical-content")
            .removeClass("bg-success bg-secondary-subtle")
            .addClass("bg-light");
        this.exitSpotAddMode();

        const spotId = parseInt(spotData.id, 10);
        const spotStep = parseInt(spotData.stepNumber, 10);
        const normalizedSpot = {
            id: spotId,
            signPageNumber: parseInt(spotData.signPageNumber, 10),
            xPos: parseInt(spotData.xPos, 10),
            yPos: parseInt(spotData.yPos, 10),
            signWidth: parseInt(spotData.signWidth, 10),
            signHeight: parseInt(spotData.signHeight, 10),
            stepNumber: spotStep,
            recipientId: spotData.recipientId != null && Number.isFinite(parseInt(spotData.recipientId, 10))
                ? parseInt(spotData.recipientId, 10)
                : null
        };

        const existingSpotIdx = spots.findIndex(spot => parseInt(spot?.id, 10) === spotId);
        if (existingSpotIdx >= 0) {
            spots[existingSpotIdx] = {...spots[existingSpotIdx], ...normalizedSpot};
        } else {
            spots.push(normalizedSpot);
        }
        this.options.setSpots(spots);
        this.refreshSpotStepOptions();

        if (this.options.isSignable() && this.canCurrentUserUseSpot(normalizedSpot)) {
            this.options.removeSignSpaceBySpotId(spotId);
            const currentParams = Array.isArray(this.options.getCurrentSignRequestParamses())
                ? [...this.options.getCurrentSignRequestParamses()]
                : [];
            const alreadyCurrent = currentParams.some(param => parseInt(param?.id, 10) === spotId);
            if (!alreadyCurrent) {
                currentParams.push({...normalizedSpot, ready: false});
                this.options.setCurrentSignRequestParamses(currentParams);
            }
            this.options.refreshSignFields();
        }
    }

    onSpotDeleted(spotId) {
        const parsedId = parseInt(spotId, 10);
        if (!Number.isFinite(parsedId)) {
            return;
        }
        const spots = Array.isArray(this.options.getSpots())
            ? this.options.getSpots().filter(spot => parseInt(spot?.id, 10) !== parsedId)
            : [];
        const currentParams = Array.isArray(this.options.getCurrentSignRequestParamses())
            ? this.options.getCurrentSignRequestParamses().filter(param => parseInt(param?.id, 10) !== parsedId)
            : [];
        this.options.setSpots(spots);
        this.options.setCurrentSignRequestParamses(currentParams);
        this.refreshSpotStepOptions();
    }

    bindSignSpaceDelete(signSpaceDiv) {
        const spotId = parseInt(signSpaceDiv.attr("data-es-spot-id"), 10);
        const deleteBtn = signSpaceDiv.find(".slot-delete-btn");
        if (!Number.isFinite(spotId) || !deleteBtn.length) {
            return;
        }
        deleteBtn.off("click" + this.options.signSpaceNamespace).on("click" + this.options.signSpaceNamespace, e => {
            e.stopPropagation();
            bootbox.confirm("Supprimer cet emplacement de signature ?", result => {
                if (!result) {
                    return;
                }
                const csrf = this.options.getCsrf();
                let url = "/ws-secure/global/delete-spot/" + this.options.getSignRequestId() + "/" + spotId + "?" + csrf.parameterName + "=" + csrf.token;
                if (this.options.getCurrentSignType() === "form") {
                    url = "/" + this.options.getUserName() + "/forms/delete-spot/" + this.options.getFormId() + "/" + spotId + "?" + csrf.parameterName + "=" + csrf.token;
                }
                $.ajax({
                    method: 'DELETE',
                    url: url,
                    success: () => {
                        signSpaceDiv.remove();
                        this.onSpotDeleted(spotId);
                    }
                });
            });
        });
    }

    enableSpotAdd() {
        this.options.exitCommentAddMode();
        this.exitSpotAddMode();
        this.refreshSpotStepOptions();
        this.options.setInsertActionsDisabled(true);
        this.options.setSignSpacesDroppableEnabled(false);
        this.activateSpotAddMode();
        $(document).off("click" + this.options.spotAddNamespace);
        $(document).on("click" + this.options.spotAddNamespace, "#delete-add-spot", () => {
            this.exitSpotAddMode();
        });
        $(document).off("keydown" + this.options.spotAddNamespace);
        $(document).on("keydown" + this.options.spotAddNamespace, e => {
            if (e.key === "Escape") {
                e.preventDefault();
                this.cancelSpotAddMode();
            }
        });
        $("#spot-modal").off("hidden.bs.modal" + this.options.spotAddNamespace);
        $("#commentHelp").remove();
        this.options.setSpotActionButtonsDisabled(true);
        this.options.startSpotPlacement();
    }

    destroy() {
        $(document).off("click" + this.options.spotAddNamespace);
        $(document).off("keydown" + this.options.spotAddNamespace);
        $("#spot-modal").off("hidden.bs.modal" + this.options.spotAddNamespace);
        this.deactivateSpotAddMode();
    }

}
