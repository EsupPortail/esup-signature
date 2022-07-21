import {EventFactory} from "../modules/utils/EventFactory.js?version=@version@";
import {Color} from "../modules/utils/Color.js?version=@version@";

export class SignRequestParams extends EventFactory {

    constructor(signRequestParamsModel, id, scale, page, userName, authUserName, restore, isSign, isVisa, isElec, isOtp, phone, light, signImages, scrollTop) {
        super();
        Object.assign(this, signRequestParamsModel);
        this.id = id;
        this.userUI = null;
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        this.signImages = signImages;
        this.currentScale = parseFloat(scale);
        this.signPageNumber = 1;
        if(page != null) this.signPageNumber = page;
        this.isOtp = isOtp;
        this.phone = phone;
        this.light = light;
        this.userName = userName;
        this.authUserName = authUserName;
        this.ready = null;
        this.isShare = userName !== authUserName;
        this.restore = restore;
        this.isSign = isSign;
        this.isVisa = isVisa;
        this.isElec = isElec;
        this.firstLaunch = true;
        this.cross = null;
        this.border = null;
        this.tools = null;
        this.divExtra = null;
        this.textareaExtra = null;
        this.textareaPart = null;
        this.textPart = "";
        this.signColorPicker = null;
        this.pdSignatureFieldName = null;
        this.restoreExtraOnTop = false;
        this.allPages = false;
        this.signImageNumber = 0;
        this.originalWidth = 150;
        this.originalHeight = 75;
        this.signWidth = 150;
        this.signHeight = 75;
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.savedText = "";
        this.offset = 0;
        this.scrollTop = scrollTop;
        if(!light) {
            this.offset = ($("#page_" + this.signPageNumber).offset().top) + (10 * (parseInt(this.signPageNumber) - 1));
        }
        if(light) {
            this.initLight();
        } else {
            this.signScale = 1;
            this.red = 0;
            this.green = 0;
            this.blue = 0;
            this.fontSize = 12;
            this.restoreExtra = false;
            this.addImage = true;

            if(restore) {
                this.addExtra = false;
                this.addWatermark = false;
                this.extraText = "";
                this.extraOnTop = true;
                this.extraType = true;
                this.extraName = true;
                this.extraDate = true;
                this.isExtraText = true;
            }
            this.init();
            if(!restore && isSign) {
                this.restoreFromFavorite();
            }
        }
        this.stringLength = 1;
        if(signRequestParamsModel == null || (this.xPos===0 && this.yPos===0)) {
            this.xPos = (parseInt($("#pdf").css("width")) / 2 / scale) - (this.signWidth * scale / 2);
            let mid = scrollTop + $(window).height() / 2;
            this.yPos = (mid - this.offset) / scale;
        }
        this.initEventListeners();
   }

    initEventListeners() {
        let self = this;
        if(self.light == null || !self.light) {
            this.cross.on("mousedown click", function(e) {
                e.stopPropagation();
                self.wantUnlock();
            });

            $("#crossTools_" + this.id).on("click", function(e) {
                e.stopPropagation();
            });
        }
        $("#signDrop_" + this.id).on("click", e => this.deleteSign());
        $("#signNextImage_" + this.id).on("click", e => this.changeSignImage(parseInt(this.signImageNumber) + 1));
        $("#signPrevImage_" + this.id).on("click", e => this.prevSignImage());
        $("#displayMoreTools_" + this.id).on("click", e => this.displayMoreTools());
        $("#hideMoreTools_" + this.id).on("click", e => this.hideMoreTools());
        $("#watermark_" + this.id).on("click", e => this.toggleWatermark());
        $("#allPages_" + this.id).on("click", e => this.toggleAllPages());
        $("#signImage_" + this.id).on("click", e => this.toggleImage());
        $("#signExtra_" + this.id).on("click", e => this.toggleExtra());
        $("#signExtraOnTop_" + this.id).on("click", e => this.toggleExtraOnTop());

        $("#extraType_" + this.id).on("click", e => this.toggleType());
        $("#extraName_" + this.id).on("click", e => this.toggleName());
        $("#extraDate_" + this.id).on("click", e => this.toggleDate());
        $("#extraText_" + this.id).on("click", e => this.toggleText());
    }

    initLight() {
        this.cross = $("#cross");
        this.border = $("#borders");
        this.tools = $("#crossTools");
        this.restoreFromFavorite();
    }

