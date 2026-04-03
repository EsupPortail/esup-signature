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
        this.scrollTop = this.getCurrentScrollTop();
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

    getScrollContainer() {
        return document.getElementById("workspace");
    }

    getCurrentScrollTop() {
        const workspace = this.getScrollContainer();
        return workspace ? workspace.scrollTop : window.scrollY;
    }

    initListeners() {
        let self = this;
        const workspace = this.getScrollContainer();
        if (workspace) {
            workspace.addEventListener('scroll', function() {
                self.scrollTop = workspace.scrollTop;
            });
        } else {
            $(window).on('scroll', function() {
                self.scrollTop = $(this).scrollTop();
            });
        }
        $(document).ready(function() {
            if(self.signImages != null && self.signImages.length === 1) {
                self.popUserUi();
            }
        });
    }

    removeSign(srpId, id) {
        if(srpId != null) {
            this.currentSignRequestParamses[srpId].ready = false;
        }
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
            addSignButton2.addClass("pulse-success");
            $("#signLaunchButton").removeClass("pulse-success");
            addSignButton2.focus();
            $("#addSignButton").removeAttr("disabled");
            $(window).unbind("beforeunload");
            this.enableForwardButton();
            this.goStep1();
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

    addSign(page, restore, signImageNumber, forceSignNumber) {
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
        this.addSignButton.removeClass("pulse-success");
        $("#addSignButton2").removeClass("pulse-success");
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
                this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, null, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, false, null, false, signImageNumber, this.scrollTop, this.csrf, this.signType));
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
                this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, favoriteSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, restore, true, this.signType === "visa", this.isOtp, this.phone, false, this.signImages, this.scrollTop));
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
                this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, favoriteSignRequestParams, id, this.currentScale, page, this.userName, this.authUserName, false, false, false, this.isOtp, this.phone, false, null, this.scrollTop));
            }
            if(signImageNumber !== 999999) {
                if(this.signType !== "visa") {
                    this.signRequestParamses.get(id).changeSignImage(signImageNumber);
                }
            }
        } else {
            if(this.currentStepMultiSign === false && this.currentStepSingleSignWithAnnotation === false) {
                alert("Impossible d'ajouter des annotations sur cette étape");
                return;
            }
            this.signRequestParamses.set(id, new SignRequestParams(this.isOtp, null, id, this.currentScale, page, this.userName, this.authUserName, restore, signImageNumber != null && signImageNumber >= 0, false, this.isOtp, this.phone, false, null, this.scrollTop));
        }
        this.signRequestParamses.get(id).addEventListener("delete", e => this.removeSign(e, id));
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

    setStepState(step, active, complete, disabled) {
        step.toggleClass("active", active);
        step.toggleClass("complete", complete);
        step.toggleClass("disable", disabled);
    }

    setButtonVariant(button, activeClass) {
        button.removeClass("btn-secondary btn-success btn-danger");
        button.addClass(activeClass);
    }

    setCertTypeHighlight(isHighlighted) {
        let selectCertType = $("#certType");
        selectCertType.toggleClass("border-success", isHighlighted);
        selectCertType.toggleClass("border-light-subtle", !isHighlighted);
        if(!isHighlighted) {
            selectCertType.trigger("blur");
        }
    }

    goStep1() {
        let step1 = $("#step-1");
        let step2 = $("#step-2");
        let step3 = $("#step-3");
        let addSignButton = $("#addSignButton2");
        let insertBtn = $("#insert-btn");
        let refuseLaunchButton = $("#refuseLaunchButton");
        let signLaunchButton = $("#signLaunchButton");
        let selectCertType = $("#certType");
        let refuseLaunchDiv = $("#refuseLaunchDiv");

        addSignButton.removeAttr("disabled");
        insertBtn.removeAttr("disabled");
        refuseLaunchButton.removeAttr("disabled");
        signLaunchButton.attr("disabled", "disabled");
        selectCertType.attr("disabled", "disabled");
        refuseLaunchDiv.removeClass("d-none");

        this.setButtonVariant(addSignButton, "btn-success");
        this.setButtonVariant(insertBtn, "btn-success");
        this.setButtonVariant(refuseLaunchButton, "btn-danger");
        this.setButtonVariant(signLaunchButton, "btn-secondary");
        this.setCertTypeHighlight(false);

        this.setStepState(step1, true, false, false);
        this.setStepState(step2, false, false, true);
        this.setStepState(step3, false, false, true);

        step1.find(".step-horizontal-v2-icon").html("1");
        step2.find(".step-horizontal-v2-icon").html("2");
    }

    goStep2() {
        let step1 = $("#step-1");
        let step2 = $("#step-2");
        let step3 = $("#step-3");
        let addSignButton = $("#addSignButton2");
        let insertBtn = $("#insert-btn");
        let refuseLaunchButton = $("#refuseLaunchButton");
        let selectCertType = $("#certType");
        let signLaunchButton = $("#signLaunchButton");
        let refuseLaunchDiv = $("#refuseLaunchDiv");

        addSignButton.attr("disabled", "disabled");
        refuseLaunchButton.attr("disabled", "disabled");
        insertBtn.removeAttr("disabled");
        signLaunchButton.attr("disabled", "disabled");
        refuseLaunchDiv.addClass("d-none");

        this.setButtonVariant(addSignButton, "btn-secondary");
        this.setButtonVariant(insertBtn, "btn-success");
        this.setButtonVariant(refuseLaunchButton, "btn-secondary");
        this.setButtonVariant(signLaunchButton, "btn-secondary");

        this.setStepState(step1, false, true, false);
        this.setStepState(step2, true, false, false);
        this.setStepState(step3, false, false, true);

        this.setCertTypeHighlight(false);
        step1.find(".step-horizontal-v2-icon").html("<i class='fi fi-rr-check'></i>");
        step2.find(".step-horizontal-v2-icon").html("2");
        let countEnable = selectCertType.find("option:not(:disabled):not([unavailable]").length;
        if(countEnable === 1) {
            selectCertType.find("option:not(:disabled):not([unavailable]").prop("selected", true);
            selectCertType.trigger("change");
            this.goStep3();
        } else {
            selectCertType.trigger("focus");
        }
        let countVisible = selectCertType.find("option:not([unavailable])").length;
        if(countVisible > 1) {
            selectCertType.removeAttr("disabled");
        }
    }

    goStep3() {
        let step1 = $("#step-1");
        let step2 = $("#step-2");
        let step3 = $("#step-3");
        let signLaunchButton = $("#signLaunchButton");

        this.setStepState(step1, false, true, false);
        this.setStepState(step2, false, true, false);
        this.setStepState(step3, true, false, false);

        signLaunchButton.removeAttr("disabled");
        signLaunchButton.removeClass("btn-secondary");
        signLaunchButton.addClass("btn-success");
        this.setCertTypeHighlight(false);

        step1.find(".step-horizontal-v2-icon").html("<i class='fi fi-rr-check'></i>");
        step2.find(".step-horizontal-v2-icon").html("<i class='fi fi-rr-check'></i>");
    }

}