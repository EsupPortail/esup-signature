import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {EventFactory} from "../../utils/EventFactory.js";
import {Color} from "../../utils/Color.js";

export class SignPosition extends EventFactory {

    constructor(signType, currentSignRequestParamses, signImageNumber, signImages, userName, authUserName, signable, forceResetSignPos, isOtp) {
        super();
        console.info("Starting sign positioning tools");
        this.userName = userName;
        this.authUserName = authUserName;
        this.pdf = $("#pdf");
        this.signImages = signImages;
        this.isOtp = isOtp;
        this.currentSignRequestParamsNum = 0;
        this.currentSignRequestParamses = currentSignRequestParamses;
        this.currentSignRequestParamses.sort((a,b) => (a.xPos > b.xPos) ? 1 : ((b.xPos > a.xPos) ? -1 : 0))
        this.currentSignRequestParamses.sort((a,b) => (a.yPos > b.yPos) ? 1 : ((b.yPos > a.yPos) ? -1 : 0))
        this.currentSignRequestParamses.sort((a,b) => (a.signPageNumber > b.signPageNumber) ? 1 : ((b.signPageNumber > a.signPageNumber) ? -1 : 0))
        this.signRequestParamses = new Map();
        this.id = 0;
        this.currentScale = 1;
        this.signType = signType;
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        if(localStorage.getItem("scale") != null) {
            this.currentScale = localStorage.getItem("scale");
        }
        if (this.signType === "visa") {
            $("#visualButton").remove();
        }
    }

    removeSign(id) {
        this.signRequestParamses.delete(id);
        if(this.signRequestParamses.size === 0) {
            $("#addSignButton").removeAttr("disabled");
        }
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
        if(imageNum != null && imageNum >= 0 && this.signImages != null && imageNum <= this.signImages.length - 1) {
            signRequestParams.signImageNumber = imageNum;
            console.debug("debug - " + "change sign image to " + imageNum);
            let img = null;
            if(this.signImages[imageNum] != null) {
                img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
                signRequestParams.cross.css("background-image", "url('" + img + "')");
                let sizes = this.getImageDimensions(img);
                sizes.then(result => signRequestParams.changeSignSize(result));
            }
        } else if(imageNum < 0) {
            signRequestParams.signImageNumber = imageNum;
            let self = this;
            this.convertImgToBase64URL('/images/' + this.faImages[Math.abs(imageNum) - 1] + '.png', function(img) {
                signRequestParams.cross.css("background-image", "url('" + img + "')");
                let sizes = self.getImageDimensions(img);
                sizes.then(result => signRequestParams.changeSignSize(result));
            });
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


    addSign(page, restore, signImageNumber, forceSignNumber) {
        let id = this.id;
        let currentSignRequestParams = null;
        if(signImageNumber != null && signImageNumber >= 0) {
            if(forceSignNumber != null) {
                currentSignRequestParams = this.currentSignRequestParamses[forceSignNumber];
            } else {
                for (let i = 0; i < this.currentSignRequestParamses.length; i++) {
                    if (this.currentSignRequestParamses[i].ready == null || !this.currentSignRequestParamses[i].ready) {
                        currentSignRequestParams = this.currentSignRequestParamses[i];
                        break;
                    }
                }
            }
        }
        this.signRequestParamses.set(id, new SignRequestParams(currentSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, restore, signImageNumber != null && signImageNumber >= 0, this.signType === "visa", this.signType === "certSign" || this.signType === "nexuSign", this.isOtp));
        if(signImageNumber != null) {
            this.changeSignImage(signImageNumber, this.signRequestParamses.get(id));
        }
        this.signRequestParamses.get(id).addEventListener("unlock", e => this.lockSigns());
        this.signRequestParamses.get(id).addEventListener("delete", e => this.removeSign(id));
        this.signRequestParamses.get(id).addEventListener("nextSign", e => this.changeSignImage(this.signRequestParamses.get(id).signImageNumber + 1, this.signRequestParamses.get(id)));
        this.signRequestParamses.get(id).addEventListener("prevSign", e => this.changeSignImage(this.signRequestParamses.get(id).signImageNumber - 1, this.signRequestParamses.get(id)));
        this.signRequestParamses.get(id).addEventListener("changeColor", e => this.changeSignColor(e, this.signRequestParamses.get(id)));
        if(signImageNumber != null && signImageNumber >= 0) {
            this.signRequestParamses.get(id).cross.addClass("drop-sign");
        }
        this.signRequestParamses.get(id).simulateDrop();
        this.id++;
        return this.signRequestParamses.get(id);
    }

    addCheckImage(page) {
        this.addSign(page, false, -1);
    }

    addTimesImage(page) {
        this.addSign(page, false, -2);
    }

    addCircleImage(page) {
        this.addSign(page, false, -3);
    }

    addText(page) {
        let signRequestParams = this.addSign(page, false, null);
        signRequestParams.turnToText();
        signRequestParams.cross.css("background-image", "");
        signRequestParams.changeSignSize(null);
    }

    // addTimesImage(page) {
    //     let signRequestParams =  this.addSign(page, false, false);
    //     this.changeSignImage(-2, signRequestParams);
    // }
}
