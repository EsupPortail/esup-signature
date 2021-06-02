import {PdfViewer} from "../../utils/PdfViewer.js";
import {SignPosition2} from "./SignPosition2.js";
import {WheelDetector} from "../../utils/WheelDetector.js";
import {Message} from "../../../prototypes/Message.js";

export class WorkspacePdf {

    constructor(isPdf, id, dataId, formId, currentSignRequestParams, signImageNumber, currentSignType, signable, postits, currentStepNumber, currentStepId, currentStepMultiSign, workflow, signImages, userName, signType, fields, stepRepeatable, status, csrf, action) {
        console.info("Starting workspace UI");
        this.isPdf = isPdf;
        this.changeModeSelector = null;
        this.action = action;
        this.dataId = dataId;
        this.formId = formId;
        this.currentSignRequestParams = currentSignRequestParams;
        this.currentSignType = currentSignType;
        this.postits = postits;
        this.signable = signable;
        this.signRequestId = id;
        this.signType = signType;
        this.stepRepeatable = stepRepeatable;
        this.status = status;
        this.csrf = csrf;
        this.forcePageNum = null;
        this.first = true;
        for (let i = 0; i < fields.length; i++) {
            let field = fields[i];
            if (field.workflowSteps != null && field.workflowSteps.includes(currentStepNumber) && field.required) {
                this.forcePageNum = field.page;
                break;
            }
        }
        if (this.isPdf) {
            this.pdfViewer = new PdfViewer('/user/signrequests/get-last-file/' + id, signable, currentStepNumber, currentStepId, this.forcePageNum, fields, false);
        }
        this.signPosition = new SignPosition2(
            signType,
            this.currentSignRequestParams,
            signImageNumber,
            signImages,
            userName, signable, this.forcePageNum);
        this.mode = 'sign';
        this.wheelDetector = new WheelDetector();
        this.signLaunchButton = $("#signLaunchButton");
        this.addSpotEnabled = false;
        this.addCommentEnabled = false;
        this.spotCursor = this.getCommentPointer("\uf3c5");
        this.commentCursor = this.getCommentPointer("\uf075");
        this.nextCommand = "none";
        this.initChangeModeSelector();
        this.initListeners();
        this.initDataFields(fields);
        if ((formId == null && workflow == null) || (currentStepMultiSign !== null && currentStepMultiSign)) {
            $("#second-tools").toggleClass("d-none d-flex");
            if($("#ws-tabs").length) {
                $("#workspace").css("margin-top", "212px");
            } else {
                $("#workspace").css("margin-top", "170px");
            }

        }
    }

