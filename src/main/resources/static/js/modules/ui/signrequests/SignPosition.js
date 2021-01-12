import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {EventFactory} from "../../utils/EventFactory.js";

export class SignPosition extends EventFactory {

    constructor(signType, xPos, yPos, signPageNumber, signImages, userName) {
        super();
        console.info("Starting sign positioning tools");
        this.cross = $('#cross_0');
        this.crossTools = $('#crossTools_0');
        this.borders = $('#borders_0');
        this.signType = signType;
        this.currentScale = 1;
        this.fixRatio = .75;
        this.extraWidth = 0;
        this.currentSign = 0;
        let signRequestParams = new SignRequestParams();
        this.signImages = signImages;
        if(this.signImages != null && this.signImages.length > 0) {
            signRequestParams.xPos = parseInt(xPos, 10) * this.currentScale;
            signRequestParams.yPos = parseInt(yPos, 10) * this.currentScale;
            signRequestParams.signPageNumber = signPageNumber;
        }
        this.signRequestParamses = [signRequestParams];
        this.userName = userName;
        this.pdf = $('#pdf');
        this.pointItEnable = true;
        this.fontSize = 12;
        this.pointItMove = false;
        this.visualActive = true;
        this.signZoomOutButton = $('#signZoomOut_0');
        this.signZoomInButton = $('#signZoomIn_0');
        this.signNextImageButton = $('#signNextImage_0');
        this.signPrevImageButton = $('#signPrevImage_0');
        this.signExtraButton = $('#signExtra_0');
        this.signDropButton = $('#signDrop_0');
        this.addSignButton = $('#addSignButton');
        if(xPos !== 0 && yPos !== 0) {
            this.updateCrossPosition();
            this.cross.css("position", "absolute");
            this.updateSignButtons();
            this.addSignButton.removeAttr("disabled");
        } else {
            this.cross.css("position", "fixed");
            this.cross.css("margin-left", "270px");
            this.cross.css("margin-top", "180px");
        }
        this.events = {};
        if(this.signType === "visa") {
            this.addText();
            $('#extraButton').hide();
        }
        if(this.signType !== "visa") {
            $(document).ready(e => this.toggleExtraInfos());
        }
        this.initListeners();
    }

    initListeners() {
        window.addEventListener("touchmove", e => this.touchIt(e), {passive: false});
        this.initCrossListeners();
        this.initCrossToolsListeners();
        this.addSignButton.on('click', e => this.addSign(e));
    }

    initCrossListeners() {
        this.borders.on('mousedown', e => this.dragSignature());
        this.borders.on('touchstart', e => this.dragSignature());
        this.borders.on('mouseup', e => this.stopDragSignature());
        this.borders.on('touchend', e => this.stopDragSignature());
    }

    initCrossToolsListeners() {
        this.signZoomOutButton.on('click', e => this.signZoomOut(e));
        this.signZoomInButton.on('click', e => this.signZoomIn(e));
        this.signNextImageButton.on('click', e => this.signNextImage(e));
        this.signPrevImageButton.on('click', e => this.signPrevImage(e));
        this.signExtraButton.on('click', e => this.toggleExtraInfos());
        this.signDropButton.on('click', e => this.removeSign(e));
    }

    unbindCrossToolsListeners() {
        this.signZoomOutButton.unbind();
        this.signZoomInButton.unbind();
        this.signNextImageButton.unbind();
        this.signPrevImageButton.unbind();
        this.signExtraButton.unbind();
        this.signDropButton.unbind();
    }

    getCurrentSignParams() {
        return this.signRequestParamses[this.currentSign];
    }

    getUiXpos() {
        return Math.round(this.getCurrentSignParams().xPos * this.currentScale / this.fixRatio);
    }

    getUiYpos() {
        return Math.round(this.getCurrentSignParams().yPos * this.currentScale / this.fixRatio);
    }

    getPdfXpos() {
        return Math.round(this.getCurrentSignParams().xPos / this.currentScale);
    }

    getPdfYpos() {
        return Math.round(this.getCurrentSignParams().yPos / this.currentScale);
    }

