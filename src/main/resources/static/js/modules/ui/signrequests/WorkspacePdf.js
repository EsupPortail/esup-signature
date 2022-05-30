import {PdfViewer} from "../../utils/PdfViewer.js?version=@version@";
import {SignPosition} from "./SignPosition.js?version=@version@";
import {WheelDetector} from "../../utils/WheelDetector.js?version=@version@";
import {Message} from "../../../prototypes/Message.js?version=@version@";

export class WorkspacePdf {

    constructor(isPdf, id, dataId, formId, currentSignRequestParamses, signImageNumber, currentSignType, signable, editable, postits, currentStepNumber, currentStepId, currentStepMultiSign, workflow, signImages, userName, authUserName, signType, fields, stepRepeatable, status, csrf, action, notSigned, attachmentAlert, attachmentRequire, isOtp, restore, phone) {
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
        this.signImageNumber = signImageNumber;
        this.restore = restore;
        this.postits = postits;
        this.notSigned = notSigned;
        this.signable = signable;
        this.editable = editable;
        this.signRequestId = id;
        this.signType = signType;
        this.stepRepeatable = stepRepeatable;
        this.status = status;
        this.csrf = csrf;
        this.currentStepMultiSign = currentStepMultiSign;
        this.forcePageNum = null;
        this.pointItEnable = true;
        this.first = true;
        for (let i = 0; i < fields.length; i++) {
            let field = fields[i];
            if (field.workflowSteps != null && field.workflowSteps.includes(currentStepNumber) && field.required) {
                this.forcePageNum = field.page;
                break;
            }
        }
        if (this.isPdf) {
            this.pdfViewer = new PdfViewer('/ws-secure/signrequests/get-last-file/' + id, signable, editable, currentStepNumber, currentStepId, this.forcePageNum, fields, false);
        }
        this.signPosition = new SignPosition(
            signType,
            currentSignRequestParamses,
            signImageNumber,
            signImages,
            userName, authUserName, signable, this.forcePageNum, this.isOtp, this.phone);
        this.currentSignRequestParamses = currentSignRequestParamses;
        this.mode = 'sign';
        this.wheelDetector = new WheelDetector();
        this.addSpotEnabled = false;
        this.addCommentEnabled = false;
        this.spotCursor = this.getCommentPointer("\uf3c5");
        this.commentCursor = this.getCommentPointer("\uf075");
        this.nextCommand = "none";
        this.initChangeModeSelector();
        this.initDataFields(fields);
        this.wsTabs = $("#ws-tabs");
        this.workspace = $("#workspace");
        this.secondTools = $("#second-tools");
        if ((formId == null && workflow == null) || currentSignRequestParamses.length === 0) {
            this.secondTools.toggleClass("d-none d-flex");
            if(this.wsTabs.length) {
                this.autocollapse();
                let self = this;
                $(window).on('resize', function () {
                    self.autocollapse();
                });
                if(this.secondTools.children().length > 0) {
                    this.workspace.css("margin-top", "216px");
                } else {
                    this.workspace.css("margin-top", "178px");
                }
            } else {
                this.workspace.css("margin-top", "170px");
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
            $('#addCommentButton').on('click', e => this.enableCommentAdd(e));
            $('#addSpotButton').on('click', e => this.enableSpotAdd(e));
            $("#spotStepNumber").on('change', e => this.changeSpotStep());
            $("#showComments").on('click', e => this.enableCommentMode());
            // this.signPosition.addEventListener("startDrag", e => this.hideAllPostits());
            // this.signPosition.addEventListener("stopDrag", e => this.showAllPostits());
            this.pdfViewer.addEventListener('ready', e => this.initWorkspace());
            this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
            this.pdfViewer.addEventListener('renderFinished', e => this.refreshAfterPageChange());
            this.pdfViewer.addEventListener('renderFinished', e => this.initForm());
            this.pdfViewer.addEventListener('change', e => this.saveData());
            this.pdfViewer.pdfDiv.on('click', e => this.clickAction(e));

            $(".postit-global-close").on('click', function () {
                $(this).parent().toggleClass("postit-small");
            });

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
        $('#addSignButton').on('click', e => this.addSign());
        $("#addCheck").on("click", e => this.signPosition.addCheckImage(this.pdfViewer.pageNum));
        $("#addTimes").on("click", e => this.signPosition.addTimesImage(this.pdfViewer.pageNum));
        $("#addCircle").on("click", e => this.signPosition.addCircleImage(this.pdfViewer.pageNum));
        $("#addText").on("click", e => this.signPosition.addText(this.pdfViewer.pageNum));

        $('[id^="deleteAttachement-"]').each(function () {
            $(this).on('click', function (e) {
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Confirmez-vous la suppression de la pièce jointe ?", function (result) {
                    if (result) {
                        location.href = $(target).attr('href');
                    }
                });
            });
        });

        $('[id^="deleteLink_"]').each(function () {
            $(this).on('click', function (e) {
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Confirmez la suppression du lien ?", function (result) {
                    if (result) {
                        location.href = $(target).attr('href');
                    }
                });
            });
        });
    }

    initSignFields() {
        for(let i = 0; i < this.currentSignRequestParamses.length; i++) {
            let currentSignRequestParams = this.currentSignRequestParamses[i];
            let signSpaceDiv = $("#signSpace_" + i);
            if (signSpaceDiv.length) {
                signSpaceDiv.remove();
            }
            if (this.mode === "sign" && this.signable) {
                let signSpaceHtml = "<div id='signSpace_" + i + "' title='Emplacement de signature : " + currentSignRequestParams.comment + "' class='sign-field sign-space'></div>";
                $("#pdf").append(signSpaceHtml);
                signSpaceDiv = $("#signSpace_" + i);
                if(currentSignRequestParams.ready == null || !currentSignRequestParams.ready) {
                    signSpaceDiv.html("Cliquez ici pour ajouter votre signature<br>" + currentSignRequestParams.comment);
                }
                if (currentSignRequestParams.ready) {
                    signSpaceDiv.removeClass("sign-field");
                }
                signSpaceDiv.show();
                let offset = $("#page_" + currentSignRequestParams.signPageNumber).offset().top - this.pdfViewer.initialOffset + (10 * (currentSignRequestParams.signPageNumber - 1));
                signSpaceDiv.css("top", Math.round(currentSignRequestParams.yPos * this.pdfViewer.scale + offset));
                signSpaceDiv.css("left", Math.round(currentSignRequestParams.xPos * this.pdfViewer.scale));
                signSpaceDiv.css("width", Math.round(currentSignRequestParams.signWidth * this.pdfViewer.scale / .75) + "px");
                signSpaceDiv.css("height", Math.round(currentSignRequestParams.signHeight * this.pdfViewer.scale / .75) + "px");
                signSpaceDiv.css("font-size", 12 *  this.pdfViewer.scale);
                this.makeItDroppable(signSpaceDiv);
                signSpaceDiv.on("click", e => this.addSign(i));
            }
        }
    }

    addSign(forceSignNumber) {
        // if(this.currentStepMultiSign != null && !this.currentStepMultiSign) {
        //    $("#addSignButton").attr("disabled", true);
        // }
        let targetPageNumber = this.pdfViewer.pageNum;
        let signNum = this.signPosition.currentSignRequestParamsNum;
        if(forceSignNumber != null) {
            signNum = forceSignNumber;
        }
        if(this.currentSignRequestParamses[signNum] != null) {
            targetPageNumber = this.currentSignRequestParamses[signNum].signPageNumber;
            this.signPosition.currentSignRequestParamsNum++;
        }
        if(localStorage.getItem('signNumber') != null && this.restore) {
            this.signImageNumber = localStorage.getItem('signNumber');
        }
        this.signPosition.addSign(targetPageNumber, this.restore, this.signImageNumber, forceSignNumber);
        if((this.signType === "nexuSign" || this.signType === "certSign") && !this.notSigned) {
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
            if (localStorage.getItem('mode') === 'comment') {
                this.enableCommentMode();
            } else {
                this.enableSignMode();
            }
            this.wheelDetector.addEventListener("down", e => this.pdfViewer.checkCurrentPage(e));
            this.wheelDetector.addEventListener("up", e => this.pdfViewer.checkCurrentPage(e));
            this.wheelDetector.addEventListener("zoomin", e => this.pdfViewer.zoomIn());
            this.wheelDetector.addEventListener("zoomout", e => this.pdfViewer.zoomOut());
        }
    }

    initForm() {
        console.info("init form");
        if(!this.formInitialized) {
            this.formInitialized = true;
            let inputs = $("#signForm .annotationLayer :input");
            $.each(inputs, (index, e) => this.listenForChange(e));
            if (this.mode === 'read' || this.mode === 'comment') {
                this.disableForm();
            }
        }
    }

    listenForChange(input) {
        $(input).change(e => this.saveData());
    }

    saveData() {
        for(let i = 1; i < this.pdfViewer.pdfDoc.numPages + 1; i++) {
            this.pdfViewer.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.pdfViewer.saveValues(items)).then(e => this.pushData(false)));
        }
    }

    pushData(redirect) {
        console.debug("debug - " + "push data");
        let formData = new Map();

        let self = this;
        let pdfViewer = this.pdfViewer;

        pdfViewer.dataFields.forEach(function (dataField) {
            formData[dataField.name] = self.pdfViewer.savedFields.get(dataField.name);
        })
        if (redirect || this.dataId != null) {
            let json = JSON.stringify(formData);
            let dataId = $('#dataId');
            $.ajax({
                data: {'formData': json},
                type: 'POST',
                url: '/user/datas/form/' + this.formId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&dataId=' + self.dataId,
                success: function (response) {
                    let message = new Message();
                    message.type = "success";
                    message.text = "Modifications enregistrées";
                    message.object = null;
                    dataId.val(response);
                    if (redirect) {
                        location.href = "/user/datas/" + response + "/update";
                    }
                }
            });
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
                if (this.currentSignRequestParamses[i].ready == null || !this.currentSignRequestParamses[i].ready) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
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
        let page = $(target).parent().parent().parent();
        $('#commentPageNumber').val(page.attr("page-num"));
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

        $.ajax({
            method: 'POST',
            url: "/user/signrequests/comment/" + this.signRequestId + "/?" + commentUrlParams,
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
        let self = this;
        this.postits.forEach((comment, iterator) => {
            if(comment.stepNumber == null) {
                let postitDiv = $('#inDocComment_' + comment.id);
                let postitButton = $('#postit' + comment.id);
                if (this.mode === 'comment') {
                    postitDiv.show();
                    postitDiv.css('left', ((parseInt(comment.posX) * this.pdfViewer.scale)) + "px");
                    let offset = $("#page_" + comment.pageNumber).offset().top - this.pdfViewer.initialOffset + 10;
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
                                        url: "/ws-secure/signrequests/delete-comment/" + self.signRequestId + "/" + comment.id + "/?" + self.csrf.parameterName + "=" + self.csrf.token,
                                        success: function () {
                                            document.location.reload();
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
                // let signSpaceHtml = "<div id='signSpace_" + iterator + "' title='Emplacement de signature " + (iterator + 1) + "' class='sign-field sign-space'></div>";
                // $("#pdf").append(signSpaceHtml);
                let signSpaceDiv = $("#signSpace_" + iterator);
                if (this.mode === 'comment') {
                    spotDiv.show();
                    signSpaceDiv.hide();
                    spotDiv.css('left', ((parseInt(spot.posX) * this.pdfViewer.scale) - 18) + "px");
                    spotDiv.css('top', ((parseInt(spot.posY) * this.pdfViewer.scale) - 48) + "px");
                    spotDiv.width(spotDiv.width() * this.pdfViewer.scale);
                    spotDiv.unbind('mouseup');
                    spotDiv.on('mouseup', function (e) {
                        e.stopPropagation();
                        bootbox.confirm("Supprimer cet emplacement de signature ?", function (result) {
                            if (result) {
                                $.ajax({
                                    method: 'DELETE',
                                    url: "/ws-secure/signrequests/delete-comment/" + self.signRequestId + "/" + spot.id + "/?" + self.csrf.parameterName + "=" + self.csrf.token,
                                    success: function () {
                                        document.location.reload();
                                    }
                                });
                            }
                        });
                    });
                } else {
                    spotDiv.hide();
                    spotDiv.unbind('mouseup');
                    if (this.signable) {
                        signSpaceDiv.show();
                        signSpaceDiv.css("top", Math.round(spot.posY * self.pdfViewer.scale / .75));
                        signSpaceDiv.css("left", Math.round(spot.posX * self.pdfViewer.scale / .75));
                        signSpaceDiv.css("width", Math.round(150 * self.pdfViewer.scale / .75) + "px");
                        signSpaceDiv.css("height", Math.round(75 * self.pdfViewer.scale / .75) + "px");
                        signSpaceDiv.css("font-size", 14 * self.pdfViewer.scale);
                        if (!self.isThereSign(signSpaceDiv)) {
                            signSpaceDiv.text("Vous devez placer une signature ici");
                        }
                        this.makeItDroppable(signSpaceDiv);
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
        this.initFormAction();
        this.initSignFields();
        $("div[id^='cross_']").each((index, e) => this.toggleSign(e));
    }

    makeItDroppable(signSpaceDiv) {
        let self = this;
        signSpaceDiv.droppable({
            tolerance: 'touch',
            accept: ".drop-sign",
            drop: function (event, ui) {
                $(this).removeClass("sign-field");
                $(this).addClass("sign-field-dropped");
                $(this).text("");
                for (let i = 0; i < self.signPosition.signRequestParamses.size; i++) {
                    let signRequestParams = Array.from(self.signPosition.signRequestParamses.values())[i];
                    let cross = signRequestParams.cross;
                    if (cross.attr("id") === ui.draggable.attr("id")) {
                        let offset = ($("#page_" + signRequestParams.signPageNumber).offset().top) - self.pdfViewer.initialOffset + (10 * (signRequestParams.signPageNumber));
                        signRequestParams.yPos = (Math.round(parseInt(signSpaceDiv.css("top")) - offset) / self.pdfViewer.scale);
                        signRequestParams.xPos = Math.round(parseInt(signSpaceDiv.css("left")) / self.pdfViewer.scale);
                        signRequestParams.applyCurrentSignRequestParams();
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
            signRequestParams.hide();
        }
        if (this.first) this.first = false;
    }

    displayDialogBox() {
        $('#pdf').unbind("mousemove");
        let postit = $("#postit");
        if (this.mode !== 'comment' || postit.is(':visible')) {
            return;
        }
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
        postit.css('left', xPos * this.pdfViewer.scale);
        let offset = $("#page_" + commentPageNumber).offset().top - this.pdfViewer.initialOffset;
        postit.css('top', yPos * this.pdfViewer.scale + offset);
        $("#postitComment").removeAttr("disabled");
        $("#spotStepNumber").removeAttr("disabled");
        $("#addSignParams").removeAttr("disabled");
        postit.show();
        this.signPosition.lockSigns();
        // this.signPosition.stopDragSignature(true);
    }

    hideComment(e) {
        e.stopPropagation();
        if (this.mode !== 'comment') {
            return;
        }
        this.signPosition.pointItEnable = true;
        $("#postit").hide();
        $('#pdf').mousemove(e => this.moveAction(e));
    }

    toggleCommentMode() {
        if (this.mode === 'comment') {
            this.enableReadMode();
            return;
        }
        this.enableCommentMode()
    }


    toggleSignMode() {
        if (this.mode === 'sign') {
            this.enableReadMode();
            return;
        }
        this.enableSignMode();
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
        this.showAllPostits();
    }

    enableCommentMode() {
        console.info("enable comments mode");
        localStorage.setItem('mode', 'comment');
        $("#postitHelp").remove();
        this.disableAllModes();
        $("#postit").removeClass("d-none");
        $("#commentHelp").removeClass("d-none");
        this.mode = 'comment';
        this.signPosition.pointItEnable = true;
        $('#workspace').addClass('alert-warning');
        $('#commentModeButton').toggleClass('btn-outline-warning');
        $('#commentsTools').show();
        if (this.changeModeSelector != null) {
            this.changeModeSelector.set("comment");
        }
        $('#commentsBar').show();
        $('#infos').show();
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
    }

    enableSignMode() {
        console.info("enable sign mode");
        localStorage.setItem('mode', 'sign');
        this.disableAllModes();
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

        if(this.signType !== 'hiddenVisa') {
            let signTools = $('#sign-tools');
            signTools.removeClass("d-none");
            signTools.addClass("d-flex");
        }

        $('#infos').show();
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
        this.showAllPostits();
    }

    disableAllModes() {
        //this.mode = 'sign';
        $('#workspace').removeClass('alert-success').removeClass('alert-secondary').removeClass('alert-warning').removeClass('alert-primary');
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        $("#commentHelp").addClass("d-none");
        // this.signPosition.crossTools.addClass('d-none');
        $('#commentsTools').hide();
        $('#commentsBar').hide();
        let signTools = $('#sign-tools');
        signTools.addClass("d-none");
        signTools.removeClass("d-flex");
        // this.signPosition.cross.addClass('d-none');
        $('#infos').hide();
        $('#postit').hide();
        $('#refusetools').hide();
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
        })
        $(".sign-space").each(function () {
            $(this).hide();
        });
        this.hideAllPostits();
    }

    pageTop() {
        console.debug("debug - " + "prev page");
        if (this.pdfViewer.pageNum > 1) {
            this.pdfViewer.prevPage();
            window.scrollTo(0, document.body.scrollHeight);
        }
    }

    pageBottom() {
        console.debug("debug - " + "next page");
        if (this.pdfViewer.pdfDoc != null && this.pdfViewer.pageNum < this.pdfViewer.pdfDoc.numPages) {
            this.pdfViewer.nextPage();
        }
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
        });
    }

    enableCommentAdd(e) {
        let saveCommentButton = $('#saveCommentButton');
        let hideCommentButton = $('#hideCommentButton');
        saveCommentButton.unbind();
        hideCommentButton.unbind();
        $('#pdf').mousemove(e => this.moveAction(e));
        let addCommentButton = $("#addCommentButton");
        addCommentButton.toggleClass("btn-primary");
        addCommentButton.toggleClass("btn-outline-dark");
        $("#addSpotButton").removeClass("btn-outline-dark");
        this.hideComment(e);
        if (this.addCommentEnabled) {
            this.addCommentEnabled = false;
            this.disablePointer();
        } else {
            let postit = $("#postit");
            postit.removeClass("alert-success");
            postit.addClass("alert-warning");
            this.addCommentEnabled = true;
            this.displayCommentPointer();
            $("#divSpotStepNumber").hide();
            $("#postitComment").attr("required", true);
        }
        this.addSpotEnabled = false;
        saveCommentButton.on('click', e => this.saveComment(e));
        hideCommentButton.on('click', e => this.hideComment(e));
    }

    enableSpotAdd(e) {
        let saveCommentButton = $('#saveCommentButton');
        let hideCommentButton = $('#hideCommentButton');
        saveCommentButton.unbind();
        hideCommentButton.unbind();
        $("#addCommentButton").removeClass("btn-outline-dark");
        let addSpotButton = $("#addSpotButton");
        addSpotButton.toggleClass("btn-primary");
        addSpotButton.toggleClass("btn-outline-dark");
        this.hideComment(e);
        if (this.addSpotEnabled) {
            this.addSpotEnabled = false;
            this.disablePointer();
        } else {
            let postit = $("#postit");
            postit.addClass("alert-success");
            postit.removeClass("alert-warning");
            $("#divSpotStepNumber").show();
            this.addSpotEnabled = true;
            $("#postitComment").removeAttr("required");
            this.displaySpotPointer();
        }
        this.addCommentEnabled = false;
        saveCommentButton.on('click', e => this.saveComment(e));
        hideCommentButton.on('click', e => this.hideComment(e));
    }

    displayCommentPointer() {
        this.pdfViewer.pdfDiv.css('cursor', 'url(' + this.getCommentPointer("\uf075") + '), auto');
    }

    displaySpotPointer() {
        this.pdfViewer.pdfDiv.css('cursor', 'url(' + this.getCommentPointer("\uf3c5") + '), auto');
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
                innerHTML: '<div style="width: 200px;"><i style="font-size: 0.6rem;" class="fas fa-signature text-success"></i><i class="fas fa-pen text-success pr-2"></i></i> <b>Remplir et signer</b></div>',
                text: 'Remplir et signer',
                value: 'sign',
                selected: true
            });
        }
        if(this.status === "draft" || this.status === "pending") {
            data.push({
                innerHTML: '<div style="width: 200px;"><i class="fas fa-comment text-warning pr-2 m-1"></i><b>Annoter</b></div>',
                text: 'Annoter',
                value: 'comment'
            });
        }
        if(this.status !== "draft" && this.status !== "pending" && this.postits.length > 0) {
            data.push({
                innerHTML: '<div style="width: 200px;"><i class="fas fa-comment text-warning pr-2 m-1"></i><b>Voir les annotations</b></div>',
                text: 'Consulter les annotations',
                value: 'comment'
            });
        }
        data.push({
            innerHTML: '<div style="width: 200px;"><i class="fas fa-eye text-info pr-2 m-1"></i><b>Mode lecture</b></div>',
            text: 'Lecture',
            value: 'read'
        });

        if($("#changeMode").length) {
            this.changeModeSelector = new SlimSelect({
                select: '#changeMode',
                showSearch: false,
                valuesUseText: false,
                onChange: e => this.changeMode(e),
                data: data
            });
        }
        if(this.changeModeSelector != null) {
            if(this.signable) {
                this.changeModeSelector.set("sign");
            } else {
                this.changeModeSelector.set("read");
            }
        }
    }

    changeMode(e) {
        let mode = e.value;
        console.info("change mode to : " + mode);
        if (mode === "sign" && this.signable) {
            this.enableSignMode();
        }
        if (mode === "comment" && this.mode !== "comment") {
            this.enableCommentMode();
        }
        if (mode === "read") {
            this.enableReadMode();
        }
    }

    initFormAction() {
        console.debug("debug - " + "eval : " + this.action);
        jQuery.globalEval(this.action);
    }

    autocollapse() {
        var menu = "#ws-tabs";
        var maxHeight = 50;
        var navHeight = this.wsTabs.innerHeight();
        if (navHeight >= maxHeight) {
            $(menu + ' .dropdown').removeClass('d-none');
            while (navHeight > maxHeight) {
                //  add child to dropdown
                var children = this.wsTabs.children(menu + ' li:not(:last-child)');
                var count = children.length;
                $(children[count - 1]).prependTo(menu + ' .dropdown-menu');
                navHeight = this.wsTabs.innerHeight();
            }
        }
        else {
            var collapsed = $(menu + ' .dropdown-menu').children(menu + ' li');
            if (collapsed.length===0) {
                $(menu + ' .dropdown').addClass('d-none');
            }
            while (navHeight < maxHeight && (this.wsTabs.children(menu + ' li').length > 0) && collapsed.length > 0) {
                //  remove child from dropdown
                collapsed = $(menu + ' .dropdown-menu').children('li');
                $(collapsed[0]).insertBefore(this.wsTabs.children(menu + ' li:last-child'));
                navHeight = this.wsTabs.innerHeight();
            }

            if (navHeight > maxHeight) {
                this.autocollapse();
            }

        }
    }
}