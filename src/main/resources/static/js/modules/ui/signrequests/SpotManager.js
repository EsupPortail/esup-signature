export class SpotManager {

    constructor(state, options = {}) {
        this.state = state;
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

    changeSpotStep() {
        let stepNumber = $("#spotStepNumber").val();
        $('[id^="liveStep-"]').each(function () {
            $(this).find(".step-vertical-content")
                .removeClass("bg-success bg-secondary-subtle")
                .addClass("bg-light");
        });
        let liveStep = $("#liveStep-" + stepNumber);
        liveStep.find(".step-vertical-content")
            .removeClass("bg-light bg-secondary-subtle")
            .addClass("bg-secondary");
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
            return step >= currentStep;
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
        this.options.setToolsDisabled(false);
        this.options.setSignSpacesDroppableEnabled(true);
        this.options.setSpotActionButtonsDisabled(false);

        const spotId = parseInt(spotData.id, 10);
        const spotStep = parseInt(spotData.stepNumber, 10);
        const currentStep = parseInt(this.options.getCurrentStepNumber(), 10);
        const normalizedSpot = {
            id: spotId,
            signPageNumber: parseInt(spotData.signPageNumber, 10),
            xPos: parseInt(spotData.xPos, 10),
            yPos: parseInt(spotData.yPos, 10),
            signWidth: parseInt(spotData.signWidth, 10),
            signHeight: parseInt(spotData.signHeight, 10),
            stepNumber: spotStep
        };

        const existingSpotIdx = spots.findIndex(spot => parseInt(spot?.id, 10) === spotId);
        if (existingSpotIdx >= 0) {
            spots[existingSpotIdx] = {...spots[existingSpotIdx], ...normalizedSpot};
        } else {
            spots.push(normalizedSpot);
        }
        this.options.setSpots(spots);

        if (this.options.isSignable() && Number.isFinite(spotStep) && Number.isFinite(currentStep) && spotStep === currentStep) {
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
        this.options.setToolsDisabled(true);
        this.options.setSignSpacesDroppableEnabled(false);
        $(document).off("click" + this.options.spotAddNamespace);
        $(document).on("click" + this.options.spotAddNamespace, "#delete-add-spot, #submit-add-spot", () => {
            this.options.setToolsDisabled(false);
            this.options.setSignSpacesDroppableEnabled(true);
            $(document).off("click" + this.options.spotAddNamespace);
        });
        $("#commentHelp").remove();
        this.options.setSpotActionButtonsDisabled(true);
        this.options.startSpotPlacement();
    }

    destroy() {
        $(document).off("click" + this.options.spotAddNamespace);
    }

}

