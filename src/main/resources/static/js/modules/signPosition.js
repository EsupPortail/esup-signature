import {SignRequestParams} from "../prototypes/signRequestParams.js";

export class SignPosition {

    constructor(xPos, yPos, signWidth, signHeight, signPageNumber, signImages) {
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
        // this.baseXpos = parseInt(xPos, 10) * this.currentScale;
        // this.baseYpos = parseInt(yPos, 10) * this.currentScale;
        // this.signPageNumber = signPageNumber;
        // this.signWidth = signWidth;
        // this.signHeight = signHeight;
        this.signImages = signImages;
        this.pdf = $('#pdf');
        this.pointItEnable = true;
        this.fontSize = 12;
        this.pointItMove = false;
        this.dateActive = false;
        this.visualActive = true;
        this.cross = $('#cross');
        this.borders = $('#borders');
        this.signZoomOutButton = $('#signZoomOut');
        this.signZoomInButton = $('#signZoomIn');
        this.signNextImageButton = $('#signNextImage');
        this.signPrevImageButton = $('#signPrevImage');
        this.addSignButton = $('#addSignButton');
        this.changeSignImage(signRequestParams.signImageNumber);
        this.initListeners();
        if(xPos !== 0 && yPos !== 0) {
            this.updateCrossPosition();
            this.cross.css("position", "absolute");
            this.updateSignButtons();
        } else {
            this.cross.css("position", "fixed");
            this.cross.css("margin-left", "270px");
            this.cross.css("margin-top", "135px");
        }
    }

    getCurrentSign() {
        return this.signRequestParamses[this.currentSign];
    }

    getUiXpos() {
        return Math.round(this.getCurrentSign().xPos / this.fixRatio);
    }

    getUiYpos() {
        return Math.round(this.getCurrentSign().yPos / this.fixRatio);
    }

    getPdfXpos() {
        return Math.round(this.getCurrentSign().xPos / this.currentScale);
    }

    getPdfYpos() {
        return Math.round(this.getCurrentSign().yPos / this.currentScale);
    }

    initListeners() {
        window.addEventListener("touchmove", e => this.touchIt(e));
        this.cross.on('mousedown', e => this.dragSignature());
        this.cross.on('touchstart', e => this.dragSignature());
        this.cross.on('mouseup', e => this.stopDragSignature());
        this.cross.on('touchend', e => this.stopDragSignature());
        this.signZoomOutButton.on('click', e => this.signZoomOut(e));
        this.signZoomInButton.on('click', e => this.signZoomIn(e));
        this.signNextImageButton.on('click', e => this.signNextImage(e));
        this.signPrevImageButton.on('click', e => this.signPrevImage(e));
        this.addSignButton.on('click', e => this.addSign(e));
    }

    addSign() {
        let signRequestParams = new SignRequestParams();
        signRequestParams.xPos = 0;
        signRequestParams.yPos = 0;
        signRequestParams.signPageNumber = this.getCurrentSign().signPageNumber;
        signRequestParams.signWidth = this.getCurrentSign().signWidth;
        signRequestParams.signHeight = this.getCurrentSign().signHeight;
        signRequestParams.signImageNumber = this.getCurrentSign().signImageNumber;
        this.signRequestParamses.push(signRequestParams);
        let okSign = this.cross.clone();
        okSign.attr("id", "sign_" + this.currentSign)
        okSign.children().removeClass("anim-border");
        okSign.appendTo(this.pdf);
        this.currentSign++;
        this.updateCrossPosition();
        this.updateSignSize();
        this.cross.css("position", "fixed");
        this.cross.css("margin-left", "270px");
        this.cross.css("margin-top", "135px");
    }

    changeSignImage(imageNum) {
        if(this.signImages != null) {
            let img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
            console.debug("change sign image to " + imageNum);
            this.cross.css("background-image", "url('" + img + "')");
            this.getCurrentSign().signImageNumber = imageNum;
            let sizes = this.getImageDimensions(img);
            sizes.then(result => this.changeSignSize(result));
        }
    }

