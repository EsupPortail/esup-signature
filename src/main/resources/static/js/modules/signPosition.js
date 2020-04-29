export class SignPosition {

    constructor(xPos, yPos, signWidth, signHeight, signPageNumber, signImages) {
        console.info("Starting sign position tools");
        this.currentScale = 1;
        this.signScale = 1;
        this.signImageNumber = 0;
        this.fixRatio = .75;
        this.baseXpos = parseInt(xPos, 10) * this.currentScale;
        this.baseYpos = parseInt(yPos, 10) * this.currentScale;
        this.signPageNumber = signPageNumber;
        this.signWidth = signWidth;
        this.signHeight = signHeight;
        this.signImages = signImages;
        this.pdf = $('#pdf');
        this.pointItEnable = true;
        this.pointItMove = false;
        this.dateActive = false;
        this.visualActive = true;
        this.cross = $('#cross');
        this.borders = $('#borders');
        this.signZoomOutButton = $('#signZoomOut');
        this.signZoomInButton = $('#signZoomIn');
        this.signNextImageButton = $('#signNextImage');
        this.signPrevImageButton = $('#signPrevImage');
        this.changeSignImage(this.signImageNumber);
        this.initListeners();
    }

    getUiXpos() {
        return Math.round(this.baseXpos / this.fixRatio);
    }

    getUiYpos() {
        return Math.round(this.baseYpos / this.fixRatio);
    }

    getPdfXpos() {
        return Math.round(this.baseXpos / this.currentScale);
    }

    getPdfYpos() {
        return Math.round(this.baseYpos / this.currentScale);
    }

    initListeners() {
        window.addEventListener("touchmove", e => this.touchmove(e));
        this.cross.on('mousedown', e => this.dragSignature());
        this.cross.on('touchstart', e => this.dragSignature());
        this.cross.on('mouseup', e => this.stopDragSignature());
        this.cross.on('touchend', e => this.stopDragSignature());
        this.signZoomOutButton.on('click', e => this.signZoomOut(e));
        this.signZoomInButton.on('click', e => this.signZoomIn(e));
        this.signNextImageButton.on('click', e => this.signNextImage(e));
        this.signPrevImageButton.on('click', e => this.signPrevImage(e));
    }

    changeSignImage(imageNum) {
        let img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
        console.debug("change sign image to " + imageNum);
        this.cross.css("background-image", "url('" + img + "')");
        this.signImageNumber = imageNum;
        let sizes = this.getImageDimensions(img);
        sizes.then(result  => this.changeSignSize(result));
    }

    changeSignSize(result) {
        this.signWidth = Math.round((result.w / 3) * this.signScale * this.currentScale);
        this.signHeight = Math.round((result.h / 3) * this.signScale * this.currentScale);
        this.cross.css('background-size', this.signWidth + 'px');
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
        if(this.signImageNumber < this.signImages.length - 1) {
            this.signImageNumber++;
        } else {
            this.signImageNumber = 0;
        }
        this.changeSignImage(this.signImageNumber);
    }

    signPrevImage(e) {
        if(this.signImageNumber > 0) {
            this.signImageNumber--;
        } else {
            this.signImageNumber = this.signImages.length - 1;
        }
        this.changeSignImage(this.signImageNumber);
    }

    signZoomOut(e) {
        this.updateSignZoom(this.signScale - 0.1);
    }

    signZoomIn(e) {
        this.updateSignZoom(this.signScale + 0.1);
    }

    updateSignZoom(signScale) {
        console.info("sign zoom to : " + signScale);
        this.signWidth = Math.round(this.signWidth / this.signScale * signScale);
        this.signHeight = Math.round(this.signHeight / this.signScale * signScale);
        this.cross.css('width', this.signWidth);
        this.cross.css('height', this.signHeight);
        this.borders.css('width', this.signWidth);
        this.borders.css('height', this.signHeight);
        this.cross.css('background-size', this.signWidth + 'px');
        this.signScale = signScale;
        this.updateSignButtons(this.getUiXpos() * this.currentScale, this.getUiYpos() * this.currentScale);
    }

    pointIt(e) {
        if(this.pointItEnable) {
            this.pointItMove = true;
            var offset = $("#pdf").offset();
            this.baseXpos = (e.pageX - offset.left) * this.fixRatio;
            this.baseYpos = (e.pageY - offset.top) * this.fixRatio;
            this.updateCrossPosition();
        }
    }

    pointIt2(e) {
        if (this.pointItEnable) {
            console.log("pointit2");
            $('#commentPosX').val(e.offsetX ? (e.offsetX) : e.clientX);
            $('#commentPosY').val(e.offsetY ? (e.offsetY) : e.clientY);
            $('#commentPageNumber').val(this.signPageNumber);
            //this.scalePosition();
        }
    }

    touchmove(e) {
        if (this.pointItEnable) {
            e.preventDefault();
            this.pointItMove = true;
            console.log("touch");
            let rect = pdf.getBoundingClientRect();
            let touch = e.touches[0] || e.changedTouches[0];
            this.posX = touch.pageX;
            this.posY = touch.pageY - (rect.top + window.scrollY);
            this.updateCrossPosition();
        }
    }

    updateScale(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        this.signWidth = this.signWidth / this.currentScale * scale;
        this.signHeight = this.signHeight / this.currentScale * scale;
        this.baseXpos = this.baseXpos / this.currentScale * scale;
        this.baseYpos = this.baseYpos / this.currentScale * scale;
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
        signPrevImage.css('left', this.getUiXpos() + (this.signWidth) + 5 + "px");
        signPrevImage.css('top', this.getUiYpos() + "px");
        signNextImage.css('left', this.getUiXpos() + (this.signWidth) + 5 + "px");
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
        this.cross.css('width', this.signWidth);
        this.cross.css('height', this.signHeight);
        this.borders.css('width', this.signWidth);
        this.borders.css('height', this.signHeight);
        this.cross.css('background-size', this.signWidth);
        $('#textVisa').css('font-size', 8 * this.currentScale + "px");
        $('#textDate').css('font-size', 8 * this.currentScale + "px");
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
            this.borders.append("<span id='textDate' class='align-top' style='font-weight : bold;font-size:" + 8 * this.currentScale + "px;'>Le "+ moment().format('DD/MM/YYYY HH:mm') +"</span>");
        } else {
            this.dateActive = false;
            textDate = document.getElementById("textDate");
            textDate.remove();
        }
    }
}