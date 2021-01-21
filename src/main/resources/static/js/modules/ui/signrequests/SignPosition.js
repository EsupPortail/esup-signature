import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {EventFactory} from "../../utils/EventFactory.js";

export class SignPosition extends EventFactory {

    constructor(signType, xPos, yPos, signPageNumber, signImages, userName, signable) {
        super();
        console.info("Starting sign positioning tools");
        this.cross = $('#cross_0');
        this.crossTools = $('#crossTools_0');
        this.borders = $('#borders_0');
        this.signType = signType;
        this.currentScale = 1;
        this.fixRatio = .75;
        this.currentSign = "0";
        let signRequestParams = new SignRequestParams();
        this.signImages = signImages;
        if(this.signImages != null && this.signImages.length > 0) {
            signRequestParams.xPos = parseInt(xPos, 10) * this.currentScale;
            signRequestParams.yPos = parseInt(yPos, 10) * this.currentScale;
            signRequestParams.signPageNumber = signPageNumber;
        }
        this.signRequestParamses = new Map();
        this.signRequestParamses.set("0", signRequestParams);
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
        this.signExtraOnTopButton = $('#signExtraOnTop_0');
        this.signDropButton = $('#signDrop_0');
        this.addSignButton = $('#addSignButton');
        if(xPos !== 0 && yPos !== 0) {
            this.updateCrossPosition();
            this.cross.css("position", "absolute");
            // this.updateSignButtons();
            this.addSignButton.removeAttr("disabled");
        } else {
            this.cross.css("position", "fixed");
            this.cross.css("margin-left", "270px");
            this.cross.css("margin-top", "180px");
        }
        this.confirmEnabled = false;
        this.events = {};
        if(this.signType === "visa") {
            this.addText();
            $('#extraButton').hide();
        }
        if(this.signType !== "visa") {
            $(document).ready(e => this.toggleExtraInfos());
        }
        this.signable = signable;
        $('#color-picker').spectrum({
            type: "color",
            showPaletteOnly: true,
            hideAfterPaletteSelect: true,
            change: color => this.changeSignColor(color)
        });
        this.initListeners();
    }

    initListeners() {
        window.addEventListener("touchmove", e => this.touchIt(e), {passive: false});
        this.initCrossListeners();
        this.initCrossToolsListeners();
        this.addSignButton.on('click', e => this.addSign(e));
        if((this.getCurrentSignParams().xPos !== 0 || this.getCurrentSignParams().yPos !== 0) && this.signable) {
            this.enableConfirmLeaveSign();
            this.confirmEnabled = true;
        }
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
        this.signExtraOnTopButton.on('click', e => this.toggleExtraPosition());
        this.signDropButton.on('click', e => this.removeSign(e));
    }

    unbindCrossToolsListeners() {
        this.signZoomOutButton.unbind();
        this.signZoomInButton.unbind();
        this.signNextImageButton.unbind();
        this.signPrevImageButton.unbind();
        this.signExtraButton.unbind();
        this.signExtraOnTopButton.unbind();
        this.signDropButton.unbind();
    }