    restoreFromFavorite() {
        let text = this.extraText;
        this.addExtra = !this.addExtra;
        this.toggleExtra();
        if(this.divExtra != null) {
            this.extraOnTop = !this.extraOnTop;
            this.toggleExtraOnTop();
            this.extraType = !this.extraType;
            this.toggleType();
            this.extraName = !this.extraName;
            this.toggleName();
            this.extraDate = !this.extraDate;
            this.toggleDate();
            this.extraText = text;
            this.isExtraText = !(this.extraText !== "");
            this.toggleText();
            this.textareaExtra.val(text);
        }
        this.addWatermark = !this.addWatermark;
        this.toggleWatermark();
        this.toggleWatermark();
        this.toggleWatermark();
    }

    init() {
        let divName = "cross_" + this.id;
        let div = "<div id='"+ divName +"' class='cross'></div>";
        $("#pdf").prepend(div);
        this.cross = $("#" + divName);
        this.cross.css("position", "absolute");
        this.cross.css("z-index", "5");
        this.cross.attr("data-id", this.id);
        let self = this;
        this.madeCrossDraggable();
        this.cross.resizable({
            aspectRatio: true,
            resize: function(event, ui) {
                if(self.isVisa) {
                    let newScale = self.getNewScale(self, ui);
                    self.signWidth = self.signWidth / self.signScale * newScale;
                    self.signHeight = self.signHeight / self.signScale * newScale;
                    self.extraWidth = self.extraWidth / self.signScale * newScale;
                    self.extraHeight = self.extraHeight / self.signScale * newScale;
                    self.signScale = newScale
                    self.refreshExtraDiv();
                    self.updateSize();
                } else if(self.textareaPart != null) {
                    self.signScale = self.getNewScale(self, ui);
                    self.resizeText();
                    self.signWidth = parseInt(self.textareaPart.css("width")) / self.currentScale;
                    self.extraWidth = self.extraWidth / self.signScale * newScale;
                } else {
                    let maxWidth = ((self.originalWidth + self.extraWidth / self.signScale) * 2 * self.currentScale);
                    let maxHeight = ((self.originalHeight + self.extraHeight / self.signScale) * 2 * self.currentScale);
                    let minWidth = ((self.originalWidth + self.extraWidth / self.signScale) * .5 * self.currentScale);
                    let minHeight = ((self.originalHeight + self.extraHeight / self.signScale) * .5 * self.currentScale);
                    if (ui.size.width >= maxWidth
                        ||
                        ui.size.height >= maxHeight
                    ) {
                        ui.size.width = maxWidth;
                        ui.size.height = maxHeight;
                    } else if (ui.size.width <= minWidth
                        ||
                        ui.size.height <= minHeight) {
                        ui.size.width = minWidth;
                        ui.size.height = minHeight;
                    }
                    let newScale = self.getNewScale(self, ui);
                    self.signWidth = self.signWidth / self.signScale * newScale;
                    self.signHeight = self.signHeight / self.signScale * newScale;
                    self.extraWidth = self.extraWidth / self.signScale * newScale;
                    if (self.addExtra) {
                        if (!self.extraOnTop) {
                            self.divExtra.css('width', Math.round(self.extraWidth * self.currentScale) + "px");
                        } else {
                            self.divExtra.css('width', Math.round(self.originalWidth * self.signScale * self.currentScale) + "px");
                        }
                    }
                    self.extraHeight = self.extraHeight / self.signScale * newScale;
                    self.signScale = newScale;
                    if(self.addImage) {
                        self.cross.css('background-size', Math.round(ui.size.width - self.extraWidth * self.currentScale) + "px");
                    }
                    if (self.addExtra) {
                        self.refreshExtraDiv();
                    }
                }
            },
            stop: function(event, ui) {
                self.signScale = self.getNewScale(self, ui);
                if(self.isSign) {
                    localStorage.setItem("zoom", self.signScale);
                }
            }
        });

        let border = "<div id='border_" + this.id +"' class='static-border' style='width: 100%; height: 100%;'></div>"
        this.cross.prepend(border);
        this.border = $("#border_" + this.id);
        this.border.css("pointer-events", "none");

        let tools = this.getTools()
        tools.removeClass("d-none");
        this.cross.prepend(tools);
        this.tools = tools;

        this.extraWidth = 0;
        this.extraHeight = 0;
        this.moreTools = $("#moreTools_" + this.id);
        this.defaultTools = $("#defaultTools_" + this.id);
        if(this.isSign) {
            this.createColorPicker();
        } else {
            this.toggleMinimalTools();
        }
        if(this.restore) {
            if(localStorage.getItem("zoom") != null) {
                this.signScale = parseFloat(localStorage.getItem("zoom"));
            }
        }
        if(this.isVisa || this.isSign) {
            this.signHeight = 0;
            this.cross.css('width', (this.signWidth * this.currentScale * this.signScale));
            this.cross.css('height', (this.signHeight * this.currentScale * this.signScale));
            if(this.isVisa) {
                this.toggleMinimalTools();
                this.toggleExtra();
                this.refreshExtraDiv();
                this.updateSize();
            }
        }
        if(this.restore && this.isSign) {
            if (JSON.parse(localStorage.getItem('signNumber')) != null) {
                this.fireEvent("nextSign", localStorage.getItem('signNumber'));
            }
            if (!this.isVisa && localStorage.getItem('addExtra') != null) {
                if (localStorage.getItem('addExtra') === "true") {
                    this.addExtra = false;
                    this.toggleExtra();
                    this.restoreExtra = true;
                }
            }
            this.restoreUserParams();
        }
        if(this.isVisa) {
            if(!this.restore) {
                this.toggleText();
            }
            this.refreshExtraDiv();
            this.updateSize();
        }
        if(this.isShare) {
            this.toggleMinimalTools();
            this.signColorPicker.spectrum("destroy");
            this.signColorPicker.hide();
            this.toggleExtra();
            this.toggleName();
            this.toggleText();
            $("#extraTools_" + this.id).addClass("d-none");
            $("#crossTools_" + this.id).css("top", "-45px");
            this.textPart = this.userName + "\nP.O.\n" + this.authUserName;
            this.textareaExtra.val(this.textPart);
            this.textareaExtra.attr("readonly", true);
            this.refreshExtraDiv();
            this.updateSize();
        }

        if(this.isOtp){
            this.toggleExtra();
            this.toggleText()
            $("#extraTypeDiv_" + this.id).html("<span>Signature OTP : " + this.phone + "<br></span>");
            $("#extraTools_" + this.id).remove();
            $("#crossTools_" + this.id).css("top", "-45px");
        }
        this.cross.attr("page", this.signPageNumber);
    }

