export class SignSpaceManager {

	constructor(state, options = {}) {
		this.state = state;
		this.options = {
			signSpaceNamespace: options.signSpaceNamespace ?? ".signSpaceManager",
			getPdfViewer: options.getPdfViewer ?? (() => null),
			getSignPlacementController: options.getSignPlacementController ?? (() => null),
			getCurrentSignRequestParamses: options.getCurrentSignRequestParamses ?? (() => []),
			getSpots: options.getSpots ?? (() => []),
			getCurrentStepNumber: options.getCurrentStepNumber ?? (() => null),
			isSignable: options.isSignable ?? (() => false),
			isEditable: options.isEditable ?? (() => false),
			isManager: options.isManager ?? (() => false),
			isCreator: options.isCreator ?? (() => false),
			getBrowserZoom: options.getBrowserZoom ?? (() => 1),
			requestAddSign: options.requestAddSign ?? (() => {}),
			findSpotIdForSignParams: options.findSpotIdForSignParams ?? (() => null),
			filterSpotsNotCurrentStep: options.filterSpotsNotCurrentStep ?? (spots => spots),
			bindSignSpaceDelete: options.bindSignSpaceDelete ?? (() => {})
		};
	}

	canViewAllSpots() {
		return this.options.isManager() || this.options.isCreator();
	}

	initSignFields() {
		const signParamsToDisplay = this.getSignParamsToDisplay();
		const pdfViewer = this.options.getPdfViewer();
		if (pdfViewer == null) {
			return;
		}
		for (let i = 0; i < signParamsToDisplay.length; i++) {
			let currentSignRequestParams = signParamsToDisplay[i];
			const isSignableField = this.isCurrentSignableParam(currentSignRequestParams) && this.options.isSignable();
			const signSpaceId = this.getSignSpaceId(currentSignRequestParams, i, isSignableField);
			let signSpaceDiv = $("#" + signSpaceId);

			if (this.options.isSignable() || this.options.isEditable()) {
				if(signSpaceDiv.length) {
					signSpaceDiv.off(this.options.signSpaceNamespace);
					try {
						if (signSpaceDiv.hasClass("ui-droppable")) {
							signSpaceDiv.droppable("destroy");
						}
					} catch (error) {
						// Ignore partially initialized droppable widgets.
					}
					signSpaceDiv.remove();
				}
				if (this.shouldHideSpotSignSpace(currentSignRequestParams)) {
					continue;
				}
				const spotId = this.options.findSpotIdForSignParams(currentSignRequestParams);
				const deleteBtnHtml = this.options.isManager() && spotId != null
					? "<button type='button' class='slot-delete-btn btn btn-sm btn-danger' title='Supprimer l’emplacement'><i class='fi fi-rr-trash'></i></button>"
					: "";
				let cssClasses = "sign-space";
				if (isSignableField) {
					cssClasses += " sign-field";
					if (currentSignRequestParams.ready) {
						cssClasses += " sign-field-dropped";
					}
				}
				const rawStepNumber = currentSignRequestParams?.stepNumber;
				const resolvedStepNumber = Number.isFinite(parseInt(rawStepNumber, 10))
					? parseInt(rawStepNumber, 10)
					: parseInt(this.options.getCurrentStepNumber(), 10);
				const currentStep = parseInt(this.options.getCurrentStepNumber(), 10);

				if (!this.canViewAllSpots()
					&& !isSignableField
					&& spotId != null
					&& Number.isFinite(currentStep)
					&& Number.isFinite(resolvedStepNumber)
					&& resolvedStepNumber < currentStep) {
					continue;
				}

				let signSpaceHtml = "<div id='" + signSpaceId + "' title='Emplacement de signature : " + (currentSignRequestParams.comment || "") + "' class='" + cssClasses + "' data-es-spot-id='" + (spotId == null ? "" : spotId) + "' data-es-step-number='" + (Number.isFinite(resolvedStepNumber) ? resolvedStepNumber : "") + "' data-es-pos-page='" + currentSignRequestParams.signPageNumber + "' data-es-pos-x='" + currentSignRequestParams.xPos + "' data-es-sign-name='" + (currentSignRequestParams.pdSignatureFieldName || "") + "' data-es-pos-y='" + currentSignRequestParams.yPos + "' data-es-sign-width='" + currentSignRequestParams.signWidth + "' data-es-sign-height='" + currentSignRequestParams.signHeight + "'>" + deleteBtnHtml + "</div>";
				$("#pdf").append(signSpaceHtml);
				signSpaceDiv = $("#" + signSpaceId);

				if (isSignableField) {
					signSpaceDiv.off("click" + this.options.signSpaceNamespace).on("click" + this.options.signSpaceNamespace, () => this.options.requestAddSign(i));
					this.renderSignSpaceContent(signSpaceDiv, {
						isSignableField: true,
						ready: currentSignRequestParams.ready === true
					});
					this.makeItDroppable(signSpaceDiv);
				} else {
					const stepNumberFromDom = parseInt(signSpaceDiv.attr("data-es-step-number"), 10);
					this.renderSignSpaceContent(signSpaceDiv, {
						isSignableField: false,
						stepNumber: stepNumberFromDom
					});
					if (Number.isFinite(stepNumberFromDom)) {
						signSpaceDiv.off("mouseenter" + this.options.signSpaceNamespace).on("mouseenter" + this.options.signSpaceNamespace, () => this.highlightLiveStep(stepNumberFromDom));
						signSpaceDiv.off("mouseleave" + this.options.signSpaceNamespace).on("mouseleave" + this.options.signSpaceNamespace, () => this.resetLiveStepHighlight());
					}
				}

				if (this.options.isManager()) {
					this.options.bindSignSpaceDelete(signSpaceDiv);
				}
				signSpaceDiv.show();
				const pageTop = pdfViewer.getPageTopInPdf(currentSignRequestParams.signPageNumber);
				const pageLeft = pdfViewer.getPageLeftInPdf(currentSignRequestParams.signPageNumber);
				const xPos = Math.round(currentSignRequestParams.xPos * pdfViewer.scale + pageLeft);
				const yPos = Math.round(currentSignRequestParams.yPos * pdfViewer.scale + pageTop);
				signSpaceDiv.css("left", xPos);
				signSpaceDiv.css("top", yPos);
				const renderedWidth = Math.round(currentSignRequestParams.signWidth * pdfViewer.scale * this.options.getBrowserZoom());
				const renderedHeight = Math.round(currentSignRequestParams.signHeight * pdfViewer.scale * this.options.getBrowserZoom());
				signSpaceDiv.css("width", renderedWidth + "px");
				signSpaceDiv.css("height", renderedHeight + "px");
				this.updateSignSpaceFontSize(signSpaceDiv, renderedHeight);
			}
		}
	}