    initListeners() {
        if (this.isPdf) {
            $('#prev').on('click', e => this.pdfViewer.prevPage());
            $('#next').on('click', e => this.pdfViewer.nextPage());
            $('#saveCommentButton').on('click', e => this.saveComment());
            $('#addCommentButton').on('click', e => this.enableCommentAdd(e));
            $('#addSpotButton').on('click', e => this.enableSpotAdd(e));
            $("#spotStepNumber").on('change', e => this.changeSpotStep());
            // this.signPosition.addEventListener("startDrag", e => this.hideAllPostits());
            // this.signPosition.addEventListener("stopDrag", e => this.showAllPostits());
            this.pdfViewer.addEventListener('ready', e => this.initWorkspace());
            this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
            this.pdfViewer.addEventListener('renderFinished', e => this.refreshAfterPageChange());
            this.pdfViewer.addEventListener('render', e => this.initForm());
            this.pdfViewer.addEventListener('change', e => this.saveData());
            if (this.signable) {
                let visualButton = $('#visualButton');
                if (this.currentSignType !== "pdfImageStamp") {
                    // visualButton.removeClass("d-none");
                    visualButton.on('click', e => this.signPosition.toggleVisual());
                }
            }
            this.wheelDetector.addEventListener("zoomin", e => this.pdfViewer.zoomIn());
            this.wheelDetector.addEventListener("zoomout", e => this.pdfViewer.zoomOut());
            this.wheelDetector.addEventListener("pagetop", e => this.pageTop());
            this.wheelDetector.addEventListener("pagebottom", e => this.pageBottom());

            this.pdfViewer.canvas.addEventListener('click', e => this.clickAction());

            $('#pdf').mousemove(e => this.moveAction(e));

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
        } else {
            this.initLaunchButtons();
        }
        $('#addSignButton').on('click', e => this.addSign(e));

        $('[id^="deleteAttachement-"]').each(function () {
            $(this).on('click', function (e) {
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Confimez la suppression de la pièce jointe ?", function (result) {
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

    addSign(e) {
        this.signPosition.addSign(this.pdfViewer.pageNum);
        // this.pdfViewer.renderPage(this.pdfViewer.pageNum);
    }

    initWorkspace() {
        console.info("init workspace");
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
            if (this.signable && this.currentSignType !== 'visa' && this.currentSignType !== 'hiddenVisa') {
                if (this.mode === 'sign') {
                    // this.signPosition.visualActive = false;
                    // this.signPosition.toggleVisual();
                }
            }
        }
        this.pdfViewer.adjustZoom();
        this.initLaunchButtons();
        this.pdfViewer.removeEventListener('ready');
        // if(this.signPosition.signImages.length === 0 && this.signType !== "visa" && this.signType !== "hiddenVisa") {
        //     this.signPosition.toggleVisual();
        // }
    }

    initLaunchButtons() {
        $("#visaLaunchButton").on('click', e => this.launchSignModal());
        this.signLaunchButton.on('click', e => this.launchSignModal());
        $("#refuseLaunchButton").on('click', function () {
            window.onbeforeunload = null;
        });
    }

    initForm(e) {
        console.info("init form");
        let inputs = $("#signForm :input");
        $.each(inputs, (index, e) => this.listenForChange(e));
         if (this.mode === 'read' || this.mode === 'comment') {
            this.disableForm();
        }

    }

    listenForChange(input) {
        $(input).change(e => this.saveData());
    }

    saveData() {
        this.pdfViewer.page.getAnnotations().then(items => this.pdfViewer.saveValues(items)).then(e => this.pushData(false));
    }

    pushData(redirect) {
        console.debug("push data");
        let formData = new Map();

        let self = this;
        let pdfViewer = this.pdfViewer;

        pdfViewer.dataFields.forEach(function (dataField) {
            let savedField = self.pdfViewer.savedFields.get(dataField.name)
            formData[dataField.name] = savedField;
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

    launchSignModal() {
        console.info("launch sign modal");
        window.onbeforeunload = null;
        let self = this;
        if (this.isPdf) {
            this.pdfViewer.checkForm().then(function (result) {
                if (result === "ok") {
                    if (self.signPosition.signRequestParamses.size === 0 && (self.signType !== "hiddenVisa")) {
                        bootbox.alert("Merci de placer la signature", function () {
                            self.pdfViewer.initSavedValues();
                            if (self.currentSignRequestParams != null && self.currentSignRequestParams[0] != null) {
                                self.pdfViewer.renderPage(self.currentSignRequestParams[0].signPageNumber);
                            }
                        });
                    } else {
                        let enableInfinite = $("#enableInfinite");
                        enableInfinite.unbind();
                        enableInfinite.on("click", function () {
                            $("#infiniteForm").toggleClass("d-none");
                            $("#launchNoInfiniteSignButton").toggle();
                            $("#signCommentNoInfinite").toggle();
                        });
                        let signModal;
                        if (self.stepRepeatable) {
                            signModal = $('#stepRepeatableModal');
                            // $('#launchNoInfiniteSignButton').hide();
                        } else {
                            signModal = $("#signModal");
                        }
                        signModal.on('shown.bs.modal', function () {
                            $("#checkValidateSignButtonEnd").focus();
                            $("#checkValidateSignButtonNext").focus();
                        });
                        signModal.modal('show');
                    }
                }
            });
        } else {
            let signModal;
            if (self.stepRepeatable) {
                signModal = $('#stepRepeatableModal');
                // $('#launchNoInfiniteSignButton').hide();
            } else {
                signModal = $("#signModal");
            }
            signModal.modal('show');
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
            console.debug("disable ");
            console.debug(e);
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
        this.signPosition.updateScales(this.pdfViewer.scale);
        this.refreshAfterPageChange();
    }

    clickAction() {
        if (this.mode === 'sign') {
            this.signPosition.lockSigns();
        } else if (this.mode === 'comment') {
            if (this.addSpotEnabled || this.addCommentEnabled) {
                this.displayDialogBox();
            }
        }
    }

    moveAction(e) {
        if (this.mode === 'sign') {
            // this.signPosition.pointIt(e);
        } else if (this.mode === 'comment') {
            if (this.addSpotEnabled || this.addCommentEnabled) {
                // this.signPosition.pointIt2(e);
            }
        }
    }

    saveComment() {
        let spotStepNumberVal = $("#spotStepNumber");
        if (this.addSpotEnabled && spotStepNumberVal.val() === "") {
            spotStepNumberVal.attr("required", true);
            $("#submitPostit").click();
            return;
        }
        let postitComment = $("#postitComment");
        if (postitComment.val() === '') {
            $("#submitPostit").click();
            return;
        }
        let commentUrlParams = "comment=" + postitComment.val() +
            // "&commentPosX=" + Math.round((parseInt($("#commentPosX").val())) * this.signPosition.fixRatio) +
            // "&commentPosY=" + Math.round((parseInt($("#commentPosY").val())) * this.signPosition.fixRatio) +
            "&commentPageNumber=" + $("#commentPageNumber").val() +
            "&spotStepNumber=" + spotStepNumberVal.val() +
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
        this.pdfViewer.renderPage(postit.pageNumber)
        this.refreshAfterPageChange();
        $('html,body').animate({scrollTop: $('#inDocComment_' + postit.id).css('top').replace('px', '')}, 'slow');
    }

    refreshAfterPageChange() {
        console.debug("refresh comments and sign pos" + this.pdfViewer.pageNum);
        $("div[id^='cross_']").each((index, e) => this.toggleSign(e));
        let self = this;
        this.postits.forEach((comment, iterator) => {
            let postitDiv = $('#inDocComment_' + comment.id);
            let postitButton = $('#postit' + comment.id);
            if (comment.pageNumber === this.pdfViewer.pageNum && this.mode === 'comment') {
                postitDiv.show();
                // postitDiv.css('left', ((parseInt(comment.posX) * this.pdfViewer.scale / this.signPosition.fixRatio) - 18) + "px");
                // postitDiv.css('top', ((parseInt(comment.posY) * this.pdfViewer.scale / this.signPosition.fixRatio) - 48) + "px");
                postitDiv.width(postitDiv.width() * this.pdfViewer.scale);
                postitButton.css("background-color", "#FFC");
                postitDiv.unbind('mouseup');
                postitDiv.on('mouseup', function (e) {
                    e.stopPropagation();
                    bootbox.confirm("Supprimer cette annotation ?", function (result) {
                        if (result) {
                            $.ajax({
                                method: 'DELETE',
                                url: "/user/signrequests/delete-comment/" + self.signRequestId + "/" + comment.id + "/?" + self.csrf.parameterName + "=" + self.csrf.token,
                                success: function () {
                                    document.location.reload();
                                }
                            });
                        }
                    });
                });
            } else {
                postitDiv.hide();
                postitButton.css("background-color", "#EEE");
                postitDiv.unbind('mouseup');
            }
        });
        this.postits.forEach((spot, iterator) => {
            let spotDiv = $('#inDocSpot_' + spot.id);
            if (spot.pageNumber === this.pdfViewer.pageNum && this.mode === 'comment') {
                spotDiv.show();
                // spotDiv.css('left', ((parseInt(spot.posX) * this.pdfViewer.scale / this.signPosition.fixRatio) - 18) + "px");
                // spotDiv.css('top', ((parseInt(spot.posY) * this.pdfViewer.scale / this.signPosition.fixRatio) - 48) + "px");
                spotDiv.width(spotDiv.width() * this.pdfViewer.scale);
                spotDiv.unbind('mouseup');
                spotDiv.on('mouseup', function (e) {
                    e.stopPropagation();
                    bootbox.confirm("Supprimer cet emplacement de signature ?", function (result) {
                        if (result) {
                            $.ajax({
                                method: 'DELETE',
                                url: "/user/signrequests/delete-comment/" + self.signRequestId + "/" + spot.id + "/?" + self.csrf.parameterName + "=" + self.csrf.token,
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
            }
        });
        let postitForm = $("#postit");
        if (postitForm.is(':visible')) {
            postitForm.css('left', (parseInt($("#commentPosX").val()) * this.pdfViewer.scale));
            postitForm.css('top', (parseInt($("#commentPosY").val()) * this.pdfViewer.scale));
            $("#postit :input").each(function () {
                $(this).removeAttr('disabled');
            });
            $("#postit :select").each(function () {
                $(this).removeAttr('disabled');
            });
        }
        this.initFormAction();
    }

    toggleSign(e) {
        let signId = $(e).attr("id").split("_")[1];
        console.log("toggle sign_ " + signId);
        let isCurrentSign = $(e).attr("data-current");
        // let signRequestParams = this.signPosition.signRequestParamses.get(signId);
        let pageNum = this.pdfViewer.pageNum;
        // if (isCurrentSign === "true" || signRequestParams.xPos === -1 || (this.signPosition.firstDrag && this.signPosition.currentSign === signId && isCurrentSign === "true") || (signRequestParams.signPageNumber === this.pdfViewer.pageNum && this.mode === 'sign')) {
        //     this.signPosition.signRequestParamses.get(signId).signPageNumber = pageNum;
        //     if (this.signPosition.visualActive) {
        //         $(e).show();
        //     }
        // } else {
        //     $(e).hide();
        // }
        if (this.first) this.first = false;
    }

    displayDialogBox() {
        let postit = $("#postit");
        if (this.mode !== 'comment' || postit.is(':visible')) {
            return;
        }
        this.signPosition.pointItEnable = false;
        let commentPosX = $("#commentPosX");
        let commentPosY = $('#commentPosY');
        let xOffset = 12;
        let yOffset = 24;
        if (this.addCommentEnabled) xOffset = 0;
        let xPos = (parseInt(commentPosX.val()) + xOffset) / this.pdfViewer.scale;
        let yPos = (parseInt(commentPosY.val()) + yOffset) / this.pdfViewer.scale;
        commentPosX.val(xPos);
        commentPosY.val(yPos);
        postit.css('left', xPos * this.pdfViewer.scale);
        postit.css('top', yPos * this.pdfViewer.scale);
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
    }

    enableReadMode() {
        console.info("enable read mode");
        this.disableAllModes();
        this.mode = 'read';
        localStorage.setItem('mode', 'read');
        this.signPosition.pointItEnable = false;
        $('#readModeButton').toggleClass('btn-outline-secondary');
        $('#rotateleft').prop('disabled', false);
        $('#rotateright').prop('disabled', false);
        $('#rotateleft').css('opacity', 1);
        $('#rotateright').css('opacity', 1);
        this.showAllPostits();
    }

    isFloat(n) {
        return Number(n) === n && n % 1 !== 0;
    }

    toggleCommentMode() {
        if (this.mode === 'comment') {
            this.enableReadMode();
            return;
        }
        this.enableCommentMode()
    }

    enableCommentMode() {
        console.info("enable comments mode");
        localStorage.setItem('mode', 'comment');
        this.disableAllModes();
        $("#postit").removeClass("d-none");
        this.mode = 'comment';
        this.signPosition.pointItEnable = true;
        $('#workspace').toggleClass('alert-warning alert-secondary');
        $('#commentModeButton').toggleClass('btn-outline-warning');
        $('#commentsTools').show();
        this.changeModeSelector.set("comment");
        $('#commentsBar').show();
        $('#infos').show();
        this.pdfViewer.renderPage(1);
        this.pdfViewer.promizeToggleFields(false);
        this.refreshAfterPageChange();
        $(".spot").each(function () {
            $(this).show();
            $(this).css('width', '0px');
        });
        $(".circle").each(function () {
            $(this).show();
            $(this).css('width', '0px');
        })
        $('#hideCommentButton').on('click', e => this.hideComment(e));
    }

    toggleSignMode() {
        if (this.mode === 'sign') {
            this.enableReadMode();
            return;
        }
        this.enableSignMode();
    }

    enableSignMode() {
        console.info("enable sign mode");
        localStorage.setItem('mode', 'sign');
        this.disableAllModes();
        this.mode = 'sign';
        this.signPosition.pointItEnable = false;
        if (this.status === 'pending') {
            $('#workspace').toggleClass('alert-secondary');
        } else {
            $('#workspace').toggleClass('alert-success');
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
            $('#signTools').show();
        }

        $('#infos').show();
        if (this.signPosition.visualActive) {
            $('#pen').removeClass('btn-outline-secondary').addClass('btn-outline-success');
            // this.signPosition.cross.removeClass('d-none');
        }
        this.pdfViewer.rotation = 0;
        if (this.currentSignRequestParams != null && this.currentSignRequestParams.length > 0 && this.currentSignRequestParams[0] != null) {
            if (!this.forcePageNum) {
                this.pdfViewer.renderPage(1);
                let signPage = this.currentSignRequestParams[0].signPageNumber;
                // this.signPosition.getCurrentSignParams().signPageNumber = signPage;
                // this.signPosition.updateCrossPosition();
            } else {
                this.pdfViewer.renderPage(this.forcePageNum);
            }
        } else {
            this.pdfViewer.renderPage(1);
        }
        // this.signPosition.updateScale(this.pdfViewer.scale);
        //this.pdfViewer.promizeToggleFields(false);
        // this.refreshAfterPageChange();
        this.showAllPostits();
    }

    disableAllModes() {
        //this.mode = 'sign';
        $('#workspace').removeClass('alert-success').removeClass('alert-secondary').removeClass('alert-warning');
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        // this.signPosition.crossTools.addClass('d-none');
        $('#commentsTools').hide();
        $('#commentsBar').hide();
        $('#signTools').hide();
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
        this.hideAllPostits();
    }

    pageTop() {
        console.debug("prev page");
        if (this.pdfViewer.pageNum > 1) {
            this.pdfViewer.prevPage();
            window.scrollTo(0, document.body.scrollHeight);
        }
    }

    pageBottom() {
        console.debug("wheel down next page");
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
        $("#addCommentButton").toggleClass("btn-outline-dark");
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
    }

    enableSpotAdd(e) {
        $("#addCommentButton").removeClass("btn-outline-dark");
        $("#addSpotButton").toggleClass("btn-outline-dark");
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
    }

    displayCommentPointer() {
        this.pdfViewer.canvas.style.cursor = 'url(' + this.getCommentPointer("\uf075") + '), auto';
    }

    displaySpotPointer() {
        this.pdfViewer.canvas.style.cursor = 'url(' + this.getCommentPointer("\uf3c5") + '), auto';
    }

    disablePointer() {
        this.pdfViewer.canvas.style.cursor = 'default';
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
        if($("#changeMode").length) {
            this.changeModeSelector = new SlimSelect({
                select: '#changeMode',
                showSearch: false,
                valuesUseText: false, // Use text instead of innerHTML for selected values - default false
                onChange: e => this.changeMode(e),
                data: [
                    {
                        innerHTML: '<div style="width: 200px"><i style="font-size: 0.6rem;" class="fas fa-signature text-success"></i><i class="fas fa-pen text-success pr-2"></i></i> <b>Remplir et signer</b></div>',
                        text: 'Remplir et signer',
                        value: 'sign',
                        selected: true
                    },
                    {
                        innerHTML: '<div style="width: 200px"><i class="fas fa-comment text-warning pr-2"></i> <b>Annoter</b></div>',
                        text: 'Annoter',
                        value: 'comment'
                    },
                    {
                        innerHTML: '<div style="width: 200px"><i class="fas fa-eye pr-2"></i> <b>Mode lecture</b></div>',
                        text: 'Lecture',
                        value: 'read'
                    },
                ]
            });
        }
    }

    changeMode(e) {
        let mode = e.value;
        console.info("change mode to : " + mode);
        if (mode === "sign") {
            this.disableAllModes();
            this.enableSignMode();
        }
        if (mode === "comment" && this.mode !== "comment") {
            this.disableAllModes();
            this.enableCommentMode();
        }
        if (mode === "read") {
            this.disableAllModes();
            this.enableReadMode();
        }
    }


    initFormAction() {
        console.debug("eval : " + this.action);
        jQuery.globalEval(this.action);
    }
}