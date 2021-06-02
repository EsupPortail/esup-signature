import {EventFactory} from "../modules/utils/EventFactory.js";
import {Color} from "../modules/utils/Color.js";

export class SignRequestParams  extends EventFactory {

    constructor(signRequestParams, id, scale, page) {
        super();
        this.id = id;
        this.cross;
        this.border;
        this.tools;
        this.signColorPicker;
        this.pdSignatureFieldName;
        this.signImageNumber = 0;
        this.signPageNumber = 1;
        this.signWidth = 150;
        this.signHeight = 75;
        this.originalWidth = 150;
        this.originalHeight = 75;
        this.xPos = -1;
        this.yPos = -1;
        this.visual = true;
        this.addWatermark = false;
        this.addExtra = false;
        this.extraOnTop = false;
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.extraText = "";
        this.signScale = 1;
        this.currentScale = parseFloat(scale);
        this.red = 0;
        this.green = 0;
        this.blue = 0;
        Object.assign(this, signRequestParams);
        this.fixRatio = .75;
        this.init(page);
        this.initEventListeners();
    }

    init(page) {
        let divName = "cross_" + this.id;
        let div = "<div id='"+ divName +"'></div>";
        $("#pdf").prepend(div);
        let cross = $("#" + divName);
        this.cross = cross;
        this.cross.css("position", "absolute");
        this.cross.css("z-index", "5");
        // cross.css("width", 150 * this.currentScale + "px");
        // cross.css("height", 75 * this.currentScale + "px");
        this.cross.attr("data-id", this.id);
        let self = this;
        this.cross.draggable({
            containment: "#pdf",
            scroll: false,
            drag: function() {
                let thisPos = $(this).position();
                let x = Math.round(thisPos.left * self.fixRatio / self.currentScale);
                let y = Math.round(thisPos.top * self.fixRatio / self.currentScale);
                self.xPos = x;
                self.yPos = y;
            }
        });
        this.cross.resizable({
            aspectRatio: true,
            resize: function(event, ui) {
                if(ui.size.width >= (self.originalWidth * 2 * self.currentScale)) {
                    ui.size.width = (self.originalWidth * 2 * self.currentScale);
                    ui.size.height = (self.originalHeight * 2 * self.currentScale);
                }
                if(ui.size.width <= (self.originalWidth / 2 * self.currentScale)) {
                    ui.size.width = (self.originalWidth / 2 * self.currentScale);
                    ui.size.height = (self.originalHeight / 2 * self.currentScale);
                }
                self.signScale = Math.round((ui.size.width / self.currentScale ) / (self.originalWidth ) * 100) / 100;
                self.signWidth = Math.round(ui.size.width / self.currentScale * self.fixRatio);
                self.signHeight = Math.round(ui.size.height / self.currentScale * self.fixRatio);
                self.cross.css('background-size', Math.round(ui.size.width));
                let thisPos = $(this).position();

                let x = Math.round(thisPos.left * self.fixRatio / self.currentScale);
                let y = Math.round(thisPos.top * self.fixRatio / self.currentScale);
                console.log("(" + x + ", " + y + ")" + self.signScale + " : " + self.signWidth + "*" + self.signHeight);
            },
            stop: function(event, ui) {
                self.signScale = Math.round((ui.size.width / self.currentScale ) / (self.originalWidth ) * 100) / 100;
                localStorage.setItem("zoom", self.signScale);
            }
        });

        let border = "<div id='border_" + this.id +"' class='static-border' style='width: 100%; height: 100%;'></div>"
        cross.prepend(border);
        this.border = $("#border_" + this.id);

        let tools = this.getTools(this.id)
        tools.removeClass("d-none");
        cross.prepend(tools);
        this.tools = tools;

        this.extraWidth = 0;
        this.extraHeight = 0;
        this.signPageNumber = page;
        this.moreTools = $("#moreTools_" + this.id);
        this.defaultTools = $("#defaultTools_" + this.id);
        this.createColorPicker();
        this.initSignSize();
        if(localStorage.getItem('addWatermark') != null && localStorage.getItem('addWatermark') === "true") {
            this.toggleWatermark();
        }
    }

    initEventListeners() {
        let self = this;
        this.cross.on("mousedown click", function (e) {
            e.stopPropagation();
            self.wantUnlock();
        });

        let deleteButton = $("#signDrop_" + this.id);
        deleteButton.on("click", e => this.deleteSign());

        let signNextImageButton = $("#signNextImage_" + this.id);
        signNextImageButton.on("click", e => this.nextSignImage());

        let signPrevImageButton = $("#signPrevImage_" + this.id);
        signPrevImageButton.on("click", e => this.prevSignImage());

        let displayMoreToolsButton = $("#displayMoreTools_" + this.id);
        displayMoreToolsButton.on("click", e => this.displayMoreTools());

        let hideMoreToolsButton = $("#hideMoreTools_" + this.id);
        hideMoreToolsButton.on("click", e => this.hideMoreTools());

        let watermarkButton = $("#watermark_" + this.id);
        watermarkButton.on("click", e => this.toggleWatermark());

    }

    initSignSize() {
        if(localStorage.getItem("zoom") != null) {
            this.signScale = parseFloat(localStorage.getItem("zoom"));
            this.signWidth = Math.round((this.signScale * this.originalWidth) / this.currentScale * this.fixRatio);
            this.signHeight = Math.round((this.signScale * this.originalHeight) / this.currentScale * this.fixRatio);
            this.cross.css('width', (this.signWidth / this.fixRatio) + "px");
            this.cross.css('height', (this.signHeight / this.fixRatio) + "px");
            this.cross.css('background-size', Math.round(this.signScale * this.originalWidth));
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

    getTools(id) {
        let tools = $("#crossTools_x").clone();
        tools.attr("id", tools.attr("id").split("_")[0] + "_" + id);
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
    }

    lock() {
        this.tools.addClass("d-none");
        this.border.removeClass("anim-border");
        this.border.addClass("static-border");
    }

    wantUnlock() {
        this.fireEvent("unlock", ["ok"]);
        this.unlock();
    }

    unlock() {
        this.border.removeClass("static-border");
        this.border.addClass("anim-border");
        this.tools.removeClass("d-none");
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
        this.signWidth = Math.round((result.w + this.extraWidth) * this.signScale * this.fixRatio);
        this.originalWidth = Math.round((result.w + this.extraWidth));
        this.originalHeight = Math.round((result.h + this.extraWidth));
        this.signHeight = Math.round((result.h + this.extraHeight) * this.signScale * this.fixRatio);
        this.cross.css('width', (this.signWidth / this.fixRatio * this.currentScale));
        this.cross.css('height', (this.signHeight / this.fixRatio * this.currentScale));
        this.cross.css('background-size', (this.signWidth - (this.extraWidth * this.signScale * this.fixRatio)) * this.currentScale / this.fixRatio);
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
            this.cross.removeClass("watermarkWidth");
            this.cross.removeClass("watermarkHeight");
            this.addWatermark = false;
        } else {
            if(this.extraOnTop) {
                this.cross.addClass("watermarkWidth");
            } else {
                this.cross.addClass("watermarkHeight");
            }
            this.addWatermark = true;
        }
        if(this.signType !== "visa" && this.signType !== "hiddenVisa") {
            localStorage.setItem('addWatermark', this.addWatermark);
        }
    }

}