    changeSignSize(result) {
        this.getCurrentSign().signWidth = Math.round((result.w / 3) * this.signScale * this.currentScale * this.fixRatio);
        this.getCurrentSign().signHeight = Math.round((result.h / 3) * this.signScale * this.currentScale * this.fixRatio);
        this.cross.css('background-size', this.getCurrentSign().signWidth + 'px');
        this.updateSignSize();
    }

    getImageDimensions(file) {
        return new Promise (function (resolved) {
            var i = new Image()
            i.onload = function(){
                resolved({w: i.width, h: i.height})
            };
            i.src = file
        })
    }

    signNextImage(e) {
        if(this.getCurrentSign().signImageNumber < this.signImages.length - 1) {
            this.getCurrentSign().signImageNumber++;
        } else {
            this.getCurrentSign().signImageNumber = 0;
        }
        this.changeSignImage(this.getCurrentSign().signImageNumber);
    }

    signPrevImage(e) {
        if(this.getCurrentSign().signImageNumber > 0) {
            this.getCurrentSign().signImageNumber--;
        } else {
            this.getCurrentSign().signImageNumber = this.signImages.length - 1;
        }
        this.changeSignImage(this.getCurrentSign().signImageNumber);
    }

    signZoomOut(e) {
        this.updateSignZoom(this.signScale - 0.1);
    }

    signZoomIn(e) {
        this.updateSignZoom(this.signScale + 0.1);
    }

    updateSignZoom(signScale) {
        console.info("sign zoom to : " + signScale);
        $('#textVisa').css('font-size', this.fontSize * this.currentScale * signScale + "px");
        $('#textDate').css('font-size', this.fontSize * this.currentScale * signScale + "px");
        this.getCurrentSign().signWidth = Math.round(this.getCurrentSign().signWidth / this.signScale * signScale);
        this.getCurrentSign().signHeight = Math.round(this.getCurrentSign().signHeight / this.signScale * signScale);
        this.cross.css('width', this.getCurrentSign().signWidth / this.fixRatio);
        this.cross.css('height', this.getCurrentSign().signHeight / this.fixRatio);
        this.borders.css('width', this.getCurrentSign().signWidth / this.fixRatio);
        this.borders.css('height', this.getCurrentSign().signHeight / this.fixRatio);
        this.cross.css('background-size', this.getCurrentSign().signWidth / this.fixRatio + 'px');
        this.signScale = signScale;
        this.updateSignButtons(this.getUiXpos() * this.currentScale, this.getUiYpos() * this.currentScale);
    }

    pointIt(e) {
        if(this.pointItEnable) {
            this.pointItMove = true;
            var offset = $("#pdf").offset();
            this.getCurrentSign().setxPos( (e.pageX - offset.left) * this.fixRatio);
            this.getCurrentSign().setyPos( (e.pageY - offset.top) * this.fixRatio);
            this.updateCrossPosition();
        }
    }

    pointIt2(e) {
        if (this.pointItEnable) {
            console.log("pointit2");
            $('#commentPosX').val(e.offsetX ? (e.offsetX) : e.clientX);
            $('#commentPosY').val(e.offsetY ? (e.offsetY) : e.clientY);
            $('#commentPageNumber').val(this.getCurrentSign().signPageNumber);
            //this.scalePosition();
        }
    }

    touchIt(e) {
        if (this.pointItEnable) {
            //e.preventDefault();
            this.pointItMove = true;
            console.log("touch");
            let rect = pdf.getBoundingClientRect();
            let touch = e.touches[0] || e.changedTouches[0];
            this.getCurrentSign().setxPos( touch.pageX * this.fixRatio);
            this.getCurrentSign().setyPos( (touch.pageY - (rect.top + window.scrollY)) * this.fixRatio);
            this.updateCrossPosition();
        }
    }

