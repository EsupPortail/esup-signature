import {SignRequestParams} from "../prototypes/SignRequestParams.js";
import {EventFactory} from "./utils/EventFactory.js";

export class SignPosition extends EventFactory {

    constructor(xPos, yPos, signWidth, signHeight, signPageNumber, signImages, userName) {
        super();
        console.info("Starting sign position tools");
        this.currentScale = 1;
        this.signScale = 1;
        this.fixRatio = .75;
        this.currentSign = 0;
        let signRequestParams = new SignRequestParams();
        signRequestParams.xPos = parseInt(xPos, 10) * this.currentScale;
        signRequestParams.yPos = parseInt(yPos, 10) * this.currentScale;
        signRequestParams.signPageNumber = signPageNumber;
        signRequestParams.signImageNumber = 0;
        signRequestParams.signWidth = signWidth * this.fixRatio;
        signRequestParams.signHeight = signHeight * this.fixRatio;
        this.signRequestParamses = [signRequestParams];
        this.signImages = signImages;
        this.userName = userName;
        this.pdf = $('#pdf');
        this.pointItEnable = true;
        this.fontSize = 12;
        this.pointItMove = false;
        this.visualActive = true;
        this.cross = $('#cross');
        this.borders = $('#borders');
        this.signZoomOutButton = $('#signZoomOut');
        this.signZoomInButton = $('#signZoomIn');
        this.signNextImageButton = $('#signNextImage');
        this.signPrevImageButton = $('#signPrevImage');
        this.addSignButton = $('#addSignButton');
        this.removeSignButton = $('#removeSignButton');
        this.changeSignImage(signRequestParams.signImageNumber);
        this.initListeners();
        if(xPos !== 0 && yPos !== 0) {
            this.updateCrossPosition();
            this.cross.css("position", "absolute");
            this.updateSignButtons();
        } else {
            this.cross.css("position", "fixed");
            this.cross.css("margin-left", "270px");
            this.cross.css("margin-top", "180px");
        }
        this.addSignButton.attr("disabled", "disabled");
        this.removeSignButton.attr("disabled", "disabled");
        this.events = {};
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

    initListeners() {
        window.addEventListener("touchmove", e => this.touchIt(e), {passive: false});
        this.initCrossListeners();
        this.signZoomOutButton.on('click', e => this.signZoomOut(e));
        this.signZoomInButton.on('click', e => this.signZoomIn(e));
        this.signNextImageButton.on('click', e => this.signNextImage(e));
        this.signPrevImageButton.on('click', e => this.signPrevImage(e));
        this.addSignButton.on('click', e => this.addSign(e));
        this.removeSignButton.on('click', e => this.removeSign(e));
    }

    initCrossListeners() {
        this.cross.on('mousedown', e => this.dragSignature());
        this.cross.on('touchstart', e => this.dragSignature());
        this.cross.on('mouseup', e => this.stopDragSignature());
        this.cross.on('touchend', e => this.stopDragSignature());
    }

    addSign() {
        console.info("add sign");
        let signRequestParams = new SignRequestParams();
        signRequestParams.xPos = 0;
        signRequestParams.yPos = 0;
        signRequestParams.signPageNumber = this.getCurrentSignParams().signPageNumber;
        signRequestParams.signWidth = this.getCurrentSignParams().signWidth;
        signRequestParams.signHeight = this.getCurrentSignParams().signHeight;
        signRequestParams.signImageNumber = this.getCurrentSignParams().signImageNumber;
        signRequestParams.addDate = false;
        signRequestParams.addName = false;
        this.signRequestParamses.push(signRequestParams);
        let okSign = this.cross.clone();
        okSign.attr("id", "sign_" + this.currentSign)
        okSign.css( "z-index", "2");
        okSign.children().attr("id", "border_" + this.currentSign);
        okSign.children().removeClass("anim-border");
        okSign.children().children().attr("id", "sign_adds_" + this.currentSign)
        okSign.appendTo(this.pdf);
        this.currentSign++;
        this.updateCrossPosition();
        this.cross.css("position", "fixed");
        this.cross.css("margin-left", "270px");
        this.cross.css("margin-top", "180px");
        this.cross.css("margin-top", "180px");
        this.cross.children().children().remove();
        this.addSignButton.attr("disabled", "disabled");
        this.removeSignButton.removeAttr("disabled");
        this.hideButtons();
        let dateButton = $('#dateButton');
        dateButton.removeClass('btn-outline-success');
        dateButton.addClass('btn-outline-dark');
        let nameButton = $('#nameButton');
        nameButton.removeClass('btn-outline-success');
        nameButton.addClass('btn-outline-dark');

    }

    removeSign() {
        this.currentSign--;
        console.info("rollback : sign_" + (this.currentSign));
        this.signRequestParamses.splice(-1,1);
        if(this.signRequestParamses.length < 2) {
            this.removeSignButton.attr("disabled", "disabled");
        }
        $("#cross").remove();
        let oldSign = $("#sign_" + (this.currentSign));
        oldSign.attr("id", "cross");
        this.cross = oldSign;
        this.cross.css( "z-index", "4");
        this.cross.children().attr("id", "borders");
        this.cross.children().addClass("anim-border");
        this.borders = this.cross.children();
        this.cross.css("position", "absolute");
        this.cross.children().children().remove();
        this.updateCrossPosition();
        this.initCrossListeners();
        if(this.getCurrentSignParams().addName) {
            this.getCurrentSignParams().addName = false;
            this.toggleName()
        }
        if(this.getCurrentSignParams().addDate) {
            this.getCurrentSignParams().addDate = false;
            this.toggleDate()
        }
    }

    changeSignImage(imageNum) {
        if(this.signImages != null) {
            let img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
            console.debug("change sign image to " + imageNum);
            this.cross.css("background-image", "url('" + img + "')");
            this.getCurrentSignParams().signImageNumber = imageNum;
            let sizes = this.getImageDimensions(img);
            sizes.then(result => this.changeSignSize(result));
        }
    }

    changeSignSize(result) {
        this.getCurrentSignParams().signWidth = Math.round((result.w / 3) * this.signScale * this.currentScale * this.fixRatio);
        this.getCurrentSignParams().signHeight = Math.round((result.h / 3) * this.signScale * this.currentScale * this.fixRatio);
        this.cross.css('background-size', this.getCurrentSignParams().signWidth + 'px');
        this.updateSignSize();
    }

    getImageDimensions(file) {
        return new Promise (function (resolved) {
            var i = new Image()
            i.onload = function(){
                resolved({w: i.width, h: i.height})
                resolved({w: i.width, h: i.height})
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
        this.updateSignZoom(this.signScale - 0.1);
    }

    signZoomIn(e) {
        this.updateSignZoom(this.signScale + 0.1);
    }

    updateSignZoom(signScale) {
        console.info("sign zoom to : " + signScale);
        this.getCurrentSignParams().signWidth = Math.round(this.getCurrentSignParams().signWidth / this.signScale * signScale);
        this.getCurrentSignParams().signHeight = Math.round(this.getCurrentSignParams().signHeight / this.signScale * signScale);
        this.signScale = signScale;
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
        // this.getCurrentSign().signWidth = this.getCurrentSign().signWidth / this.currentScale * scale;
        // this.getCurrentSign().signHeight = this.getCurrentSign().signHeight / this.currentScale * scale;
        // this.getCurrentSign().setxPos( this.getCurrentSign().xPos / this.currentScale * scale);
        // this.getCurrentSign().setyPos( this.getCurrentSign().yPos / this.currentScale * scale);
        $('div[id^="sign_"]').each((index, e) => this.updateOtherSignPosition(e, scale));
        this.currentScale = scale;
        this.updateCrossPosition();
    }

    updateSignButtons() {
        console.info("update buttons");
        let signZoomIn = $("#signZoomIn");
        let signZoomOut = $("#signZoomOut");

        signZoomIn.css('left', this.getUiXpos() - 35 + "px");
        signZoomIn.css('top', this.getUiYpos() + "px");
        signZoomOut.css('left', this.getUiXpos() - 35 + "px");
        signZoomOut.css('top', this.getUiYpos() + 32 + "px");

        let signPrevImage = $("#signPrevImage");
        let signNextImage = $("#signNextImage");
        signPrevImage.css('left', this.getUiXpos() + (this.getCurrentSignParams().signWidth * this.currentScale / this.fixRatio) + 5 + "px");
        signPrevImage.css('top', this.getUiYpos() + "px");
        signNextImage.css('left', this.getUiXpos() + (this.getCurrentSignParams().signWidth * this.currentScale / this.fixRatio) + 5 + "px");
        signNextImage.css('top', this.getUiYpos() + 32 + "px");
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
        console.info("update sign size");
        this.cross.css('width', this.getCurrentSignParams().signWidth * this.currentScale / this.fixRatio);
        this.cross.css('height', this.getCurrentSignParams().signHeight * this.currentScale / this.fixRatio);
        this.borders.css('width', this.getCurrentSignParams().signWidth * this.currentScale / this.fixRatio);
        this.borders.css('height', this.getCurrentSignParams().signHeight * this.currentScale / this.fixRatio);
        this.cross.css('background-size', this.getCurrentSignParams().signWidth * this.currentScale / this.fixRatio);
        $('#textVisa').css('font-size', this.fontSize * this.currentScale * this.signScale + "px");
        let textDate = $('#textDate');
        textDate.css('font-size', this.fontSize * this.currentScale * this.signScale + "px");
        textDate.css('top', "-" + 30 * this.currentScale * this.signScale + "px");
        let textName = $('#textName');
        textName.css('font-size', this.fontSize * this.currentScale * this.signScale + "px");
        textName.css('top', "-" + 30 * this.currentScale * this.signScale + "px");
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
        document.body.style.cursor = "move";
        this.pointItEnable = true;
        this.hideButtons();
    }

    hideButtons() {
        $('#signZoomIn').addClass('d-none');
        $('#signZoomOut').addClass('d-none');
        $('#signNextImage').addClass('d-none');
        $('#signPrevImage').addClass('d-none');
    }

    showButtons() {
        $('#signZoomIn').removeClass('d-none');
        $('#signZoomOut').removeClass('d-none');
        $('#signNextImage').removeClass('d-none');
        $('#signPrevImage').removeClass('d-none');
    }

    toggleVisual() {
        console.log("toggle visual");
        if(this.visualActive) {
            this.visualActive = false;
            $('#dateButton').prop('disabled', true);
            $('#nameButton').prop('disabled', true);
        } else {
            this.visualActive = true;
            $('#dateButton').prop('disabled', false);
            $('#nameButton').prop('disabled', false);
        }
        this.cross.toggle();
        $('#pen').toggleClass('btn-outline-success btn-outline-dark').children().toggleClass('fa-eye-slash fa-eye');
    }

    toggleDate() {
        console.log("toggle date");
        $('#dateButton').toggleClass('btn-outline-success btn-outline-dark');
        if(!this.getCurrentSignParams().addDate) {
            this.getCurrentSignParams().addDate = true;
                this.borders.append("<span id='textDate' class='align-top sign-text' style='top:-" + this.fontSize * this.currentScale * this.signScale * 2.5 + "px; font-size:" + this.fontSize * this.currentScale * this.signScale + "px;'>Le "+ moment().format('DD/MM/YYYY HH:mm:ss') +"</span>");
        } else {
            this.getCurrentSignParams().addDate = false;
            document.getElementById("textDate").remove();
        }
    }

    toggleName() {
        console.log("toggle name");
        $('#nameButton').toggleClass('btn-outline-success btn-outline-dark');
        if(!this.getCurrentSignParams().addName) {
            this.getCurrentSignParams().addName = true;
            //TODO if visa
            this.borders.prepend("<span id='textName' class='align-top sign-text' style='top:-" + this.fontSize * this.currentScale * this.signScale * 2.5 + "px; font-size:" + this.fontSize * this.currentScale * this.signScale + "px;'>" +
                "Signé par "+ this.userName +"</span>");

        } else {
            this.getCurrentSignParams().addName = false;
            document.getElementById("textName").remove();
        }
    }

}