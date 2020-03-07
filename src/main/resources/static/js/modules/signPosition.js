export class SignPosition {

    pdf = document.getElementById("pdf");
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
    scale = 1;

    constructor(xPos, yPos) {
        this.posX = parseInt(xPos, 10);
        this.posY = parseInt(yPos, 10);
        this.init()
    }

    init() {
        window.addEventListener("touchmove", e => this.touchmove(e));
    }

    setScale(scale) {
        this.scale = scale;
    }

    point(e) {
        if(this.mode === 'sign') {
            this.pointIt(e);
        } else if(this.mode === 'comment') {
            this.pointIt2(e);
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

    pointIt(e) {
        if(this.pointItEnable) {
            this.pointItMove = true;
            this.posX = e.offsetX ? (e.offsetX) : e.clientX;
            this.posY = e.offsetY ? (e.offsetY) : e.clientY;
            this.updateCrossPosition();
        }
    }

    updateCrossPosition() {
        console.log("updat cross pos");
        this.cross.style.backgroundColor = 'rgba(0, 255, 0, .5)';
        this.cross.style.left = (posX + 15) + "px";
        this.cross.style.top = posY + "px";
        document.getElementById("xPos").value = Math.round(posX / scale);
        document.getElementById("yPos").value = Math.round(posY / scale);
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
            var dataURL = pointerCanvas.toDataURL('image/png')
            $('#pdf').css('cursor', 'url('+dataURL+'), auto');
            this.posX = e.offsetX ? (e.offsetX)
                : e.clientX - this.pointerDiv.offsetLeft;
            this.posY = e.offsetY ? (e.offsetY)
                : e.clientY - this.pointerDiv.offsetTop;
            document.getElementById("xPos").value = Math.round(posX / scale);
            document.getElementById("yPos").value = Math.round(posY / scale);
        }
    }


    resetPosition() {
        console.log("reset position");
        this.cross.style.left = (this.startPosX * zoom) + "px";
        this.cross.style.top = (this.startPosY * zoom)  + "px";
        document.getElementById("xPos").value = this.startPosX;
        document.getElementById("yPos").value = this.startPosY;
        this.cross.style.pointerEvents = "auto";
        document.body.style.cursor = "default";
        this.pointItEnable = false;
        this.pointItMove = false
    }


    refreshSign() {
        this.cross.css('left',  parseInt(cross.css('left')) / this.oldscale * this.scale);
        this.cross.css('top', parseInt(cross.css('top'))  / this.oldscale * this.scale);
        this.cross.css('backgroundSize', this.signWidth * this.scale);
        this.cross.css('width', this.signWidth * this.scale);
        this.cross.css('height', this.signHeight * this.scale);
        this.borders.css('width', this.signWidth * this.scale);
        this.borders.css('height', this.signHeight * this.scale);
        $('#textVisa').css('font-size', 8 * this.scale);
    }

    resetSign() {
        console.log("reset sign to "  + this.posX + " " + this.posY);
        this.cross.show();
        this.cross.css('left', ((this.posX * this.scale) + 15) + "px");
        this.cross.css('top', this.posY * this.scale);
        this.cross.css('backgroundSize', this.signWidth * this.scale);
        this.cross.css('width', this.signWidth * this.scale);
        this.cross.css('height', this.signHeight * this.scale);
        this.borders.css('width', this.signWidth * this.scale);
        this.borders.css('height', this.signHeight * this.scale);
        $('#textVisa').css('font-size', 8 * this.scale);
    }

    savePosition() {
        if(pointItEnable && pointItMove) {
            console.log("save");
            this.startPosX = Math.round(posX * scale - 15);
            this.startPosY = Math.round(posY * scale);
        }
        this.cross.style.backgroundColor= 'rgba(0, 255, 0, 0)';
        this.cross.style.pointerEvents = "auto";
        document.body.style.cursor = "default";
        this.pointItEnable = false;
        this.pointItMove = false
    }

    dragSignature() {
        console.log("drag");
        this.cross.style.pointerEvents = "none";
        $('#pdf').style.pointerEvents = "auto";
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
            this.cross.style.width = 200;
            this.cross.style.height = this.signPosition.cross.offsetHeight + 20;
            this.borders.style.width = 200;
            this.borders.style.height = borders.offsetHeight + 20;
            this.borders.insertAdjacentHTML("beforeend", "<span id='textDate' class='align-top' style='font-size:" + 8 * this.scale + "px;'>Le "+ moment().format('DD/MM/YYYY HH:mm') +"</span>");
        } else {
            this.dateActive = false;
            this.cross.style.width = 100;
            this.cross.style.height = this.signPosition.cross.offsetHeight - 20;
            this.borders.style.width = 100;
            this.borders.style.height = borders.offsetHeight - 20;
            textDate = document.getElementById("textDate");
            textDate.remove();
        }
    }
}