    addSign() {
        console.info("add sign");
        let signRequestParams = new SignRequestParams();
        let currentSign = this.currentSign;
        signRequestParams.xPos = 0;
        signRequestParams.yPos = 0;
        signRequestParams.signPageNumber = this.getCurrentSignParams().signPageNumber;
        signRequestParams.addExtra = this.getCurrentSignParams().addExtra;
        signRequestParams.signScale = this.getCurrentSignParams().signScale;
        signRequestParams.addDate = false;
        signRequestParams.addName = false;
        this.signRequestParamses.push(signRequestParams);
        let okSign = this.cross.clone();
        okSign.attr("id", "cross_" + currentSign)
        okSign.css( "z-index", "2");
        okSign.children().removeClass("anim-border");
        okSign.appendTo(this.pdf);
        okSign.on("click", e => this.switchSign(e));
        this.currentSign++;
        this.updateCrossPosition();
        this.cross.css("position", "fixed");
        this.cross.css("margin-left", "270px");
        this.cross.css("margin-top", "180px");
        this.cross.css("margin-top", "180px");
        this.cross.attr("id", "cross_" + (currentSign + 1));
        this.cross.children().each(function (e) {
            console.log($(this));
            $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + (currentSign + 1));
        });
        this.cross.children().children().each(function (e) {
            console.log($(this));
            $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + (currentSign + 1));
        });
        this.signZoomOutButton = $('#signZoomOut_' + (currentSign + 1));
        this.signZoomInButton = $('#signZoomIn_' + (currentSign + 1));
        this.signNextImageButton = $('#signNextImage_' + (currentSign + 1));
        this.signPrevImageButton = $('#signPrevImage_' + (currentSign + 1));
        this.signExtraButton = $('#signExtra_' + (currentSign + 1));
        this.addSignButton.attr("disabled", "disabled");
        this.hideButtons();
        let dateButton = $('#dateButton');
        dateButton.removeClass('btn-outline-success');
        dateButton.addClass('btn-outline-dark');
        let nameButton = $('#nameButton');
        nameButton.removeClass('btn-outline-success');
        nameButton.addClass('btn-outline-dark');
        this.changeSignImage(0);
    }

    switchSign(e) {
        let changeCross = $(e.currentTarget);
        let currentSign = changeCross.attr("id").split("_")[1];
        console.info("switch to " + currentSign);
        this.borders.removeClass("anim-border");
        this.borders.unbind();
        changeCross.unbind();
        this.cross.on("click", e => this.switchSign(e));
        this.currentSign = currentSign;
        this.cross = $('#cross_' + currentSign);
        this.crossTools = $('#crossTools_' + currentSign);
        this.borders = $('#borders_' + currentSign);
        this.borders.addClass("anim-border");
        this.initCrossListeners();
        this.unbindCrossToolsListeners();
        this.signZoomOutButton = $('#signZoomOut_' + currentSign);
        this.signZoomInButton = $('#signZoomIn_' + currentSign);
        this.signNextImageButton = $('#signNextImage_' + currentSign);
        this.signPrevImageButton = $('#signPrevImage_' + currentSign);
        this.signExtraButton = $('#signExtra_' + currentSign);
        this.signDropButton = $('#signDrop_' + currentSign);
        this.initCrossToolsListeners();
    }

    removeSign(e) {
        if(this.signRequestParamses.length > 1) {
            let dropCross = $(e.currentTarget);
            let dropId = dropCross.attr("id").split("_")[1];
            console.info("rollback : sign_" + (this.currentSign));
            this.signRequestParamses.splice(dropId, 1);
            $("#cross_" + dropId).remove();
            this.currentSign = this.signRequestParamses.length - 1;
        }
    }

    changeSignImage(imageNum) {
        if(this.signImages != null) {
            console.debug("change sign image to " + imageNum);
            let img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
            this.cross.css("background-image", "url('" + img + "')");
            let sizes = this.getImageDimensions(img);
            sizes.then(result => this.changeSignSize(result));
        }
    }

    changeSignSize(result) {
        this.getCurrentSignParams().signWidth = Math.round((result.w + this.extraWidth) * this.getCurrentSignParams().signScale * this.currentScale * this.fixRatio);
        this.getCurrentSignParams().signHeight = Math.round((result.h) * this.getCurrentSignParams().signScale * this.currentScale * this.fixRatio);
        this.updateSignSize();
    }

    getImageDimensions(file) {
        return new Promise (function (resolved) {
            var i = new Image()
            i.onload = function(){
                resolved({w: i.width / 3, h: i.height / 3})
            };
            i.src = file
        })
    }

    signNextImage(e) {
        if(this.getCurrentSignParams().signImageNumber < this.signImages.length - 1) {
            this.getCurrentSignParams().signImageNumber++;
        } else {
            this.getCurrentSignParams().signImageNumber = 0;
        }
        this.changeSignImage(this.getCurrentSignParams().signImageNumber);
    }

    signPrevImage(e) {
        if(this.getCurrentSignParams().signImageNumber > 0) {
            this.getCurrentSignParams().signImageNumber--;
        } else {
            this.getCurrentSignParams().signImageNumber = this.signImages.length - 1;
        }
        this.changeSignImage(this.getCurrentSignParams().signImageNumber);
    }

    signZoomOut(e) {
        this.updateSignZoom(this.getCurrentSignParams().signScale - 0.1);
    }

    signZoomIn(e) {
        this.updateSignZoom(this.getCurrentSignParams().signScale + 0.1);
    }

    updateSignZoom(signScale) {
        console.info("sign zoom to : " + signScale);
        this.getCurrentSignParams().signWidth = Math.round(this.getCurrentSignParams().signWidth / this.getCurrentSignParams().signScale * signScale);
        this.getCurrentSignParams().signHeight = Math.round(this.getCurrentSignParams().signHeight / this.getCurrentSignParams().signScale * signScale);
        this.getCurrentSignParams().signScale = signScale;
        this.updateSignSize();
        this.updateSignButtons();
    }

    pointIt(e) {
        if(this.pointItEnable) {
            this.pointItMove = true;
            var offset = $("#pdf").offset();
            this.getCurrentSignParams().setxPos( (e.pageX - offset.left) / this.currentScale * this.fixRatio);
            this.getCurrentSignParams().setyPos( (e.pageY - offset.top) / this.currentScale * this.fixRatio);
            this.updateCrossPosition();
        }
    }

    pointIt2(e) {
        if (this.pointItEnable) {
            console.log("pointit2");
            $('#commentPosX').val(e.offsetX ? (e.offsetX) : e.clientX);
            $('#commentPosY').val(e.offsetY ? (e.offsetY) : e.clientY);
            $('#commentPageNumber').val(this.getCurrentSignParams().signPageNumber);
        }
    }

    touchIt(e) {
        if (this.pointItEnable) {
            e.preventDefault();
            this.pointItMove = true;
            console.log("touch");
            let rect = pdf.getBoundingClientRect();
            let touch = e.touches[0] || e.changedTouches[0];
            this.getCurrentSignParams().setxPos( (touch.pageX - (rect.left)) / this.currentScale * this.fixRatio);
            this.getCurrentSignParams().setyPos( (touch.pageY - (rect.top + window.scrollY)) / this.currentScale * this.fixRatio);
            this.updateCrossPosition();
        }
    }

    updateScale(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        $('div[id^="sign_"]').each((index, e) => this.updateOtherSignPosition(e, scale));
        this.currentScale = scale;
        this.updateCrossPosition();
    }

    updateSignButtons() {
        console.debug("update buttons");
        this.crossTools.css('left', 0  + "px");
        this.crossTools.css('top', -45 + "px");
    }

    updateCrossPosition() {
        console.debug("update cross pos to : " + this.getUiXpos() + " " + this.getUiYpos());
        console.debug("update sign pos to : " + this.getCurrentSignParams().xPos + " " + this.getCurrentSignParams().yPos);
        if(this.posX < 0) this.posX = 0;
        if(this.posY < 0) this.posY = 0;
        // this.cross.css('backgroundColor', 'rgba(0, 255, 0, .5)');
        this.cross.css('left', this.getUiXpos() + "px");
        this.cross.css('top', this.getUiYpos() + "px");
        this.updateSignSize();
    }

    updateOtherSignPosition(sign, scale) {
        let newTop = parseInt($(sign).css("top"), 10) / this.currentScale * scale;
        let newLeft = parseInt($(sign).css("left"), 10) / this.currentScale * scale;
        $(sign).css('top', newTop + "px");
        $(sign).css('left', newLeft + "px");
        let newWidth = parseInt($(sign).css("width"), 10) / this.currentScale * scale;
        let newHeight = parseInt($(sign).css("height"), 10) / this.currentScale * scale;
        $(sign).css('width', newWidth + "px");
        $(sign).css('height', newHeight + "px");
        $(sign).css('background-size', newWidth);

    }

    updateSignSize() {
        console.debug("update sign size " + this.getCurrentSignParams().signWidth);
        this.cross.css('width', (this.getCurrentSignParams().signWidth / this.fixRatio * this.currentScale));
        this.cross.css('height', (this.getCurrentSignParams().signHeight / this.fixRatio * this.currentScale));
        this.borders.css('width', (this.getCurrentSignParams().signWidth / this.fixRatio * this.currentScale));
        this.borders.css('height', (this.getCurrentSignParams().signHeight / this.fixRatio * this.currentScale));
        this.cross.css('background-size', (this.getCurrentSignParams().signWidth - (this.extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio);
        $('#textVisa').css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        let textDate = $('#textDate');
        textDate.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textDate.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        let textName = $('#textName');
        textName.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textName.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        let textExtra = $('#textExtra_' + this.currentSign);
        textExtra.css('margin-left', (this.getCurrentSignParams().signWidth - (this.extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio + "px");
        textExtra.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textExtra.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        this.updateSignButtons();
    }

    stopDragSignature() {
        console.info("stop drag");
        this.fireEvent('stopDrag', ['ok']);
        this.cross.css('backgroundColor', 'rgba(0, 255, 0, .0)');
        this.cross.css('pointerEvents', "auto");
        document.body.style.cursor = "default";
        if(this.pointItEnable) {
            this.updateSignButtons();
            this.showButtons();
        }
        this.pointItEnable = false;
        this.pointItMove = false
        $('body').removeClass('disable-div-selection cursor-move');
        this.addSignButton.removeAttr("disabled");
    }

    dragSignature() {
        console.info("start drag");
        this.fireEvent('startDrag', ['ok']);
        this.cross.css('pointerEvents', "none");
        if(this.cross.css('position') !== 'absolute') {
            this.cross.css('top', window.scrollY);
            this.posY = window.scrollY;
        }
        this.cross.css('position', "absolute");
        this.cross.css('margin-left', 0);
        this.cross.css('margin-top', 0);
        this.posY = window.scrollY;
        this.pdf.css('pointerEvents', "auto");
        $('body').addClass('disable-div-selection cursor-move');

        this.pointItEnable = true;
        this.hideButtons();
    }

    hideButtons() {
        this.crossTools.addClass('d-none');
    }

    showButtons() {
        this.crossTools.removeClass('d-none');
    }

    toggleVisual() {
        console.log("toggle visual");
        if(this.visualActive) {
            this.visualActive = false;
            this.hideButtons();
        } else {
            this.visualActive = true;
        }
        this.cross.toggle();
        $('#pen').toggleClass('btn-outline-success btn-outline-dark').children().toggleClass('fa-eye-slash fa-eye');
    }

    addText() {
        console.log("toggle date");
        $('#dateButton').toggleClass('btn-outline-success btn-outline-dark');
        this.borders.append("<span id='textName' class='align-top visa-text' style='top:-" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale * 3 + "px; font-size:" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px;'>" +
            "Visé par " + this.userName + "</span>");
        this.borders.append("<span id='textDate' class='align-top visa-text' style='top:-" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale * 3 + "px; font-size:" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px;'>Le " + moment().format('DD/MM/YYYY HH:mm:ss') + "</span>");
    }

    toggleExtraInfos() {
        console.log("toggle extra");
        $('#extraButton').toggleClass('btn-outline-success btn-outline-dark');
        if(this.getCurrentSignParams().addExtra) {
            this.getCurrentSignParams().addExtra = false;
            this.extraWidth = 0;
            this.getCurrentSignParams().signWidth = this.getCurrentSignParams().signWidth - 200;
            document.getElementById("textExtra_" + this.currentSign).remove();
        } else {
            this.getCurrentSignParams().addExtra = true;
            this.extraWidth = 200;
            this.getCurrentSignParams().signWidth = this.getCurrentSignParams().signWidth + 200;
            let signTypeText = "Signature calligraphique";
            if(this.signType === "certSign" || this.signType === "nexuSign") {
                signTypeText = "Signature électronique";
            }
            this.borders.append("<span id='textExtra_" + this.currentSign + "' class='align-top visa-text' style='margin-left: " + this.extraWidth * this.currentScale + "px; font-size:" + this.fontSize * this.currentScale * this.signScale + "px;'>" +
                signTypeText +
                "<br>" +
                "Signé par " + this.userName +
                "<br>" +
                "Le " + moment().format('DD/MM/YYYY HH:mm:ss [GMT] Z') +
                "</span>");
        }
        this.changeSignImage(this.getCurrentSignParams().signImageNumber);
    }

}