	renderSignSpaceContent(signSpaceDiv, { isSignableField = false, ready = false, stepNumber = null } = {}) {
		signSpaceDiv.children(".sign-content").remove();
		this.updateSignSpaceDeleteButtonVisibility(signSpaceDiv, {
			isSignableField,
			ready
		});
		if (isSignableField) {
			if (!ready) {
				signSpaceDiv.append("<div class='sign-content'><span class='sign-icon fi fi-rr-add'></span><span class='sign-text text-uppercase'>Votre signature ici</span></div>");
			}
		} else {
			const parsedStepNumber = parseInt(stepNumber, 10);
			const stepLabel = Number.isFinite(parsedStepNumber) ? " étape " + parsedStepNumber : "";
			signSpaceDiv.append("<div class='sign-content'><span class='sign-text text-uppercase'>Emplacement de signature" + stepLabel + "</span></div>");
		}
		this.updateSignSpaceFontSize(signSpaceDiv);
	}

	updateSignSpaceDeleteButtonVisibility(signSpaceDiv, { isSignableField = false, ready = false } = {}) {
		const deleteBtn = signSpaceDiv.children(".slot-delete-btn");
		if (!deleteBtn.length) {
			return;
		}
		if (isSignableField) {
			deleteBtn.toggle(!ready);
			return;
		}
		deleteBtn.show();
	}

	updateSignSpaceFontSize(signSpaceDiv, renderedHeight = null) {
		const effectiveHeight = Number.isFinite(renderedHeight)
			? renderedHeight
			: parseInt(signSpaceDiv.css("height"), 10);
		if (!Number.isFinite(effectiveHeight)) {
			return;
		}
		signSpaceDiv.css("font-size", Math.round(effectiveHeight * 0.15) + "px");
		signSpaceDiv.find(".sign-icon").css("font-size", Math.round(effectiveHeight * 0.45) + "px");
	}

