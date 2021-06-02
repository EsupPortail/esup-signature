import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {EventFactory} from "../../utils/EventFactory.js";
import {Color} from "../../utils/Color.js";

export class SignPosition2 extends EventFactory {

    constructor(signType, currentSignRequestParams, signImageNumber, signImages, userName, signable, forceResetSignPos) {
        super();
        console.info("Starting sign positioning tools");
        this.pdf = $("#pdf");
        this.signImages = signImages;
        this.signRequestParamses = [];
        this.fixRatio = .75;
        this.currentScale = 1;
        if(localStorage.getItem("scale") != null) {
            this.currentScale = localStorage.getItem("scale");
        }
        if(signable) {
            this.addSign(1);
        }
    }

    addSign(page) {
        let self = this;
        let signRequestParams = new SignRequestParams();
        let id = this.signRequestParamses.length;
        let divName = "cross_" + id;
        let div = "<div id='"+ divName +"' class='static-border'></div>";
        let tools = this.getTools()
        tools.removeClass("d-none");
        this.pdf.prepend(div);
        let cross = $("#" + divName);
        cross.prepend(tools);
        cross.css("position", "absolute");
        cross.css("z-index", "5");
        // cross.css("width", 150 * this.currentScale + "px");
        // cross.css("height", 75 * this.currentScale + "px");
        cross.attr("data-id", id);
        cross.draggable({
            containment: "#pdf",
            scroll: false,
            drag: function() {
                let signRequestParams = self.signRequestParamses[$(this).attr("data-id")];
                let thisPos = $(this).position();
                let x = Math.round(thisPos.left * self.fixRatio / self.currentScale);
                let y = Math.round(thisPos.top * self.fixRatio / self.currentScale);
                // $(this).text(
                //     "(" + x + ", " + y + ")" +  signRequestParams.signWidth + "*" + signRequestParams.signHeight + " " + signRequestParams.signPageNumber
                // );

                signRequestParams.xPos = x;
                signRequestParams.yPos = y;
            }
        });
        cross.resizable({
            aspectRatio: true,
            maxHeight: 300,
            minHeight: 50,
            resize: function( event, ui ) {
                let signRequestParams = self.signRequestParamses[$(this).attr("data-id")];
                let currentScale = parseFloat(self.currentScale);
                signRequestParams.signScale = Math.round(((ui.size.width) / currentScale * self.fixRatio) / (signRequestParams.originalWidth / self.fixRatio) * 10) / 10;
                signRequestParams.signWidth = Math.round(ui.size.width / currentScale * self.fixRatio);
                signRequestParams.signHeight = Math.round(ui.size.height / currentScale * self.fixRatio);
                signRequestParams.cross.css('background-size', Math.round(ui.size.width));
                let thisPos = $(this).position();

                let x = Math.round(thisPos.left * self.fixRatio / self.currentScale);
                let y = Math.round(thisPos.top * self.fixRatio / self.currentScale);
                console.log("(" + x + ", " + y + ")" +  signRequestParams.signScale);

            }
        });
        signRequestParams.cross = cross;
        signRequestParams.tools = tools;
        signRequestParams.extraWidth = 0;
        signRequestParams.extraHeight = 0;
        signRequestParams.signPageNumber = page;
        this.signRequestParamses[id] = signRequestParams;
        this.changeSignImage(0, signRequestParams);
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

    changeSignImage(imageNum, signRequestParams) {
        if(imageNum >= 0) {
            if (this.signImages != null) {
                console.debug("change sign image to " + imageNum);
                if (imageNum == null) {
                    imageNum = 0;
                }
                let img = null;
                if(this.signImages[imageNum] != null) {
                    img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
                    signRequestParams.signImageNumber = imageNum;
                    signRequestParams.cross.css("background-image", "url('" + img + "')");
                }
                let sizes = this.getImageDimensions(img);
                sizes.then(result => this.changeSignSize(result, signRequestParams));
            }
        } else {
            if(imageNum < 0) {
                let self = this;
                this.convertImgToBase64URL('/images/' + this.faImages[Math.abs(imageNum) - 1] + '.png', function(img) {
                    signRequestParams.cross.css("background-image", "url('" + img + "')");
                    let sizes = self.getImageDimensions(img);
                    sizes.then(result => self.changeSignSize(result, signRequestParams));
                });
            }
        }
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

    changeSignSize(result, signRequestParams) {
        signRequestParams.signWidth = Math.round((result.w + signRequestParams.extraWidth) * signRequestParams.signScale * this.fixRatio);
        signRequestParams.originalWidth = Math.round((result.w + signRequestParams.extraWidth) * signRequestParams.signScale * this.fixRatio);
        signRequestParams.signHeight = Math.round((result.h + signRequestParams.extraHeight) * signRequestParams.signScale * this.fixRatio);
        signRequestParams.cross.css('width', (signRequestParams.signWidth / this.fixRatio * this.currentScale));
        signRequestParams.cross.css('height', (signRequestParams.signHeight / this.fixRatio * this.currentScale));
        signRequestParams.cross.css('background-size', (signRequestParams.signWidth - (signRequestParams.extraWidth * signRequestParams.signScale * this.fixRatio)) * this.currentScale / this.fixRatio);
    }

    updateScale(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        for(let i = 0 ; i < this.signRequestParamses.length; i++) {
            let width = parseInt(this.signRequestParamses[i].cross.css("width"), 10);
            let height = parseInt(this.signRequestParamses[i].cross.css("height"), 10);
            let newWidth = Math.round(width / this.currentScale * scale);
            let newHeight = Math.round(height / this.currentScale * scale);
            let thisPos = this.signRequestParamses[i].cross.position();
            let x = thisPos.left;
            let y = thisPos.top;
            let xNew = Math.round((x / this.currentScale * scale));
            let yNew = Math.round((y / this.currentScale * scale));
            this.signRequestParamses[i].cross.css("width", newWidth + "px");
            this.signRequestParamses[i].cross.css("height", newHeight + "px");
            this.signRequestParamses[i].cross.css('background-size', newWidth);
            this.signRequestParamses[i].cross.css('left', xNew + 'px');
            this.signRequestParamses[i].cross.css('top', yNew + 'px');
        }
        this.currentScale = scale;
    }

    getTools(id) {
        let tools = $("#crossTools_0").clone();
        tools.children().each(function (e) {
            $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + id);
        });
        tools.children().children().each(function (e) {
            if($(this).attr("id")) {
                if($(this).attr('id').split("_")[0] === "textExtra") {
                    $(this).remove();
                } else {
                    $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + id);
                }
            }
        });
        return tools;
    }

    // signZoomOut(e) {
    //     e.stopPropagation();
    //     let zoom = this.getCurrentSignParams().signScale - 0.1;
    //     this.updateSignZoom(zoom);
    //     localStorage.setItem("zoom", Math.round(zoom * 10) / 10);
    // }
    //
    // signZoomIn(e) {
    //     e.stopPropagation();
    //     let zoom = this.getCurrentSignParams().signScale + 0.1;
    //     this.updateSignZoom(zoom);
    //     localStorage.setItem("zoom", Math.round(zoom * 10) / 10);
    // }

}