    restoreUserParams() {
        if (JSON.parse(localStorage.getItem('addWatermark')) != null) {
            if(JSON.parse(localStorage.getItem('addWatermark')) === true) {
                this.addWatermark = false;
                this.toggleWatermark();
            }
        }
        if(this.addExtra) {
            if (JSON.parse(localStorage.getItem('extraOnTop')) != null) {
                if (JSON.parse(localStorage.getItem('extraOnTop')) === false) {
                    if (this.divExtra != null && this.extraOnTop) {
                        this.toggleExtraOnTop();
                    }
                }
            }
            if (JSON.parse(localStorage.getItem('extraType')) != null) {
                if (JSON.parse(localStorage.getItem('extraType')) === false) {
                    if (this.divExtra != null && this.extraType) {
                        this.toggleType();
                    }
                }
            }
            if (JSON.parse(localStorage.getItem('extraName')) != null) {
                if (JSON.parse(localStorage.getItem('extraName')) === false) {
                    if (this.divExtra != null && this.extraName) {
                        this.toggleName();
                    }
                }
            }
            if (JSON.parse(localStorage.getItem('extraText')) != null) {
                if (JSON.parse(localStorage.getItem('extraText')) === false) {
                    if (this.divExtra != null && this.isExtraText) {
                        this.toggleText();
                    }
                }
            }
            if (JSON.parse(localStorage.getItem('extraDate')) != null) {
                if (JSON.parse(localStorage.getItem('extraDate')) === false) {
                    if (this.divExtra != null && this.extraDate) {
                        this.toggleDate();
                    }
                }
            }
        }
        if (JSON.parse(localStorage.getItem('addImage')) != null && this.signImages.length > 0) {
            if(JSON.parse(localStorage.getItem('addImage')) === false) {
                this.addImage = true;
                this.toggleImage();
            } else {
                let signImageBtn = $("#signImage_" + this.id);
                signImageBtn.addClass("btn-outline-light");
                if(!this.addExtra) {
                    signImageBtn.attr("disabled", true);
                }
            }
        } else {
            if(!this.addExtra) {
                $("#signImage_" + this.id).attr("disabled", true);
            }
        }
    }

    getNewScale(self, ui) {
        if (!self.addExtra || self.extraOnTop) {
            return Math.round((ui.size.width / self.currentScale) / (self.originalWidth) * 100) / 100;
        } else {
            return Math.round((ui.size.height / self.currentScale) / (self.originalHeight) * 100) / 100;
        }
    }

    madeCrossDraggable() {
        let self = this;
        this.cross.draggable({
            containment: "#pdf",
            scroll: false,
            drag: function(event, ui) {
                self.signPageNumber = self.cross.attr("page");
                self.xPos = Math.round(ui.position.left / self.currentScale);
                self.yPos = Math.round((ui.position.top - (($("#page_" + self.signPageNumber).offset().top) - $("#page_1").offset().top)) / self.currentScale);
                if(self.firstLaunch) {
                    self.firstLaunch = false;
                }
            }
        });
    }

