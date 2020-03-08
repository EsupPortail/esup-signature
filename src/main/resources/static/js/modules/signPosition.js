export class SignPosition {

    pdf = $('#pdf');
    pointerDiv;
    startPosX;
    startPosY;
    signWidth;
    signHeight;
    posX;
    posY;
    pointItMove = false;
    dateActive = false;
    cross = $('#cross');
    borders = $('#borders');
    currentScale = 1;

    constructor(xPos, yPos) {
        this.posX = parseInt(xPos, 10);
        this.posY = parseInt(yPos, 10);
        this.init()
    }

    init() {
        console.log("init signPosition event listeners");
        window.addEventListener("touchmove", e => this.touchmove(e));
        this.cross.on('mousedown', e => this.dragSignature());
        this.cross.on('touchstart', e => this.dragSignature());
        this.cross.on('mouseup', e => this.savePosition());
        this.cross.on('touchend', e => this.savePosition());


    }
    //
    // setScale(scale) {
    //     this.scale = scale;
    // }


    pointIt(e) {
        if(this.pointItEnable) {
            this.pointItMove = true;
            this.posX = e.offsetX ? (e.offsetX) : e.clientX;
            this.posY = e.offsetY ? (e.offsetY) : e.clientY;
            this.updateCrossPosition();
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

    pointIt2(e) {
        if (this.pointItEnable) {
            var pointerCanvas = document.createElement("canvas");
            pointerCanvas.width = 24;
            pointerCanvas.height = 24;
            var pointerCtx = pointerCanvas.getContext("2d");
            pointerCtx.fillStyle = "#000000";
            pointerCtx.font = "24px FontAwesome";
            pointerCtx.textAlign = "center";
            pointerCtx.textBaseline = "middle";
            pointerCtx.fillText("\uf075", 12, 12);
            let dataURL = pointerCanvas.toDataURL('image/png')
            pdf.css('cursor', 'url('+dataURL+'), auto');
            this.posX = e.offsetX ? (e.offsetX)
                : e.clientX - this.pointerDiv.offsetLeft;
            this.posY = e.offsetY ? (e.offsetY)
                : e.clientY - this.pointerDiv.offsetTop;
            document.getElementById("xPos").value = Math.round(this.posX / this.currentScale);
            document.getElementById("yPos").value = Math.round(this.posY / this.currentScale);
        }
    }


    resetPosition() {
        console.log("reset position");
        this.cross.css('left', (this.startPosX * zoom) + "px");
        this.cross.css('top', (this.startPosY * zoom)  + "px");
        document.getElementById("xPos").value = this.startPosX;
        document.getElementById("yPos").value = this.startPosY;
        this.cross.css('pointer-events', 'auto');
        document.body.style.cursor = "default";
        this.pointItEnable = false;
        this.pointItMove = false
    }


    refreshSign(scale) {
        console.log("refresh sign with new scale : " + scale);
        this.cross.css('left',  parseInt(this.cross.css('left')) / this.currentScale * scale);
        this.cross.css('top', parseInt(this.cross.css('top'))  / this.currentScale * scale);
        this.updateSignSize(scale);
        $('#textVisa').css('font-size', 8 * scale);
        this.currentScale = scale;
    }

    resetSign() {
        console.log("reset sign to "  + this.posX + " " + this.posY);
        this.cross.show();
        this.cross.css('left', ((this.posX * this.currentScale) + 15) + "px");
        this.cross.css('top', this.posY * this.currentScale);
        this.updateSignSize(this.currentScale);
        $('#textVisa').css('font-size', 8 * this.currentScale);
    }

    updateSignSize(scale) {
        this.cross.css('backgroundSize', this.signWidth * scale);
        this.cross.css('width', this.signWidth * scale);
        this.cross.css('height', this.signHeight * scale);
        this.borders.css('width', this.signWidth * scale);
        this.borders.css('height', this.signHeight * scale);
    }

    savePosition() {
        if(this.pointItEnable && this.pointItMove) {
            console.log("save position to  :" + Math.round(this.posX * this.scale - 15) + " " + Math.round(this.posY * this.currentScale));
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
            this.cross.css('width', 200);
            this.cross.css('height', this.cross.outerHeight() + 20);
            this.borders.style.width = 200;
            this.borders.style.height = this.borders.outerHeight() + 20;
            this.borders.append("<span id='textDate' class='align-top' style='font-size:" + 8 * this.scale + "px;'>Le "+ moment().format('DD/MM/YYYY HH:mm') +"</span>");
        } else {
            this.dateActive = false;
            this.cross.css('width', 100);
            this.cross.css('height', this.cross.outerHeight() - 20);
            this.borders.style.width = 100;
            this.borders.style.height = this.borders.outerHeight() - 20;
            textDate = document.getElementById("textDate");
            textDate.remove();
        }
    }
}
