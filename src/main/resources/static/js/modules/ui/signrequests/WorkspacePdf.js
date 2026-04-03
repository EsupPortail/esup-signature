import {PdfViewer} from "../../utils/PdfViewer.js?version=@version@";
import {SignPosition} from "./SignPosition.js?version=@version@";
import {WheelDetector} from "../../utils/WheelDetector.js?version=@version@";

export class WorkspacePdf {

    constructor(isPdf, id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, comments, spots, currentStepNumber, currentStepMultiSign, currentStepSingleSignWithAnnotation, workflow, signImages, userName, authUserName, fields, stepRepeatable, status, csrf, action, notSigned, attachmentAlert, attachmentRequire, isOtp, restore, phone, isManager) {
        console.info("Starting workspace UI");
        this.ready = false;
        this.formInitialized = false;
        this.isPdf = isPdf;
        this.isOtp = isOtp;
        this.phone = phone;
        this.changeModeSelector = null;
        this.displayComments = false;
        this.action = action;
        this.dataId = dataId;
        this.formId = formId;
        this.userName = userName;
        this.workflow = workflow;
        this.signImageNumber = signImageNumber;
        this.restore = restore;
        this.comments = comments;
        this.spots = spots;
        this.notSigned = notSigned;
        this.signable = signable;
        this.editable = editable;
        this.isManager = isManager;
        this.signRequestId = id;
        this.currentSignType = currentSignType;
        this.currentStepNumber = currentStepNumber;
        this.stepRepeatable = stepRepeatable;
        this.status = status;
        this.csrf = csrf;
        this.currentStepMultiSign = currentStepMultiSign;
        this.forcePageNum = null;
        this.pointItEnable = true;
        this.first = true;
        this.actionInitialyzed = false;
        this.saveAlert = false;
        this.scrollTop = 0;
        this.nextCommand = "none";
        this.hoverLiveStepState = null;
        if(fields != null) {
            for (let i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.workflowSteps != null && field.workflowSteps.includes(currentStepNumber) && field.required) {
                    this.forcePageNum = field.page;
                    break;
                }
            }
        }
        if (this.isPdf) {
            if(currentSignType === "form") {
                this.pdfViewer = new PdfViewer('/' + userName + '/forms/get-file/' + id, signable, editable, currentStepNumber, this.forcePageNum, fields, true);
            } else {
                this.pdfViewer = new PdfViewer('/ws-secure/global/get-last-file-pdf/' + id, signable, editable, currentStepNumber, this.forcePageNum, fields, false);
            }
        }
        this.signPosition = new SignPosition(
            currentSignType,
            currentSignRequestParamses,
            currentStepMultiSign,
            currentStepSingleSignWithAnnotation,
            signImageNumber,
            signImages,
            userName, authUserName, signable, this.forcePageNum, this.isOtp, this.phone, this.csrf);
        this.currentSignRequestParamses = currentSignRequestParamses;
        // Mode system deprecated: UI is now driven by rights (signable/editable).
        this.wheelDetector = new WheelDetector();
        this.addSpotEnabled = false;
        this.addCommentEnabled = false;
        this.nextCommand = "none";
        this.initChangeModeSelector();
        this.initDataFields(fields);
        this.wsTabs = $("#ws-tabs");
        this.navWidth = this.wsTabs.innerWidth();
        this.addSignButton = $("#addSignButton");
        this.lastWidth = window.innerWidth;
        this.lastHeight = window.innerHeight;
        if (currentSignType === "form" || (formId == null && !workflow) || currentSignRequestParamses.length === 0) {
            if(this.wsTabs.length) {
                this.autocollapse();
                let self = this;
                const THRESHOLD = 100;
                const DEBOUNCE_DELAY = 100;
                let resizeTimer = null;
                $(window).on("resize", () => {
                    clearTimeout(resizeTimer);
                    resizeTimer = setTimeout(() => {
                        const w = window.innerWidth;
                        const h = window.innerHeight;
                        const deltaW = Math.abs(w - self.lastWidth);
                        const deltaH = Math.abs(h - self.lastHeight);
                        if (w === self.lastWidth || (deltaW < THRESHOLD && deltaH < THRESHOLD)) return;
                        self.autocollapse();
                        self.lastWidth = w;
                        self.lastHeight = h;
                    }, DEBOUNCE_DELAY);
                });
            }
        }
        let root = document.querySelector(':root');
        root.setAttribute("style", "scroll-behavior: auto;");
        this.initListeners();
        if (this.isPdf) {
            this.signPosition.updateScales(this.pdfViewer.scale);
        } else {
            this.initWorkspace();
        }
    }

    initListeners() {
        if (this.isPdf) {
            $('#prev').on('click', e => this.pdfViewer.prevPage());
            $('#next').on('click', e => this.pdfViewer.nextPage());
            $('#end-button').on('click', e => this.pdfViewer.nextPage());
            $('#addCommentButton').on('click', e => this.enableCommentAdd(e));
            $('#addSpotButton').on('click', e => this.enableSpotAdd());
            $('#addCommentButton2').on('click', e => this.enableCommentAdd(e));
            $('#addSpotButton2').on('click', e => this.enableSpotAdd());
            $("#spotStepNumber").on('change', e => this.changeSpotStep());
            $("#showComments").on('click', e => this.showComments());
            $("#hideComments").on("click", () => {
                if (this.displayComments === true) {
                    this.hideComments();
                } else {
                    this.showComments();
                }
            });
            // this.signPosition.addEventListener("startDrag", e => this.hideAllPostits());
            // this.signPosition.addEventListener("stopDrag", e => this.showAllPostits());
            this.pdfViewer.addEventListener('renderFinished', e => this.initWorkspacePdf());
            if(this.currentSignType !== "form") {
                this.pdfViewer.addEventListener('reachEnd', e => this.markAsViewed());
            }
            this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
            if(this.isPdf) {
                this.pdfViewer.addEventListener('change', e => this.saveData(localStorage.getItem('disableFormAlert') === "true"));
            }
            $(".postit-global-close").on('click', function () {
                if($(this).parent().hasClass("postit-small")) {
                    $(this).parent().resizable("enable");
                } else {
                    $(this).parent().resizable("disable");
                }
                $(this).parent().toggleClass("postit-small");
                const buttons = $(this).parent().find('button');
                buttons.each(function() {
                    if(!$(this).hasClass("postit-global-close")) {
                        $(this).toggle();
                    }
                });
            });

            $(".postit-copy").on("click", async function (e) {
                let snackbar = document.getElementById("snackbar");
                snackbar.className = "show";
                let text = $("#postit-text-" + $(e.target).attr("es-postit-id")).text();
                try {
                    if (navigator.clipboard && navigator.clipboard.writeText) {
                        await navigator.clipboard.writeText(text);
                        snackbar.innerText = "Texte copié dans le presse-papier";
                    } else {
                        throw new Error("Accès au presse-papier non supporté");
                    }
                } catch (err) {
                    console.warn("Erreur clipboard, utilisation du fallback :", err);

                    let tempTextarea = document.createElement("textarea");
                    tempTextarea.value = text;
                    document.body.appendChild(tempTextarea);
                    tempTextarea.style.position = "absolute";
                    tempTextarea.style.left = "-9999px";
                    tempTextarea.select();
                    tempTextarea.setSelectionRange(0, 99999); // Support mobile

                    try {
                        let success = document.execCommand("copy");
                        snackbar.innerText = success ? "Texte copié..." : "Échec de la copie";
                    } catch (error) {
                        console.error("Impossible de copier le texte :", error);
                        snackbar.innerText = "Erreur : Impossible de copier le texte";
                    }

                    document.body.removeChild(tempTextarea);
                }

                setTimeout(() => {
                    snackbar.className = snackbar.className.replace("show", "");
                }, 3000);
            });

            if(this.comments != null) {
                this.comments.forEach((postit, index) => {
                    let postitButton = $('#postit' + postit.id);
                    postitButton.on('click', e => this.focusComment(postit));
                    postitButton.on('mouseover', function () {
                        $('#inDocComment_' + postit.id).addClass('circle-background');
                        postitButton.addClass('circle-border');
                    });
                    postitButton.on('mouseout', function () {
                        $('#inDocComment_' + postit.id).removeClass('circle-background');
                        postitButton.removeClass('circle-border');
                    });
                });
            }
        }
        this.addSignButton.on('click', e => this.addSign());
        $("#addSignButton2").on('click', e => this.addSign());
        $("#addSignButton3").on('click', e => this.addSign());
        $("#addParaphButton").on('click', e => this.addParaph());
        $("#addParaphButton2").on('click', e => this.addParaph());
        $("#addCheck").on("click", e => this.signPosition.addCheckImage(this.pdfViewer.pageNum));
        $("#addTimes").on("click", e => this.signPosition.addTimesImage(this.pdfViewer.pageNum));
        $("#addCircle").on("click", e => this.signPosition.addCircleImage(this.pdfViewer.pageNum));
        $("#addMinus").on("click", e => this.signPosition.addMinusImage(this.pdfViewer.pageNum));
        $("#addText").on("click ", e => this.signPosition.addText(this.pdfViewer.pageNum));

        let signImageBtn = $("#signImageBtn");
        signImageBtn.unbind();
        let self = this;
        signImageBtn.on('click', function () {
            self.signPosition.popUserUi();
        });
        this.notviewedAnim();
    }

    notviewedAnim() {
        let div = document.querySelector('.jumping');
        if(div == null) return;
        let isPaused = false;
        function togglePause() {
            isPaused = !isPaused;
            if (!isPaused) {
                div.style.animationPlayState = 'running';
            } else {
                div.style.animationPlayState = 'paused';
                setTimeout(togglePause, 3000);
            }
        }

        div.addEventListener('animationiteration', () => {
            togglePause();
        });
    }

    markAsViewed() {
        $.ajax({
            url: "/ws-secure/global/viewed/" + this.signRequestId + "?" + this.csrf.parameterName + "=" + this.csrf.token,
            type: 'POST',
            success: function () {
                $(".not-viewed").remove();
            }
        });
    }

    initSignFields() {
        const signParamsToDisplay = this.getSignParamsToDisplay();
        for (let i = 0; i < signParamsToDisplay.length; i++) {
            let currentSignRequestParams = signParamsToDisplay[i];
            const isSignableField = this.isCurrentSignableParam(currentSignRequestParams) && this.signable;
            const signSpaceId = this.getSignSpaceId(currentSignRequestParams, i, isSignableField);
            let signSpaceDiv = $("#" + signSpaceId);

            if (this.signable || this.editable) {
                if(signSpaceDiv.length) {
                    signSpaceDiv.unbind();
                    signSpaceDiv.remove();
                }
                const spotId = this.findSpotIdForSignParams(currentSignRequestParams);
                const deleteBtnHtml = (this.editable && this.isManager) && spotId != null
                    ? "<button type='button' class='slot-delete-btn btn btn-sm btn-danger' title='Supprimer l’emplacement'><i class='fi fi-rr-trash'></i></button>"
                    : "";
                // Style: sign-field uniquement pour les emplacements réellement signables.
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
                    : parseInt(this.currentStepNumber, 10);

                let signSpaceHtml = "<div id='" + signSpaceId + "' title='Emplacement de signature : " + (currentSignRequestParams.comment || "") + "' class='" + cssClasses + "' data-es-spot-id='" + (spotId == null ? "" : spotId) + "' data-es-step-number='" + (Number.isFinite(resolvedStepNumber) ? resolvedStepNumber : "") + "' data-es-pos-page='" + currentSignRequestParams.signPageNumber + "' data-es-pos-x='" + currentSignRequestParams.xPos + "' data-es-sign-name='" + (currentSignRequestParams.pdSignatureFieldName || "") + "' data-es-pos-y='" + currentSignRequestParams.yPos + "' data-es-sign-width='" + currentSignRequestParams.signWidth + "' data-es-sign-height='" + currentSignRequestParams.signHeight + "'>" + deleteBtnHtml + "</div>";
                $("#pdf").append(signSpaceHtml);
                signSpaceDiv = $("#" + signSpaceId);

                if (isSignableField) {
                    signSpaceDiv.on("click", e => this.addSign(i));
                    if(currentSignRequestParams.ready == null || !currentSignRequestParams.ready) {
                        signSpaceDiv.append("<div class='sign-content'><span class='sign-icon fi fi-rr-add'></span><span class='sign-text text-uppercase'>Votre signature ici</span></div>");
                    }
                    this.makeItDroppable(signSpaceDiv);
                } else {
                    signSpaceDiv.append("<div class='sign-content'><span class='sign-text text-uppercase'>Emplacement de signature</span></div>");
                    const stepNumberFromDom = parseInt(signSpaceDiv.attr("data-es-step-number"), 10);
                    if (Number.isFinite(stepNumberFromDom)) {
                        signSpaceDiv.on("mouseenter", () => this.highlightLiveStep(stepNumberFromDom));
                        signSpaceDiv.on("mouseleave", () => this.resetLiveStepHighlight());
                    }
                }

                if (this.editable) {
                    this.bindSignSpaceDelete(signSpaceDiv);
                }
                signSpaceDiv.show();
                const pageTop = this.pdfViewer.getPageTopInPdf(currentSignRequestParams.signPageNumber);
                const pageLeft = this.pdfViewer.getPageLeftInPdf(currentSignRequestParams.signPageNumber);
                const xPos = Math.round(currentSignRequestParams.xPos * this.pdfViewer.scale + pageLeft);
                const yPos = Math.round(currentSignRequestParams.yPos * this.pdfViewer.scale + pageTop);
                signSpaceDiv.css("left", xPos);
                signSpaceDiv.css("top", yPos);
                const renderedWidth = Math.round(currentSignRequestParams.signWidth * this.pdfViewer.scale * this.getBrowserZoom());
                const renderedHeight = Math.round(currentSignRequestParams.signHeight * this.pdfViewer.scale * this.getBrowserZoom());
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
        const spotId = this.findSpotIdForSignParams(signParams);
        if (spotId != null) {
            return "signSpace_spot_" + spotId;
        }
        return "signSpace_readonly_" + index;
    }

    isCurrentSignableParam(signParams) {
        const currentParams = Array.isArray(this.currentSignRequestParamses) ? this.currentSignRequestParamses : [];
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
        const currentParams = Array.isArray(this.currentSignRequestParamses) ? this.currentSignRequestParamses : [];
        const spots = Array.isArray(this.spots) ? this.spots : [];

        // Cas manager editable + signable:
        // - currentSignRequestParamses = emplacements signables de l'etape courante
        // - spots = emplacements manager; on ajoute seulement les spots hors etape courante.
        if (this.editable && this.isManager && this.signable) {
            const otherStepSpots = this.filterSpotsNotCurrentStep(spots);
            const merged = [...currentParams, ...otherStepSpots];
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

        // Cas manager editable non-signable: afficher les spots manager (complet).
        if (this.editable && this.isManager) {
            return spots;
        }

        return currentParams;
    }

    filterSpotsNotCurrentStep(spots) {
        const currentStep = parseInt(this.currentStepNumber, 10);
        if (!Number.isFinite(currentStep)) {
            return spots;
        }
        return spots.filter(spot => {
            const step = parseInt(spot?.stepNumber, 10);
            if (!Number.isFinite(step)) {
                return true;
            }
            return step !== currentStep;
        });
    }

    findSpotIdForSignParams(signParams) {
        if (signParams != null && signParams.id != null && Number.isFinite(parseInt(signParams.id, 10))) {
            return parseInt(signParams.id, 10);
        }
        if (!Array.isArray(this.spots)) {
            return null;
        }
        const page = parseInt(signParams.signPageNumber, 10);
        const x = parseInt(signParams.xPos, 10);
        const y = parseInt(signParams.yPos, 10);
        const width = parseInt(signParams.signWidth, 10);
        const height = parseInt(signParams.signHeight, 10);
        for (let i = 0; i < this.spots.length; i++) {
            const spot = this.spots[i];
            const samePosition = parseInt(spot.signPageNumber, 10) === page && parseInt(spot.xPos, 10) === x && parseInt(spot.yPos, 10) === y;
            const sameSize = !Number.isFinite(width) || !Number.isFinite(height)
                || (parseInt(spot.signWidth, 10) === width && parseInt(spot.signHeight, 10) === height);
            if (samePosition && sameSize) {
                return spot.id;
            }
        }
        return null;
    }

    bindSignSpaceDelete(signSpaceDiv) {
        const spotId = parseInt(signSpaceDiv.attr("data-es-spot-id"), 10);
        const deleteBtn = signSpaceDiv.find(".slot-delete-btn");
        if (!Number.isFinite(spotId) || !deleteBtn.length) {
            return;
        }
        deleteBtn.on("click", e => {
            e.stopPropagation();
            bootbox.confirm("Supprimer cet emplacement de signature ?", result => {
                if (!result) {
                    return;
                }
                let url = "/ws-secure/global/delete-spot/" + this.signRequestId + "/" + spotId + "?" + this.csrf.parameterName + "=" + this.csrf.token;
                if (this.currentSignType === "form") {
                    url = "/" + this.userName + "/forms/delete-spot/" + this.formId + "/" + spotId + "?" + this.csrf.parameterName + "=" + this.csrf.token;
                }
                $.ajax({
                    method: 'DELETE',
                    url: url,
                    success: () => {
                        signSpaceDiv.remove();
                    }
                });
            });
        });
    }

    refreshSignFields() {
        $(".sign-space").each((_, element) => {
            const signSpaceDiv = $(element);
            const signHeight = parseFloat(signSpaceDiv.attr("data-es-sign-height"));
            const renderedHeight = Math.round(signHeight * this.pdfViewer.scale);
            const renderedWidth = Math.round(parseFloat(signSpaceDiv.attr("data-es-sign-width")) * this.pdfViewer.scale);
            signSpaceDiv.css("width", renderedWidth + "px");
            signSpaceDiv.css("height", renderedHeight + "px");
            const pageNum = parseInt(signSpaceDiv.attr("data-es-pos-page"), 10);
            const pageTop = this.pdfViewer.getPageTopInPdf(pageNum);
            const pageLeft = this.pdfViewer.getPageLeftInPdf(pageNum);
            signSpaceDiv.css("left", signSpaceDiv.attr("data-es-pos-x") * this.pdfViewer.scale + pageLeft + 'px');
            signSpaceDiv.css("top", signSpaceDiv.attr("data-es-pos-y") * this.pdfViewer.scale + pageTop + 'px');
            signSpaceDiv.css("font-size", Math.round(renderedHeight * 0.15) + "px");
            signSpaceDiv.find(".sign-icon").css("font-size", Math.round(renderedHeight * 0.45) + "px");
        });
    }

    addSign(forceSignNumber) {
        // alert(forceSignNumber)
        if(!this.notSigned && this.signPosition.signsList.length > 0) {
            bootbox.alert("Ce document contient déjà une signature électronique certifiée, il n’est donc pas possible d’ajouter d'autre visuel de signature.")
            return;
        }
        this.pdfViewer.annotationLinkRemove();
        let targetPageNumber = this.pdfViewer.pageNum;

        let signNum = null;
        if(forceSignNumber != null) {
            signNum = forceSignNumber;
        } else {
            for (let i = 0; i < this.currentSignRequestParamses.length; i++) {
                if (!this.currentSignRequestParamses[i].ready) {
                    this.signPosition.currentSignRequestParamsNum = i;
                    signNum = i;
                    break;
                }
            }
        }
        if(this.currentSignRequestParamses[signNum] != null) {
            targetPageNumber = this.currentSignRequestParamses[signNum].signPageNumber;
        }
        if(JSON.parse(localStorage.getItem('signNumber')) != null && this.restore) {
            this.signImageNumber = localStorage.getItem('signNumber');
        }
        this.signPosition.addSign(targetPageNumber, this.restore, this.signImageNumber, signNum);
        this.signPosition.goStep2();
    }


    addParaph() {
        if(!this.notSigned && this.signPosition.signsList.length > 0) {
            bootbox.alert("Ce document contient déjà une signature électronique certifiée, il n’est donc pas possible d’ajouter d'autre visuel de signature.")
            return;
        }
        let srp = this.signPosition.addSign(this.pdfViewer.pageNum, false, 999997);
        srp.initParaph();
    }

    initWorkspace() {
        console.info("init workspace");
        if(!this.ready) {
            this.ready = true;
            this.displayComments = new URL(window.location.href).searchParams.has("annotation");
            this.enableSignMode();
        }
    }

    initWorkspacePdf() {
        console.info("init workspace pdf");
        if(!this.ready) {
            this.ready = true;
            const url = new URL(window.location.href);
            this.displayComments = url.searchParams.has("annotation");
            this.enableSignMode();
            this.wheelDetector.addEventListener("down", e => this.pdfViewer.checkCurrentPage(e));
            this.wheelDetector.addEventListener("up", e => this.pdfViewer.checkCurrentPage(e));
            this.wheelDetector.addEventListener("zoomin", e => this.pdfViewer.zoomOut(e));
            this.wheelDetector.addEventListener("zoomout", e => this.pdfViewer.zoomIn(e));
            this.wheelDetector.addEventListener("zoominit", e => this.pdfViewer.zoomInit(e));
        }
        this.refreshAfterPageChange();
        this.initForm();
        $("#content").on('mousedown', e => this.signPosition.lockSigns());
        this.signPosition.updateScales(this.pdfViewer.scale);
    }

    initForm() {
        console.info("init form");
        if(!this.formInitialized) {
            this.formInitialized = true;
            if (!this.editable) {
                this.disableForm();
            }
        }
    }

    async saveData(disableAlert) {
        try {
            let promises = [];
            for(let i = 1; i < this.pdfViewer.pdfDoc.numPages + 1; i++) {
                let promise = this.pdfViewer.pdfDoc.getPage(i)
                    .then(page => page.getAnnotations())
                    .then(items => this.pdfViewer.saveValues(items));
                promises.push(promise);
            }
            await Promise.all(promises);
            this.pushData(false, disableAlert);
        } catch(error) {
            console.error('Erreur lors de la sauvegarde:', error);
        }
    }

    pushData(redirect, disableAlert) {
        console.debug("debug - " + "push data");
        let formData = new Map();

        let self = this;
        let pdfViewer = this.pdfViewer;

        pdfViewer.dataFields.forEach(function (dataField) {
            formData[dataField.name] = self.pdfViewer.savedFields.get(dataField.name);
        });

        if (redirect || this.dataId != null) {
            let json = JSON.stringify(formData);
            let dataId = $('#dataId');
            $.ajax({
                data: {'formData': json},
                type: 'POST',
                url: '/user/datas/form/' + this.formId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&dataId=' + self.dataId,
                success: function (response) {
                    dataId.val(response);
                    if (redirect) {
                        location.href = "/user/datas/" + response + "/update";
                    }
                }
            });
        } else {
            if(!this.saveAlert && !disableAlert) {
                this.saveAlert = true;
                bootbox.confirm({
                    message : "Attention, <p>Vous modifier les champs d’un PDF en dehors d’une procédure de formulaire esup-signature.<br>Dans ce cas, vos modifications seront prises en compte seulement si vous allez jusqu’à la signature du document. <br>Dans le cas contraire, si vous abandonnez, votre saisie sera perdue.</p>",
                    buttons: {
                        cancel: {
                            label: 'Ok',
                            className: 'btn-primary'
                        },
                        confirm: {
                            label: 'Ne plus me montrer ce message',
                            className: 'btn-secondary'
                        }
                    },
                    callback: function (result) {
                        if (result) {
                            localStorage.setItem('disableFormAlert', true)
                        }
                    }
                });
            }
        }
        this.executeNextCommand();
    }

    executeNextCommand() {
        if (this.nextCommand === "next") {
            this.pdfViewer.nextPage();
        } else if (this.nextCommand === "prev") {
            this.pdfViewer.prevPage()
        }
        this.nextCommand = "none";
    }

    initDataFields() {
        if (this.pdfViewer) {
            if (this.pdfViewer.dataFields.length > 0 && this.pdfViewer.dataFields[0].defaultValue != null) {
                for (let i = 0; i < this.pdfViewer.dataFields.length; i++) {
                    this.pdfViewer.savedFields.set(this.pdfViewer.dataFields[i].name, this.pdfViewer.dataFields[i].defaultValue);
                }
            }
        }
    }

    checkSignsPositions() {
        let testSign = Array.from(this.signPosition.signRequestParamses.values());
        if(testSign.filter(s => s.signImageNumber >= 0 && s.isSign).length > 0) {
            for (let i = 0; i < this.currentSignRequestParamses.length; i++) {
                if ((this.currentSignRequestParamses[i].ready == null || !this.currentSignRequestParamses[i].ready)) {
                    return i;
                }
            }
            return null;
        } else {
            return 0;
        }
    }

    static validateForm() {
        let valid = true;
        $("#signForm :input").each(function () {
            let input = $(this).get(0);
            if (!input.checkValidity()) {
                valid = false;
            }
        });
        if (!valid) {
            $("#checkDataSubmit").click();
        }
        return valid;
    }

    disableForm() {
        $("#signForm :input").not(':input[type=button], :input[type=submit], :input[type=reset]').each(function (i, e) {
            console.debug("debug - " + "disable ");
            console.debug("debug - " + e);
            e.disabled = true;
        });
    }

    launchValidate() {
        if (!WorkspacePdf.validateForm()) {
            $("#visaLaunchButton").attr('disabled', true);
        } else {
            $("#visaLaunchButton").attr('disabled', false);
        }

    }

    refreshWorkspace() {
        console.info("refresh workspace");
        this.pdfViewer.startRender();
        localStorage.setItem("scale", this.pdfViewer.scale);
    }

    clickAction(e) {
        this.signPosition.lockSigns();
        if (this.addSpotEnabled || this.addCommentEnabled) {
            this.displayDialogBox();
        }
    }

    moveAction(e) {
        if (this.addSpotEnabled || this.addCommentEnabled) {
            this.pointIt2(e);
        }
    }

    pointIt2(e) {
        let target = e.target;
        let page = $(target).closest('.pdf-page');
        if(page.length) {
            let pageNumber = page.attr("page-num") || page.attr("id")?.split("_")[1];
            const pageRect = page.get(0).getBoundingClientRect();
            $('#commentPageNumber').val(pageNumber);
            let xPos = Math.round(e.clientX - pageRect.left);
            let yPos = Math.round(e.clientY - pageRect.top);
            $("#commentPosX").val(xPos);
            $('#commentPosY').val(yPos);
            console.debug("debug - mouse pos : " + xPos + ", " + yPos);
            return;
        }
        let xPos = e.offsetX ? (e.offsetX) : e.clientX;
        let yPos = e.offsetY ? (e.offsetY) : e.clientY;
        $("#commentPosX").val(xPos);
        $('#commentPosY').val(yPos);
        console.debug("debug - mouse pos : " + xPos + ", " + yPos);
    }

    saveComment() {
        let spotStepNumberVal = $("#spotStepNumber");
        if (this.addSpotEnabled && spotStepNumberVal.val() === "") {
            spotStepNumberVal.attr("required", true);
            $("#submitPostit").click();
            return;
        }
        let postitComment = $("#postitComment");
        if (!this.addSpotEnabled && postitComment.val() === '') {
            $("#submitPostit").click();
            return;
        }
        let xPos = parseInt($("#commentPosX").val());
        let yPos = parseInt($("#commentPosY").val());
        let spotStepNumber = "";
        if(this.addSpotEnabled) {
            spotStepNumber = spotStepNumberVal.val();
        }
        let commentUrlParams =
            "comment=" + encodeURIComponent(postitComment.val()) +
            "&commentPosX=" + Math.round(xPos) +
            "&commentPosY=" + Math.round(yPos) +
            "&commentPageNumber=" + $("#commentPageNumber").val() +
            "&spotStepNumber=" + spotStepNumber +
            "&" + this.csrf.parameterName + "=" + this.csrf.token;
        let postitDiv = $("#postit");
        if(postitDiv.length) {
            postitDiv.html("<div class=\"spinner-border\" role=\"status\">\n" +
                "  <span class=\"visually-hidden\">Enregistrement</span>\n" +
                "</div>");
        }
        let mode = "user";
        if(this.isOtp) {
            mode = "otp";
        }
        $.ajax({
            method: 'POST',
            url: "/" + mode + "/signrequests/comment/" + this.signRequestId + "?" + commentUrlParams,
            success: function () {
                document.location.reload();
            }
        });
    }

    focusComment(postit) {
        this.refreshAfterPageChange();
        this.pdfViewer.animateScrollToPosition(parseInt($('#inDocComment_' + postit.id).css('top').replace('px', ''), 10));
    }

    getPageOffsets(pageNum) {
        const normalizedPage = Number.parseInt(pageNum, 10) || 1;
        return {
            top: this.pdfViewer.getPageTopInPdf(normalizedPage),
            left: this.pdfViewer.getPageLeftInPdf(normalizedPage)
        };
    }

    refreshAfterPageChange() {
        console.debug("debug - " + "refresh comments and sign pos" + this.pdfViewer.pageNum);
        // this.removeSignFields();
        let self = this;
        this.comments.forEach((comment, iterator) => {
            if(comment.stepNumber == null) {
                let postitDiv = $('#inDocComment_' + comment.id);
                let postitButton = $('#postit' + comment.id);
                if (this.editable || this.displayComments) {
                    postitDiv.show();
                    const pageOffsets = this.getPageOffsets(comment.pageNumber);
                    postitDiv.css('left', ((parseInt(comment.posX) * this.pdfViewer.scale) + pageOffsets.left) + "px");
                    postitDiv.css('top', ((parseInt(comment.posY) * this.pdfViewer.scale) - 48 + pageOffsets.top) + "px");
                    postitDiv.width(postitDiv.width() * this.pdfViewer.scale);
                    postitButton.css("background-color", "#FFC");
                    postitDiv.unbind('mouseup');
                    if((self.status === "draft" || self.status === "pending")) {
                        let deletable = postitDiv.attr('es-comment-delete') === "true";
                        let buttons = {
                            cancel: {
                                label: 'Fermer',
                                className: 'btn-secondary'
                            }
                        };
                        if(deletable) {
                            buttons = {
                                confirm: {
                                    label: 'Supprimer',
                                    className: 'btn-danger'
                                },
                                cancel: {
                                    label: 'Fermer',
                                    className: 'btn-secondary'
                                }
                            };
                        }
                        postitDiv.on('mouseup', function (e) {
                            e.stopPropagation();
                            bootbox.dialog({
                                title: postitDiv.attr("es-comment-title"),
                                message: postitDiv.attr("es-comment-text"),
                                buttons: buttons,
                                callback: function (result) {
                                    if (result) {
                                        bootbox.confirm('Confirmer la suppression', function (result2){
                                            if(result2) {
                                                $.ajax({
                                                    method: 'DELETE',
                                                    url: "/ws-secure/global/delete-comment/" + self.signRequestId + "/" + comment.id + "?" + self.csrf.parameterName + "=" + self.csrf.token,
                                                    success: function () {
                                                        document.location.reload();
                                                        $("#addSpotButton").attr("disabled", false);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            }).find('.modal-content').css({'background-color': 'rgb(255, 255, 204)'});
                        });
                    }
                } else {
                    postitDiv.hide();
                    postitButton.css("background-color", "#EEE");
                    postitDiv.unbind('mouseup');
                }
            }
        });
        let postitForm = $("#postit");
        if (postitForm.is(':visible')) {
            const commentPageNumber = $("#commentPageNumber").val();
            const pageOffsets = this.getPageOffsets(commentPageNumber);
            postitForm.css('left', (parseInt($("#commentPosX").val()) * this.pdfViewer.scale) + pageOffsets.left);
            postitForm.css('top', (parseInt($("#commentPosY").val()) * this.pdfViewer.scale) + pageOffsets.top);
            $("#postit :input").each(function () {
                $(this).removeAttr('disabled');
            });
            postitForm.children('select[name="spotStepNumber"]').each(function () {
                $(this).removeAttr('disabled');
            });
        }
        if(this.status === "pending") {
            this.initFormAction();
        }
        if(this.currentSignType !== "form" && this.currentSignType !== "hiddenVisa") {
            if(this.first) {
                this.initSignFields();
            } else {
                this.refreshSignFields();
            }
        }
        // $("div[id^='cross_']").each((index, e) => this.toggleSign(e));
        this.pdfViewer.pdfDiv.css('opacity', 1);
        this.first = false;
    }

    makeItDroppable(signSpaceDiv) {
        let self = this;
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
                for (let i = 0; i < self.signPosition.signRequestParamses.size; i++) {
                    let signRequestParams = Array.from(self.signPosition.signRequestParamses.values())[i];
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
                        // Prevent simulateDrop from re-running a synthetic drag that may shift persisted yPos.
                        signRequestParams.firstLaunch = false;
                        // applyCurrentSignRequestParams() uses #getPageLayout to position cross, no offset param needed.
                        signRequestParams.applyCurrentSignRequestParams();
                        let ui = { size: { width: 0, height: 0 }};
                        let width = parseInt(cross.css("width"));
                        let height = parseInt(cross.css("height"));
                        let maxWidth  = parseInt(signSpaceDiv.css("width"));
                        let maxHeight = parseInt(signSpaceDiv.css("height"));
                        let ratio = width / height;
                        ui.size.width  = maxWidth;
                        ui.size.height = ui.size.width / ratio;
                        if (ui.size.height > maxHeight) {
                            ui.size.height = maxHeight;
                            ui.size.width  = ui.size.height * ratio;
                        }
                        ui.size.width = ui.size.width - 2;
                        ui.size.height = ui.size.height - 2;
                        signRequestParams.resize(ui);
                        cross.css("width", signRequestParams.signWidth * self.pdfViewer.scale);
                        cross.css("background-size", signRequestParams.signWidth * self.pdfViewer.scale);
                        cross.css("height", signRequestParams.signHeight * self.pdfViewer.scale);
                        signRequestParams.dropped = true;
                        self.signPosition.goStep2();
                        console.log("real place : " + signRequestParams.xPos +", " + signRequestParams.yPos);
                    }
                }
                self.signPosition.currentSignRequestParamses[$(this).attr("id").split("_")[1]].ready = true;
            },
            out: function (event, ui) {
                if ($(this).data("locked") != null && $(this).data("locked") !== ui.draggable.attr("id")) {
                    return;
                }
                $(this).removeData("locked");
                $(this).addClass("sign-field");
                $(this).removeClass("sign-field-dropped");
                self.signPosition.currentSignRequestParamses[$(this).attr("id").split("_")[1]].ready = false;
                $(this).html("<div class='sign-content'><span class='sign-icon fi fi-rr-add'></span><span class='sign-text text-uppercase'>Placer la signature ici</span></div>");
                $(this).css("pointer-events", "auto");
                for (let i = 0; i < self.signPosition.signRequestParamses.size; i++) {
                    let signRequestParams = Array.from(self.signPosition.signRequestParamses.values())[i];
                    let cross = signRequestParams.cross;
                    if (cross.attr("id") === ui.draggable.attr("id")) {
                        cross.resizable("enable");
                        signRequestParams.signSpace = null;
                        self.signPosition.goStep1();
                    }
                }
            }
        });
    }


    // toggleSign(e) {
    //     let signId = $(e).attr("id").split("_")[1];
    //     console.log("toggle sign_" + signId);
    //     let signRequestParams = this.signPosition.signRequestParamses.get(parseInt(signId));
    //     if (this.mode === 'sign') {
    //         signRequestParams.show();
    //     } else {
    //         if(signRequestParams.signImages !== 999999) {
    //             signRequestParams.hide();
    //         }
    //     }
    //     if (this.first) this.first = false;
    // }

    displayDialogBox() {
        $('#pdf').unbind("mousemove");
        let comment = $("#comment-div");
        if (comment.is(':visible')) {
            return;
        }
        console.log(comment);
        this.signPosition.pointItEnable = false;
        $('#pdf').css('cursor', 'default');
        let commentPosX = $("#commentPosX");
        let commentPosY = $('#commentPosY');
        let commentPageNumber = $("#commentPageNumber").val();
        let xOffset = 12;
        let yOffset = 24;
        if (this.addCommentEnabled) xOffset = 0;
        let xPos = (parseInt(commentPosX.val()) + xOffset) / this.pdfViewer.scale;
        let yPos = (parseInt(commentPosY.val()) + yOffset) / this.pdfViewer.scale;
        commentPosX.val(Math.round(xPos));
        commentPosY.val(Math.round(yPos));
        const pageOffsets = this.getPageOffsets(commentPageNumber);
        comment.css('left', xPos * this.pdfViewer.scale + pageOffsets.left);
        comment.css('top', yPos * this.pdfViewer.scale + pageOffsets.top);
        $("#postitComment").removeAttr("disabled");
        $("#spotStepNumber").removeAttr("disabled");
        comment.show();
        this.signPosition.lockSigns();
        // this.signPosition.stopDragSignature(true);
    }

    hideComment(e) {
        e.stopPropagation();
        const url = new URL(window.location.href);
        
        window.location.href = url.toString();
    }

    enableReadMode() {
        this.displayComments = true;
        this.enableSignMode();
    }

    showComments() {
        $("#postit").removeClass("d-none");
        $("#commentHelp").removeClass("d-none");
        this.displayComments = true;
        this.signPosition.pointItEnable = true;
        if (this.changeModeSelector != null) {
            this.changeModeSelector.setSelected("comment");
        }
        // $('#addCommentButton2').removeClass('d-none');
        $('#commentsBar').show();
        this.refreshAfterPageChange();
        $(".circle").each(function () {
            $(this).show();
            $(this).css('width', '0px');
        })
        this.showAllPostits();
    }

    hideComments() {
        $("#postit").addClass("d-none");
        $("#commentHelp").addClass("d-none");
        this.displayComments = false;
        this.signPosition.pointItEnable = false;

        if (this.changeModeSelector != null) {
            this.changeModeSelector.setSelected(null);
        }

        // $('#addCommentButton2').addClass('d-none');
        $('#commentsBar').hide();
        $(".circle").each(function () {
            $(this).hide();
            $(this).css('width', '');
        });
        this.hideAllPostits && this.hideAllPostits();
    }

    enableCommentMode() {
        this.displayComments = true;
        this.enableSignMode();
    }

    enableSignMode() {
        console.info("apply unified workspace ui");
        this.disableAllModes();
        this.signPosition.pointItEnable = false;
        if (this.status === 'deleted') {
            $('#workspace').addClass('alert-danger');
        } else {
            $('#workspace').addClass('alert-success');
        }

        if (this.signable) {
            $('#sign-tools').removeClass("d-none");
            $('#signTools').removeClass("d-none");
            $('#signLaunchButton').removeClass('d-none');
            $('#addSignButton2').removeClass('d-none');
            $('#addParaphButton').removeClass('d-none');
            $('#visaLaunchButton').removeClass('d-none');
            $('#signButtons').removeClass('d-none');
            $('#forward-btn').removeClass('d-none');
            $('#refuseLaunchButton').removeClass('d-none');
            $('#trashLaunchButton').removeClass('d-none');
        }

        if (this.editable) {
            $('#commentsTools').show();
            $('#addCommentButton2').removeClass('d-none');
            $('#addSpotButton2').removeClass('d-none');
            $('#commentsBar').show();
            $('#postit').removeClass("d-none");
            $('#commentHelp').removeClass("d-none");
            $('#insert-btn-div').show();
            $('#insert-btn').show();
        }

        if (this.displayComments) {
            $(".circle").show().css('width', '0px');
            this.showAllPostits();
        } else {
            $(".circle").hide();
            this.hideAllPostits();
        }

        if (this.signable || this.editable) {
            $(".sign-space").show();
        } else {
            $(".sign-space").hide();
        }

        if(this.signable && this.currentSignType !== 'hiddenVisa') {
            $("#addSignButton2").focus();
        }

        $('#infos').show();
        $('#insert-btn-div').show();
        let insertBtn = $('#insert-btn');
        insertBtn.show();
        insertBtn.removeClass("btn-warning");
        if(this.isPdf) {
            if (this.currentSignRequestParamses != null && this.currentSignRequestParamses.length > 0 && this.currentSignRequestParamses[0] != null) {
                if (this.forcePageNum) {
                    this.pdfViewer.scrollToPage(this.forcePageNum);
                }
            } else {
                this.pdfViewer.scrollToPage(1);
            }
        }
        $("#cross_999999").remove();
        $("#addCommentButton").attr("disabled", false);
        $("#addSpotButton").attr("disabled", false);
        if(this.isPdf) {
            this.refreshAfterPageChange();
        }
    }

    disableAllModes() {
        $('#workspace').removeClass('alert-success').removeClass('alert-secondary').removeClass('alert-warning').removeClass('alert-primary');
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        $('#addCommentButton2').addClass('d-none');
        $('#addSpotButton2').addClass('d-none');
        $('#signLaunchButton').addClass('d-none');
        $('#forward-btn').addClass('d-none');
        $('#addSignButton2').addClass('d-none');
        $('#addParaphButton').addClass('d-none');
        $('#visaLaunchButton').addClass('d-none');
        $('#refuseLaunchButton').addClass('d-none');
        $("#commentHelp").addClass("d-none");
        $('#commentsTools').hide();
        $('#commentsBar').hide();
        $('#sign-tools').addClass("d-none");
        $('#signTools').addClass("d-none");
        $('#infos').hide();
        $('#postit').hide();
        $('#refusetools').hide();
        $('#insert-btn-div').hide();
        $('#pdf').css('cursor', 'default');
        $('#hideCommentButton').unbind();
        $(".spot").each(function () {
            $(this).hide();
        });
        $(".circle").each(function () {
            $(this).hide();
        });
        this.hideAllPostits();
    }

    hideAllPostits() {
        $(".postit-global").each(function () {
            $(this).addClass("d-none");
        });
    }

    showAllPostits() {
        $(".postit-global").each(function () {
            let element = $(this);
            $(this).removeClass("d-none");
            $(this).draggable();
            $(this).on('mousedown', function (e) {
                let postit = $(this).attr('id');
                $(".postit-global").each(function () {
                    if($(this).attr('id') === postit) {
                        $(this).css('z-index', 1001);
                    }else {
                        $(this).css('z-index', 1000);
                    }
                });
            });
            $(this).find("p").first().on('mousedown', function (e) {
                $(this).parent().toggleClass("postitarea-auto");
            });
            $(this).find("p").first().on('scroll', function (e) {
                $(this).addClass("postitarea-basic");
            });
            $(this).resizable({
                aspectRatio: false,
                minWidth: 215,
                minHeight: 215,
                resize: function( event, ui ) {
                    let postit = document.querySelector(".postitarea");
                    let parent = postit.closest("#potit-comment");

                    if (postit && parent) {
                        let lineHeight = parseFloat(window.getComputedStyle(postit).lineHeight);
                        let availableHeight = parent.clientHeight;
                        let lines = Math.floor(availableHeight / lineHeight);
                        postit.style.webkitLineClamp = lines;
                    }
                }
            });
        });
    }

    enableCommentAdd(e) {
        let saveCommentButton = $('#saveCommentButton');
        let hideCommentButton = $('#hideCommentButton');
        saveCommentButton.unbind();
        hideCommentButton.unbind();
        let last = 0;
        let inside = false;
        $('#pdf')
            .on('mouseenter', () => { inside = true; })
            .on('mouseleave', () => { inside = false; });
        $('#pdf').on('click', e => {
            if (!inside) {
                e.preventDefault();
                e.stopPropagation();
                return;
            }
            this.clickAction(e);
        });
        $('#pdf').on('mousemove', e => {
            const now = performance.now();
            if (now - last < 50) return; // 50ms = ~20 fps
            last = now;
            this.moveAction(e);
        });
        $(document).off('keydown');
        document.addEventListener('keydown', function onEscCapture(e) {
            if (e.key === 'Escape') {
                e.stopImmediatePropagation?.();
                e.stopPropagation();
                e.preventDefault();
                location.reload();
            }
        }, { capture: true});
        $("#addSpotButton").attr("disabled", true);
        $("#addCommentButton").addClass("btn-danger");
        let addCommentButton2 = $("#addCommentButton2");
        addCommentButton2.addClass("bg-danger");
        addCommentButton2.children().addClass("text-white");
        addCommentButton2.attr("title", "Annuler l'ajout d'annotation");
        $("#addSpotButton2").attr("disabled", true);
        if (this.addCommentEnabled) {
            location.reload();
        } else {
            this.enableAddComment();
        }
        this.addSpotEnabled = false;
        saveCommentButton.on('click', e => this.saveComment(e));
        hideCommentButton.on('click', e => this.hideComment(e));
    }

    disableAddComment() {
        this.addCommentEnabled = false;
        this.disablePointer();
        $("#divSpotStepNumber").show();
        $(".textLayer").each(function () {
            $(this).removeClass("text-disable-selection");
        });
    }

    enableAddComment() {
        let postit = $("#postit");
        postit.removeClass("alert-success");
        postit.addClass("alert-warning");
        this.addCommentEnabled = true;
        this.displayCommentPointer();
        $("#divSpotStepNumber").hide();
        $("#postitComment").attr("required", true);
        $(".textLayer").each(function () {
            $(this).addClass("text-disable-selection");
        });
    }

    enableSpotAdd() {
        this.disableAddComment();
        $("#commentHelp").remove();
        $("#addSpotButton").attr("disabled", true);
        $("#addCommentButton").attr("disabled", true);
        $("#addSpotButton2").attr("disabled", true);
        $("#addCommentButton2").attr("disabled", true);
        this.signPosition.addSign(this.pdfViewer.pageNum, false, 999999, null);
    }

    displayCommentPointer() {
        const png = '/images/icons/rr-commentaire-alt.png';
        this.pdfViewer.pdfDiv.css('cursor', `url("${png}"), auto`);
    }

    disablePointer() {
        this.pdfViewer.pdfDiv.css('cursor', 'default');
    }

    changeSpotStep() {
        let stepNumber = $("#spotStepNumber").val();
        $('[id^="liveStep-"]').each(function () {
            $(this).removeClass("bg-success");
        });
        let liveStep = $("#liveStep-" + stepNumber);
        liveStep.addClass("bg-success");
    }

    initChangeModeSelector() {
        let self = this;
        $("#changeMode1").on("click", function(e) {
            self.displayComments = !self.displayComments;
            self.enableSignMode();
        });
        $("#changeMode2").on("click", function(e) {
            self.enableSignMode();
        });
    }

    changeMode(e) {
        if (this.ready) {
            this.enableSignMode();
        }
    }

    initFormAction() {
        if(!this.actionInitialyzed) {
            console.debug("debug - " + "eval : " + this.action);
            jQuery.globalEval(this.action);
            this.actionInitialyzed = true;
        }
    }

    autocollapse() {
        let menu = "#ws-tabs";
        let maxWidth = $("#workspace").innerWidth() - 50;
        const calculateTotalWidth = () => {
            let total = 0;
            const listItems = document.querySelectorAll('#ws-tabs > li');
            listItems.forEach(li => {
                const rect = li.getBoundingClientRect();
                total += rect.width;
                const style = window.getComputedStyle(li);
                total += parseFloat(style.marginLeft) + parseFloat(style.marginRight);
            });
            return total;
        };
        let totalWidth = calculateTotalWidth();
        if (totalWidth >= maxWidth) {
            $(menu + ' .dropdown').removeClass('d-none');
            totalWidth = calculateTotalWidth();
            while (totalWidth > maxWidth) {
                let children = $(menu + ' > li.file-tab');
                let count = children.length;
                if (count === 0) break; // Sécurité
                $(children[count - 1]).prependTo(menu + ' .dropdown-menu');
                totalWidth = calculateTotalWidth();
                console.warn("Nouvelle largeur : " + totalWidth);
            }
        }
        else {
            let collapsed = $(menu + ' .dropdown-menu > li');

            if (collapsed.length === 0) {
                $(menu + ' .dropdown').addClass('d-none');
                return;
            }
            const dropdownWidth = $(menu + ' .dropdown')[0].getBoundingClientRect().width;
            const safeMaxWidth = maxWidth - dropdownWidth - 50; // marge de sécurité

            let i = 0;
            while (i < collapsed.length && totalWidth < safeMaxWidth) {
                const itemWidth = collapsed[i].getBoundingClientRect().width;
                const style = window.getComputedStyle(collapsed[i]);
                const margins = parseFloat(style.marginLeft) + parseFloat(style.marginRight);
                const estimatedWidth = totalWidth + itemWidth + margins;
                if (estimatedWidth >= safeMaxWidth) break;
                $(collapsed[i]).insertBefore($(menu + ' > li.dropdown'));
                totalWidth = calculateTotalWidth();
                i++;
            }
            if ($(menu + ' .dropdown-menu > li').length === 0) {
                $(menu + ' .dropdown').addClass('d-none');
            }
        }
    }

    getBrowserZoom() {
        return 1 || 1;
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

}
