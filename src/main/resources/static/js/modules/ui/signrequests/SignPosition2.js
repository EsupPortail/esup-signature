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
        this.addSign();
        this.fixRatio = .75;
        this.currentScale = 1;
    }

    addSign() {
        let signRequestParams = new SignRequestParams();
        let divName = "cross_" + this.signRequestParamses.length;
        let div = "<div id='"+ divName +"' class='static-border'></div>";
        this.pdf.prepend(div);
        let cross = $("#" + divName);
        cross.css("position", "absolute");
        cross.css("z-index", "5");
        cross.css("width", "200px");
        cross.css("height", "150px");
        cross.draggable();
        signRequestParams.cross = cross;
        signRequestParams.extraWidth = 0;
        signRequestParams.extraHeight = 0;
        this.signRequestParamses[this.signRequestParamses.length] = signRequestParams;
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
        signRequestParams.signHeight = Math.round((result.h + signRequestParams.extraHeight) * signRequestParams.signScale * this.fixRatio);
        signRequestParams.cross.css('width', (signRequestParams.signWidth / this.fixRatio * this.currentScale));
        signRequestParams.cross.css('height', (signRequestParams.signHeight / this.fixRatio * this.currentScale));
        signRequestParams.cross.css('background-size', (signRequestParams.signWidth - (signRequestParams.extraWidth * signRequestParams.signScale * this.fixRatio)) * this.currentScale / this.fixRatio);
    }

}