    applyCurrentSignRequestParams(initialOffset) {
        let pageOffset = $("#page_" + this.signPageNumber).offset().top;
        let offset = pageOffset - initialOffset + (10 * (parseInt(this.signPageNumber)));
        this.cross.css('top', Math.round(this.yPos * this.currentScale + offset) + 'px');
        this.cross.css('left', Math.round(this.xPos * this.currentScale) + 'px');
    }

    deleteSign() {
        this.cross.attr("remove", "true");
        this.cross.simulate("drag", {
            dx: 9999999999,
            dy: 9999999999
        });
        this.cross.remove();
        this.fireEvent("delete", ["ok"]);
    }

    getTools() {
        let self = this;
        let tools = $("#crossTools_x").clone();
        tools.attr("id", tools.attr("id").split("_")[0] + "_" + self.id);
        tools.children().each(function (e) {
            $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + self.id);
        });
        tools.children().children().each(function (e) {
            if($(this).attr("id")) {
                if($(this).attr('id').split("_")[0] === "textExtra") {
                    $(this).remove();
                } else {
                    $(this).attr("id", $(this).attr("id").split("_")[0] + "_" + self.id);
                }
            }
        });
        return tools;
    }

    updateScale(scale) {
        let width = parseInt(this.cross.css("width"), 10);
        let height = parseInt(this.cross.css("height"), 10);
        let newWidth = Math.round(width / this.currentScale * scale);
        let newHeight = Math.round(height / this.currentScale * scale);
        let thisPos = this.cross.position();
        let x = thisPos.left;
        let y = thisPos.top;
        let xNew = Math.round((x / this.currentScale * scale));
        let yNew = Math.round((y / this.currentScale * scale));
        this.cross.css("width", newWidth + "px");
        this.cross.css("height", newHeight + "px");
        if(this.addImage) {
            if(this.extraOnTop) {
                this.cross.css('background-size', newWidth);
            } else {
                this.cross.css('background-size', newWidth / 2);
            }
        }
        if(this.addExtra) {
            this.divExtra.css("width", this.extraWidth * scale + "px");
        }
        this.cross.css('left', xNew + 'px');
        this.cross.css('top', yNew + 'px');
        this.currentScale = scale;
        if(this.divExtra != null) {
            this.refreshExtraDiv();
        }
        if(this.textareaPart != null) {
            this.resizeText();
        }
    }

    lock() {
        this.tools.addClass("d-none");
        this.border.removeClass("anim-border");
        this.border.addClass("static-border");
        if(this.textareaExtra != null) {
            this.textareaExtra.addClass("sign-textarea-lock");
        }
    }

    wantUnlock() {
        this.fireEvent("unlock", ["ok"]);
        this.unlock();
    }

    unlock() {
        this.border.removeClass("static-border");
        this.border.addClass("anim-border");
        this.tools.removeClass("d-none");
        if(this.textareaExtra != null) {
            this.textareaExtra.removeClass("sign-textarea-lock");
        }
    }


    prevSignImage() {
        if(this.signImageNumber > 0) {
            this.changeSignImage(parseInt(this.signImageNumber) - 1);
        } else {
            if(this.signImages.length > 0) {
                this.changeSignImage(this.signImages.length - 1);
            }
        }
    }

    changeSignSize(result) {
        if(result != null) {
            this.originalWidth = Math.round((result.w));
            this.originalHeight = Math.round((result.h));
            this.signWidth = Math.round(this.originalWidth * this.signScale) + this.extraWidth;
            this.signHeight = Math.round(this.originalHeight * this.signScale) + this.extraHeight;
            this.cross.css('width', (this.signWidth * this.currentScale));
            this.cross.css('height', (this.signHeight * this.currentScale));
            if(this.addImage) {
                this.cross.css('background-size', (this.signWidth - this.extraWidth) * this.currentScale);
            }
            this.updateSize();
        } else {
            this.signWidth = Math.round(parseInt(this.cross.css("width")) / this.currentScale);
            this.signHeight = Math.round(parseInt(this.cross.css("height")) / this.currentScale);
        }
    }

    show() {
        this.cross.css('opacity', '1');
        this.cross.draggable("enable");
        this.cross.css("z-index", 5);
    }

    hide() {
        this.cross.css('opacity', '0');
        this.cross.draggable("disable");
        this.cross.css("z-index", -1);
    }

    simulateDrop() {
        let x = Math.round(this.xPos * this.currentScale);
        let y = Math.round(this.yPos * this.currentScale + $("#page_" + this.signPageNumber).offset().top - $("#page_1").offset().top + (10 * (parseInt(this.signPageNumber) - 1)));
        let self = this;
        this.cross.on("dragstop", function(){
            let test = self.scrollTop + $(window).height();
            if(y > test) {
                window.scrollTo(0, y);
            }
            $(this).unbind("dragstop");
        });
        this.cross.simulate("drag", {
            handle: "corner",
            moves: 1,
            dx: x,
            dy: y
        });

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
        this.signColorPicker = $('#signColorPicker_' + this.id);
        this.signColorPicker.spectrum({
            type: "color",
            showPaletteOnly: true,
            hideAfterPaletteSelect: true,
            preferredFormat: "hex",
            change: color => this.changeSignColor(color)
        });
    }

    toggleAllPages() {
        if(this.allPages) {
            $("#allPages_" + this.id).removeClass("btn-outline-light");
            this.allPages = false;
        } else {
            $("#allPages_" + this.id).addClass("btn-outline-light");
            this.allPages = true;
        }
    }

    toggleWatermark() {
        if(this.addWatermark) {
            $("#watermark_" + this.id).removeClass("btn-outline-light");
            this.cross.removeClass("watermark-width");
            this.cross.removeClass("watermark-height");
            this.addWatermark = false;
        } else {
            $("#watermark_" + this.id).addClass("btn-outline-light");
            if(this.extraOnTop) {
                this.cross.addClass("watermark-width");
            } else {
                this.cross.addClass("watermark-height");
            }
            this.addWatermark = true;
        }
        if(!this.firstLaunch) {
            localStorage.setItem('addWatermark', this.addWatermark);
        }
    }

    toggleImage() {
        console.log("toggle sign image");
        if(this.addImage) {
            if(this.addExtra) {
                if(this.extraOnTop) {
                    this.restoreExtraOnTop = true;
                    this.toggleExtraOnTop();
                }
                this.divExtra.removeClass("div-extra-right");
                this.divExtra.addClass("div-extra-top");
                this.signImageNumber = 0;
                this.extraWidth = 0;
                this.signWidth = this.signWidth / 2;
                this.cross.css('background-size', 0);
                $("#signImage_" + this.id).removeClass("btn-outline-light");
                $("#signExtra_" + this.id).attr("disabled", true);
                $("#signExtraOnTop_" + this.id).attr("disabled", true);
                $("#signPrevImage_" + this.id).attr("disabled", true);
                $("#signNextImage_" + this.id).attr("disabled", true);
                localStorage.setItem('addImage', false);
                this.addImage = !this.addImage;
                this.refreshExtraDiv()
                this.updateSize();
            }
        } else {
            if(!this.extraOnTop) {
                this.divExtra.removeClass("div-extra-top");
                this.divExtra.addClass("div-extra-right");
            }
            this.signImageNumber = 0;
            this.extraWidth = this.signWidth;
            this.signWidth = this.signWidth * 2;
            this.cross.css('background-size', (this.signWidth - this.extraWidth) * this.currentScale);
            $("#signImage_" + this.id).addClass("btn-outline-light");
            $("#signExtra_" + this.id).attr("disabled", false);
            $("#signExtraOnTop_" + this.id).attr("disabled", false);
            $("#signPrevImage_" + this.id).attr("disabled", false);
            $("#signNextImage_" + this.id).attr("disabled", false);
            if(this.restoreExtraOnTop) {
                this.restoreExtraOnTop = false;
                this.toggleExtraOnTop();
            }
            localStorage.setItem('addExtra', true);
            localStorage.setItem('addImage', true);
            this.addImage = !this.addImage;
            this.refreshExtraDiv()
            this.updateSize();
        }

    }


    toggleExtra() {
        this.addExtra = !this.addExtra;
        let self = this;
        if(this.addExtra) {
            $("#signExtra_" + this.id).addClass("btn-outline-light");
            $("#signImage_" + this.id).attr("disabled", false);
            $("#signExtraOnTop_" + this.id).removeAttr("disabled");
            if(this.divExtra == null) {
                this.typeSign = "Signature calligraphique";
                if (this.isVisa) this.typeSign = "Visa";
                if (this.isElec) this.typeSign = "Signature électronique";
                let divExtraHtml = "<div id='divExtra_" + this.id + "' class='div-extra div-extra-top'></div>";
                this.cross.prepend(divExtraHtml);
                this.divExtra = $("#divExtra_" + this.id);
                this.divExtra.append("<span id='extraTypeDiv_"+ this.id +"' >" + this.typeSign + "<br/></span>");
                this.divExtra.append("<span id='extraNameDiv_"+ this.id +"' >" + this.userName + "<br/></span>");
                this.divExtra.append("<span id='extraDateDiv_"+ this.id +"'>le " + moment().format('DD/MM/YYYY HH:mm:ss [GMT]Z') + "<br/></span>");
                setInterval(function() {
                    self.refreshDate();
                }, 1000);
                this.addTextArea();
            } else {
                this.divExtra.removeClass("d-none");
            }
            $("#extraTools_" + this.id).removeClass("d-none");
            $("#crossTools_" + this.id).css("top", "-75px");
            this.refreshExtraDiv();
            this.extraHeight = Math.round(parseInt(this.divExtra.css("height")) / this.currentScale);
            this.signHeight += this.extraHeight;
            if(!this.restoreExtra && this.restore) {
                this.restoreUserParams();
                this.restoreExtra = true;
            }
        } else {
            if(!this.extraOnTop) {
                this.toggleExtraOnTop();
            }
            $("#signImage_" + this.id).attr("disabled", true);
            $("#signExtra_" + this.id).removeClass("btn-outline-light");
            $("#signExtraOnTop_" + this.id).attr("disabled", true);
            if(this.divExtra != null) {
                this.divExtra.addClass("d-none");
            }
            $("#extraTools_" + this.id).addClass("d-none");
            $("#crossTools_" + this.id).css("top", "-45px");
            this.signHeight -= this.extraHeight;
            this.extraHeight = 0;
        }
        // this.toggleType();
        // this.toggleName();
        // this.toggleText();
        this.updateSize();
        if(!this.firstLaunch) {
            localStorage.setItem('addExtra', this.addExtra);
        }
    }

    toggleExtraOnTop() {
        if(this.addWatermark) {
            this.cross.toggleClass("watermark-width watermark-height")
        }
        if(this.divExtra != null) {
            if(!this.extraOnTop) {
                this.divExtra.addClass("d-none");
                this.signWidth -= this.extraWidth;
                this.extraWidth = 0;
                // this.updateSize();
                this.divExtra.removeClass("d-none");
                this.extraOnTop = true;
                this.refreshExtraDiv();
                this.extraHeight = Math.round(parseInt(this.divExtra.css("height")) / this.currentScale);
                this.signHeight = this.originalHeight * this.signScale + this.extraHeight
                if(this.light == null || !this.light) {
                    this.cross.css("width", this.signWidth * this.currentScale + "px");
                    this.cross.css("height", this.signHeight * this.currentScale + "px");
                }
                this.divExtra.addClass("div-extra-top");
                this.divExtra.removeClass("div-extra-right");
            } else {
                $("#signExtraOnTop_" + this.id).removeClass("disabled");
                this.divExtra.addClass("d-none");
                this.signHeight -= this.extraHeight;
                this.extraHeight = 0;
                this.updateSize();
                this.divExtra.removeClass("d-none");
                this.extraOnTop = false;
                this.refreshExtraDiv();
                this.signWidth = parseInt(this.cross.css("width")) / this.currentScale * 2;
                this.extraWidth = this.signWidth / 2;
                if(this.light == null || !this.light) {
                    this.cross.css("width", this.signWidth * this.currentScale + "px");
                }
                this.divExtra.css("width", this.extraWidth * this.currentScale + "px");
                this.divExtra.addClass("div-extra-right");
                this.divExtra.removeClass("div-extra-top");
            }
            if(!this.firstLaunch) {
                localStorage.setItem('extraOnTop', this.extraOnTop);
            }
        }
        // this.updateSize();
    }

    refreshDate() {
        $("#extraDateDiv_" + this.id).html("le " + moment().format('DD/MM/YYYY HH:mm:ss [GMT]Z') + "<br/>");
    }

    toggleType() {
        if(!this.extraName && !this.extraDate && !this.isExtraText && this.extraType) return;
        if(this.extraType) {
            $("#extraTypeDiv_" + this.id).hide();
            $("#extraType_" + this.id).removeClass("btn-outline-light");
        } else {
            $("#extraTypeDiv_" + this.id).show();
            $("#extraType_" + this.id).addClass("btn-outline-light");
        }
        this.extraType = !this.extraType;
        this.updateSize();
        this.refreshExtraDiv();
        if(!this.firstLaunch) {
            localStorage.setItem('extraType', this.extraType);
        }
    }

    toggleName() {
        if(!this.extraType && !this.extraDate && !this.isExtraText && this.extraName) return;
        if(this.extraName) {
            $("#extraNameDiv_" + this.id).hide();
            $("#extraName_" + this.id).removeClass("btn-outline-light");
        } else {
            $("#extraNameDiv_" + this.id).show();
            $("#extraName_" + this.id).addClass("btn-outline-light");
        }
        this.extraName = !this.extraName;
        this.updateSize();
        this.refreshExtraDiv();
        if(!this.firstLaunch) {
            localStorage.setItem('extraName', this.extraName);
        }
    }

    toggleDate() {
        if(!this.extraType && !this.extraName && !this.isExtraText && this.extraDate) return;
        if(this.extraDate) {
            $("#extraDateDiv_" + this.id).hide();
            $("#extraDate_" + this.id).removeClass("btn-outline-light");
        } else {
            $("#extraDateDiv_" + this.id).show();
            $("#extraDate_" + this.id).addClass("btn-outline-light");
        }
        this.extraDate = !this.extraDate;
        this.updateSize();
        this.refreshExtraDiv();
        if(!this.firstLaunch) {
            localStorage.setItem('extraDate', this.extraDate);
        }
    }

    toggleText() {
        if((!this.extraType && !this.extraDate && !this.extraName && this.isExtraText) || this.divExtra == null) return;
        let textExtra = $("#textExtra_" + this.id);
        if(this.isExtraText) {
            $("#extraText_" + this.id).removeClass("btn-outline-light");
            textExtra.hide();
            this.savedText = this.textareaExtra.val();
            this.textareaExtra.val("");
            this.extraText = "";
        } else {
            textExtra.show();
            $("#extraText_" + this.id).addClass("btn-outline-light");
            this.extraText = this.savedText;
            this.textareaExtra.val(this.savedText);
        }
        // if(this.extraText === "") {
        //     this.extraText = this.textareaExtra.val();
        // } else {
        //     this.extraText = "";
        // }
        this.isExtraText = !this.isExtraText;
        this.updateSize();
        this.refreshExtraDiv();
        if(!this.firstLaunch) {
            localStorage.setItem('extraText', this.isExtraText);
        }
    }

    updateSize() {
        if(this.light == null || !this.light) {
            if(this.extraOnTop) {
                this.signHeight -= this.extraHeight;
                this.extraHeight = 0;
                if(this.divExtra != null) {
                    this.extraHeight = Math.round(parseInt(this.divExtra.css("height")) / this.currentScale);
                }
                this.signHeight += this.extraHeight;
                this.cross.css("height", this.signHeight * this.currentScale + "px");
            } else {
                this.signWidth -= this.extraWidth;
                if(this.addImage) {
                    this.extraWidth = Math.round(this.originalWidth * this.signScale);
                }
                this.signWidth += this.extraWidth;
                this.cross.css("width", this.signWidth * this.currentScale + "px");
                if(this.divExtra != null) {
                    this.divExtra.css("width", this.extraWidth * this.currentScale + "px");
                }
            }
        }
    }

    addTextArea() {
        let divExtraHtml = "<textarea id='textExtra_" + this.id + "' class='sign-textarea align-top' rows='1' cols='30'></textarea>";
        this.divExtra.append(divExtraHtml);
        this.textareaExtra = $("#textExtra_" + this.id);
        this.textareaExtra.css('width', '100%');
        this.textareaExtra.attr('cols', '30');
        this.textareaExtra.attr('rows', '1');
        this.textareaExtra.on("input", e => this.refreshExtraDiv());
    }

    refreshExtraDiv() {
        if(this.divExtra != null) {
            let maxLines = 2;
            if(this.extraOnTop) maxLines = 1;
            if(!this.extraName) maxLines++;
            if(!this.extraDate) maxLines++;
            if(!this.extraType) maxLines++;
            let fontSize = this.fontSize * this.currentScale * this.signScale;
            this.divExtra.css("font-size", Math.round(fontSize));
            let text = this.textareaExtra.val();
            let lines = text.split(/\r|\r\n|\n/);
            text = "";
            if(lines.length > maxLines) {
                lines.pop();
            }

            for(let i = 0; i < lines.length; i++) {
                let c = document.createElement("canvas");
                let ctx = c.getContext("2d");
                ctx.font = fontSize + "px Gravity";
                let txt = lines[i];
                console.log(ctx.measureText(txt).width + " " + this.textareaExtra.css("width"));
                if(ctx.measureText(txt).width < (parseInt(this.textareaExtra.css("width")) - (ctx.measureText(txt).width / txt.length) * 2)) {
                    text += txt;
                    this.stringLength = txt.length;
                } else {
                    text += txt.substring(0, this.stringLength);
                }
                if(i < lines.length - 1) {
                    text += "\n";
                }
            }
            this.extraText = text;
            this.textareaExtra.val(text);
            let rows = parseInt(this.textareaExtra.attr("rows"));
            if(lines.length !== rows) {
                this.textareaExtra.attr("rows", lines.length);
                this.updateSize();
            }
            if(this.light) {
                this.divExtra.css("font-size", "unset");
            }
        }
    }

    toggleMinimalTools() {
        $("#signPrevImage_" + this.id).hide();
        $("#signNextImage_" + this.id).hide();
        $("#hideMoreTools_" + this.id).hide();
        $("#signExtra_" + this.id).hide();
        $("#signExtraOnTop_" + this.id).hide();
        $("#watermark_" + this.id).hide();
        $("#allPages_" + this.id).hide();
        $("#signColorPicker_" + this.id).hide();
        this.addWatermark = true;
        this.toggleWatermark();
    }

    turnToText() {
        $("#signImage_" + this.id).remove();
        let self = this;
        let divExtraHtml = "<textarea id='textPart_" + this.id + "' class='sign-textarea align-top' rows='1' style='overflow: hidden'></textarea>";
        this.cross.append(divExtraHtml);
        this.textareaPart = $("#textPart_" + this.id);
        this.textareaPart.css('width', '100%');
        this.textareaPart.on("input", function () {
            self.resizeText();
        });
        this.resizeText();
        this.cross.resizable("destroy");
        let textGrow = $("#textGrow_" + this.id);
        let textReduce = $("#textReduce_" + this.id);
        textGrow.show();
        textReduce.show();
        textGrow.on("click", function () {
            self.fontSize = self.fontSize + 1
            self.resizeText();
        });
        textReduce.on("click", function () {
            self.fontSize = self.fontSize - 1
            self.resizeText();
        });
        this.cross.draggable("enable");
        this.textareaPart.css('pointer-events', 'none');
        this.textareaPart.focusout(function (){
            self.cross.draggable("enable");
            self.textareaPart.css('pointer-events', 'none');
        });
        this.cross.mouseup(function (){
            self.cross.draggable("disable");
            self.textareaPart.css('pointer-events', 'auto');
            self.textareaPart.focus();
        });
    }

    resizeText() {
        let fontSize = this.fontSize * this.currentScale * this.signScale;
        this.textareaPart.css("font-size", Math.round(fontSize));
        this.signWidth = Math.round(parseInt(this.textareaPart.css("width")) / this.currentScale);
        this.signHeight = Math.round(parseInt(this.textareaPart.css("height")) / this.currentScale);
        let text = this.textareaPart.val();
        this.textPart = text;
        let lines = text.split(/\r|\r\n|\n/);
        if(lines[0].length > 0) {
            this.textareaPart.css('width', '');
        }
        let width = 28;
        for(let i = 0; i < lines.length; i++) {
            if(lines[i].length >= width) {
                width = lines[i].length;
            }
        }
        this.textareaPart.attr("cols", width);
        this.textareaPart.attr("rows", lines.length);
        this.cross.css("width", this.textareaPart.css("width"));
        this.cross.css("height", this.textareaPart.css("height"));
    }

    changeSignImage(imageNum) {
        if(imageNum != null && imageNum >= 0) {
            if(this.signImages != null) {
                if(imageNum > this.signImages.length - 1) {
                    imageNum = 0;
                }
                this.signImageNumber = imageNum;
                console.debug("debug - " + "change sign image to " + imageNum);
                let img = null;
                if(this.signImages[imageNum] != null) {
                    img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
                    this.cross.css("background-image", "url('" + img + "')");
                    let sizes = this.getImageDimensions(img);
                    sizes.then(result => this.changeSignSize(result));
                    localStorage.setItem('signNumber', imageNum);
                } else {
                    let self = this;
                    $.get({
                        url: "/ws-secure/users/get-default-image",
                        success: function(data) {
                            img = "data:image/PNG;charset=utf-8;base64, " + data;
                            self.cross.css("background-image", "url('" + img + "')");
                            let sizes = self.getImageDimensions(img);
                            sizes.then(result => self.changeSignSize(result));
                            localStorage.setItem('signNumber', imageNum);                        }
                    });

                }
            }
        } else if(imageNum < 0) {
            this.signImageNumber = imageNum;
            let self = this;
            this.convertImgToBase64URL('/images/' + this.faImages[Math.abs(imageNum) - 1] + '.png', function(img) {
                self.cross.css("background-image", "url('" + img + "')");
                let sizes = self.getImageDimensions(img);
                sizes.then(result => self.changeSignSize(result));
            });
            this.addExtra = true;
            this.extraOnTop = true;
            this.toggleExtra();
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

    changeSignColor(color) {
        console.info("change color to : " + color);
        const rgb = Color.hexToRgb(color);

        this.red = rgb[0];
        this.green = rgb[1];
        this.blue = rgb[2];

        let cross = this.cross;
        if (this.signImages[this.signImageNumber] != null) {
            let img = "data:image/jpeg;charset=utf-8;base64" +
                ", " + this.signImages[this.signImageNumber];
            Color.changeColInUri(img, "#000000", color).then(function (e) {
                cross.css("background-image", "url('" + e + "')");
            })
        }
        let textExtra = $("#divExtra_" + this.id);
        textExtra.css({"color" : color + ""});
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

}