	getSignSpaceId(signParams, index, isSignableField) {
		if (isSignableField) {
			return "signSpace_" + index;
		}
		const spotId = this.options.findSpotIdForSignParams(signParams);
		if (spotId != null) {
			return "signSpace_spot_" + spotId;
		}
		return "signSpace_readonly_" + index;
	}

	isCurrentSignableParam(signParams) {
		const currentParams = Array.isArray(this.options.getCurrentSignRequestParamses()) ? this.options.getCurrentSignRequestParamses() : [];
		for (let i = 0; i < currentParams.length; i++) {
			const current = currentParams[i];
			if (current?.id != null && signParams?.id != null && parseInt(current.id, 10) === parseInt(signParams.id, 10)) {
				return true;
			}
			const sameGeo = parseInt(current?.signPageNumber, 10) === parseInt(signParams?.signPageNumber, 10)
				&& parseInt(current?.xPos, 10) === parseInt(signParams?.xPos, 10)
				&& parseInt(current?.yPos, 10) === parseInt(signParams?.yPos, 10)
				&& parseInt(current?.signWidth, 10) === parseInt(signParams?.signWidth, 10)
				&& parseInt(current?.signHeight, 10) === parseInt(signParams?.signHeight, 10);
			if (sameGeo) {
				return true;
			}
		}
		return false;
	}

	getSignParamsToDisplay() {
		const currentParams = Array.isArray(this.options.getCurrentSignRequestParamses()) ? this.options.getCurrentSignRequestParamses() : [];
		const spots = Array.isArray(this.options.getSpots()) ? this.options.getSpots() : [];

		if (this.options.isEditable() && this.canViewAllSpots()) {
			const managerSpots = this.options.isCreator()
				? spots
				: this.options.filterSpotsNotCurrentStep(spots);
			const merged = [...currentParams, ...managerSpots];
			const byKey = new Map();
			for (let i = 0; i < merged.length; i++) {
				const item = merged[i];
				const idKey = item != null && item.id != null ? "id:" + item.id : null;
				const geoKey = "geo:" + [item?.signPageNumber, item?.xPos, item?.yPos, item?.signWidth, item?.signHeight].join("|");
				const key = idKey || geoKey;
				if (!byKey.has(key)) {
					byKey.set(key, item);
				}
			}
			return Array.from(byKey.values());
		}

		if (this.options.isEditable() && this.options.isManager()) {
			return spots;
		}

		return currentParams;
	}

	refreshSignFields() {
		const pdfViewer = this.options.getPdfViewer();
		if (pdfViewer == null) {
			return;
		}
		$(".sign-space").each((_, element) => {
			const signSpaceDiv = $(element);
			const signHeight = parseFloat(signSpaceDiv.attr("data-es-sign-height"));
			const renderedHeight = Math.round(signHeight * pdfViewer.scale);
			const renderedWidth = Math.round(parseFloat(signSpaceDiv.attr("data-es-sign-width")) * pdfViewer.scale);
			signSpaceDiv.css("width", renderedWidth + "px");
			signSpaceDiv.css("height", renderedHeight + "px");
			const pageNum = parseInt(signSpaceDiv.attr("data-es-pos-page"), 10);
			const pageTop = pdfViewer.getPageTopInPdf(pageNum);
			const pageLeft = pdfViewer.getPageLeftInPdf(pageNum);
			signSpaceDiv.css("left", signSpaceDiv.attr("data-es-pos-x") * pdfViewer.scale + pageLeft + 'px');
			signSpaceDiv.css("top", signSpaceDiv.attr("data-es-pos-y") * pdfViewer.scale + pageTop + 'px');
			this.updateSignSpaceFontSize(signSpaceDiv, renderedHeight);
		});
	}

	findPlacedSignRequestParams(draggableId) {
		const signPlacementController = this.options.getSignPlacementController();
		if (signPlacementController == null || signPlacementController.signRequestParamses == null) {
			return null;
		}
		const allParams = Array.from(signPlacementController.signRequestParamses.values());
		for (let i = 0; i < allParams.length; i++) {
			const signRequestParams = allParams[i];
			const cross = signRequestParams?.cross;
			if (cross != null && cross.attr("id") === draggableId) {
				return signRequestParams;
			}
		}
		return null;
	}

