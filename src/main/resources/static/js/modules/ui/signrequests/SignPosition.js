import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {EventFactory} from "../../utils/EventFactory.js";
import {Color} from "../../utils/Color.js";

export class SignPosition extends EventFactory {

    constructor(signType, currentSignRequestParams, signImageNumber, signImages, userName, signable, forceResetSignPos) {
        super();
        console.info("Starting sign positioning tools");
        this.cross = $('#cross_0');
        this.crossTools = $('#crossTools_0');
        this.defaultTools = $('#defaultTools_0');
        this.moreTools = $('#moreTools_0');
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
        if (currentSignRequestParams != null) {
            for (let i = 0; i < currentSignRequestParams.length; i++) {
                let signRequestParams = new SignRequestParams();
                if (this.signImageNumber != null) {
                    signRequestParams.signImageNumber = signImageNumber;
                }
                if (this.signImages != null && this.signImages.length > 0) {
                    if (currentSignRequestParams[i] != null) {
                        if (currentSignRequestParams[i].xPos > -1 && currentSignRequestParams[i].yPos > -1) {
                            signRequestParams.xPos = currentSignRequestParams[i].xPos;
                            signRequestParams.yPos = currentSignRequestParams[i].yPos;
                            signRequestParams.signPageNumber = currentSignRequestParams[i].signPageNumber;
                        }
                    }
                }
                if(localStorage.getItem('addWatermark') != null) {
                    signRequestParams.addWatermark = localStorage.getItem('addWatermark') === 'true';
                }
                this.signRequestParamses.set(i + "", signRequestParams);
            }
        } else {
            let signRequestParams = new SignRequestParams();
            if (this.signImageNumber != null && signType !== 'visa' || signType !== 'hiddenVisa') {
                signRequestParams.signImageNumber = this.signImageNumber;
            }
            this.signRequestParamses.set("0", signRequestParams);
        }
        this.userName = userName;
        this.pdf = $('#pdf');
        this.pointItEnable = true;
        this.fontSize = 12;
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
        this.displayMoreToolsButton = $('#displayMoreTools_0');
        this.hideMoreToolsButton = $('#hideMoreTools_0');
        this.watermarkButton = $('#watermark_0');
        this.createColorPicker();
        this.addSignButton = $('#addSignButton');
        if (this.getCurrentSignParams().xPos > -1 && this.getCurrentSignParams().yPos > -1 && forceResetSignPos == null) {
            this.cross.css("position", "absolute");
            this.addSignButton.removeAttr("disabled");
            this.lockCurrentSign(false);
        } else {
            this.cross.attr("data-current", "true");
            this.cross.css("position", "fixed");
            this.cross.css("margin-left", "270px");
            if($("#ws-tabs").length) {
                this.cross.css("margin-top", "222px");
            } else {
                this.cross.css("margin-top", "180px");
            }
            this.cross.css("left", "0px");
            this.cross.css("top", "0px");
        }
        this.confirmEnabled = false;
        this.events = {};
        if (this.signType !== "visa" && this.signType !== "hiddenVisa" && this.signable) {
            // $(document).ready(e => this.toggleExtraInfos());

        }
        if (this.signType === "visa" || this.signType === "hiddenVisa") {
            if(!this.getCurrentSignParams().addWatermark) this.toggleWatermark();
            this.toggleExtraInfos();
            this.visualActive = false;
        } else {
            if(this.getCurrentSignParams().addWatermark)  {
                this.getCurrentSignParams().addWatermark = false;
                this.toggleWatermark();
            }
        }
        if (this.signType === "visa") {
            this.toggleVisual();
            $("#visualButton").remove();
        }
        if(this.signType === "nexuSign" || this.signType === "certSign") {
            $("#visualButton").removeClass("d-none");
        }
        this.initListeners();
        this.borders.addClass("anim-border");
        this.borders.removeClass("static-border");
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        if(localStorage.getItem("zoom") != null) {
            this.updateSignZoom(parseFloat(localStorage.getItem("zoom")));
        }
    }

    initListeners() {
        window.addEventListener("touchmove", e => this.touchIt(e), {passive: false});
        $("#addCheck").on("click", e => this.addCheckImage());
        $("#addTimes").on("click", e => this.addTimesImage());
        $("#addCircle").on("click", e => this.addCircleImage());
        $("#addMinus").on("click", e => this.addMinusImage());
        this.initCrossListeners();
        this.initCrossToolsListeners();
    }

