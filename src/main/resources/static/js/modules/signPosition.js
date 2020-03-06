export class SignPosition {

    pointerDiv;
    startPosX;
    startPosY;
    signWidth;
    signHeight;
    pointItMove = false;

    constructor() {
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
                : e.clientX - pointerDiv.offsetLeft;
            this.posY = e.offsetY ? (e.offsetY)
                : e.clientY - pointerDiv.offsetTop;
            document.getElementById("xPos").value = Math.round(posX / scale);
            document.getElementById("yPos").value = Math.round(posY / scale);
        }
    }


    resetPosition() {
        console.log("out");
        var cross = document.getElementById("cross");
        if(cross != null) {
            cross.style.left = (startPosX * zoom) + "px";
            cross.style.top = (startPosY * zoom)  + "px";
            document.getElementById("xPos").value = startPosX;
            document.getElementById("yPos").value = startPosY;
            cross.style.pointerEvents = "auto";
            document.body.style.cursor = "default";
            pointItEnable = false;
            pointItMove = false
        }
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
        this.cross.css('left', this.posX * this.scale + 15);
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
            startPosX = Math.round(posX * scale - 15);
            startPosY = Math.round(posY * scale);
        }
        cross.style.backgroundColor= 'rgba(0, 255, 0, 0)';
        cross.style.pointerEvents = "auto";
        document.body.style.cursor = "default";
        pointItEnable = false;
        pointItMove = false
    }

    dragSignature() {
        console.log("drag");
        cross.style.pointerEvents = "none";
        pdf.style.pointerEvents = "auto";
        document.body.style.cursor = "move";
        pointItEnable = true;
    }
}