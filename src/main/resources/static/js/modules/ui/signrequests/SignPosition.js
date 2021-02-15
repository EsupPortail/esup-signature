import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {EventFactory} from "../../utils/EventFactory.js";
import {Color} from "../../utils/Color.js";

export class SignPosition extends EventFactory {

    constructor(signType, currentSignRequestParams, signImageNumber, signImages, userName, signable, forceResetSignPos) {
        super();
        console.info("Starting sign positioning tools");
        this.cross = $('#cross_0');
        this.crossTools = $('#crossTools_0');
        this.borders = $('#borders_0');
        this.signType = signType;
        this.currentScale = 1;
        this.fixRatio = .75;
        this.currentSign = "0";
        this.signImages = signImages;
        this.signImageNumber = signImageNumber;
        this.signRequestParamses = new Map();
        this.signable = signable;
        this.forceResetSignPos = forceResetSignPos;
        this.firstDrag = false;
        if(currentSignRequestParams != null) {
            for (let i = 0; i < currentSignRequestParams.length; i++) {
                let signRequestParams = new SignRequestParams();
                if (this.signImageNumber != null) {
                    signRequestParams.signImageNumber = signImageNumber;
                }
                if (this.signImages != null && this.signImages.length > 0) {
                    if (currentSignRequestParams[i].xPos > -1 && currentSignRequestParams[i].yPos > -1) {
                        signRequestParams.xPos = currentSignRequestParams[i].xPos;
                        signRequestParams.yPos = currentSignRequestParams[i].yPos;
                        signRequestParams.signPageNumber = currentSignRequestParams[i].signPageNumber;
                    }
                }
                this.signRequestParamses.set(i + "", signRequestParams);
            }
        } else {
            let signRequestParams = new SignRequestParams();
            if(this.signImageNumber != null) {
                signRequestParams.signImageNumber = this.signImageNumber;
            }
            this.signRequestParamses.set("0", signRequestParams);
        }
        this.userName = userName;
        this.pdf = $('#pdf');
        this.pointItEnable = true;
        this.fontSize = 12;
        this.pointItMove = false;
        this.visualActive = true;
        this.signUndoButton = $('#signUndo_0');
        this.signZoomOutButton = $('#signZoomOut_0');
        this.signZoomInButton = $('#signZoomIn_0');
        this.signNextImageButton = $('#signNextImage_0');
        this.signPrevImageButton = $('#signPrevImage_0');
        this.signExtraButton = $('#signExtra_0');
        this.signExtraOnTopButton = $('#signExtraOnTop_0');
        this.signDropButton = $('#signDrop_0');
        this.signColorPicker = $('#signColorPicker_0');
        this.signColorPicker.spectrum({
            type: "color",
            showPaletteOnly: true,
            hideAfterPaletteSelect: true,
            preferredFormat: "hex",
            change: color => this.changeSignColor(color)
        });
        this.addSignButton = $('#addSignButton');
        if(this.getCurrentSignParams().xPos > -1 && this.getCurrentSignParams().yPos > -1 && forceResetSignPos == null) {
            this.cross.css("position", "absolute");
            this.addSignButton.removeAttr("disabled");
        } else {
            this.cross.css("position", "fixed");
            this.cross.css("margin-left", "270px");
            this.cross.css("margin-top", "180px");
            this.cross.css("left", "0px");
            this.cross.css("top", "0px");
        }
        this.confirmEnabled = false;
        this.events = {};
        if(this.signType !== "visa" && this.signable) {
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
        this.showButtons();
        this.signUndoButton.on('click', e => this.updateSignZoom(1));
        this.signZoomOutButton.on('click', e => this.signZoomOut(e));
        this.signZoomInButton.on('click', e => this.signZoomIn(e));
        this.signNextImageButton.on('click', e => this.signNextImage(e));
        this.signPrevImageButton.on('click', e => this.signPrevImage(e));
        this.signExtraButton.on('click', e => this.toggleExtraInfos());
        this.signExtraOnTopButton.on('click', e => this.toggleExtraPosition());
        this.signDropButton.on('click', e => this.removeSign(e));
        this.signColorPicker.spectrum({
            type: "color",
            showPaletteOnly: true,
            hideAfterPaletteSelect: true,
            preferredFormat: "hex",
            change: color => this.changeSignColor(color)
        });
    }

    unbindCrossToolsListeners() {
        $('.sp-replacer').each(function (){
            $(this).remove();
        });
        this.signColorPicker.spectrum('destroy');
        this.signColorPicker.hide();
        this.signUndoButton.unbind();
        this.signZoomOutButton.unbind();
        this.signZoomInButton.unbind();
        this.signNextImageButton.unbind();
        this.signPrevImageButton.unbind();
        this.signExtraButton.unbind();
        this.signExtraOnTopButton.unbind();
        this.signDropButton.unbind();
        this.hideButtons();
    }

    getCurrentSignParams() {
        return this.signRequestParamses.get(this.currentSign);
    }

    getUiXpos() {
        return Math.round(this.getCurrentSignParams().xPos * this.currentScale / this.fixRatio);
    }

    getUiYpos() {
        return Math.round(this.getCurrentSignParams().yPos * this.currentScale / this.fixRatio);
    }

    addSign() {
        this.hideButtons();
        let okSign = this.cross.clone();
        okSign.css( "z-index", "2");
        okSign.children().removeClass("anim-border");
        okSign.appendTo(this.pdf);
        okSign.on("click", e => this.switchSignToTarget(e));
        console.info("add sign");
        let currentSign = (parseInt(this.currentSign) + 1) + "";
        let signRequestParams;
        if(this.signRequestParamses.get(currentSign) == null) {
            signRequestParams = new SignRequestParams();

            signRequestParams.xPos = -1;
            signRequestParams.yPos = -1;
            signRequestParams.signPageNumber = this.getCurrentSignParams().signPageNumber;
            this.cross.css("position", "fixed");
            this.cross.css("margin-left", "270px");
            this.cross.css("margin-top", "180px");
            this.addSignButton.attr("disabled", "disabled");

        } else {
            signRequestParams = this.signRequestParamses.get(currentSign);
        }
        signRequestParams.addExtra = this.getCurrentSignParams().addExtra;
        signRequestParams.extraWidth = this.getCurrentSignParams().extraWidth;
        signRequestParams.signScale = this.getCurrentSignParams().signScale;
        signRequestParams.signImageNumber = this.signImageNumber;
        signRequestParams.addDate = false;
        signRequestParams.addName = false;
        this.signRequestParamses.set(currentSign + "", signRequestParams);
        this.currentSign = currentSign;
        this.updateCrossPosition();
        this.cross.attr("id", "cross_" + currentSign);
        this.cross.children().each(function (e) {
            $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + currentSign);
        });
        this.cross.children().children().each(function (e) {
            if($(this).attr("id")) {
                if($(this).attr('id').split("_")[0] === "textExtra") {
                    $(this).remove();
                } else {
                    $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + currentSign);
                }
            }
        });
        $('.sp-replacer').each(function (){
            $(this).remove();
        });
        if(this.getCurrentSignParams().addExtra) {
            this.getCurrentSignParams().addExtra = false;
            this.toggleExtraInfos();
        }
        this.signColorPicker.spectrum('destroy');
        this.signColorPicker.hide();
        this.signUndoButton = $('#signUndo_' + currentSign);
        this.signZoomOutButton = $('#signZoomOut_' + currentSign);
        this.signZoomInButton = $('#signZoomIn_' + currentSign);
        this.signNextImageButton = $('#signNextImage_' + currentSign);
        this.signPrevImageButton = $('#signPrevImage_' + currentSign);
        this.signExtraButton = $('#signExtra_' + currentSign);
        this.signColorPicker = $('#signColorPicker_' + currentSign);
        this.signColorPicker.spectrum({
            type: "color",
            showPaletteOnly: true,
            hideAfterPaletteSelect: true,
            preferredFormat: "hex",
            change: color => this.changeSignColor(color)
        });
        this.hideButtons();
        let dateButton = $('#dateButton');
        dateButton.removeClass('btn-outline-success');
        dateButton.addClass('btn-outline-dark');
        let nameButton = $('#nameButton');
        nameButton.removeClass('btn-outline-success');
        nameButton.addClass('btn-outline-dark');
        this.changeSignImage(this.getCurrentSignParams().signImageNumber);
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
        this.borders = $('#borders_' + currentSign);
        this.borders.addClass("anim-border");
        this.initCrossListeners();
        this.unbindCrossToolsListeners();
        this.crossTools = $('#crossTools_' + currentSign);
        this.signUndoButton = $('#signUndo_' + currentSign);
        this.signZoomOutButton = $('#signZoomOut_' + currentSign);
        this.signZoomInButton = $('#signZoomIn_' + currentSign);
        this.signNextImageButton = $('#signNextImage_' + currentSign);
        this.signPrevImageButton = $('#signPrevImage_' + currentSign);
        this.signExtraButton = $('#signExtra_' + currentSign);
        this.signDropButton = $('#signDrop_' + currentSign);
        this.signColorPicker = $('#signColorPicker_' + currentSign);
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
            if(imageNum == null) {
                imageNum = 0;
            }
            let img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
            this.getCurrentSignParams().signImageNumber = imageNum;
            this.cross.css("background-image", "url('" + img + "')");
            let sizes = this.getImageDimensions(img);
            sizes.then(result => this.changeSignSize(result));
        }
    }

    changeSignSize(result) {
        let currentSignParams = this.getCurrentSignParams();
        this.getCurrentSignParams().signWidth = Math.round((result.w + this.getCurrentSignParams().extraWidth) * this.getCurrentSignParams().signScale * this.fixRatio);
        this.getCurrentSignParams().signHeight = Math.round((result.h + this.getCurrentSignParams().extraHeight) * this.getCurrentSignParams().signScale * this.fixRatio);
        this.changeSignColor(Color.rgbToHex(this.getCurrentSignParams().red, this.getCurrentSignParams().green, this.getCurrentSignParams().blue));
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
            // console.debug("mouse pos : " + $("#commentPosX").val() + ", " + $('#commentPosY').val());
        }
    }

    pointIt2(e) {
        if (this.pointItEnable) {
            $("#commentPosX").val(e.offsetX ? (e.offsetX) : e.clientX);
            $('#commentPosY').val(e.offsetY ? (e.offsetY) : e.clientY);
            $('#commentPageNumber').val(this.getCurrentSignParams().signPageNumber);
            console.debug("mouse pos : " + $("#commentPosX").val() + ", " + $('#commentPosY').val());
        }
    }

    touchIt(e) {
        if (this.pointItEnable) {
            e.preventDefault();
            this.pointItMove = true;
            console.debug("touch");
            let rect = pdf.getBoundingClientRect();
            let touch = e.touches[0] || e.changedTouches[0];
            this.getCurrentSignParams().setxPos( (touch.pageX - (rect.left)) / this.currentScale * this.fixRatio);
            this.getCurrentSignParams().setyPos( (touch.pageY - (rect.top + window.scrollY)) / this.currentScale * this.fixRatio);
            this.updateCrossPosition();
        }
    }

    updateScale(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        $('div[id^="cross_"]').each((index, e) => this.updateOtherSignPosition(e, scale));
        this.currentScale = scale;
        if(this.forceResetSignPos == null) {
            this.updateCrossPosition();
        }
    }

    updateCrossPosition() {
        console.debug("update cross pos to : " + this.getUiXpos() + " " + this.getUiYpos());
        console.debug("update sign pos to : " + this.getCurrentSignParams().xPos + " " + this.getCurrentSignParams().yPos);
        // if(this.posX < 0) this.posX = -1;
        // if(this.posY < 0) this.posY = -1;
        this.cross.css('left', this.getUiXpos() + "px");
        this.cross.css('top', this.getUiYpos() + "px");
        this.updateSignSize();
    }

    updateOtherSignPosition(sign, scale) {
        let id = $(sign).attr('id').split("_")[1];
        if(id !== this.currentSign) {
            let cross = $("#cross_" + id);
            let newTop = parseInt($(sign).css("top"), 10) / this.currentScale * scale;
            let newLeft = parseInt($(sign).css("left"), 10) / this.currentScale * scale;
            cross.css('top', newTop + "px");
            cross.css('left', newLeft + "px");
            let borders = $("#borders_" + id);
            let newWidth = parseInt($(sign).css("width"), 10) / this.currentScale * scale;
            let newHeight = parseInt($(sign).css("height"), 10) / this.currentScale * scale;
            cross.css('width', newWidth + "px");
            cross.css('height', newHeight + "px");
            cross.css('background-size', newWidth);
            borders.css('width', newWidth + "px");
            borders.css('height', newHeight + "px");
            let textExtra = $('#textExtra_' + id);
            let signRequestParams = this.signRequestParamses.get(id);
            if (!signRequestParams.extraOnTop) {
                textExtra.css('margin-left', (signRequestParams.signWidth - (signRequestParams.extraWidth * signRequestParams.signScale * this.fixRatio)) * scale / this.fixRatio + "px");
            }
            textExtra.css('font-size', this.fontSize * scale * signRequestParams.signScale + "px");
            textExtra.css('top', "-" + 30 * scale * signRequestParams.signScale + "px");
        }

    }

    updateSignSize() {
        console.log("update sign size " + this.getCurrentSignParams().signWidth);
        let cross = this.cross;
        let borders = this.borders;
        cross.css('width', (this.getCurrentSignParams().signWidth / this.fixRatio * this.currentScale));
        cross.css('height', (this.getCurrentSignParams().signHeight / this.fixRatio * this.currentScale));
        borders.css('width', (this.getCurrentSignParams().signWidth / this.fixRatio * this.currentScale));
        borders.css('height', (this.getCurrentSignParams().signHeight / this.fixRatio * this.currentScale));
        cross.css('background-size', (this.getCurrentSignParams().signWidth - (this.getCurrentSignParams().extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio);
        // let textDate = $('#textDate');
        // textDate.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        // textDate.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        // let textName = $('#textName');
        // textName.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        // textName.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        let textExtra = $('#textExtra_' + this.currentSign);
        if(!this.getCurrentSignParams().extraOnTop) {
            textExtra.css('margin-left', (this.getCurrentSignParams().signWidth - (this.getCurrentSignParams().extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio + "px");
        }
        textExtra.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textExtra.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
    }

    stopDragSignature() {
        console.info("stop drag");
        this.fireEvent('stopDrag', ['ok']);
        this.cross.css('pointerEvents', "auto");
        document.body.style.cursor = "default";
        if(this.pointItEnable) {
            this.showButtons();
        }
        this.pointItEnable = false;
        this.pointItMove = false
        $('body').removeClass('disable-div-selection cursor-move');
        this.addSignButton.removeAttr("disabled");
    }

    dragSignature() {
        console.info("start drag");
        this.firstDrag = true;
        this.cross.css('pointerEvents', "none");
        if(this.cross.css('position') !== 'absolute') {
            this.cross.css('top', window.scrollY);
            // this.posY = window.scrollY;
        }
        this.cross.css('position', "absolute");
        this.cross.css('margin-left', 0);
        this.cross.css('margin-top', 0);
        // this.posY = window.scrollY;
        this.pdf.css('pointerEvents', "auto");
        $('body').addClass('disable-div-selection cursor-move');
        this.pointItEnable = true;
        this.hideButtons();
        if(!this.confirmEnabled && this.signable) {
            this.enableConfirmLeaveSign();
            this.confirmEnabled = true;
        }
        this.fireEvent('startDrag', ['ok']);
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
            this.toggleExtraInfos();
            this.cross.hide();
        } else {
            this.visualActive = true;
            this.cross.show();
            this.toggleExtraInfos();
        }
    }

    // addText() {
    //     console.log("toggle date");
    //     $('#dateButton').toggleClass('btn-outline-success btn-outline-dark');
    //     this.borders.append("<span id='textName' class='align-top visa-text' style='top:-" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale * 3 + "px; font-size:" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px;'>" +
    //         "Visé par " + this.userName + "</span>");
    //     this.borders.append("<span id='textDate' class='align-top visa-text' style='top:-" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale * 3 + "px; font-size:" + this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px;'>Le " + moment().format('DD/MM/YYYY HH:mm:ss') + "</span>");
    // }

    enableConfirmLeaveSign() {
        window.onbeforeunload = function(e) {
            return true;
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
            let textSign = "Signé";
            if(this.signType === "visa") textSign = "Visé";
            let textExtra = $("<span id='textExtra_" + this.currentSign + "' class='align-top visa-text' style='font-size:" + this.fontSize * this.currentScale * this.signScale + "px;user-select: none;\n" +
                "                        -moz-user-select: none;\n" +
                "                        -khtml-user-select: none;\n" +
                "                        -webkit-user-select: none;\n" +
                "                        -o-user-select: none;'>" +
                signTypeText +
                textSign + " par " + this.userName +
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

    changeSignColor(color) {
        console.info("change color to : " + color);
        const rgb = Color.hexToRgb(color);

        this.getCurrentSignParams().red = rgb[0];
        this.getCurrentSignParams().green = rgb[1];
        this.getCurrentSignParams().blue = rgb[2];

        let img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[this.getCurrentSignParams().signImageNumber];
        let cross = this.cross;
        Color.changeColInUri(img, "#000000", color).then(function (e) {
            cross.css("background-image", "url('" + e + "')");
        })
    }

}