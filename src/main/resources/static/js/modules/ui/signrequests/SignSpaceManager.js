export class SignSpaceManager {

	constructor(state, options = {}) {
		this.state = state;
		this.options = {
			signSpaceNamespace: options.signSpaceNamespace ?? ".signSpaceManager",
			getPdfViewer: options.getPdfViewer ?? (() => null),
			getSignPosition: options.getSignPosition ?? (() => null),
			getCurrentSignRequestParamses: options.getCurrentSignRequestParamses ?? (() => []),
			getSpots: options.getSpots ?? (() => []),
			getCurrentStepNumber: options.getCurrentStepNumber ?? (() => null),
			isSignable: options.isSignable ?? (() => false),
			isEditable: options.isEditable ?? (() => false),
			isManager: options.isManager ?? (() => false),
			getBrowserZoom: options.getBrowserZoom ?? (() => 1),
			requestAddSign: options.requestAddSign ?? (() => {}),
			findSpotIdForSignParams: options.findSpotIdForSignParams ?? (() => null),
			filterSpotsNotCurrentStep: options.filterSpotsNotCurrentStep ?? (spots => spots),
			bindSignSpaceDelete: options.bindSignSpaceDelete ?? (() => {})
		};
		this.hoverLiveStepState = null;
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
				const spotId = this.options.findSpotIdForSignParams(currentSignRequestParams);
				const deleteBtnHtml = (this.options.isSignable() && this.options.isEditable()) && spotId != null
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

				if (!isSignableField
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
					if(currentSignRequestParams.ready == null || !currentSignRequestParams.ready) {
						signSpaceDiv.append("<div class='sign-content'><span class='sign-icon fi fi-rr-add'></span><span class='sign-text text-uppercase'>Votre signature ici</span></div>");
					}
					this.makeItDroppable(signSpaceDiv);
				} else {
					const stepNumberFromDom = parseInt(signSpaceDiv.attr("data-es-step-number"), 10);
					const stepLabel = Number.isFinite(stepNumberFromDom) ? " étape " + stepNumberFromDom : "";
					signSpaceDiv.append("<div class='sign-content'><span class='sign-text text-uppercase'>Emplacement de signature" + stepLabel + "</span></div>");
					if (Number.isFinite(stepNumberFromDom)) {
						signSpaceDiv.off("mouseenter" + this.options.signSpaceNamespace).on("mouseenter" + this.options.signSpaceNamespace, () => this.highlightLiveStep(stepNumberFromDom));
						signSpaceDiv.off("mouseleave" + this.options.signSpaceNamespace).on("mouseleave" + this.options.signSpaceNamespace, () => this.resetLiveStepHighlight());
					}
				}

				if (this.options.isEditable()) {
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
				signSpaceDiv.css("font-size", Math.round(renderedHeight * 0.15) + "px");
				signSpaceDiv.find(".sign-icon").css("font-size", Math.round(renderedHeight * 0.45) + "px");
			}
		}
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

		if (this.options.isEditable() && this.options.isManager() && this.options.isSignable()) {
			const otherStepSpots = this.options.filterSpotsNotCurrentStep(spots);
			const merged = [...currentParams, ...otherStepSpots];
			const currentStep = parseInt(this.options.getCurrentStepNumber(), 10);
			const filteredMerged = merged.filter(item => {
				if (!Number.isFinite(currentStep)) {
					return true;
				}
				const step = parseInt(item?.stepNumber, 10);
				if (!Number.isFinite(step)) {
					return true;
				}
				return step >= currentStep;
			});
			const byKey = new Map();
			for (let i = 0; i < filteredMerged.length; i++) {
				const item = filteredMerged[i];
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
			signSpaceDiv.css("font-size", Math.round(renderedHeight * 0.15) + "px");
			signSpaceDiv.find(".sign-icon").css("font-size", Math.round(renderedHeight * 0.45) + "px");
		});
	}

	makeItDroppable(signSpaceDiv) {
		const signPosition = this.options.getSignPosition();
		const pdfViewer = this.options.getPdfViewer();
		if (signPosition == null || pdfViewer == null) {
			return;
		}
		signSpaceDiv.droppable({
			tolerance: "touch",
			hoverClass: "drop-hover",
			accept: ".drop-sign",
			drop: function (event, ui) {
				if ($(this).data("locked") != null) {
					return;
				}
				$(this).data("locked", ui.draggable.attr("id"));
				$(this).removeClass("sign-field");
				$(this).addClass("sign-field-dropped");
				$(this).css("pointer-events", "none");
				$(this).text("");
				for (let i = 0; i < signPosition.signRequestParamses.size; i++) {
					let signRequestParams = Array.from(signPosition.signRequestParamses.values())[i];
					let cross = signRequestParams.cross;
					if (cross.attr("id") === ui.draggable.attr("id")) {
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
						let width = parseInt(cross.css("width"));
						let height = parseInt(cross.css("height"));
						let maxWidth  = parseInt(signSpaceDiv.css("width"));
						let maxHeight = parseInt(signSpaceDiv.css("height"));
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
						signPosition.goStep2();
						console.log("real place : " + signRequestParams.xPos +", " + signRequestParams.yPos);
					}
				}
				signPosition.currentSignRequestParamses[$(this).attr("id").split("_")[1]].ready = true;
			},
			out: function (event, ui) {
				if ($(this).data("locked") != null && $(this).data("locked") !== ui.draggable.attr("id")) {
					return;
				}
				$(this).removeData("locked");
				$(this).addClass("sign-field");
				$(this).removeClass("sign-field-dropped");
				signPosition.currentSignRequestParamses[$(this).attr("id").split("_")[1]].ready = false;
				$(this).html("<div class='sign-content'><span class='sign-icon fi fi-rr-add'></span><span class='sign-text text-uppercase'>Placer la signature ici</span></div>");
				$(this).css("pointer-events", "auto");
				for (let i = 0; i < signPosition.signRequestParamses.size; i++) {
					let signRequestParams = Array.from(signPosition.signRequestParamses.values())[i];
					let cross = signRequestParams.cross;
					if (cross.attr("id") === ui.draggable.attr("id")) {
						cross.resizable("enable");
						signRequestParams.signSpace = null;
						signPosition.goStep1();
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
		if (this.hoverLiveStepState == null) {
			this.hoverLiveStepState = [];
			$("[id^='liveStep-'].bg-success").each((_, element) => {
				const id = $(element).attr("id");
				if (id) {
					this.hoverLiveStepState.push(id);
				}
			});
		}
		const liveStep = $("#liveStep-" + parsedStepNumber);
		if (liveStep.length) {
			liveStep.find(".step-vertical-content").toggleClass("bg-light bg-secondary-subtle");
		}
	}

	resetLiveStepHighlight() {
		$("[id^='liveStep-']").find(".step-vertical-content").removeClass("bg-secondary-subtle");
		$("[id^='liveStep-']").find(".step-vertical-content").addClass("bg-light");
		this.hoverLiveStepState = null;
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

}