	applySignRequestParamsToSignSpace(signSpaceDiv, signRequestParams) {
		const signPlacementController = this.options.getSignPlacementController();
		const pdfViewer = this.options.getPdfViewer();
		if (signPlacementController == null || pdfViewer == null || signSpaceDiv == null || !signSpaceDiv.length || signRequestParams == null) {
			return false;
		}
		const cross = signRequestParams.cross;
		if (cross == null || !cross.length) {
			return false;
		}
		signSpaceDiv.data("locked", cross.attr("id"));
		signSpaceDiv.removeClass("sign-field");
		signSpaceDiv.addClass("sign-field-dropped");
		signSpaceDiv.css("pointer-events", "none");
		this.renderSignSpaceContent(signSpaceDiv, {
			isSignableField: true,
			ready: true
		});

		signRequestParams.signSpace = signSpaceDiv;
		const pageNum = parseInt(signSpaceDiv.attr("data-es-pos-page"), 10);
		const targetX = parseInt(signSpaceDiv.attr("data-es-pos-x"), 10);
		const targetY = parseInt(signSpaceDiv.attr("data-es-pos-y"), 10);
		signRequestParams.signPageNumber = pageNum;
		signRequestParams.xPos = Number.isFinite(targetX) ? targetX : 0;
		signRequestParams.yPos = Number.isFinite(targetY) ? targetY : 0;
		cross.attr("page", pageNum);
		signRequestParams.firstLaunch = false;
		signRequestParams.applyCurrentSignRequestParams();

		let resizedUi = { size: { width: 0, height: 0 }};
		let width = parseInt(cross.css("width"), 10);
		let height = parseInt(cross.css("height"), 10);
		let maxWidth  = parseInt(signSpaceDiv.css("width"), 10);
		let maxHeight = parseInt(signSpaceDiv.css("height"), 10);
		if (!Number.isFinite(width) || width <= 0) {
			width = Math.round(signRequestParams.signWidth * pdfViewer.scale);
		}
		if (!Number.isFinite(height) || height <= 0) {
			height = Math.round(signRequestParams.signHeight * pdfViewer.scale);
		}
		if (!Number.isFinite(maxWidth) || maxWidth <= 0 || !Number.isFinite(maxHeight) || maxHeight <= 0) {
			return false;
		}
		let ratio = width / height;
		resizedUi.size.width  = maxWidth;
		resizedUi.size.height = resizedUi.size.width / ratio;
		if (resizedUi.size.height > maxHeight) {
			resizedUi.size.height = maxHeight;
			resizedUi.size.width  = resizedUi.size.height * ratio;
		}
		resizedUi.size.width = resizedUi.size.width - 2;
		resizedUi.size.height = resizedUi.size.height - 2;
		signRequestParams.resize(resizedUi);
		cross.css("width", signRequestParams.signWidth * pdfViewer.scale);
		cross.css("background-size", signRequestParams.signWidth * pdfViewer.scale);
		cross.css("height", signRequestParams.signHeight * pdfViewer.scale);
		signRequestParams.dropped = true;

		const slotIndex = parseInt(signSpaceDiv.attr("id")?.split("_")[1], 10);
		if (Number.isFinite(slotIndex) && signPlacementController.currentSignRequestParamses?.[slotIndex] != null) {
			signPlacementController.currentSignRequestParamses[slotIndex].ready = true;
		}
		signRequestParams.ready = true;
		if (typeof signRequestParams.refreshVisualState === "function") {
			signRequestParams.refreshVisualState();
		}
		if (typeof signPlacementController.refreshSteps === "function") {
			signPlacementController.refreshSteps();
		}
		return true;
	}

	placeSignOnSlot(slotIndex, signRequestParams) {
		const signSpaceDiv = $("#signSpace_" + slotIndex);
		if (!signSpaceDiv.length) {
			return false;
		}
		return this.applySignRequestParamsToSignSpace(signSpaceDiv, signRequestParams);
	}