    initCrossListeners() {
        this.borders.on('mousedown', e => this.dragSignature());
        this.borders.on('touchstart', e => this.dragSignature());
        this.borders.on('mouseup', e => this.stopDragSignature(false));
        this.borders.on('click', function (e) {
            e.stopPropagation();
        });
        this.borders.on('touchend', e => this.stopDragSignature(false));
    }

    initCrossToolsListeners() {
        this.signZoomOutButton.on('click', e => this.signZoomOut(e));
        this.signZoomInButton.on('click', e => this.signZoomIn(e));
        if(this.signRequestParamses.size > 1) {
            this.signDropButton.show();
            this.signDropButton.on('click', e => this.removeSign(e));
        } else {
            this.signDropButton.hide();
        }
        if(this.getCurrentSignParams().signImageNumber >= 0) {
            this.showButtons();
            this.signUndoButton.show();
            this.signUndoButton.on('click', e => this.resetSign());
            if (this.signImages != null && this.signImages.length > 1) {
                this.signNextImageButton.show();
                this.signPrevImageButton.show();
                this.signNextImageButton.on('click', e => this.signNextImage(e));
                this.signPrevImageButton.on('click', e => this.signPrevImage(e));
            } else {
                this.signNextImageButton.hide();
                this.signPrevImageButton.hide();
            }
            this.signExtraButton.on('click', e => this.toggleExtraInfos());
            this.signExtraOnTopButton.on('click', e => this.toggleExtraPosition());
            if(this.signType !== "visa" && this.signType !== "hiddenVisa") {
                this.displayMoreToolsButton.show();
            }
            this.displayMoreToolsButton.on('click', e => this.displayMoreTools());
            this.hideMoreToolsButton.on('click', e => this.hideMoreTools());
            this.watermarkButton.on('click', e => this.toggleWatermark());
            this.createColorPicker();
            this.crossTools.unbind();
            this.crossTools.on('click', function (e) {
                e.stopPropagation();
            });
            this.crossTools.children().each(function () {
                $(this).unbind();
                $(this).on('click', function (e) {
                    e.stopPropagation();
                });
            });
        }
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
        this.displayMoreToolsButton.unbind();
        this.hideMoreToolsButton.unbind();
        this.watermarkButton.unbind();
        this.hideButtons();
    }


    displayMoreTools() {
        this.moreTools.removeClass('d-none');
        this.defaultTools.addClass('d-none');
    }

    hideMoreTools() {
        this.moreTools.addClass('d-none');
        this.defaultTools.removeClass('d-none');
    }

