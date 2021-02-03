import {PdfViewer} from "../../utils/PdfViewer.js";
import {SignPosition} from "./SignPosition.js";
import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {WheelDetector} from "../../utils/WheelDetector.js";

export class WorkspacePdf {

    constructor(isPdf, id, currentSignRequestParams, currentSignType, signable, postits, currentStepNumber, signImages, userName, signType, fields, stepRepeatable, status, csrf) {
        console.info("Starting workspace UI");
        this.isPdf = isPdf;
        this.currentSignRequestParams =  [ new SignRequestParams(currentSignRequestParams) ];
        this.currentSignType = currentSignType;
        this.postits = postits;
        this.signable = signable;
        this.signRequestId = id;
        this.signType = signType;
        this.stepRepeatable  = stepRepeatable;
        this.status = status;
        this.csrf = csrf;
        this.signPosition = new SignPosition(
            signType,
            this.currentSignRequestParams[0].xPos,
            this.currentSignRequestParams[0].yPos,
            this.currentSignRequestParams[0].signPageNumber,
            signImages,
            userName, signable);
        if(this.isPdf) {
            this.pdfViewer = new PdfViewer('/user/signrequests/get-last-file/' + id, signable, currentStepNumber);
        }
        //this.signPageNumber = document.getElementById('signPageNumber');
        this.mode = 'sign';
        this.xmlHttpMain = new XMLHttpRequest();
        this.wheelDetector = new WheelDetector();
        this.signLaunchButton = $("#signLaunchButton");
        this.initListeners();
        this.initDataFields(fields);
    }