    updateScale(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        this.getCurrentSign().signWidth = this.getCurrentSign().signWidth / this.currentScale * scale;
        this.getCurrentSign().signHeight = this.getCurrentSign().signHeight / this.currentScale * scale;
        this.getCurrentSign().setxPos( this.getCurrentSign().xPos / this.currentScale * scale);
        this.getCurrentSign().setyPos( this.getCurrentSign().yPos / this.currentScale * scale);
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
        signPrevImage.css('left', this.getUiXpos() + (this.getCurrentSign().signWidth / this.fixRatio) + 5 + "px");
        signPrevImage.css('top', this.getUiYpos() + "px");
        signNextImage.css('left', this.getUiXpos() + (this.getCurrentSign().signWidth / this.fixRatio) + 5 + "px");
        signNextImage.css('top', this.getUiYpos() + 32 + "px");
    }

    updateCrossPosition() {
        console.info("update cross pos to : " + this.getUiXpos() + " " + this.getUiYpos());
        if(this.posX < 0) this.posX = 0;
        if(this.posY < 0) this.posY = 0;
        this.cross.css('backgroundColor', 'rgba(0, 255, 0, .5)');
        this.cross.css('left', this.getUiXpos() + "px");
        this.cross.css('top', this.getUiYpos() + "px");
        this.updateSignSize();
    }

    updateSignSize() {
        this.cross.css('width', this.getCurrentSign().signWidth / this.fixRatio);
        this.cross.css('height', this.getCurrentSign().signHeight / this.fixRatio);
        this.borders.css('width', this.getCurrentSign().signWidth / this.fixRatio);
        this.borders.css('height', this.getCurrentSign().signHeight / this.fixRatio);
        this.cross.css('background-size', this.getCurrentSign().signWidth / this.fixRatio);
        $('#textVisa').css('font-size', this.fontSize * this.currentScale * this.signScale + "px");
        $('#textDate').css('font-size', this.fontSize * this.currentScale * this.signScale + "px");
        this.updateSignButtons(this.getUiXpos(), this.getUiYpos());
    }

    stopDragSignature() {
        console.info("stop drag");
        this.cross.css('backgroundColor', 'rgba(0, 255, 0, 0)');
        this.cross.css('pointerEvents', "auto");
        document.body.style.cursor = "default";
        this.pointItEnable = false;
        this.pointItMove = false
    }

    dragSignature() {
        console.info("start drag");
        this.cross.css('pointerEvents', "none");
        this.cross.css('position', "absolute");
        this.cross.css('margin-left', 0);
        this.cross.css('margin-top', 0);
        $('#signZoomIn').removeClass('d-none');
        $('#signZoomOut').removeClass('d-none');
        $('#signNextImage').removeClass('d-none');
        $('#signPrevImage').removeClass('d-none');
        this.pdf.css('pointerEvents', "auto");
        document.body.style.cursor = "move";
        this.pointItEnable = true;
    }

    toggleVisual() {
        console.log("toggle visual");
        if(this.visualActive) {
            this.visualActive = false;
            $('#dateButton').prop('disabled', true);
        } else {
            this.visualActive = true;
            $('#dateButton').prop('disabled', false);
        }
        this.cross.toggle();
        $('#pen').toggleClass('btn-outline-success btn-outline-dark').children().toggleClass('fa-eye-slash fa-eye');
    }

    toggleDate() {
        console.log("toggle date");
        $('#dateButton').toggleClass('btn-outline-success btn-outline-dark');
        var textDate;
        if(!this.dateActive) {
            this.dateActive = true;
            this.borders.append("<span id='textDate' class='align-top' style='font-size:" + this.fontSize * this.currentScale * this.signScale + "px;'>Le "+ moment().format('DD/MM/YYYY HH:mm') +"</span>");
        } else {
            this.dateActive = false;
            textDate = document.getElementById("textDate");
            textDate.remove();
        }
    }
}