    createColorPicker() {
        this.signColorPicker.spectrum({
            type: "color",
            showPaletteOnly: true,
            hideAfterPaletteSelect: true,
            preferredFormat: "hex",
            change: color => this.changeSignColor(color)
        });
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
        this.unbindCrossToolsListeners();
        let okSign = this.cross.clone();
        okSign.css( "z-index", "1027");
        okSign.children().removeClass("anim-border");
        okSign.children().addClass("static-border");
        okSign.attr("data-current", "false");
        okSign.appendTo(this.pdf);
        okSign.on("mousedown", e => this.switchSignToTarget(e));
        console.info("add sign");
        let currentSign = (this.signRequestParamses.size + 1) + "";
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
        this.cross.children().children().children().each(function (e) {
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
        this.signExtraButton.removeAttr("disabled");
        this.signColorPicker = $('#signColorPicker_' + currentSign);
        this.displayMoreToolsButton = $('#displayMoreTools_' + currentSign);
        this.hideMoreToolsButton = $('#hideMoreTools_' + currentSign);
        this.watermarkButton = $('#watermark_' + currentSign);
        this.defaultTools = $('#defaultTools_' + currentSign);
        this.moreTools = $('#moreTools_' + currentSign);
        this.hideMoreTools();
        this.createColorPicker();
        this.hideButtons();
        let dateButton = $('#dateButton');
        dateButton.removeClass('btn-outline-success');
        dateButton.addClass('btn-outline-dark');
        let nameButton = $('#nameButton');
        nameButton.removeClass('btn-outline-success');
        nameButton.addClass('btn-outline-dark');
        // this.changeSignImage(this.getCurrentSignParams().signImageNumber);
        this.hideMoreTools();
        this.initCrossToolsListeners();
        this.resetSign();
    }

    switchSignToTarget(e) {
        e.stopPropagation();
        let changeCross = $(e.currentTarget);
        this.lockCurrentSign();
        this.switchSign(changeCross.attr("id").split("_")[1]);

    }

    switchSign(currentSign) {
        console.info("switch to " + currentSign);
        if(this.signRequestParamses.lenght > 1) this.lockCurrentSign();
        this.currentSign = currentSign;
        this.cross = $('#cross_' + currentSign);
        this.cross.attr("data-current", "true");
        this.cross.unbind();
        this.borders = $('#borders_' + currentSign);
        this.cross.children().removeClass("static-border");
        this.borders.addClass("anim-border");
        this.borders.removeClass("static-border");
        this.initCrossListeners();
        this.unbindCrossToolsListeners();
        this.crossTools = $('#crossTools_' + currentSign);
        this.signUndoButton = $('#signUndo_' + currentSign);
        this.signZoomOutButton = $('#signZoomOut_' + currentSign);
        this.signZoomInButton = $('#signZoomIn_' + currentSign);
        this.signNextImageButton = $('#signNextImage_' + currentSign);
        this.signPrevImageButton = $('#signPrevImage_' + currentSign);
        this.signExtraButton = $('#signExtra_' + currentSign);
        this.signExtraOnTopButton = $('#signExtraOnTop_' + currentSign);
        this.signDropButton = $('#signDrop_' + currentSign);
        this.signColorPicker = $('#signColorPicker_' + currentSign);
        this.displayMoreToolsButton = $('#displayMoreTools_' + currentSign);
        this.hideMoreToolsButton = $('#hideMoreTools_' + currentSign);
        this.watermarkButton = $('#watermark_' + currentSign);
        this.defaultTools = $('#defaultTools_' + currentSign);
        this.moreTools = $('#moreTools_' + currentSign);
        this.hideMoreTools();
        this.initCrossToolsListeners();
        this.dragSignature();
        let textExtra = $("#textExtra_" + this.currentSign);
        textExtra.on("input", e => this.refreshExtraText(e));
        textExtra.on("click mouseup mousedown", function (e){
            e.stopPropagation();
        });
    }

    lockCurrentSign() {
        console.info("lock current sign");
        this.borders.removeClass("anim-border");
        this.borders.addClass("static-border");
        this.cross.attr("data-current", "false");
        this.borders.unbind();
        this.cross.on("mousedown", e => this.switchSignToTarget(e));
        this.hideButtons();
    }

    removeSign(e) {
        let dropCross = $(e.currentTarget);
        let dropId = dropCross.attr("id").split("_")[1];
        console.info("drop : sign_" + (this.currentSign));
        this.signRequestParamses.delete(dropId);
        $("#cross_" + dropId).remove();
        this.currentSign = Array.from(this.signRequestParamses.keys())[this.signRequestParamses.size - 1];
    }

    changeSignImage(imageNum) {
        if(imageNum >= 0) {
            if (this.signImages != null) {
                console.debug("change sign image to " + imageNum);
                if (imageNum == null) {
                    imageNum = 0;
                }
                let img = null;
                if(this.signImages[imageNum] != null) {
                    img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
                    this.getCurrentSignParams().signImageNumber = imageNum;
                    this.cross.css("background-image", "url('" + img + "')");
                }
                let sizes = this.getImageDimensions(img);
                sizes.then(result => this.changeSignSize(result));
            }
        } else {
            if(imageNum < 0) {
                let self = this;
                this.convertImgToBase64URL('/images/' + this.faImages[Math.abs(imageNum) - 1] + '.png', function(img) {
                    self.cross.css("background-image", "url('" + img + "')");
                    let sizes = self.getImageDimensions(img);
                    sizes.then(result => self.changeSignSize(result));
                });
            }
        }
    }

    changeSignSize(result) {
        // if(this.signImages[this.getCurrentSignParams().signImageNumber] != null) {
            this.getCurrentSignParams().signWidth = Math.round((result.w + this.getCurrentSignParams().extraWidth) * this.getCurrentSignParams().signScale * this.fixRatio);
            this.getCurrentSignParams().signHeight = Math.round((result.h + this.getCurrentSignParams().extraHeight) * this.getCurrentSignParams().signScale * this.fixRatio);
            this.changeSignColor(Color.rgbToHex(this.getCurrentSignParams().red, this.getCurrentSignParams().green, this.getCurrentSignParams().blue));
            this.updateSignSize();
        // }
    }

    getImageDimensions(file) {
        return new Promise (function (resolved) {
            if(file != null) {
                let i = new Image();
                i.onload = function(){
                    resolved({w: i.width / 3, h: i.height / 3})
                };
                i.src = file
            } else {
                resolved({w: 200, h: 75})
            }
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

    changeToFaImage(number) {
        this.getCurrentSignParams().signImageNumber = 0 - number;
        this.changeSignImage(0 - number);
        this.signExtraButton.attr("disabled", true);
        this.removeExtra();
        this.hideMoreTools();
        this.displayMoreToolsButton.hide();
        this.displayMoreToolsButton.unbind();
        this.signUndoButton.hide();
        this.signUndoButton.unbind();
        this.signNextImageButton.hide();
        this.signNextImageButton.unbind();
        this.signPrevImageButton.hide();
        this.signPrevImageButton.unbind();
        this.forceRemoveExtra();
    }

    signZoomOut(e) {
        e.stopPropagation();
        let zoom = this.getCurrentSignParams().signScale - 0.1;
        this.updateSignZoom(zoom);
        localStorage.setItem("zoom", Math.round(zoom * 10) / 10);
    }

    signZoomIn(e) {
        e.stopPropagation();
        let zoom = this.getCurrentSignParams().signScale + 0.1;
        this.updateSignZoom(zoom);
        localStorage.setItem("zoom", Math.round(zoom * 10) / 10);
    }

    resetSign() {
        this.updateSignZoom(1)
        this.getCurrentSignParams().signImageNumber = this.signImageNumber;
        this.changeSignImage(this.signImageNumber);
        this.changeSignColor("#000000");
        if(this.signType !== "visa" && this.signType !== "hiddenVisa") {
            this.removeExtra();
        }
        this.signExtraButton.removeAttr("disabled");
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
            let offset = $("#pdf").offset();
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
        let textExtra = $('#textExtra_' + this.currentSign);
        if(!this.getCurrentSignParams().extraOnTop) {
            textExtra.css('margin-left', (this.getCurrentSignParams().signWidth - (this.getCurrentSignParams().extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio + "px");
            textExtra.css('width', ((this.getCurrentSignParams().extraWidth * this.getCurrentSignParams().signScale * this.fixRatio)) * this.currentScale / this.fixRatio + "px");
        }
        textExtra.css('font-size', this.fontSize * this.currentScale * this.getCurrentSignParams().signScale + "px");
        textExtra.css('top', "-" + 30 * this.currentScale * this.getCurrentSignParams().signScale + "px");
        this.colorPickerStopPropagation();
    }

    stopDragSignature(lock) {
        console.info("stop drag " + lock);
        this.fireEvent('stopDrag', ['ok']);
        this.cross.css('pointerEvents', "auto");
        document.body.style.cursor = "default";
        if(this.pointItEnable) {
            this.showButtons();
        }
        this.pointItEnable = false;
        $('body').removeClass('disable-div-selection cursor-move');
        if(this.getCurrentSignParams().xPos > -1 && this.getCurrentSignParams().yPos > -1) {
            this.addSignButton.removeAttr("disabled");
        }
        if(lock) this.lockCurrentSign();
    }

    dragSignature() {
        console.info("start drag");
        if(this.cross.attr("data-current") === "true") {
            this.firstDrag = true;
            this.cross.css('pointerEvents', "none");
            if (this.cross.css('position') !== 'absolute') {
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
            if (!this.confirmEnabled && this.signable) {
                this.enableConfirmLeaveSign();
                this.confirmEnabled = true;
            }
            this.fireEvent('startDrag', ['ok']);
        } else {
            this.cross.attr("data-current", "true");
            this.borders.addClass("anim-border");
            this.borders.removeClass("static-border");
            this.showButtons();
        }
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
            this.getCurrentSignParams().visual = false;
            this.cross.addClass("d-none");
        } else {
            this.visualActive = true;
            this.getCurrentSignParams().visual = true;
            this.cross.show();
            this.cross.removeClass("d-none");
            this.toggleExtraInfos();
            if(this.signType === "visa" || this.signType === "hiddenVisa") {
                this.cross.css("width", 300);
                this.cross.css("height", 150);
                this.borders.css("width", 300);
                this.borders.css("height", 150);
                this.getCurrentSignParams().signWidth = 150;
                this.getCurrentSignParams().signHeight = 75;
                this.displayMoreToolsButton.hide();
                $("#signUndo_0").hide();
            }
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

    toggleWatermark() {
        if(this.getCurrentSignParams().addWatermark) {
            this.cross.removeClass("watermarkWidth");
            this.cross.removeClass("watermarkHeight");
            this.getCurrentSignParams().addWatermark = false;
        } else {
            if(this.getCurrentSignParams().extraOnTop) {
                this.cross.addClass("watermarkWidth");
            } else {
                this.cross.addClass("watermarkHeight");
            }
            this.getCurrentSignParams().addWatermark = true;
        }
        if(this.signType !== "visa" && this.signType !== "hiddenVisa") {
            localStorage.setItem('addWatermark', this.getCurrentSignParams().addWatermark);
        }
    }

    toggleExtraInfos() {
        console.log("toggle extra");
        $('#extraButton').toggleClass('btn-outline-success btn-outline-dark');
        if(this.getCurrentSignParams().addExtra) {
            this.removeExtra();
        } else {
            this.signExtraOnTopButton.removeAttr("disabled");
            this.getCurrentSignParams().addExtra = true;
            let signTypeText = "";
            let textSign = "Signé";
            if(this.signType === "visa" || this.signType === "hiddenVisa") textSign = "Visé";
            let defaultText = signTypeText +
                textSign + " par " + this.userName +
                "\n" +
                "Le " + moment().format('DD/MM/YYYY HH:mm:ss');
            if(this.getCurrentSignParams().extraText != null && this.getCurrentSignParams().extraText !== "") {
                defaultText = this.getCurrentSignParams().extraText;
            }
            let fontSize = this.fontSize * this.currentScale * this.getCurrentSignParams().signScale;
            let textExtra = $("<textarea id='textExtra_" + this.currentSign + "' class='sign-textarea align-top visa-text' style='font-size:" + fontSize + "px;user-select: none;\n" +
                "                        -moz-user-select: none;\n" +
                "                        -khtml-user-select: none;\n" +
                "                        -webkit-user-select: none;\n" +
                "                        -o-user-select: none;'>" +
                defaultText +
                "</textarea>");
            this.getCurrentSignParams().extraText = defaultText;
            let lines = defaultText.split(/\r|\r\n|\n/);
            let count = lines.length;
            textExtra.attr("rows", count);
            this.cross.removeClass("watermarkWidth");
            this.cross.removeClass("watermarkHeight");
            if(this.getCurrentSignParams().extraOnTop) {
                if(this.getCurrentSignParams().addWatermark) {
                    this.cross.addClass("watermarkWidth");
                }
                this.borders.append(textExtra);
                let textExtraHeight = textExtra.height();
                this.getCurrentSignParams().extraHeight = textExtraHeight;
                this.getCurrentSignParams().extraWidth = 0;
                this.getCurrentSignParams().signHeight = this.getCurrentSignParams().signHeight + textExtraHeight;
            } else {
                if(this.getCurrentSignParams().addWatermark) {
                    this.cross.addClass("watermarkHeight");
                }
                this.borders.append(textExtra);
                console.log(textExtra);
                let textExtraWidth = this.getCurrentSignParams().signWidth / this.fixRatio;
                this.getCurrentSignParams().extraHeight = 0;
                this.getCurrentSignParams().extraWidth = textExtraWidth;
                this.getCurrentSignParams().signWidth = this.getCurrentSignParams().signWidth + textExtraWidth;
            }
            textExtra.on("input", e => this.refreshExtraText(e));
            textExtra.on("click mouseup mousedown", function (e){
                e.stopPropagation();
            });
        }
        this.changeSignImage(this.getCurrentSignParams().signImageNumber);
    }

    refreshExtraText(e) {
        let target = $(e.target);
        let text = target.val();
        let lines = text.split(/\r|\r\n|\n/);
        let count = lines.length;
        target.attr("rows", count);
        this.getCurrentSignParams().extraText = target.val();
        if(this.getCurrentSignParams().extraOnTop && this.signType !== "visa" && this.signType !== "hiddenVisa") {
            let textExtraHeight = target.height();
            this.getCurrentSignParams().extraHeight = textExtraHeight;
            this.getCurrentSignParams().signHeight = this.getCurrentSignParams().signHeight + textExtraHeight;
            this.changeSignImage(this.getCurrentSignParams().signImageNumber);
        }
    }

    toggleExtraPosition() {
        this.getCurrentSignParams().extraOnTop = !this.getCurrentSignParams().extraOnTop;
        this.getCurrentSignParams().addExtra = !this.getCurrentSignParams().addExtra;
        this.removeExtra();
        this.toggleExtraInfos();
    }

    removeExtra() {
        this.getCurrentSignParams().addExtra = false;
        if(this.signType !== "visa" && this.signType !== "hiddenVisa") {
            this.getCurrentSignParams().extraWidth = 0;
            this.getCurrentSignParams().extraHeight = 0;
            if (this.getCurrentSignParams().extraOnTop) {
                this.getCurrentSignParams().signWidth = this.getCurrentSignParams().signWidth - 200;
            }
        }
        let textExtra = $("#textExtra_" + this.currentSign);
        this.getCurrentSignParams().extraText = textExtra.val();
        textExtra.remove();
        this.colorPickerStopPropagation();
        this.signExtraOnTopButton.attr("disabled", true);
    }

    forceRemoveExtra() {
        this.getCurrentSignParams().addExtra = false;
        this.getCurrentSignParams().extraWidth = 0;
        this.getCurrentSignParams().extraHeight = 0;
        if (this.getCurrentSignParams().extraOnTop) {
            this.getCurrentSignParams().signWidth = this.getCurrentSignParams().signWidth - 200;
        }
        let textExtra = $("#textExtra_" + this.currentSign);
        this.getCurrentSignParams().extraText = textExtra.val();
        textExtra.remove();
        this.colorPickerStopPropagation();
        this.signExtraOnTopButton.attr("disabled", true);
    }

    changeSignColor(color) {
        console.info("change color to : " + color);
        const rgb = Color.hexToRgb(color);

        this.getCurrentSignParams().red = rgb[0];
        this.getCurrentSignParams().green = rgb[1];
        this.getCurrentSignParams().blue = rgb[2];

        let cross = this.cross;
        if (this.signImages[this.getCurrentSignParams().signImageNumber] != null) {
            let img = "data:image/jpeg;charset=utf-8;base64" +
                ", " + this.signImages[this.getCurrentSignParams().signImageNumber];
            Color.changeColInUri(img, "#000000", color).then(function (e) {
                cross.css("background-image", "url('" + e + "')");
            })
        }
        let textExtra = $("#textExtra_" + this.currentSign);
        textExtra.css({"color" : color + ""});
    }

    colorPickerStopPropagation() {
        $(".sp-replacer, .sp-preview, .sp-preview-inner, .sp-dd, .sp-palette-container").each(function (){
            $(this).on("mouseup", function (e) {
                e.preventDefault();
                e.stopPropagation();
            });
        });
    }

    convertImgToBase64URL(url, callback, outputFormat){
        let img = new Image();
        img.crossOrigin = 'Anonymous';
        img.onload = function(){
            let canvas = document.createElement('CANVAS'),
                ctx = canvas.getContext('2d'), dataURL;
            canvas.height = img.height;
            canvas.width = img.width;
            ctx.drawImage(img, 0, 0);
            dataURL = canvas.toDataURL(outputFormat);
            callback(dataURL);
            canvas = null;
        };
        img.src = url;
    }

    addCheckImage() {
        this.addSign();
        this.changeToFaImage(1);
    }

    addTimesImage() {
        this.addSign();
        this.changeToFaImage(2);
    }

    addCircleImage() {
        this.addSign();
        this.changeToFaImage(3);
    }

    addMinusImage() {
        this.addSign();
        this.changeToFaImage(4);
    }

}