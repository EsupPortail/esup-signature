import {PdfViewer} from "../../utils/PdfViewer.js?version=@version@";
import {SignPosition} from "./SignPosition.js?version=@version@";
import {WheelDetector} from "../../utils/WheelDetector.js?version=@version@";

export class WorkspacePdf {

    constructor(isPdf, id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, postits, currentStepNumber, currentStepMultiSign, workflow, signImages, userName, authUserName, fields, stepRepeatable, status, csrf, action, notSigned, attachmentAlert, attachmentRequire, isOtp, restore, phone) {
        console.info("Starting workspace UI");
        this.ready = false;
        this.formInitialized = false;
        this.isPdf = isPdf;
        this.isOtp = isOtp;
        this.phone = phone;
        this.changeModeSelector = null;
        this.action = action;
        this.dataId = dataId;
        this.formId = formId;
        this.workflow = workflow;
        this.signImageNumber = signImageNumber;
        this.restore = restore;
        this.postits = postits;
        this.notSigned = notSigned;
        this.signable = signable;
        this.editable = editable;
        this.signRequestId = id;
        this.currentSignType = currentSignType;
        this.stepRepeatable = stepRepeatable;
        this.status = status;
        this.csrf = csrf;
        this.currentStepMultiSign = currentStepMultiSign;
        this.forcePageNum = null;
        this.pointItEnable = true;
        this.first = true;
        this.saveAlert = false;
        this.scrollTop = 0;
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
                this.pdfViewer = new PdfViewer('/admin/forms/get-file/' + id, signable, editable, currentStepNumber, this.forcePageNum, fields, true);
            } else {
                this.pdfViewer = new PdfViewer('/ws-secure/global/get-last-file-pdf/' + id, signable, editable, currentStepNumber, this.forcePageNum, fields, false);
            }
        }
        this.signPosition = new SignPosition(
            currentSignType,
            currentSignRequestParamses,
            currentStepMultiSign,
            signImageNumber,
            signImages,
            userName, authUserName, signable, this.forcePageNum, this.isOtp, this.phone, this.csrf);
        this.currentSignRequestParamses = currentSignRequestParamses;
        this.mode = 'sign';
        this.wheelDetector = new WheelDetector();
        this.addSpotEnabled = false;
        this.addCommentEnabled = false;
        this.nextCommand = "none";
        this.initChangeModeSelector();
        this.initDataFields(fields);
        this.wsTabs = $("#ws-tabs");
        this.navWidth = this.wsTabs.innerWidth();
        this.addSignButton = $("#addSignButton")
        if (currentSignType === "form" || (formId == null && !workflow) || currentSignRequestParamses.length === 0) {
            if(this.wsTabs.length) {
                this.autocollapse();
                let self = this;
                $(window).on('resize', function (e) {
                    if(e.target.tagName == null) {
                        self.autocollapse();
                    }
                });
            }
        }
        let root = document.querySelector(':root');
        root.setAttribute("style", "scroll-behavior: auto;");
        this.initListeners();
    }

    initListeners() {
        if (this.isPdf) {
            $('#prev').on('click', e => this.pdfViewer.prevPage());
            $('#next').on('click', e => this.pdfViewer.nextPage());
            $('#end-button').on('click', e => this.pdfViewer.nextPage());
            $('#addCommentButton').on('click', e => this.enableCommentAdd(e));
            $('#addSpotButton').on('click', e => this.enableSpotAdd(e));
            $("#spotStepNumber").on('change', e => this.changeSpotStep());
            $("#showComments").on('click', e => this.enableCommentMode());
            // this.signPosition.addEventListener("startDrag", e => this.hideAllPostits());
            // this.signPosition.addEventListener("stopDrag", e => this.showAllPostits());
            this.pdfViewer.addEventListener('renderFinished', e => this.initWorkspace());
            this.pdfViewer.addEventListener('reachEnd', e => this.markAsViewed());
            this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
            this.pdfViewer.addEventListener('change', e => this.saveData(localStorage.getItem('disableFormAlert') === "true"));

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

            $(".postit-copy").on('click', function (e) {
                let snackbar = document.getElementById("snackbar");
                snackbar.className = "show";
                let text = $("#postit-text-" + $(e.target).attr("es-postit-id")).text();
                if (window.isSecureContext && navigator.clipboard) {
                    navigator.clipboard.writeText(text);
                    snackbar.innerText = "Texte copié dans le presse papier";
                } else {
                    snackbar.innerText = "Impossible de copier le texte d'une application non sécurisée";
                }
                setTimeout(function(){ snackbar.className = snackbar.className.replace("show", ""); }, 3000);
            });
            if(this.postits != null) {
                this.postits.forEach((postit, index) => {
                    let postitButton = $('#postit' + postit.id);
                    postitButton.on('click', e => this.focusComment(postit));
                    postitButton.on('mouseover', function () {
                        $('#inDocComment_' + postit.id).addClass('text-danger');
                        postitButton.addClass('circle-border');
                    });
                    postitButton.on('mouseout', function () {
                        $('#inDocComment_' + postit.id).removeClass('text-danger');
                        postitButton.removeClass('circle-border');
                    });
                });
            }
        }
        this.addSignButton.on('click', e => this.addSign());
        $('#addSignButton2').on('click', e => this.addSign());
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

    removeSignFields() {
        for(let i = 0; i < this.currentSignRequestParamses.length; i++) {
            let toDeleteSignSpace = $("#signSpace_" + i);
            toDeleteSignSpace.unbind();
            toDeleteSignSpace.remove();
        }
    }

    initSignFields() {
        for(let i = 0; i < this.currentSignRequestParamses.length; i++) {
            let currentSignRequestParams = this.currentSignRequestParamses[i];
            let signSpaceDiv = $("#signSpace_" + i);
            if (this.mode === "sign" && this.signable) {
                if(signSpaceDiv.length) {
                    signSpaceDiv.unbind();
                    signSpaceDiv.remove();
                }
                let signSpaceHtml = "<div id='signSpace_" + i + "' title='Emplacement de signature : " + currentSignRequestParams.comment + "' class='sign-field sign-space' data-es-pos-page='" + currentSignRequestParams.signPageNumber + "' data-es-pos-x='" + currentSignRequestParams.xPos + "' data-es-pos-y='" + currentSignRequestParams.yPos + "' data-es-width='" + currentSignRequestParams.signWidth + "' data-es-height='" + currentSignRequestParams.signHeight + "'></div>";
                $("#pdf").append(signSpaceHtml);
                signSpaceDiv = $("#signSpace_" + i);
                signSpaceDiv.on("click", e => this.addSign(i));
                if(currentSignRequestParams.ready == null || !currentSignRequestParams.ready) {
                    if(this.currentSignType !== "visa") {
                        signSpaceDiv.html("Cliquez ici pour insérer votre signature");
                    } else {
                        signSpaceDiv.html("Cliquez ici pour insérer votre visa");
                    }
                }
                if (currentSignRequestParams.ready) {
                    signSpaceDiv.removeClass("sign-field");
                }
                signSpaceDiv.show();
                let offset = Math.round($("#page_" + currentSignRequestParams.signPageNumber).offset().top - this.pdfViewer.initialOffset + 10);
                let xPos = Math.round(currentSignRequestParams.xPos * this.pdfViewer.scale);
                let yPos = Math.round(currentSignRequestParams.yPos * this.pdfViewer.scale + offset);
                signSpaceDiv.css("top", yPos);
                signSpaceDiv.css("left", xPos);
                signSpaceDiv.css("width", Math.round(parseInt(signSpaceDiv.attr("data-es-width")) * this.pdfViewer.scale) + "px");
                signSpaceDiv.css("height", Math.round(parseInt(signSpaceDiv.attr("data-es-height")) * this.pdfViewer.scale) + "px");
                signSpaceDiv.css("font-size", 12 *  this.pdfViewer.scale);
                this.makeItDroppable(signSpaceDiv);
            }
        }
    }

    addSign(forceSignNumber) {
        this.pdfViewer.annotationLinkRemove();
        let targetPageNumber = this.pdfViewer.pageNum;
        let signNum = this.signPosition.currentSignRequestParamsNum;
        if(forceSignNumber != null) {
            signNum = forceSignNumber;
        }
        if(this.currentSignRequestParamses[signNum] != null) {
            targetPageNumber = this.currentSignRequestParamses[signNum].signPageNumber;
            this.signPosition.currentSignRequestParamsNum++;
        }
        if(JSON.parse(localStorage.getItem('signNumber')) != null && this.restore) {
            this.signImageNumber = localStorage.getItem('signNumber');
        }
        this.signPosition.addSign(targetPageNumber, this.restore, this.signImageNumber, forceSignNumber);
        if((this.currentSignType === "nexuSign" || this.currentSignType === "certSign") && !this.notSigned) {
            $("#addSignButton").attr("disabled", true);
        }
    }

    initWorkspace() {
        console.info("init workspace");
        if(!this.ready) {
            this.ready = true;
            if (localStorage.getItem('mode') === null) {
                this.mode = "comment";
                localStorage.setItem('mode', this.mode);
            }
            if (this.status === 'draft') {
                this.mode = "comment";
                localStorage.setItem('mode', this.mode);
            } else {
                this.mode = "sign";
                localStorage.setItem('mode', this.mode);
            }
            console.info("init to " + this.mode + " mode");
            if(this.signable) {
                if (localStorage.getItem('mode') === 'comment') {
                    this.enableCommentMode();
                } else if (this.currentSignType !== 'form') {
                    this.enableSignMode();
                }
            } else if(!this.editable) {
                this.enableSignMode();
            } else {
                this.enableReadMode();
            }
            this.wheelDetector.addEventListener("down", e => this.pdfViewer.checkCurrentPage(e));
            this.wheelDetector.addEventListener("up", e => this.pdfViewer.checkCurrentPage(e));
            this.wheelDetector.addEventListener("zoomin", e => this.pdfViewer.zoomIn());
            this.wheelDetector.addEventListener("zoomout", e => this.pdfViewer.zoomOut());
            if(this.currentSignType === "form") {
                this.enableCommentMode();
            }
        }
        this.refreshAfterPageChange();
        this.initForm();
        this.pdfViewer.pdfDiv.on('mousedown', e => this.clickAction(e));

    }

    initForm() {
        console.info("init form");
        if(!this.formInitialized) {
            this.formInitialized = true;
            if (this.mode === 'read' || this.mode === 'comment') {
                this.disableForm();
            }
        }
    }

    saveData(disableAlert) {
        let self = this;
        for(let i = 1; i < this.pdfViewer.pdfDoc.numPages + 1; i++) {
            this.pdfViewer.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.pdfViewer.saveValues(items)).then(function(){
                if(i === self.pdfViewer.pdfDoc.numPages) {
                    self.pushData(false, disableAlert);
                }
            }));
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
                if ((this.currentSignRequestParamses[i].ready == null || !this.currentSignRequestParamses[i].ready) && (this.formId != null || this.dataId != null || this.workflow === true)) {
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
        this.signPosition.updateScales(this.pdfViewer.scale);
        // this.refreshAfterPageChange();
    }

    clickAction(e) {
        if (this.mode === 'sign') {
            this.signPosition.lockSigns();
        } else if (this.mode === 'comment') {
            if (this.addSpotEnabled || this.addCommentEnabled) {
                this.displayDialogBox();
            }
        }
    }

    moveAction(e) {
        if (this.addSpotEnabled || this.addCommentEnabled) {
            this.pointIt2(e);
        }
    }

    pointIt2(e) {
        let target = e.target;
        let page = $(target).parent().parent();
        let pageNumber = page.attr("page-num");
        $('#commentPageNumber').val(pageNumber);
        let offset = 0;
        if(page.attr("page-num") !== undefined) {
            offset = $("#page_" + page.attr("page-num")).offset().top;
        }
        let xPos = e.offsetX ? (e.offsetX) : e.clientX;
        let yPos = e.pageY - offset;
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
        let commentUrlParams = "comment=" + encodeURIComponent(postitComment.val()) +
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
        $.ajax({
            method: 'POST',
            url: "/user/signrequests/comment/" + this.signRequestId + "?" + commentUrlParams,
            success: function () {
                document.location.reload();
            }
        });
    }

    focusComment(postit) {
        this.refreshAfterPageChange();
        $('html,body').animate({scrollTop: $('#inDocComment_' + postit.id).css('top').replace('px', '')}, 'slow');
    }

    refreshAfterPageChange() {
        console.debug("debug - " + "refresh comments and sign pos" + this.pdfViewer.pageNum);
        // this.removeSignFields();
        let self = this;
        this.postits.forEach((comment, iterator) => {
            if(comment.stepNumber == null) {
                let postitDiv = $('#inDocComment_' + comment.id);
                let postitButton = $('#postit' + comment.id);
                if (this.mode === 'comment') {
                    postitDiv.show();
                    postitDiv.css('left', ((parseInt(comment.posX) * this.pdfViewer.scale)) + "px");
                    let pageOffset = $("#page_" + comment.pageNumber).offset();
                    let offset = 10;
                    if(pageOffset) {
                        offset = pageOffset.top - this.pdfViewer.initialOffset + 10;
                    }
                    postitDiv.css('top', ((parseInt(comment.posY) * this.pdfViewer.scale) - 48 + offset) + "px");
                    postitDiv.width(postitDiv.width() * this.pdfViewer.scale);
                    postitButton.css("background-color", "#FFC");
                    postitDiv.unbind('mouseup');
                    if((self.status === "draft" || self.status === "pending") && postitDiv.attr('title') !== undefined) {
                        postitDiv.on('mouseup', function (e) {
                            e.stopPropagation();
                            bootbox.confirm("Supprimer cette annotation ?", function (result) {
                                if (result) {
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
                        });
                    }
                } else {
                    postitDiv.hide();
                    postitButton.css("background-color", "#EEE");
                    postitDiv.unbind('mouseup');
                }
            }
        });
        let index = 0;
        this.postits.forEach((spot, iterator) => {
            if(spot.stepNumber != null) {
                let spotDiv = $('#inDocSpot_' + spot.id);
                let signDiv = $('#inDocSign_' + spot.id);
                if (this.mode === 'comment') {
                    spotDiv.show();
                    let page = $("#page_" + spot.pageNumber);
                    let offset = 0;
                    if(page.offset() != null) {
                        offset = page.offset().top - this.pdfViewer.initialOffset + 10;
                    }
                    let posX = Math.round((parseInt(spot.posX) * this.pdfViewer.scale));
                    let posY = Math.round((parseInt(spot.posY) * this.pdfViewer.scale) + offset);
                    console.log("spot pos : " + posX + ", " + posY);
                    spotDiv.css('left',  posX + "px");
                    spotDiv.css('top',  posY + "px");
                    spotDiv.width(150 * this.pdfViewer.scale);
                    spotDiv.width(75 * this.pdfViewer.scale);
                    if(signDiv != null) {
                        signDiv.css("width", Math.round(150 * self.pdfViewer.scale) + "px");
                        signDiv.css("height", Math.round(75 * self.pdfViewer.scale) + "px");
                        signDiv.css("font-size", 12 * self.pdfViewer.scale);
                    }
                    spotDiv.unbind('mouseup');
                    if(signDiv.attr("data-es-delete")) {
                        spotDiv.on('mouseup', function (e) {
                            e.stopPropagation();
                            bootbox.confirm("Supprimer cet emplacement de signature ?", function (result) {
                                if (result) {
                                    let url = "/ws-secure/global/delete-comment/" + self.signRequestId + "/" + spot.id + "?" + self.csrf.parameterName + "=" + self.csrf.token;
                                    if (self.currentSignType === "form") {
                                        url = "/admin/forms/delete-spot/" + self.formId + "/" + spot.id + "?" + self.csrf.parameterName + "=" + self.csrf.token;
                                    }
                                    $.ajax({
                                        method: 'DELETE',
                                        url: url,
                                        success: function () {
                                            location.reload();
                                        }
                                    });
                                }
                            });
                        });
                    }
                }
                index++;
            }
        });
        let postitForm = $("#postit");
        if (postitForm.is(':visible')) {
            postitForm.css('left', (parseInt($("#commentPosX").val()) * this.pdfViewer.scale));
            postitForm.css('top', (parseInt($("#commentPosY").val()) * this.pdfViewer.scale));
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
        if(this.currentSignType !== "form") {
            this.initSignFields();
        }
        $("div[id^='cross_']").each((index, e) => this.toggleSign(e));
        this.pdfViewer.pdfDiv.css('opacity', 1);
    }

    makeItDroppable(signSpaceDiv) {
        let self = this;
        signSpaceDiv.droppable({
            tolerance: 'touch',
            accept: ".drop-sign",
            drop: function (event, ui) {
                $(this).removeClass("sign-field");
                $(this).addClass("sign-field-dropped");
                $(this).css("pointer-events", "none");
                $(this).text("");
                for (let i = 0; i < self.signPosition.signRequestParamses.size; i++) {
                    let signRequestParams = Array.from(self.signPosition.signRequestParamses.values())[i];
                    let cross = signRequestParams.cross;
                    if (cross.attr("id") === ui.draggable.attr("id")) {
                        let offset = Math.round($("#page_" + signSpaceDiv.attr("data-es-pos-page")).offset().top) - self.pdfViewer.initialOffset + 10;
                        signRequestParams.xPos = signSpaceDiv.attr("data-es-pos-x");
                        signRequestParams.yPos = signSpaceDiv.attr("data-es-pos-y");
                        signRequestParams.applyCurrentSignRequestParams(offset);
                        let ui = { size: { width: 0, height: 0 }};
                        ui.size.width = parseInt(signSpaceDiv.css("width"));
                        let width = parseInt(cross.css("width"));
                        let height = parseInt(cross.css("height"));
                        ui.size.height = height * (ui.size.width / width);
                        signRequestParams.resize(ui);
                        cross.css("width", signRequestParams.signWidth * self.pdfViewer.scale);
                        cross.css("background-size", signRequestParams.signWidth * self.pdfViewer.scale);
                        cross.css("height", signRequestParams.signHeight * self.pdfViewer.scale);
                        signRequestParams.dropped = true;
                        console.log("real place : " + signRequestParams.xPos +", " + signRequestParams.yPos + " - offset " + offset);
                        cross.resizable("disable");
                    }
                }
                self.signPosition.currentSignRequestParamses[$(this).attr("id").split("_")[1]].ready = true;
            },
            out: function (event, ui) {
                if (!self.isThereSign($(this))) {
                    $(this).addClass("sign-field");
                    $(this).removeClass("sign-field-dropped");
                    let id = $(this).attr("id").split("_")[1];
                    self.signPosition.currentSignRequestParamses[$(this).attr("id").split("_")[1]].ready = false;
                    $(this).text("Vous devez placer une signature ici");
                    $(this).css("pointer-events", "auto");
                }
                for (let i = 0; i < self.signPosition.signRequestParamses.size; i++) {
                    let signRequestParams = Array.from(self.signPosition.signRequestParamses.values())[i];
                    let cross = signRequestParams.cross;
                    if (cross.attr("id") === ui.draggable.attr("id")) {
                        cross.resizable("enable");
                    }
                }
            }
        });
    }

    isThereSign(spot) {
        let isThereSign = false;
        for (let i = 0; i < this.signPosition.signRequestParamses.size; i++) {
            let testX = Array.from(this.signPosition.signRequestParamses.values())[i].xPos;
            let testY = Array.from(this.signPosition.signRequestParamses.values())[i].yPos;
            let xx = Math.round(parseInt(spot.css("left")) / this.pdfViewer.scale);
            let yy = Math.round(parseInt(spot.css("top")) / this.pdfViewer.scale);
            let test = Array.from(this.signPosition.signRequestParamses.values())[i].cross.attr("remove");
            if (testX === xx && testY === yy && test !== "true") {
                isThereSign = true;

            }
        }
        return isThereSign;
    }

    toggleSign(e) {
        let signId = $(e).attr("id").split("_")[1];
        console.log("toggle sign_" + signId);
        let signRequestParams = this.signPosition.signRequestParamses.get(parseInt(signId));
        if (this.mode === 'sign') {
            signRequestParams.show();
        } else {
            if(signRequestParams.signImages !== 999999) {
                signRequestParams.hide();
            }
        }
        if (this.first) this.first = false;
    }

    displayDialogBox() {
        $('#pdf').unbind("mousemove");
        let comment = $("#comment-div");
        if (this.mode !== 'comment' || comment.is(':visible')) {
            return;
        }
        console.log(comment);
        this.signPosition.pointItEnable = false;
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
        comment.css('left', xPos * this.pdfViewer.scale);
        let offset = $("#page_" + commentPageNumber).offset().top - this.pdfViewer.initialOffset;
        comment.css('top', yPos * this.pdfViewer.scale + offset);
        $("#postitComment").removeAttr("disabled");
        $("#spotStepNumber").removeAttr("disabled");
        $("#addSignParams").removeAttr("disabled");
        comment.show();
        this.signPosition.lockSigns();
        // this.signPosition.stopDragSignature(true);
    }

    hideComment(e) {
        e.stopPropagation();
        if (this.mode !== 'comment') {
            return;
        }
        this.addCommentEnabled = false;
        this.disablePointer();
        this.signPosition.pointItEnable = true;
        let addCommentButton = $("#addCommentButton");
        addCommentButton.toggleClass("btn-primary");
        addCommentButton.toggleClass("btn-outline-dark");
        $("#comment-div").hide();
        $("#addSpotButton").attr("disabled", false);
        $('#pdf').mousemove(e => this.moveAction(e));
    }

    enableReadMode() {
        console.info("enable read mode");
        this.disableAllModes();
        this.mode = 'read';
        localStorage.setItem('mode', 'read');
        this.signPosition.pointItEnable = false;
        $('#readModeButton').toggleClass('btn-outline-secondary');
        $('#workspace').addClass('alert-primary');
        $('#rotateleft').prop('disabled', false);
        $('#rotateright').prop('disabled', false);
        $('#rotateleft').css('opacity', 1);
        $('#rotateright').css('opacity', 1);
        $('#insert-btn-div').hide();
        this.showAllPostits();
        $(".sign-space").each(function () {
            $(this).hide();
        });
    }

    enableCommentMode() {
        console.info("enable comments mode");
        localStorage.setItem('mode', 'comment');
        $("#postitHelp").remove();
        this.disableAllModes();
        $("#changeMode1").attr("checked", true);
        $("#postit").removeClass("d-none");
        $("#commentHelp").removeClass("d-none");
        this.mode = 'comment';
        this.signPosition.pointItEnable = true;
        $('#workspace').addClass('alert-warning');
        $('#commentModeButton').toggleClass('btn-outline-warning');
        $('#commentsTools').removeClass('d-none');
        if (this.changeModeSelector != null) {
            this.changeModeSelector.setSelected("comment");
        }
        $('#signTools').addClass("d-none");
        $('#commentsBar').show();
        $('#infos').show();
        $('#insert-btn-div').show();
        let insertBtn = $('#insert-btn');
        insertBtn.removeClass("pulse-primary");
        insertBtn.removeClass("btn-outline-primary");
        insertBtn.removeClass("btn-light");
        insertBtn.addClass("btn-warning");
        this.pdfViewer.promiseToggleFields(false);
        this.refreshAfterPageChange();
        $(".spot").each(function () {
            $(this).show();
            $(this).css('width', '0px');
        });
        $(".circle").each(function () {
            $(this).show();
            $(this).css('width', '0px');
        })
        this.showAllPostits();
        $(".sign-space").each(function () {
            $(this).hide();
        });
    }

    enableSignMode() {
        console.info("enable sign mode");
        localStorage.setItem('mode', 'sign');
        this.disableAllModes();
        $("#changeMode2").attr("checked", true);
        this.mode = 'sign';
        this.signPosition.pointItEnable = false;
        if (this.status === 'pending') {
            $('#workspace').addClass('alert-secondary');
        } else if (this.status === 'deleted') {
            $('#workspace').addClass('alert-danger');
        } else {
            $('#workspace').addClass('alert-success');
        }
        $(".circle").each(function () {
            $(this).hide();
        });
        $(".spot").each(function () {
            $(this).hide();
        });
        $('#signButtons').removeClass('d-none');
        $('#signModeButton').toggleClass('btn-outline-success');
        $('#sign-tools').removeClass("d-none");
        if(this.currentSignType !== 'hiddenVisa') {
            $("#addSignButton2").focus();
        }

        $('#infos').show();
        $('#insert-btn-div').show();
        let insertBtn = $('#insert-btn');
        insertBtn.addClass("pulse-primary");
        insertBtn.addClass("btn-outline-primary");
        insertBtn.addClass("btn-light");
        insertBtn.removeClass("btn-warning");
        this.pdfViewer.rotation = 0;
        if (this.currentSignRequestParamses != null && this.currentSignRequestParamses.length > 0 && this.currentSignRequestParamses[0] != null) {
            if (this.forcePageNum) {
                this.pdfViewer.scrollToPage(this.forcePageNum);
            }
        } else {
            this.pdfViewer.scrollToPage(1);
        }
        // this.signPosition.updateScale(this.pdfViewer.scale);
        //this.pdfViewer.promiseToggleFields(false);
        // this.refreshAfterPageChange();
        $("#cross_999999").remove();
        $("#addCommentButton").attr("disabled", false);
        $("#addSpotButton").attr("disabled", false);
        this.showAllPostits();
        $(".sign-space").each(function () {
            $(this).show();
        });
        $('#signLaunchButton').removeClass('d-none');
        $('#addSignButton2').removeClass('d-none');
        $('#refuseLaunchButton').removeClass('d-none');
        $('#trashLaunchButton').removeClass('d-none');
        $('#commentsTools').addClass('d-none');
        $('#signTools').removeClass("d-none");
        this.refreshAfterPageChange();
    }

    disableAllModes() {
        $('#workspace').removeClass('alert-success').removeClass('alert-secondary').removeClass('alert-warning').removeClass('alert-primary');
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        $('#signLaunchButton').addClass('d-none');
        $('#addSignButton2').addClass('d-none');
        $('#refuseLaunchButton').addClass('d-none');
        $("#commentHelp").addClass("d-none");
        $('#commentsTools').hide();
        $('#commentsBar').hide();
        $('#sign-tools').addClass("d-none");
        $('#infos').hide();
        $('#postit').hide();
        $('#refusetools').hide();
        $('#insert-btn-div').hide();
        $('#rotateleft').prop('disabled', true);
        $('#rotateright').prop('disabled', true);
        $('#rotateleft').css('opacity', 0);
        $('#rotateright').css('opacity', 0);
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
            $(this).resizable({
                aspectRatio: true
            });
        });
    }

    enableCommentAdd(e) {
        // $("#addSpotButton").toggleAttribute("disabled", true);
        let saveCommentButton = $('#saveCommentButton');
        let hideCommentButton = $('#hideCommentButton');
        saveCommentButton.unbind();
        hideCommentButton.unbind();
        $('#pdf').mousemove(e => this.moveAction(e));
        let addCommentButton = $("#addCommentButton");
        addCommentButton.toggleClass("btn-primary");
        addCommentButton.toggleClass("btn-outline-light");
        // this.hideComment(e);
        if (this.addCommentEnabled) {
            this.addCommentEnabled = false;
            this.disablePointer();
            $("#addSpotButton").attr("disabled", false);
            $("#divSpotStepNumber").show();
            $(".textLayer").each(function(){
                $(this).removeClass("text-disable-selection");
            });
        } else {
            let postit = $("#postit");
            postit.removeClass("alert-success");
            postit.addClass("alert-warning");
            this.addCommentEnabled = true;
            this.displayCommentPointer();
            $("#divSpotStepNumber").hide();
            $("#postitComment").attr("required", true);
            $("#addSpotButton").attr("disabled", true);
            $(".textLayer").each(function(){
               $(this).addClass("text-disable-selection");
            });
        }
        this.addSpotEnabled = false;
        saveCommentButton.on('click', e => this.saveComment(e));
        hideCommentButton.on('click', e => this.hideComment(e));
    }

    enableSpotAdd(e) {
        $("#commentHelp").remove();
        $("#addSpotButton").attr("disabled", true);
        $("#addCommentButton").attr("disabled", true);
        this.signPosition.addSign(this.pdfViewer.pageNum, false, 999999, null);
    }

    displayCommentPointer() {
        this.pdfViewer.pdfDiv.css('cursor', 'url(' + this.getCommentPointer("\uf075") + '), auto');
    }

    disablePointer() {
        this.pdfViewer.pdfDiv.css('cursor', 'default');
    }

    getCommentPointer(code) {
        let pointerCanvas = document.createElement("canvas");
        pointerCanvas.width = 24;
        pointerCanvas.height = 24;
        let pointerCtx = pointerCanvas.getContext("2d");
        pointerCtx.fillStyle = "#000000";
        pointerCtx.font = "24px FontAwesome";
        pointerCtx.textAlign = "center";
        pointerCtx.textBaseline = "middle";
        pointerCtx.fillText(code, 12, 12);
        return pointerCanvas.toDataURL('image/png');
    }

    changeSpotStep() {
        let stepNumber = $("#spotStepNumber").val();
        $('[id^="liveStep-"]').each(function () {
            $(this).removeClass("bg-success");
            $(this).addClass("bg-white");
        });
        let liveStep = $("#liveStep-" + stepNumber);
        liveStep.removeClass("bg-white");
        liveStep.addClass("bg-success");
    }

    initChangeModeSelector() {
        let data = [];
        if(this.signable) {
            data.push({
                html: '<div style="width: 200px;"><i style="font-size: 0.6rem;" class="fas fa-signature text-success"></i><i class="fas fa-pen text-success pr-2"></i></i> <b>Signature</b></div>',
                text: 'Signature',
                value: 'sign',
                selected: true
            });
        }
        if(this.status === "draft" || this.status === "pending") {
            data.push({
                html: '<div style="width: 200px;"><i class="fas fa-comment text-warning pr-2 m-1"></i><b>Annotation</b></div>',
                text: 'Annotation',
                value: 'comment'
            });
        }
        if(this.status !== "draft" && this.status !== "pending" && this.postits.length > 0) {
            data.push({
                html: '<div style="width: 200px;"><i class="fas fa-comment text-warning pr-2 m-1"></i><b>Voir les annotations</b></div>',
                text: 'Consulter les annotations',
                value: 'comment'
            });
        }
        // data.push({
        //     html: '<div style="width: 200px;"><i class="fas fa-eye text-info pr-2 m-1"></i><b>Mode lecture</b></div>',
        //     text: 'Lecture',
        //     value: 'read'
        // });
        let self = this;
        $("#changeMode1").on("click", function(e) {
            self.changeMode("comment");
        });
        $("#changeMode2").on("click", function(e) {
            self.changeMode("sign");
        });
        if(this.changeModeSelector != null) {
            if(!this.signable) {
                this.enableCommentMode();
            }
        }
    }

    changeMode(e) {
        if(this.ready) {
            let mode = e;
            console.info("change mode to : " + mode);
            if (mode === "sign" && this.signable) {
                this.enableSignMode();
            }
            if (mode === "comment" && this.mode !== "comment") {
                this.enableCommentMode();
            } else {
                if(this.signable) {
                    this.enableSignMode();
                } else {
                    this.enableReadMode();
                }
            }
            if (mode === "read") {
                this.enableReadMode();
            }
        }
    }

    initFormAction() {
        console.debug("debug - " + "eval : " + this.action);
        jQuery.globalEval(this.action);
    }

    autocollapse() {
        let menu = "#ws-tabs";
        let maxWidth = $("#workspace").width() - 100;
        console.info(maxWidth);
        if (this.navWidth >= maxWidth) {
            $(menu + ' .dropdown').removeClass('d-none');
            while (this.navWidth > maxWidth) {
                let children = this.wsTabs.children(menu + ' li:not(:last-child)');
                let count = children.length;
                this.navWidth = this.navWidth - $(children[count - 1]).width();
                console.warn(this.navWidth);

                $(children[count - 1]).prependTo(menu + ' .dropdown-menu');
            }
        } else if (this.navWidth < maxWidth - 300) {
            let collapsed = $(menu + ' .dropdown-menu').children(menu + ' li');
            if (collapsed.length===0) {
                $(menu + ' .dropdown').addClass('d-none');
            }
            collapsed = $(menu + ' .dropdown-menu').children('li');
            let i = 0;
            while (this.navWidth < maxWidth && (this.wsTabs.children(menu + ' li').length > 0) && collapsed.length > i + 1) {
                $(collapsed[i]).insertBefore(this.wsTabs.children(menu + ' li:last-child'));
                this.navWidth = this.navWidth + $(collapsed[i]).width();
                if(this.navWidth >= maxWidth) break;
                i++;
            }

        }
    }
}