    initListeners() {
        if(this.isPdf) {
            $('#prev').on('click', e => this.pdfViewer.prevPage());
            $('#next').on('click', e => this.pdfViewer.nextPage());
            $('#saveCommentButton').on('click', e => this.saveComment());
            this.signPosition.addEventListener("startDrag", e => this.hideAllPostits());
            this.signPosition.addEventListener("stopDrag", e => this.showAllPostits());
            this.pdfViewer.addEventListener('ready', e => this.initWorkspace());
            this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
            this.pdfViewer.addEventListener('renderFinished', e => this.refreshAfterPageChange());
            this.pdfViewer.addEventListener('render', e => this.initForm());
            if (document.getElementById('commentModeButton') != null) {
                document.getElementById('commentModeButton').addEventListener('click', e => this.toggleCommentMode());
                if (this.signable) {
                    document.getElementById('signModeButton').addEventListener('click', e => this.toggleSignMode());
                    let visualButton = document.getElementById('visualButton')
                    if (this.currentSignType !== "pdfImageStamp") {
                        visualButton.classList.remove("d-none");
                        visualButton.addEventListener('click', e => this.signPosition.toggleVisual());
                    }
                }
                document.getElementById('hideCommentButton').addEventListener('click', e => this.hideComment());
                $("#addSignParams").on('change', e => this.togglePostitMode(e));
            }

            this.wheelDetector.addEventListener("zoomin", e => this.pdfViewer.zoomIn());
            this.wheelDetector.addEventListener("zoomout", e => this.pdfViewer.zoomOut());
            this.wheelDetector.addEventListener("pagetop", e => this.pageTop());
            this.wheelDetector.addEventListener("pagebottom", e => this.pageBottom());

            this.pdfViewer.canvas.addEventListener('mouseup', e => this.clickAction());

            // this.pdfViewer.canvas.addEventListener('mousemove', e => this.moveAction(e));
            $('#pdf').mousemove(e => this.moveAction(e));

            $(".postit-global-close").on('click', function () {
                $(this).parent().toggleClass("postit-small");
            });

            this.postits.forEach((postit, index) => {
                let postitButton = $('#postit' + postit.id);
                postitButton.on('click', e => this.focusComment(postit));
                postitButton.on('mouseover', function (){
                    $('#inDocComment_' + postit.id).addClass('text-danger');
                    postitButton.addClass('circle-border');
                });
                postitButton.on('mouseout', function (){
                    $('#inDocComment_' + postit.id).removeClass('text-danger');
                    postitButton.removeClass('circle-border');
                });
            });
        }

        $('[id^="deleteAttachement_"]').each(function (){
            $(this).on('click', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Confimez la suppression de la pièce jointe ?", function (result) {
                    if(result) {
                        location.href = $(target).attr('href');
                    }
                });
            });
        });

        $('[id^="deleteLink_"]').each(function (){
            $(this).on('click', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Confirmez la suppression du lien ?", function (result) {
                    if(result) {
                        location.href = $(target).attr('href');
                    }
                });
            });
        });

        $("#visaLaunchButton").on('click', e => this.launchSignModal());
        this.signLaunchButton.on('click', e => this.launchSignModal());
        $("#refuseLaunchButton").on('click', function (){
            window.onbeforeunload = null;
        });

    }

    initWorkspace() {
        console.info("init workspace");
        if(localStorage.getItem('mode') === null) {
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
        if(localStorage.getItem('mode') === 'comment') {
            this.enableCommentMode();
        } else {
            this.enableSignMode();
            if(this.signable && this.currentSignType === 'visa') {
                if(this.mode === 'sign') {
                    this.signPosition.toggleVisual();
                }
            }
        }
        this.pdfViewer.adjustZoom();
        this.pdfViewer.removeEventListener('ready');
    }

    initForm(e) {
        console.info("init form");
        $("#signForm :input").each(function () {
            $(this).on('change', e => WorkspacePdf.launchValidate());
        });
        if(this.mode === 'read' || this.mode === 'comment') {
            this.disableForm();
        }
    }

    initDataFields(fields) {
        if(this.pdfViewer) {
            this.pdfViewer.setDataFields(fields);
            if (this.pdfViewer.dataFields.length > 0 && this.pdfViewer.dataFields[0].defaultValue != null) {
                for (let i = 0 ; i < this.pdfViewer.dataFields.length ; i++) {
                    this.pdfViewer.savedFields.set(this.pdfViewer.dataFields[i].name, this.pdfViewer.dataFields[i].defaultValue);
                }
            }
        }
    }

    launchSignModal() {
        console.info("launch sign modal");
        window.onbeforeunload = null;
        if(this.signPosition.getCurrentSignParams().xPos === -1 && this.signType !== "visa") {
            bootbox.alert("Merci de placer la signature");
        } else {
            if (WorkspacePdf.validateForm()) {
                let signModal = null;
                if (this.stepRepeatable) {
                    signModal = $('#stepRepeatableModal');
                    $('#launchSignButton').hide();
                } else {
                    signModal = $("#signModal");
                }
                signModal.on('shown.bs.modal', function(){
                    $("#checkRepeatableButtonEnd").focus();
                    $("#checkRepeatableButtonNext").focus();
                });
                signModal.modal('show');
            }
        }
    }

    static validateForm() {
        let valid = true;
        $("#signForm :input").each(function() {
            let input = $(this).get(0);
            if (!input.checkValidity()) {
                valid = false;
            }
        });
        if(!valid) {
            $("#checkDataSubmit").click();
        }
        return valid;
    }

    disableForm() {
        $("#signForm :input").not(':input[type=button], :input[type=submit], :input[type=reset]').each(function(i, e) {
            console.debug("disable ");
            console.debug(e);
            e.disabled = true;
        });
    }

    static launchValidate() {
        if(!WorkspacePdf.validateForm()) {
            $("#visaLaunchButton").attr('disabled', true);
        } else {
            $("#visaLaunchButton").attr('disabled', false);
        }

    }

    refreshWorkspace() {
        console.info("refresh workspace");
        this.signPosition.updateScale(this.pdfViewer.scale);
        this.refreshAfterPageChange();
    }

    clickAction() {
        if(this.mode === 'sign') {
            this.signPosition.stopDragSignature();
        } else if(this.mode === 'comment') {
            this.displayComment();
        }
    }

    moveAction(e) {
        if(this.mode === 'sign') {
            this.signPosition.pointIt(e);
        } else if(this.mode === 'comment') {
            this.displayCommentPointer();
            this.signPosition.pointIt2(e);
        }
    }

    saveComment() {
        let commentUrlParams = "comment=" + $("#postitComment").val() +
            "&commentPosX=" + Math.round((parseInt($("#commentPosX").val())) * this.signPosition.fixRatio) +
            "&commentPosY=" + Math.round((parseInt($("#commentPosY").val())) * this.signPosition.fixRatio) +
            "&commentPageNumber=" + $("#commentPageNumber").val() +
            "&commentStepNumber=" + $("#commentStepNumber").val() +
            "&addSignParams=" + $("#addSignParams").is(":checked") +
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
        this.signPosition.getCurrentSignParams().signPageNumber = this.pdfViewer.pageNum;
        $("div[id^='sign_']").each((index, e) => this.toggleSign(e));
        let self = this;
        this.postits.forEach((comment, iterator) => {
            let postitDiv = $('#inDocComment_' + comment.id);
            let postitButton = $('#postit' + comment.id);
            if (comment.pageNumber === this.pdfViewer.pageNum && this.mode === 'comment') {
                postitDiv.show();
                postitDiv.css('left', ((parseInt(comment.posX) * this.pdfViewer.scale / this.signPosition.fixRatio) - 18) + "px");
                postitDiv.css('top', ((parseInt(comment.posY) * this.pdfViewer.scale / this.signPosition.fixRatio) - 48) + "px");
                postitDiv.width(postitDiv.width() * this.pdfViewer.scale);
                postitButton.css("background-color", "#FFC");
                postitDiv.unbind('mouseup');
                postitDiv.on('mouseup', function (e) {
                    e.stopPropagation();
                    bootbox.confirm("Supprimer ce commentaire ?", function(result) {
                        if(result) {
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
                spotDiv.css('left', ((parseInt(spot.posX) * this.pdfViewer.scale / this.signPosition.fixRatio) - 18) + "px");
                spotDiv.css('top', ((parseInt(spot.posY) * this.pdfViewer.scale / this.signPosition.fixRatio) - 48) + "px");
                spotDiv.width(spotDiv.width() * this.pdfViewer.scale);
                spotDiv.unbind('mouseup');
                spotDiv.on('mouseup', function (e) {
                    e.stopPropagation();
                    bootbox.confirm("Supprimer cet emplacement de signature ?", function(result) {
                        if(result) {
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
    }

    toggleSign(e) {
        console.log("toggle sign_ " + $(e));
        let signId = $(e).attr("id").split("_")[1];
        let signRequestParams = this.signPosition.signRequestParamses[signId];
        console.log(signRequestParams.signPageNumber + " = " + this.signPosition.getCurrentSignParams().signPageNumber);
        if(signRequestParams.signPageNumber == this.signPosition.getCurrentSignParams().signPageNumber && this.mode === 'sign') {
            $(e).show();
        } else {
            $(e).hide();
        }

    }

    getCommentPointer() {
        let pointerCanvas = document.createElement("canvas");
        pointerCanvas.width = 24;
        pointerCanvas.height = 24;
        let pointerCtx = pointerCanvas.getContext("2d");
        pointerCtx.fillStyle = "#000000";
        pointerCtx.font = "24px FontAwesome";
        pointerCtx.textAlign = "center";
        pointerCtx.textBaseline = "middle";
        pointerCtx.fillText("\uf245", 12, 12);
        return pointerCanvas.toDataURL('image/png');
    }

    displayCommentPointer() {
        this.pdfViewer.canvas.style.cursor = 'url(' + this.getCommentPointer() + '), auto';
    }

    displayComment() {
        let postit = $("#postit");
        if(this.mode !== 'comment' || postit.is(':visible')) {
            return;
        }
        this.signPosition.pointItEnable = false;
        let commentPosX = $("#commentPosX");
        let commentPosY = $('#commentPosY');
        let xPos = parseInt(commentPosX.val()) / this.pdfViewer.scale;
        let yPos = parseInt(commentPosY.val()) / this.pdfViewer.scale;
        commentPosX.val(xPos);
        commentPosY.val(yPos);
        postit.css('left', xPos * this.pdfViewer.scale);
        postit.css('top', yPos * this.pdfViewer.scale);
        $("#postitComment").removeAttr("disabled");
        $("#commentStepNumber").removeAttr("disabled");
        $("#addSignParams").removeAttr("disabled");
        postit.show();
        this.signPosition.stopDragSignature();
    }

    hideComment() {
        if(this.mode !== 'comment') {
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
        this.showAllPostits();
    }

    isFloat(n){
        return Number(n) === n && n % 1 !== 0;
    }

    toggleCommentMode() {
        if(this.mode === 'comment') {
            this.enableReadMode();
            return;
        }
        this.enableCommentMode()
    }

    enableCommentMode() {
        console.info("enable comments mode");
        localStorage.setItem('mode', 'comment');
        this.disableAllModes();
        this.mode = 'comment';
        this.signPosition.pointItEnable = true;
        $('#workspace').toggleClass('alert-warning alert-secondary');
        $('#commentModeButton').toggleClass('btn-outline-warning');
        $('#commentsTools').show();
        $('#infos').show();
        $(".spot").each(function( index ) {
            $(this).show();
        });
        this.pdfViewer.renderPage(1);
        this.pdfViewer.promizeToggleFields(false);
        this.refreshAfterPageChange();
    }

    toggleSignMode() {
        if(this.mode === 'sign') {
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
        if(this.status === 'pending') {
            $('#workspace').toggleClass('alert-secondary');
        } else {
            $('#workspace').toggleClass('alert-success');
        }
        $(".circle").each(function( index ) {
            $(this).hide();
        });
        $(".spot").each(function( index ) {
            $(this).hide();
        });
        $('#signButtons').removeClass('d-none');
        $('#signModeButton').toggleClass('btn-outline-success');
        $('#signTools').show();

        $('#infos').show();
        if(this.signPosition.visualActive) {
            $('#pen').removeClass('btn-outline-secondary').addClass('btn-outline-success');
            this.signPosition.cross.show();
        }
        this.pdfViewer.rotation = 0;
        this.pdfViewer.renderPage(this.currentSignRequestParams[0].signPageNumber);
        this.signPosition.updateScale(this.pdfViewer.scale);
        //this.pdfViewer.promizeToggleFields(false);
        this.refreshAfterPageChange();
        this.showAllPostits();
    }

    disableAllModes() {
        //this.mode = 'sign';
        $('#workspace').removeClass('alert-success').removeClass('alert-secondary').removeClass('alert-warning');
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        this.signPosition.crossTools.addClass('d-none');
        $('#commentsTools').hide();

        $('#signTools').hide();
        this.signPosition.cross.hide();

        $('#infos').hide();
        $('#postit').hide();
        $('#refusetools').hide();
        $('#rotateleft').prop('disabled', true);
        $('#rotateright').prop('disabled', true);
        $('#pdf').css('cursor', 'default');

        this.hideAllPostits();
    }

    pageTop() {
        console.debug("prev page");
        if(this.pdfViewer.pageNum > 1) {
            this.pdfViewer.prevPage();
            window.scrollTo(0, document.body.scrollHeight);
        }
    }

    pageBottom() {
        console.debug("wheel down next page");
        if(this.pdfViewer.pageNum < this.pdfViewer.pdfDoc.numPages) {
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
        });
    }

    togglePostitMode(e) {
        $("#postit").toggleClass("badge-warning badge-success");
        let stepNumber = $(commentStepNumber).val();
        $("#liveStep-" + stepNumber).toggleClass("bg-white bg-success");
    }

}