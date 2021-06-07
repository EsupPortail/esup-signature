import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {EventFactory} from "../../utils/EventFactory.js";
import {Color} from "../../utils/Color.js";

export class SignPosition2 extends EventFactory {

    constructor(signType, currentSignRequestParams, signImageNumber, signImages, userName, signable, forceResetSignPos) {
        super();
        console.info("Starting sign positioning tools");
        this.userName = userName;
        this.pdf = $("#pdf");
        this.signImages = signImages;
        this.signRequestParamses = new Map();
        this.id = 0;
        this.currentScale = 1;
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        if(localStorage.getItem("scale") != null) {
            this.currentScale = localStorage.getItem("scale");
        }
        if(signable) {
            this.addSign(1, true, true);
        }
    }

    addSign(page, restore, isSign) {
        let id = this.id;
        this.signRequestParamses.set(id, new SignRequestParams(null, id, this.currentScale, page, this.userName, restore, isSign));
        this.changeSignImage(0, this.signRequestParamses.get(id));
        this.signRequestParamses.get(id).addEventListener("unlock", e => this.lockSigns());
        this.signRequestParamses.get(id).addEventListener("delete", e => this.removeSign(id));
        this.signRequestParamses.get(id).addEventListener("nextSign", e => this.changeSignImage(this.signRequestParamses.get(id).signImageNumber + 1, this.signRequestParamses.get(id)));
        this.signRequestParamses.get(id).addEventListener("prevSign", e => this.changeSignImage(this.signRequestParamses.get(id).signImageNumber - 1, this.signRequestParamses.get(id)));
        this.signRequestParamses.get(id).addEventListener("changeColor", e => this.changeSignColor(e, this.signRequestParamses.get(id)));
        this.id++;
        return this.signRequestParamses.get(id);
    }

    removeSign(id) {
        this.signRequestParamses.delete(id);
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
        signRequestParams.signImageNumber = imageNum;
        if(imageNum >= 0) {
            if (this.signImages != null) {
                console.debug("change sign image to " + imageNum);
                if (imageNum == null) {
                    imageNum = 0;
                }
                let img = null;
                if(this.signImages[imageNum] != null) {
                    img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
                    signRequestParams.cross.css("background-image", "url('" + img + "')");
                    let sizes = this.getImageDimensions(img);
                    sizes.then(result => signRequestParams.changeSignSize(result));
                }
            }
        } else {
            if(imageNum < 0) {
                let self = this;
                this.convertImgToBase64URL('/images/' + this.faImages[Math.abs(imageNum) - 1] + '.png', function(img) {
                    signRequestParams.cross.css("background-image", "url('" + img + "')");
                    let sizes = self.getImageDimensions(img);
                    sizes.then(result => signRequestParams.changeSignSize(result));
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

    updateScales(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        this.signRequestParamses.forEach(function (signRequestParams){
            signRequestParams.updateScale(scale);
        });
        this.currentScale = scale;
    }

    lockSigns() {
        this.signRequestParamses.forEach(function (signRequestParams){
            signRequestParams.lock();
        });
    }


    changeSignColor(color, signRequestParams) {
        console.info("change color to : " + color);
        const rgb = Color.hexToRgb(color);

        signRequestParams.red = rgb[0];
        signRequestParams.green = rgb[1];
        signRequestParams.blue = rgb[2];

        let cross = signRequestParams.cross;
        if (this.signImages[signRequestParams.signImageNumber] != null) {
            let img = "data:image/jpeg;charset=utf-8;base64" +
                ", " + this.signImages[signRequestParams.signImageNumber];
            Color.changeColInUri(img, "#000000", color).then(function (e) {
                cross.css("background-image", "url('" + e + "')");
            })
        }
        // let textExtra = $("#textExtra_" + this.currentSign);
        // textExtra.css({"color" : color + ""});
    }

    addCheckImage(page) {
        let signRequestParams =  this.addSign(page, false, false);
        this.changeSignImage(-1, signRequestParams);
    }

    addTimesImage(page) {
        let signRequestParams =  this.addSign(page, false, false);
        this.changeSignImage(-2, signRequestParams);
    }

}
