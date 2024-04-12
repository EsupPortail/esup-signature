import {SignRequestParams} from "../../../prototypes/SignRequestParams.js?version=@version@";
import {EventFactory} from "../../utils/EventFactory.js?version=@version@";

export class SignPosition extends EventFactory {

    constructor(signType, currentSignRequestParamses, signImageNumber, signImages, userName, authUserName, signable, forceResetSignPos, isOtp, phone, csrf) {
        super();
        console.info("Starting sign positioning tools");
        this.userName = userName;
        this.authUserName = authUserName;
        this.pdf = $("#pdf");
        this.signImages = signImages;
        this.isOtp = isOtp;
        this.phone = phone;
        this.csrf = csrf;
        this.currentSignRequestParamsNum = 0;
        this.currentSignRequestParamses = currentSignRequestParamses;
        if(currentSignRequestParamses != null) {
            this.currentSignRequestParamses.sort((a, b) => (a.xPos > b.xPos) ? 1 : ((b.xPos > a.xPos) ? -1 : 0))
            this.currentSignRequestParamses.sort((a, b) => (a.yPos > b.yPos) ? 1 : ((b.yPos > a.yPos) ? -1 : 0))
            this.currentSignRequestParamses.sort((a, b) => (a.signPageNumber > b.signPageNumber) ? 1 : ((b.signPageNumber > a.signPageNumber) ? -1 : 0))
        }
        this.signRequestParamses = new Map();
        this.id = 0;
        this.currentScale = 1;
        this.scrollTop = 0;
        this.signType = signType;
        this.forwardButton = $("#forward-btn");
        this.addSignButton = $("#addSignButton")
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        if(localStorage.getItem("scale") != null) {
            this.currentScale = localStorage.getItem("scale");
        }
        if (this.signType === "visa") {
            $("#visualButton").remove();
        }
        this.initListeners();
    }

    initListeners() {
        let self = this;
        $(window).on('scroll', function() {
            self.scrollTop = $(this).scrollTop();
        });
    }

    removeSign(id) {
        this.signRequestParamses.delete(id);
        if(this.signRequestParamses.size === 0) {
            this.addSignButton.addClass("pulse-primary");
            $("#addSignButton2").addClass("pulse-primary");
            $("#addSignButton").removeAttr("disabled");
            $(window).unbind("beforeunload");
            this.enableForwardButton();
        }
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

    disableForwardButton() {
        if(this.forwardButton.length) {
            this.forwardButton.addClass("disabled");
        }
    }

    enableForwardButton() {
        if(this.forwardButton.length) {
            this.forwardButton.removeClass("disabled");
        }
    }

    addSign(page, restore, signImageNumber, forceSignNumber) {
        this.disableForwardButton();
        $(window).bind("beforeunload",function(event) {
            return "You have some unsaved changes";
        });
        this.addSignButton.removeClass("pulse-primary");
        $("#addSignButton2").removeClass("pulse-primary");
        let id = this.id;
        let currentSignRequestParams = null;
        if(signImageNumber != null && signImageNumber >= 0 && signImageNumber !== 999999) {
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
        if(signImageNumber != null) {
            let favoriteSignRequestParams = currentSignRequestParams;
            if (signImageNumber === 999999) {
                id = 999999;
                this.signRequestParamses.set(id, new SignRequestParams(null, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, false, false, false, false, signImageNumber, this.scrollTop, this.csrf, this.signType));
            } else if(signImageNumber >= 0) {
                if(JSON.parse(sessionStorage.getItem("favoriteSignRequestParams")) != null) {
                    favoriteSignRequestParams = JSON.parse(sessionStorage.getItem("favoriteSignRequestParams"));
                    if(currentSignRequestParams != null) {
                        favoriteSignRequestParams.xPos = currentSignRequestParams.xPos;
                        favoriteSignRequestParams.yPos = currentSignRequestParams.yPos;
                    }
                }
                this.signRequestParamses.set(id, new SignRequestParams(favoriteSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, restore, true, this.signType === "visa", this.signType === "certSign" || this.signType === "nexuSign", this.isOtp, this.phone, false, this.signImages, this.scrollTop));
            } else {
                this.signRequestParamses.set(id, new SignRequestParams(favoriteSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, this.signType === "certSign" || this.signType === "nexuSign", this.isOtp, this.phone, false, null, this.scrollTop));
            }
            this.signRequestParamses.get(id).changeSignImage(signImageNumber);
        } else {
            this.signRequestParamses.set(id, new SignRequestParams(null, id, this.currentScale, page, this.userName, this.authUserName, restore, signImageNumber != null && signImageNumber >= 0, false, this.signType === "certSign" || this.signType === "nexuSign", this.isOtp, this.phone, false, null, this.scrollTop));
        }
        this.signRequestParamses.get(id).addEventListener("unlock", e => this.lockSigns());
        this.signRequestParamses.get(id).addEventListener("delete", e => this.removeSign(id));
        if (signImageNumber != null && signImageNumber >= 0) {
            this.signRequestParamses.get(id).cross.addClass("drop-sign");
        }
        if (signImageNumber < 0) {
            $("#signImage_" + id).addClass("d-none");
        }
        this.signRequestParamses.get(id).addEventListener("sizeChanged", e => this.signRequestParamses.get(id).simulateDrop());
        let srp = this.signRequestParamses.get(id);
        this.id++;
        return srp;
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

    addMinusImage(page) {
        this.addSign(page, false, -4);
    }

    addText(page) {
        let signRequestParams = this.addSign(page, false, null);
        signRequestParams.turnToText();
        signRequestParams.cross.css("background-image", "");
        signRequestParams.changeSignSize(null);
    }
}