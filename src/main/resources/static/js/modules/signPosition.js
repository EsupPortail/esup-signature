export class SignPosition {

    constructor(xPos, yPos, signWidth, signHeight, signPageNumber) {
        console.info("Starting sign position tools");
        this.startPosX = parseInt(xPos, 10);
        this.startPosY = parseInt(yPos, 10);
        this.posX = xPos;
        this.posY = yPos;
        this.signPageNumber = signPageNumber;
        this.signWidth = signWidth;
        this.signHeight = signHeight;
        this.pdf = $('#pdf');
        this.pointItEnable = true;
        this.pointItMove = false;
        this.dateActive = false;
        this.visualActive = true;
        this.cross = $('#cross');
        this.borders = $('#borders');
        this.signZoomOutButton = $('#signZoomOut');
        this.signZoomInButton = $('#signZoomIn');
        this.currentScale = 1;
        this.signScale = 1;
        this.initListeners()
    }

    initListeners() {
        window.addEventListener("touchmove", e => this.touchmove(e));
        this.cross.on('mousedown', e => this.dragSignature());
        this.cross.on('touchstart', e => this.dragSignature());
        this.cross.on('mouseup', e => this.savePosition());
        this.cross.on('touchend', e => this.savePosition());
        this.signZoomOutButton.on('click', e => this.signZoomOut(e));
        this.signZoomInButton.on('click', e => this.signZoomIn(e));
    }

    signZoomOut(e) {
        this.updateSignZoom(this.signScale - 0.1);
        this.signScale = this.signScale - 0.1;
    }

    signZoomIn(e) {
        this.updateSignZoom(this.signScale + 0.1);
        this.signScale = this.signScale + 0.1;
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
        // this.cross.css('zoom', signScale);
        // this.cross.css('left', this.posX + "px");
        // this.cross.css('top', this.posY + "px");
    }

    pointIt(e) {
        if(this.pointItEnable) {
            this.pointItMove = true;
            var offset = $("#pdf").offset();
            this.posX = e.pageX - offset.left;
            this.posY = e.pageY - offset.top;
            // this.posX = e.offsetX ? (e.offsetX) : e.clientX;
            // this.posY = e.offsetY ? (e.offsetY) : e.clientY;
            this.updateCrossPosition();
        }
    }

    pointIt2(e) {
        if (this.pointItEnable) {
            console.debug("pointit2");
            this.posX = e.offsetX ? (e.offsetX) : e.clientX;
            this.posY = e.offsetY ? (e.offsetY) : e.clientY;
            $('#commentPosX').val(this.posX);
            $('#commentPosY').val(this.posY);
            $('#commentPageNumber').val(this.signPageNumber);
            this.scalePosition(1);
        }
    }

    scalePosition(scale) {
        this.posX = Math.round(this.posX  / this.currentScale  * scale);
        this.posY = Math.round(this.posY  / this.currentScale  * scale);
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

    // refreshSign(scale) {
    //     console.debug("refresh sign with new scale : " + scale);
    //     this.cross.css('left', parseInt(this.cross.css('left')) / this.currentScale  * scale);
    //     this.cross.css('top', parseInt(this.cross.css('top'))  / this.currentScale * scale);
    //     this.updateSignSize(scale);
    //     $('#textVisa').css('font-size', 8 * scale);
    //     $('#textDate').css('font-size', 8 * scale)
    //     this.currentScale = scale;
    // }

    resetSign() {
        console.info("reset sign to "  + this.startPosX + " " + this.startPosY);
        this.cross.css('left', this.startPosX * this.currentScale);
        this.cross.css('top', this.startPosY * this.currentScale);
        this.updateSignSize(this.currentScale);
        $('#textVisa').css('font-size', 8 * this.currentScale);
    }


    updateCrossPosition() {
        if(this.posX < 0) this.posX = 0;
        if(this.posY < 0) this.posY = 0;
        this.cross.css('backgroundColor', 'rgba(0, 255, 0, .5)');
        this.cross.css('left', this.posX + "px");
        this.cross.css('top', this.posY + "px");
        $("#signZoomIn").css('left', this.posX - 35 + "px");
        $("#signZoomIn").css('top', this.posY + "px");
        $("#signZoomOut").css('left', this.posX - 35 + "px");
        $("#signZoomOut").css('top', this.posY + 30 + "px");
        console.debug("update cross pos to : " + this.posX + " " + this.posY);
        this.scalePosition(1);
        console.debug("save cross pos to : " + this.posX + " " + this.posY);
    }

    updateSignSize(scale) {
        console.info("update sign from scale : " + this.currentScale + " to " + scale);
        this.signWidth = Math.round(this.signWidth / this.currentScale * scale);
        this.signHeight = Math.round(this.signHeight / this.currentScale * scale);
        this.cross.css('left', this.posX * scale);
        this.cross.css('top', this.posY * scale);
        $("#signZoomIn").css('left', this.posX  * scale - 35);
        $("#signZoomIn").css('top', this.posY * scale);
        $("#signZoomOut").css('left', this.posX * scale - 35);
        $("#signZoomOut").css('top', this.posY * scale + 30);
        this.cross.css('width', this.signWidth);
        this.cross.css('height', this.signHeight);
        this.borders.css('width', this.signWidth);
        this.borders.css('height', this.signHeight);
        this.cross.css('background-size', this.signWidth);
        $('#textVisa').css('font-size', 8 * scale + "px");
        $('#textDate').css('font-size', 8 * scale + "px");
        this.currentScale = scale;
    }

    savePosition() {
        if(this.pointItEnable && this.pointItMove) {
            console.info("save position to  :" + this.posX + " " + this.posY);
            this.startPosX = this.posX;
            this.startPosY = this.posY;
        }
        this.cross.css('backgroundColor', 'rgba(0, 255, 0, 0)');
        this.cross.css('pointerEvents', "auto");
        document.body.style.cursor = "default";
        this.pointItEnable = false;
        this.pointItMove = false
    }

    dragSignature() {
        console.log("drag");
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
