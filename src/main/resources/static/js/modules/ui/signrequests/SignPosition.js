import {SignRequestParams} from "../../../prototypes/SignRequestParams.js?version=@version@";
import {EventFactory} from "../../utils/EventFactory.js?version=@version@";
import {UserUi} from '../users/UserUi.js?version=@version@';

export class SignPosition extends EventFactory {

    constructor(signType, currentSignRequestParamses, currentStepMultiSign, currentStepSingleSignWithAnnotation, signImageNumber, signImages, userName, authUserName, signable, forceResetSignPos, isOtp, phone, csrf) {
        super();
        console.info("Starting sign positioning tools");
        this.userName = userName;
        this.authUserName = authUserName;
        this.signImages = signImages;
        this.currentStepMultiSign = currentStepMultiSign;
        this.currentStepSingleSignWithAnnotation = currentStepSingleSignWithAnnotation;
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
        this.signsList = [];
        this.currentScale;
        this.scrollTop = 0;
        this.signType = signType;
        this.forwardButton = $("#forward-btn");
        this.addSignButton = $("#addSignButton");
        $("#signLaunchButton").focus();
        $("#addSignButton2").focus();
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        // if(localStorage.getItem("scale") != null) {
        //     this.currentScale = localStorage.getItem("scale");
        // }
        this.initListeners();
    }

    initListeners() {
        let self = this;
        $(window).on('scroll', function(e) {
            self.scrollTop = $(this).scrollTop();
        });
        $(document).ready(function() {
            if(self.signImages != null && self.signImages.length === 1) {
                self.popUserUi();
            }
        });
    }

    removeSign(id) {
        this.signRequestParamses.delete(id);
        if(this.signsList.includes(id)) {
            this.signsList.splice(this.signsList.indexOf(id), 1);
        }
        if(this.signsList.length === 0) {
            $('#addSignButton').removeAttr('disabled');
        }
        if(this.signRequestParamses.size === 0) {
            let addSignButton2 = $("#addSignButton2");
            addSignButton2.removeClass("d-none");
            addSignButton2.addClass("pulse-primary");
            $("#signLaunchButton").removeClass("pulse-success");
            addSignButton2.focus();
            $("#addSignButton").removeAttr("disabled");
            $(window).unbind("beforeunload");
            this.enableForwardButton();
        }
    }

    updateScales(scale) {
        console.info("update sign scale from " + this.currentScale + " to " + scale);
        this.currentScale = scale;
        this.signRequestParamses.forEach(function (signRequestParams){
            signRequestParams.updateScale(scale);
        });
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

    popUserUi() {
        if (this.userUI == null) {
            this.userUI = new UserUi();
        }
        $("#add-sign-image").modal("show");
    }

    addSign(page, restore, signImageNumber, forceSignNumber, signField) {
        if (this.signImages != null && this.signImages.length === 1) {
            this.popUserUi();
            return;
        }
        this.disableForwardButton();
        $(window).bind("beforeunload", function (event) {
            console.log("beforeunload déclenché");
            event.preventDefault();
            event.returnValue = "";
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
                this.signRequestParamses.get(id).addEventListener("sizeChanged", e => this.signRequestParamses.get(id).simulateDrop());
                this.signRequestParamses.get(id).changeSignSize(null);

            } else if(signImageNumber >= 0) {
                if(this.currentStepMultiSign === false && this.signsList.length > 0) {
                    alert("Impossible d'ajouter plusieurs signatures sur cette étape");
                    return;
                }
                if(JSON.parse(sessionStorage.getItem("favoriteSignRequestParams")) != null) {
                    favoriteSignRequestParams = JSON.parse(sessionStorage.getItem("favoriteSignRequestParams"));
                    if(currentSignRequestParams != null) {
                        favoriteSignRequestParams.xPos = currentSignRequestParams.xPos;
                        favoriteSignRequestParams.yPos = currentSignRequestParams.yPos;
                    }
                }
                this.signRequestParamses.set(id, new SignRequestParams(favoriteSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, restore, true, this.signType === "visa", this.signType === "certSign" || this.signType === "nexuSign", this.isOtp, this.phone, false, this.signImages, this.scrollTop));
                this.signsList.push(id);
                if(this.currentStepMultiSign === false && this.signRequestParamses.size > 0) {
                    if(this.currentStepSingleSignWithAnnotation === false) {
                        $('#insert-btn').attr('disabled', 'disabled');
                    } else {
                        $('#addSignButton').attr('disabled', 'disabled');
                    }
                }
            } else {
                if(this.currentStepMultiSign === false && this.currentStepSingleSignWithAnnotation === false) {
                    alert("Impossible d'ajouter des annotations sur cette étape");
                    return;
                }
                this.signRequestParamses.set(id, new SignRequestParams(favoriteSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, this.signType === "certSign" || this.signType === "nexuSign", this.isOtp, this.phone, false, null, this.scrollTop));
            }
            if(signImageNumber !== 999999) {
                this.signRequestParamses.get(id).changeSignImage(signImageNumber);
            }
        } else {
            if(this.currentStepMultiSign === false && this.currentStepSingleSignWithAnnotation === false) {
                alert("Impossible d'ajouter des annotations sur cette étape");
                return;
            }
            this.signRequestParamses.set(id, new SignRequestParams(null, id, this.currentScale, page, this.userName, this.authUserName, restore, signImageNumber != null && signImageNumber >= 0, false, this.signType === "certSign" || this.signType === "nexuSign", this.isOtp, this.phone, false, null, this.scrollTop));
        }
        // this.signRequestParamses.get(id).addEventListener("unlock", e => this.lockSigns());
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
        if(signRequestParams != null) {
            signRequestParams.turnToText();
            signRequestParams.cross.css("background-image", "");
            signRequestParams.changeSignSize(null);
            signRequestParams.textareaPart.focus();
        }
    }

    getBrowserZoom() {
        return window.devicePixelRatio || 1;
    }

}