	makeItDroppable(signSpaceDiv) {
		const signPlacementController = this.options.getSignPlacementController();
		const pdfViewer = this.options.getPdfViewer();
		if (signPlacementController == null || pdfViewer == null) {
			return;
		}
		const manager = this;
		signSpaceDiv.droppable({
			tolerance: "touch",
			hoverClass: "ui-droppable-hover",
			accept: ".drop-sign",
			drop: function (event, ui) {
				if ($(this).data("locked") != null) {
					return;
				}
				const signRequestParams = manager.findPlacedSignRequestParams(ui.draggable.attr("id"));
				if (signRequestParams != null) {
					manager.applySignRequestParamsToSignSpace($(this), signRequestParams);
				}
			},
			out: function (event, ui) {
				if ($(this).data("locked") != null && $(this).data("locked") !== ui.draggable.attr("id")) {
					return;
				}
				$(this).removeData("locked");
				$(this).removeClass("sign-space-disabled ui-state-disabled");
				$(this).addClass("sign-field");
				$(this).removeClass("sign-field-dropped");
				const slotIndex = parseInt($(this).attr("id")?.split("_")[1], 10);
				if (Number.isFinite(slotIndex) && signPlacementController.currentSignRequestParamses?.[slotIndex] != null) {
					signPlacementController.currentSignRequestParamses[slotIndex].ready = false;
				}
				manager.renderSignSpaceContent($(this), {
					isSignableField: true,
					ready: false
				});
				$(this).css("pointer-events", "auto");
				if ($(this).hasClass("ui-droppable")) {
					try {
						$(this).droppable("enable");
					} catch (error) {
						// Ignore partially initialized droppable widgets.
					}
				}
				for (let i = 0; i < signPlacementController.signRequestParamses.size; i++) {
					let signRequestParams = Array.from(signPlacementController.signRequestParamses.values())[i];
					let cross = signRequestParams.cross;
					if (cross.attr("id") === ui.draggable.attr("id")) {
						if (cross.data("ui-resizable") || cross.data("resizable")) {
							cross.resizable("enable");
						}
						signRequestParams.signSpace = null;
						signRequestParams.ready = false;
						signRequestParams.dropped = false;
						if (typeof signRequestParams.refreshVisualState === "function") {
							signRequestParams.refreshVisualState();
						}
						if (typeof signPlacementController.refreshSteps === "function") {
							signPlacementController.refreshSteps();
						}
					}
				}
			}
		});
	}

	setSignSpacesDroppableEnabled(enabled) {
		$(".sign-space").each((_, element) => {
			const signSpace = $(element);
			if (signSpace.hasClass("ui-droppable")) {
				try {
					signSpace.droppable(enabled ? "enable" : "disable");
				} catch (error) {
					// Ignore non-initialized droppable elements.
				}
			}
			signSpace.toggleClass("sign-space-disabled", !enabled);
		});
	}

	highlightLiveStep(stepNumber) {
		const parsedStepNumber = parseInt(stepNumber, 10);
		if (!Number.isFinite(parsedStepNumber)) {
			return;
		}
		this.resetLiveStepHighlight();
		const liveStep = $("#liveStep-" + parsedStepNumber);
		if (liveStep.length) {
			liveStep.find(".step-vertical-content").addClass("step-vertical-content-hover");
		}
	}

	resetLiveStepHighlight() {
		$("[id^='liveStep-']").find(".step-vertical-content").removeClass("step-vertical-content-hover");
	}

	destroy() {
		$(".sign-space").each((_, element) => {
			const signSpace = $(element);
			signSpace.off(this.options.signSpaceNamespace);
			try {
				if (signSpace.hasClass("ui-droppable")) {
					signSpace.droppable("destroy");
				}
			} catch (error) {
				// Ignore partially initialized droppable widgets.
			}
		});
	}

	isSpotParamAlreadyUsed(signParams) {
		const spotId = this.options.findSpotIdForSignParams(signParams);
		return spotId != null && signParams?.ready === true;
	}

	isSpotFromPreviousStep(signParams) {
		const spotId = this.options.findSpotIdForSignParams(signParams);
		if (spotId == null) {
			return false;
		}
		const spotStep = parseInt(signParams?.stepNumber, 10);
		const currentStep = parseInt(this.options.getCurrentStepNumber(), 10);
		return Number.isFinite(spotStep)
			&& Number.isFinite(currentStep)
			&& spotStep < currentStep;
	}

	shouldHideSpotSignSpace(signParams) {
		return this.isSpotParamAlreadyUsed(signParams) || this.isSpotFromPreviousStep(signParams);
	}
}
