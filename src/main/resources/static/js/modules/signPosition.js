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
        this.currentScale = 1;
        this.init()
    }

    init() {
        window.addEventListener("touchmove", e => this.touchmove(e));
        this.cross.on('mousedown', e => this.dragSignature());
        this.cross.on('touchstart', e => this.dragSignature());
        this.cross.on('mouseup', e => this.savePosition());
        this.cross.on('touchend', e => this.savePosition());


    }

    pointIt(e) {
        if(this.pointItEnable) {
            this.pointItMove = true;
            this.posX = e.offsetX ? (e.offsetX) : e.clientX;
            this.posY = e.offsetY ? (e.offsetY) : e.clientY;
            this.updateCrossPosition();
        }
    }

    pointIt2(e) {
        if (this.pointItEnable) {
            console.debug("pointit2");
            this.posX = e.offsetX ? (e.offsetX) : e.clientX;
            this.posY = e.offsetY ? (e.offsetY) : e.clientY;
            document.getElementById("xPos").value = Math.round(this.posX + 15 / this.currentScale);
            document.getElementById("yPos").value = Math.round(this.posY / this.currentScale);
        }
    }

    touchmove(e) {
        if (this.pointItEnable) {
            e.preventDefault();
            this.pointItMove = true;
            console.log("touch");
            var rect = pdf.getBoundingClientRect();
            var touch = e.touches[0] || e.changedTouches[0];
            this.posX = touch.pageX;
            this.posY = touch.pageY - (rect.top + window.scrollY);
            this.updateCrossPosition();
        }
    }

    updateCrossPosition() {
        console.debug("update cross pos to : " + (this.posX + 15) + " " + (this.posY));
        this.cross.css('backgroundColor', 'rgba(0, 255, 0, .5)');
        this.cross.css('left', (this.posX + 15) + "px");
        this.cross.css('top', this.posY + "px");
        document.getElementById("xPos").value = Math.round(this.posX / this.currentScale);
        document.getElementById("yPos").value = Math.round(this.posY / this.currentScale);
    }

    resetPosition() {
        console.log("reset position");
        this.cross.css('left', (this.startPosX * zoom) + "px");
        this.cross.css('top', (this.startPosY * zoom)  + "px");
        this.posX.value = this.startPosX;
        this.posY.value = this.startPosY;
        this.cross.css('pointer-events', 'auto');
        document.body.style.cursor = "default";
        this.pointItEnable = false;
        this.pointItMove = false
    }


    refreshSign(scale) {
        console.debug("refresh sign with new scale : " + scale);
        this.cross.css('left',  ((parseInt(this.cross.css('left')) - (15)) / this.currentScale)  * scale + (15));
        this.cross.css('top', parseInt(this.cross.css('top'))  / this.currentScale * scale);
        this.updateSignSize(scale);
        $('#textVisa').css('font-size', 8 * scale);
        this.currentScale = scale;
    }

    resetSign() {
        console.info("reset sign to "  + this.startPosX + " " + this.startPosY);
        this.cross.css('left', ((this.startPosX * this.currentScale) + 15) + "px");
        this.cross.css('top', this.startPosY * this.currentScale);
        this.updateSignSize(this.currentScale);
        $('#textVisa').css('font-size', 8 * this.currentScale);
    }

    updateSignSize(scale) {
        this.cross.css('width', this.cross.width() / this.currentScale * scale);
        this.cross.css('height', this.cross.height() / this.currentScale * scale);
        this.borders.css('width', this.borders.width() / this.currentScale * scale);
        this.borders.css('height', this.borders.height() / this.currentScale * scale);

        this.cross.css('background-size', parseInt(this.cross.css('background-size')) / this.currentScale * scale);
    }

    savePosition() {
        if(this.pointItEnable && this.pointItMove) {
            console.info("save position to  :" + Math.round(this.posX * this.currentScale - 15) + " " + Math.round(this.posY * this.currentScale));
            this.startPosX = Math.round(this.posX * this.currentScale - 15);
            this.startPosY = Math.round(this.posY * this.currentScale);
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
        pdf.style.pointerEvents = "auto";
        document.body.style.cursor = "move";
        this.pointItEnable = true;
    }

    toggleVisual() {
        console.log("toggle visual");
        if(this.visualActive) {
            this.visualActive = false;
            $('#clock').prop('disabled', true);
        } else {
            this.visualActive = true;
            $('#clock').prop('disabled', false);
        }
        this.cross.toggle();
        $('#pen').toggleClass('btn-outline-success btn-outline-dark').children().toggleClass('fa-eye-slash fa-eye');
    }

    toggleDate() {
        console.log("toggle date");
        $('#clock').toggleClass('btn-outline-success btn-outline-dark');
        var textDate;
        if(!this.dateActive) {
            this.dateActive = true;
            this.borders.append("<span id='textDate' class='align-top' style='font-weight : bold;font-size:" + 8 * this.currentScale + "px;line-height:" + 8 * this.currentScale + "px;'>Le "+ moment().format('DD/MM/YYYY HH:mm') +"</span>");
        } else {
            this.dateActive = false;
            textDate = document.getElementById("textDate");
            textDate.remove();
        }
    }
}