    getCurrentSignParams() {
        return this.signRequestParamses.get(this.currentSign + "");
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
        signRequestParams.xPos = 0;
        signRequestParams.yPos = 0;
        signRequestParams.signPageNumber = this.getCurrentSignParams().signPageNumber;
        signRequestParams.addExtra = this.getCurrentSignParams().addExtra;
        signRequestParams.extraWidth = this.getCurrentSignParams().extraWidth;
        signRequestParams.signScale = this.getCurrentSignParams().signScale;
        signRequestParams.addDate = false;
        signRequestParams.addName = false;
        let okSign = this.cross.clone();
        // okSign.attr("id", "cross_" + currentSign)
        okSign.css( "z-index", "2");
        okSign.children().removeClass("anim-border");
        okSign.appendTo(this.pdf);
        okSign.on("click", e => this.switchSignToTarget(e));
        let currentSign = Array.from(this.signRequestParamses.keys())[this.signRequestParamses.size - 1] + 1;
        this.signRequestParamses.set(currentSign + "", signRequestParams);
        this.currentSign = currentSign;
        this.updateCrossPosition();
        this.cross.css("position", "fixed");
        this.cross.css("margin-left", "270px");
        this.cross.css("margin-top", "180px");
        this.cross.css("margin-top", "180px");
        this.cross.attr("id", "cross_" + currentSign);
        this.cross.children().each(function (e) {
            $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + currentSign);
        });
        this.cross.children().children().each(function (e) {
            $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + currentSign);
        });
        this.signZoomOutButton = $('#signZoomOut_' + currentSign);
        this.signZoomInButton = $('#signZoomIn_' + currentSign);
        this.signNextImageButton = $('#signNextImage_' + currentSign);
        this.signPrevImageButton = $('#signPrevImage_' + currentSign);
        this.signExtraButton = $('#signExtra_' + currentSign);
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

    switchSignToTarget(e) {
        let changeCross = $(e.currentTarget);
        this.switchSign(changeCross.attr("id").split("_")[1]);

    }

    switchSign(currentSign) {
        console.info("switch to " + currentSign);
        this.borders.removeClass("anim-border");
        this.borders.unbind();
        this.cross.on("click", e => this.switchSignToTarget(e));
        this.currentSign = currentSign;
        this.cross = $('#cross_' + currentSign);
        this.cross.unbind();
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
        if(this.signRequestParamses.size > 1) {
            let dropCross = $(e.currentTarget);
            let dropId = dropCross.attr("id").split("_")[1];
            console.info("drop : sign_" + (this.currentSign));
            this.signRequestParamses.delete(dropId);
            $("#cross_" + dropId).remove();
            this.currentSign = Array.from(this.signRequestParamses.keys())[this.signRequestParamses.size - 1];
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
        this.getCurrentSignParams().signWidth = Math.round((result.w + this.getCurrentSignParams().extraWidth) * this.getCurrentSignParams().signScale * this.currentScale * this.fixRatio);
        this.getCurrentSignParams().signHeight = Math.round((result.h + this.getCurrentSignParams().extraHeight) * this.getCurrentSignParams().signScale * this.currentScale * this.fixRatio);
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
        if(signScale > 0.2 && signScale < 2) {
            this.getCurrentSignParams().signWidth = Math.round(this.getCurrentSignParams().signWidth / this.getCurrentSignParams().signScale * signScale);
            this.getCurrentSignParams().signHeight = Math.round(this.getCurrentSignParams().signHeight / this.getCurrentSignParams().signScale * signScale);
            this.getCurrentSignParams().signScale = signScale;
            this.updateSignSize();
            // this.updateSignButtons();
        }
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
    //
    // updateSignButtons() {
    //     console.debug("update buttons");
    //     this.crossTools.css('left', 0  + "px");
    //     this.crossTools.css('top', -45 + "px");
    // }

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
        this.cross.css('background-size', (this.getCurrentSignParams().signWidth - (this.getCurrentSignParams().extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio);
        $('#textVisa').css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        let textDate = $('#textDate');
        textDate.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textDate.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        let textName = $('#textName');
        textName.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textName.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        let textExtra = $('#textExtra_' + this.currentSign);
        if(!this.getCurrentSignParams().extraOnTop) {
            textExtra.css('margin-left', (this.getCurrentSignParams().signWidth - (this.getCurrentSignParams().extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio + "px");
        }
        textExtra.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textExtra.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        // this.updateSignButtons();
    }

    stopDragSignature() {
        console.info("stop drag");
        this.fireEvent('stopDrag', ['ok']);
        // this.cross.css('backgroundColor', 'rgba(0, 255, 0, .0)');
        this.cross.css('pointerEvents', "auto");
        document.body.style.cursor = "default";
        if(this.pointItEnable) {
            // this.updateSignButtons();
            this.showButtons();
        }
        this.pointItEnable = false;
        this.pointItMove = false
        $('body').removeClass('disable-div-selection cursor-move');
        this.addSignButton.removeAttr("disabled");
        if(!this.confirmEnabled) {
            this.enableConfirmLeaveSign();
            this.confirmEnabled = true;
        }
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


    enableConfirmLeaveSign() {
        window.onbeforeunload = function(){
            return confirm("Une signature est en cours sur ce document, voulez abandonner les modifications ?");
        };
    }

    toggleExtraInfos() {
        console.log("toggle extra");
        $('#extraButton').toggleClass('btn-outline-success btn-outline-dark');
        if(this.getCurrentSignParams().addExtra) {
            this.removeExtra();
        } else {
            this.getCurrentSignParams().addExtra = true;
            let signTypeText = "";
            if(this.signType === "certSign" || this.signType === "nexuSign") {
                signTypeText = "Signature électronique<br/>";
            }
            let textExtra = $("<span id='textExtra_" + this.currentSign + "' class='align-top visa-text' style='font-size:" + this.fontSize * this.currentScale * this.signScale + "px;user-select: none;\n" +
                "                        -moz-user-select: none;\n" +
                "                        -khtml-user-select: none;\n" +
                "                        -webkit-user-select: none;\n" +
                "                        -o-user-select: none;'>" +
                signTypeText +
                "Signé par " + this.userName +
                "<br>" +
                "Le " + moment().format('DD/MM/YYYY HH:mm:ss') +
                "</span>");

            if(this.getCurrentSignParams().extraOnTop) {
                this.borders.append(textExtra);
                let textExtraHeight = textExtra.height() * this.fixRatio;
                this.getCurrentSignParams().extraHeight = textExtraHeight;
                this.getCurrentSignParams().extraWidth = 0;
                this.getCurrentSignParams().signHeight = this.getCurrentSignParams().signHeight + textExtraHeight;
            } else {
                this.borders.append(textExtra);
                console.log(textExtra);
                let textExtraWidth = textExtra.width();
                this.getCurrentSignParams().extraHeight = 0;
                this.getCurrentSignParams().extraWidth = textExtraWidth;
                this.getCurrentSignParams().signWidth = this.getCurrentSignParams().signWidth + textExtraWidth;
            }
        }
        this.changeSignImage(this.getCurrentSignParams().signImageNumber);
    }

    toggleExtraPosition() {
        this.getCurrentSignParams().extraOnTop = !this.getCurrentSignParams().extraOnTop;
        this.getCurrentSignParams().addExtra = !this.getCurrentSignParams().addExtra;
        this.removeExtra();
        this.toggleExtraInfos();
    }

    removeExtra() {
        this.getCurrentSignParams().addExtra = false;
        this.getCurrentSignParams().extraWidth = 0;
        this.getCurrentSignParams().extraHeight = 0;
        this.getCurrentSignParams().signWidth = this.getCurrentSignParams().signWidth - 200;
        $("#textExtra_" + this.currentSign).remove();
    }

    hexToRgb(hex) {
        // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
        const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
        hex = hex.replace(shorthandRegex, (m, r, g, b) => {
            return r + r + g + g + b + b;
        });

        const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result
            ? [
                parseInt(result[1], 16),
                parseInt(result[2], 16),
                parseInt(result[3], 16),
            ]
            : null;
    }

    changeSignColor(color) {
        console.info("change color");
        const rgb = this.hexToRgb(color.toHexString());

        this.getCurrentSignParams().red = rgb[0];
        this.getCurrentSignParams().green = rgb[1];
        this.getCurrentSignParams().blue = rgb[2];

        let img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[this.getCurrentSignParams().signImageNumber];

        this.cross.css("background-image", "url('" + this.changeColInUri(img, "#000000", color.toHexString()) + "')");

    }

    changeColInUri(data,colfrom,colto) {
        // create fake image to calculate height / width
        var img = document.createElement("img");
        img.src = data;
        img.style.visibility = "hidden";
        document.body.appendChild(img);

        var canvas = document.createElement("canvas");
        canvas.width = img.offsetWidth;
        canvas.height = img.offsetHeight;

        var ctx = canvas.getContext("2d");
        ctx.drawImage(img,0,0);

        // remove image
        img.parentNode.removeChild(img);

        // do actual color replacement
        var imageData = ctx.getImageData(0,0,canvas.width,canvas.height);
        var data = imageData.data;

        var rgbfrom = this.hexToRGB(colfrom);
        var rgbto = this.hexToRGB(colto);

        var r,g,b;
        for(var x = 0, len = data.length; x < len; x+=4) {
            r = data[x];
            g = data[x+1];
            b = data[x+2];

            if((r == rgbfrom.r) &&
                (g == rgbfrom.g) &&
                (b == rgbfrom.b)) {

                data[x] = rgbto.r;
                data[x+1] = rgbto.g;
                data[x+2] = rgbto.b;

            }
        }

        ctx.putImageData(imageData,0,0);

        return canvas.toDataURL();
    }

    hexToRGB(hexStr) {
        var col = {};
        col.r = parseInt(hexStr.substr(1,2),16);
        col.g = parseInt(hexStr.substr(3,2),16);
        col.b = parseInt(hexStr.substr(5,2),16);
        return col;
    }

}