import {EventFactory} from "../modules/utils/EventFactory.js";

export class SignRequestParams  extends EventFactory {

    constructor(signRequestParams, id, scale, page, userName, authUserName, restore, isSign, isVisa, isElec, isOtp) {
        super();
        this.signRequestParams = signRequestParams;
        Object.assign(this, signRequestParams);
        this.id = id;
        this.currentScale = parseFloat(scale);
        this.signPageNumber = 1;
        this.isOtp = isOtp;
        if(page != null) this.signPageNumber = page;
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
        this.allPages = false;
        this.signImageNumber = 0;
        this.originalWidth = 150;
        this.originalHeight = 75;
        this.signWidth = 150;
        this.signHeight = 75;
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.signFieldPresent = true;
        if(signRequestParams == null) {
            this.xPos = (parseInt($("#pdf").css("width")) / 2 / scale) - (this.signWidth * scale / 2);
            let mid = $(window).scrollTop() + Math.floor($(window).height() / 2);
            this.yPos = Math.round(mid / scale) - (this.signHeight * scale / 2);
            this.signFieldPresent = false;
        }
        this.addWatermark = false;
        this.addExtra = false;
        this.extraText = "";
        this.signScale = 1;
        this.red = 0;
        this.green = 0;
        this.blue = 0;
        this.fontSize = 12;
        this.extraOnTop = true;
        this.extraType = true;
        this.extraName = true;
        this.extraDate = true;
        this.isExtraText = true;
        this.restoreExtra = false;
        this.init();
        this.initEventListeners();
    }

