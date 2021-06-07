import {EventFactory} from "../modules/utils/EventFactory.js";

export class SignRequestParams  extends EventFactory {

    constructor(signRequestParams, id, scale, page, userName, restore, isSign) {
        super();
        this.id = id;
        this.userName = userName;
        this.cross;
        this.isSign = isSign;
        this.border;
        this.tools;
        this.divExtra;
        this.textareaExtra;
        this.signColorPicker;
        this.pdSignatureFieldName;
        this.signImageNumber = 0;
        this.signPageNumber = 1;
        if(page != null) this.signPageNumber = page;
        this.originalWidth = 150;
        this.originalHeight = 75;
        this.signWidth = 150;
        this.signHeight = 75;
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.xPos = -1;
        this.yPos = -1;
        this.visual = true;
        this.addWatermark = false;
        this.addExtra = false;
        this.extraOnTop = true;
        this.extraType = true;
        this.extraName = true;
        this.extraDate = true;
        this.isExtraText = true;
        this.extraText = "";
        this.signScale = 1;
        this.currentScale = parseFloat(scale);
        this.red = 0;
        this.green = 0;
        this.blue = 0;
        this.fontSize = 12;
        this.restore = restore;
        Object.assign(this, signRequestParams);
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
        this.cross.draggable({
            containment: "#pdf",
            scroll: false,
            drag: function() {
                let thisPos = $(this).position();
                let x = Math.round(thisPos.left / self.currentScale);
                let y = Math.round(thisPos.top / self.currentScale);
                self.xPos = x;
                self.yPos = y;
            }
        });
        this.cross.resizable({
            aspectRatio: true,
            resize: function(event, ui) {
                let maxWidth = ((self.originalWidth + self.extraWidth / self.signScale) * 2 * self.currentScale);
                let maxHeight = ((self.originalHeight + self.extraHeight / self.signScale) * 2 * self.currentScale);
                let minWidth = ((self.originalWidth + self.extraWidth / self.signScale) * .5 * self.currentScale);
                let minHeight = ((self.originalHeight + self.extraHeight / self.signScale) * .5 * self.currentScale);
                if(ui.size.width >= maxWidth
                    ||
                    ui.size.height >= maxHeight
                ) {
                    ui.size.width = maxWidth;
                    ui.size.height = maxHeight;
                } else
                if(ui.size.width <= minWidth
                    ||
                    ui.size.height <= minHeight) {
                    ui.size.width = minWidth;
                    ui.size.height = minHeight;
                }
                // self.saveScale(ui);
                let newScale;
                if(!self.addExtra || self.extraOnTop) {
                    newScale = Math.round((ui.size.width / self.currentScale) / (self.originalWidth) * 100) / 100;
                } else {
                    newScale = Math.round((ui.size.height / self.currentScale) / (self.originalHeight) * 100) / 100;
                }
                self.signWidth = self.signWidth / self.signScale * newScale;
                self.signHeight = self.signHeight / self.signScale * newScale;
                self.extraWidth = self.extraWidth / self.signScale * newScale;
                if(self.addExtra) {
                    if (!self.extraOnTop) {
                        self.divExtra.css('width', Math.round(self.extraWidth * self.currentScale) + "px");
                    } else {
                        self.divExtra.css('width', Math.round(self.originalWidth * self.signScale * self.currentScale) + "px");
                    }
                }
                self.extraHeight = self.extraHeight / self.signScale * newScale;
                self.signScale = newScale;
                self.cross.css('background-size', Math.round(ui.size.width - self.extraWidth * self.currentScale) + "px");
                if(self.addExtra) {
                    self.refreshExtraDiv();
                }
            },
            stop: function(event, ui) {
                let newScale;
                if(!self.addExtra || self.extraOnTop) {
                    newScale = Math.round((ui.size.width / self.currentScale) / (self.originalWidth) * 100) / 100;
                } else {
                    newScale = Math.round((ui.size.height / self.currentScale) / (self.originalHeight) * 100) / 100;
                }

                self.signScale = newScale;
                localStorage.setItem("zoom", self.signScale);
            }
        });

        this.cross.on("click", function (){
            $("#defaultTools_" + self.id).removeClass("d-none");
            $("#extraTools_" + self.id).addClass("d-none");
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
            this.initSignSize();
            if (localStorage.getItem('addWatermark') != null && localStorage.getItem('addWatermark') === "true") {
                this.toggleWatermark();
            }
        }
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
        $("#signExtra_" + this.id).on("click", e => this.toggleExtra());
        $("#signExtraOnTop_" + this.id).on("click", e => this.toggleExtraOnTop());

        $("#extraType_" + this.id).on("click", e => this.toggleType());
        $("#extraName_" + this.id).on("click", e => this.toggleName());
        $("#extraDate_" + this.id).on("click", e => this.toggleDate());
        $("#extraText_" + this.id).on("click", e => this.toggleText());

    }

    initSignSize() {
        if(localStorage.getItem("zoom") != null) {
            this.signScale = parseFloat(localStorage.getItem("zoom"));
        }
    }

    saveScale(ui) {
        if (this.extraOnTop) {
            this.signScale = Math.round((ui.size.width / this.currentScale) / (this.originalWidth) * 100) / 100;
        } else {
            this.signScale = Math.round((ui.size.height / this.currentScale) / (this.originalHeight) * 100) / 100;
        }
    }

    deleteSign() {
        this.fireEvent("delete", ["ok"]);
        this.cross.remove();
    }

    setxPos(x) {
        this.xPos = Math.round(x);
    }

    setyPos(y) {
        this.yPos = Math.round(y);
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
            this.textareaExtra.focus();
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
        this.originalWidth = Math.round((result.w));
        this.originalHeight = Math.round((result.h));
        this.signWidth = Math.round(this.originalWidth * this.signScale) + this.extraWidth;
        this.signHeight = Math.round(this.originalHeight * this.signScale) + this.extraHeight;
        this.cross.css('width', (this.signWidth * this.currentScale));
        this.cross.css('height', (this.signHeight * this.currentScale));
        this.cross.css('background-size', (this.signWidth - this.extraWidth) * this.currentScale);
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

    toggleWatermark() {
        if(this.addWatermark) {
            $("#watermark_" + this.id).addClass("disabled");
            this.cross.removeClass("watermark-width");
            this.cross.removeClass("watermark-height");
            this.addWatermark = false;
        } else {
            $("#watermark_" + this.id).removeClass("disabled");
            if(this.extraOnTop) {
                this.cross.addClass("watermark-width");
            } else {
                this.cross.addClass("watermark-height");
            }
            this.addWatermark = true;
        }
        if(this.signType !== "visa" && this.signType !== "hiddenVisa") {
            localStorage.setItem('addWatermark', this.addWatermark);
        }
    }

    toggleExtra() {
        this.addExtra = !this.addExtra;
        if(this.addExtra) {
            $("#signExtra_" + this.id).removeClass("disabled");
            $("#signExtraOnTop_" + this.id).removeAttr("disabled");
            if(this.divExtra == null) {
                this.typeSign = "Signature calligraphique";
                if (this.signType === "visa" || this.signType === "hiddenVisa") this.typeSign = "Visa";
                if (this.signType === "certSign" || this.signType === "nexuSign") this.typeSign = "Signature électronique";
                let divExtraHtml = "<div id='divExtra_" + this.id + "' class='div-extra div-extra-top'></div>";
                this.cross.prepend(divExtraHtml);
                this.divExtra = $("#divExtra_" + this.id);
                this.divExtra.append("<span id='extraTypeDiv_"+ this.id +"' >" + this.typeSign + "<br/></span>");
                this.divExtra.append("<span id='extraNameDiv_"+ this.id +"' >" + this.userName + "<br/></span>");
                this.divExtra.append("<span id='extraDateDiv_"+ this.id +"'>le " + moment().format('DD/MM/YYYY HH:mm:ss') + "<br/></span>");
                let self = this;
                setInterval(function() {
                    self.refreshDate();
                }, 1000);
                this.addTextArea();
                this.divExtra.on("click", function (e){
                    e.stopPropagation();
                    $(this).focus();
                    $("#defaultTools_" + self.id).addClass("d-none");
                    $("#extraTools_" + self.id).removeClass("d-none");
                });
            } else {
                this.divExtra.removeClass("d-none");
            }
            this.refreshExtraDiv();
            this.extraHeight = Math.round(parseInt(this.divExtra.css("height")) / this.currentScale);
            this.signHeight += this.extraHeight;
            this.textareaExtra.focus();
        } else {
            if(!this.extraOnTop) {
                this.toggleExtraOnTop();
            }
            $("#signExtra_" + this.id).addClass("disabled");
            $("#signExtraOnTop_" + this.id).attr("disabled", true);
            this.divExtra.addClass("d-none");
            this.signHeight -= this.extraHeight;
            this.extraHeight = 0;
       }
        this.updateSize();
    }

    toggleExtraOnTop() {
        if(this.addWatermark) {
            this.cross.toggleClass("watermark-width watermark-height")
        }
        if(!this.extraOnTop) {
            $("#signExtraOnTop_" + this.id).addClass("disabled");
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
        $("#extraTypeDiv_" + this.id).toggle();
        $("#extraType_" + this.id).toggleClass("disabled");
        this.extraType = !this.extraType;
        this.updateSize();
    }

    toggleName() {
        if(!this.extraType && !this.extraDate && !this.isExtraText && this.extraName) return;
        $("#extraNameDiv_" + this.id).toggle();
        $("#extraName_" + this.id).toggleClass("disabled");
        this.extraName = !this.extraName;
        this.updateSize();
    }

    toggleDate() {
        if(!this.extraType && !this.extraName && !this.isExtraText && this.extraDate) return;
        $("#extraDateDiv_" + this.id).toggle();
        $("#extraDate_" + this.id).toggleClass("disabled");
        this.extraDate = !this.extraDate;
        this.updateSize();
    }

    toggleText() {
        if(!this.extraType && !this.extraDate && !this.extraName && this.isExtraText) return;
        $("#textExtra_" + this.id).toggle();
        this.textareaExtra.toggleClass("disabled");
        $("#extraText_" + this.id).toggleClass("disabled");
        if(this.extraText === "") {
            this.extraText = this.textareaExtra.val();
        } else {
            this.extraText = "";
        }
        this.isExtraText = !this.isExtraText;
        this.updateSize();
    }

    updateSize() {
        if(this.extraOnTop) {
            this.signHeight -= this.extraHeight;
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
        let divExtraHtml = "<textarea id='textExtra_" + this.id + "' class='sign-textarea align-top' rows='1' cols='30' ></textarea>";
        this.divExtra.append(divExtraHtml);
        this.textareaExtra = $("#textExtra_" + this.id);
        this.textareaExtra.on("input", e => this.refreshExtraDiv());
    }

    refreshExtraDiv() {
        let maxLines = 2;
        if(this.extraOnTop) maxLines = 1;
        if(!this.extraName) maxLines++;
        if(!this.extraDate) maxLines++;
        if(!this.extraType) maxLines++;
        let fontSize = this.fontSize * this.currentScale * this.signScale;
        this.divExtra.css("font-size", fontSize);
        let text = this.textareaExtra.val();
        let lines = text.split(/\r|\r\n|\n/);
        text = "";
        if(lines.length > maxLines) {
            lines.pop();
        }
        for(let i = 0; i < lines.length; i++) {
            text += lines[i].substring(0, 25);
            if(i < lines.length - 1) {
                text += "\n";
            }
        }
        this.extraText = text;
        this.textareaExtra.val(text);
        if(lines.length != this.textareaExtra.attr("rows")) {
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
        $("#signColorPicker_" + this.id).hide();
        this.addWatermark = true;
        this.toggleWatermark();

    }

}