    init() {
        let divName = "cross_" + this.id;
        let div = "<div id='"+ divName +"' class='cross'></div>";
        $("#pdf").prepend(div);
        let cross = $("#" + divName);
        this.cross = cross;
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
                    self.cross.css('background-size', Math.round(ui.size.width - self.extraWidth * self.currentScale) + "px");
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
        cross.prepend(border);
        this.border = $("#border_" + this.id);
        this.border.css("pointer-events", "none");

        let tools = this.getTools()
        tools.removeClass("d-none");
        cross.prepend(tools);
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
        if(this.restore) {
            if (localStorage.getItem('signNumber') != null) {
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
                this.toggleType();
                this.toggleName();
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
    }

    restoreUserParams() {
        if (localStorage.getItem('addWatermark') != null) {
            if(localStorage.getItem('addWatermark') === "true") {
                this.addWatermark = false;
                this.toggleWatermark();
            }
        }
        if(this.addExtra) {
            if (localStorage.getItem('extraType') != null) {
                if (localStorage.getItem('extraType') === "false") {
                    if (this.divExtra != null && this.extraType) {
                        this.toggleType();
                    }
                }
            }
            if (localStorage.getItem('extraName') != null) {
                if (localStorage.getItem('extraName') === "false") {
                    if (this.divExtra != null && this.extraName) {
                        this.toggleName();
                    }
                }
            }
            if (localStorage.getItem('extraText') != null) {
                if (localStorage.getItem('extraText') === "false") {
                    if (this.divExtra != null && this.isExtraText) {
                        this.toggleText();
                    }
                }
            }
            if (localStorage.getItem('extraDate') != null) {
                if (localStorage.getItem('extraDate') === "false") {
                    if (this.divExtra != null && this.extraDate) {
                        this.toggleDate();
                    }
                }
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
            drag: function() {
                let thisPos = $(this).position();
                let x = Math.round(thisPos.left / self.currentScale);
                let y = Math.round(thisPos.top / self.currentScale);
                if(!self.firstLaunch) {
                    self.xPos = x;
                    self.yPos = y;
                } else {
                    if(self.signFieldPresent) {
                        window.scrollTo(self.xPos * self.currentScale, self.yPos * self.currentScale);
                    }
                    self.firstLaunch = false;
                }
            }
        });
    }

    initEventListeners() {
        let self = this;
        this.cross.on("mousedown click", function (e) {
            e.stopPropagation();
            self.wantUnlock();
        });

        $("#crossTools_" + this.id).on("click", function (e){
            e.stopPropagation();
        });

        $("#signDrop_" + this.id).on("click", e => this.deleteSign());
        $("#signNextImage_" + this.id).on("click", e => this.nextSignImage());
        $("#signPrevImage_" + this.id).on("click", e => this.prevSignImage());
        $("#displayMoreTools_" + this.id).on("click", e => this.displayMoreTools());
        $("#hideMoreTools_" + this.id).on("click", e => this.hideMoreTools());
        $("#watermark_" + this.id).on("click", e => this.toggleWatermark());
        $("#allPages_" + this.id).on("click", e => this.toggleAllPages());
        $("#signExtra_" + this.id).on("click", e => this.toggleExtra());
        $("#signExtraOnTop_" + this.id).on("click", e => this.toggleExtraOnTop());

        $("#extraType_" + this.id).on("click", e => this.toggleType());
        $("#extraName_" + this.id).on("click", e => this.toggleName());
        $("#extraDate_" + this.id).on("click", e => this.toggleDate());
        $("#extraText_" + this.id).on("click", e => this.toggleText());

    }

    applyCurrentSignRequestParams() {
        this.cross.css('top', this.yPos * this.currentScale + 'px');
        this.cross.css('left', this.xPos * this.currentScale + 'px');
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
        this.cross.css('background-size', newWidth);
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

    nextSignImage() {
        this.fireEvent("nextSign", ["ok"]);
    }

    prevSignImage() {
        if(this.signImageNumber > 0) {
            this.fireEvent("prevSign", ["ok"]);
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
            this.cross.css('background-size', (this.signWidth - this.extraWidth) * this.currentScale);
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
        let x = this.xPos * this.currentScale;
        let y = this.yPos * this.currentScale;
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
            change: color => this.fireEvent("changeColor", [color])
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

    toggleExtra() {
        this.addExtra = !this.addExtra;
        let self = this;
        if(this.addExtra) {
            $("#signExtra_" + this.id).addClass("btn-outline-light");
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
                this.divExtra.append("<span id='extraDateDiv_"+ this.id +"'>le " + moment().format('DD/MM/YYYY HH:mm:ss') + "<br/></span>");
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
            if(!this.restoreExtra) {
                this.restoreUserParams();
                this.restoreExtra = true;
            }
        } else {
            if(!this.extraOnTop) {
                this.toggleExtraOnTop();
            }

            $("#signExtra_" + this.id).removeClass("btn-outline-light");
            $("#signExtraOnTop_" + this.id).attr("disabled", true);
            this.divExtra.addClass("d-none");
            $("#extraTools_" + this.id).addClass("d-none");
            $("#crossTools_" + this.id).css("top", "-45px");
            this.signHeight -= this.extraHeight;
            this.extraHeight = 0;
        }
        // this.toggleType();
        // this.toggleName();
        // this.toggleText();
        this.updateSize();
        if(this.addExtra) {
            this.textareaExtra.focus();
        }
        if(!this.firstLaunch) {
            localStorage.setItem('addExtra', this.addExtra);
        }
    }

    toggleExtraOnTop() {
        if(this.addWatermark) {
            this.cross.toggleClass("watermark-width watermark-height")
        }
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
            this.cross.css("width",  this.signWidth * this.currentScale + "px");
            this.cross.css("height",  this.signHeight * this.currentScale + "px");
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
            this.extraWidth = this.signWidth /2;
            this.cross.css("width",  this.signWidth * this.currentScale + "px");
            this.divExtra.css("width", this.extraWidth * this.currentScale + "px");
            this.divExtra.addClass("div-extra-right");
            this.divExtra.removeClass("div-extra-top");
        }
        // this.updateSize();
    }

    refreshDate() {
        $("#extraDateDiv_" + this.id).html("le " + moment().format('DD/MM/YYYY HH:mm:ss') + "<br/>");
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
        if(!this.extraType && !this.extraDate && !this.extraName && this.isExtraText) return;
        let textExtra = $("#textExtra_" + this.id);
        if(this.isExtraText) {
            textExtra.hide();
            this.textareaExtra.removeClass("btn-outline-light");
        } else {
            textExtra.show();
            this.textareaExtra.addClass("btn-outline-light");
        }
        $("#extraText_" + this.id).toggleClass("btn-outline-light");
        if(this.extraText === "") {
            this.extraText = this.textareaExtra.val();
        } else {
            this.extraText = "";
        }
        this.isExtraText = !this.isExtraText;
        this.updateSize();
        this.refreshExtraDiv();
        if(!this.firstLaunch) {
            localStorage.setItem('extraText', this.isExtraText);
        }
    }

    updateSize() {
        if(this.extraOnTop) {
            this.signHeight -= this.extraHeight;
            this.extraHeight = 0;
            this.extraHeight = Math.round(parseInt(this.divExtra.css("height")) / this.currentScale);
            this.signHeight += this.extraHeight;
            this.cross.css("height", this.signHeight * this.currentScale + "px");
        } else {
            this.signWidth -= this.extraWidth;
            this.extraWidth = Math.round(this.originalWidth * this.signScale);
            this.signWidth += this.extraWidth;
            this.cross.css("width", this.signWidth * this.currentScale + "px");
            this.divExtra.css("width", this.extraWidth * this.currentScale + "px");
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
            text += lines[i].substring(0, 20);
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
}