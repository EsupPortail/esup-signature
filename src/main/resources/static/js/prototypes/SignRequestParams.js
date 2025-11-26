import {EventFactory} from "../modules/utils/EventFactory.js?version=@version@";
import {Color} from "../modules/utils/Color.js?version=@version@";
import {UserUi} from '../modules/ui/users/UserUi.js?version=@version@';
import {UserSignaturePad} from "../modules/ui/users/UserSignaturePad.js?version=@version@";

export class SignRequestParams extends EventFactory {

    constructor(signRequestParamsModel, id, scale, page, userName, authUserName, restore, isSign, isVisa, isElec, phone, light, signImages, scrollTop, csrf, signType) {
        super();
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.signWidth = 200;
        this.signHeight = 100;
        this.addWatermark = null;
        this.extraText = "";
        this.addExtra = false;
        this.extraOnTop = true;
        this.extraType = null;
        this.extraName = null;
        this.extraDate = null;
        this.isExtraText = null;
        this.signImageNumber = 0;
        this.pdSignatureFieldName = null;
        Object.assign(this, signRequestParamsModel);
        this.isExtraText = !(this.extraText !== "");
        this.originalWidth = this.signWidth;
        this.originalHeight = this.signHeight;
        this.id = id;
        this.userUI = null;
        this.faImages = ["check-solid", "times-solid", "circle-regular", "minus-solid"];
        this.signImages = signImages;
        this.currentScale = parseFloat(scale);
        this.signPageNumber = 1;
        if(page != null) this.signPageNumber = page;
        this.phone = phone;
        this.light = light;
        this.userName = userName;
        this.authUserName = authUserName;
        this.ready = null;
        this.isShare = (userName !== authUserName);
        this.restore = restore;
        this.isSign = isSign;
        this.isVisa = isVisa;
        this.signScale = 0.5 * this.getBrowserZoom();
        localStorage.setItem("zoom", this.signScale);
        this.firstLaunch = true;
        this.firstCrossAlert = true;
        this.cross = null;
        this.signSpace = null;
        this.submitAddSpotBtn = null;
        this.border = null;
        this.tools = null;
        this.divExtra = null;
        this.textareaExtra = null;
        this.textareaPart = null;
        this.textPart = null;
        this.signRequestId = null;
        this.spotStepNumber = null;
        this.signColorPicker = null;
        this.restoreExtraOnTop = false;
        this.allPages = false;
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.savedText = "";
        this.offset = 0;
        this.dropped = false;
        this.scrollTop = scrollTop;
        this.csrf = csrf;
        this.signType = signType;
        this.userSignaturePad = null;
        this.canvasBtn = null;
        this.canvas = null;
        this.padMargin = 0;
        this.inside = true;
        if(!light) {
            let signPage = $("#page_" + this.signPageNumber);
            if(signPage != null && signPage.offset() != null) {
                this.offset = (signPage.offset().top);
            }
        }
        if(signImages === 999999) {
            this.#initSpot();
        } else if(light) {
            this.#initLight();
        } else {
            this.red = 0;
            this.green = 0;
            this.blue = 0;
            this.fontSize = 10;
            this.addImage = true;
            if(restore && !isVisa) {
                this.addExtra = false;
                this.addWatermark = false;
                this.extraText = "";
                this.extraOnTop = true;
                this.extraType = false;
                this.extraName = false;
                this.extraDate = false;
                this.isExtraText = false;
            }
            this.#initCross();
            if(!restore && isSign) {
                this.#restoreFromFavorite();
            }
        }
        this.stringLength = 1;
        if(signRequestParamsModel == null || (this.xPos===0 && this.yPos===0)) {
            this.xPos = ((parseInt($("#pdf").css("width")) / 2 / scale) - (this.signWidth * scale / 2)) / this.getBrowserZoom();
            let mid = scrollTop + $(window).height() / 2;
            this.yPos = (mid - this.offset) / scale / this.getBrowserZoom();
        }
        this.lastWidth = window.innerWidth;
        this.lastHeight = window.innerHeight;
        this.#initEventListeners();
    }

    #initEventListeners() {
        let self = this;
        if(self.light == null || !self.light) {
            this.cross.on("mousedown click", function(e) {
                e.stopPropagation();
                self.#wantUnlock();
            });
            $("#crossTools_" + this.id).on("click", function(e) {
                e.stopPropagation();
            });
        }
        $("#signDrop_" + this.id).on("mousedown", e => this.#deleteSign());
        $("#signNextImage_" + this.id).on("mousedown", e => this.#nextSignImage());
        $("#signPrevImage_" + this.id).on("mousedown", e => this.#prevSignImage());
        $("#displayMoreTools_" + this.id).on("mousedown", e => this.#displayMoreTools());
        $("#watermark_" + this.id).on("mousedown", e => this.#toggleWatermark(e));
        this.canvasBtn = $("#canvasBtn_" + this.id);
        this.canvasBtn.on("mousedown", function(){
            self.#enableCanvas();
        });
        $("#allPages_" + this.id).on("mousedown", e => this.#toggleAllPages());
        $("#signImage_" + this.id).on("mousedown", e => this.#toggleImage());
        $("#signImageBtn_" + this.id).on("mousedown", e => this.#toggleSignModal(e));
        $("#signExtra_" + this.id).on("mousedown", e => this.#toggleExtra());
        $("#signExtraOnTop_" + this.id).on("mousedown", e => this.#toggleExtraOnTop());

        $("#extraType_" + this.id).on("mousedown", e => this.#toggleType());
        $("#extraName_" + this.id).on("mousedown", e => this.#toggleName());
        $("#extraDate_" + this.id).on("mousedown", e => this.#toggleDate());
        $("#extraText_" + this.id).on("mousedown", e => this.#toggleText());
        const THRESHOLD = 100;
        const DEBOUNCE_DELAY = 100;
        let resizeTimer = null;
        $(window).on("resize", () => {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(() => {
                const w = window.innerWidth;
                const h = window.innerHeight;
                const deltaW = Math.abs(w - self.lastWidth);
                const deltaH = Math.abs(h - self.lastHeight);
                if (w === self.lastWidth || (deltaW < THRESHOLD && deltaH < THRESHOLD)) return;
                self.cross.css('top', Math.round(self.yPos * self.currentScale * self.getBrowserZoom()) + 'px');
                self.cross.css('left', Math.round(self.xPos * self.currentScale * self.getBrowserZoom()) + 'px');
                self.cross.css('width', Math.round(self.signWidth * self.currentScale * self.getBrowserZoom()) + 'px');
                self.cross.css('height', Math.round(self.signHeight * self.currentScale * self.getBrowserZoom()) + 'px');
                if(self.addExtra) {
                    self.divExtra.css("width", self.extraWidth * self.currentScale * self.getBrowserZoom() + "px");
                    self.divExtra.css("font-size", Math.round(10 * self.currentScale * self.signScale * self.getBrowserZoom()) + "px");
                }
                self.lastWidth = w;
                self.lastHeight = h;
            }, DEBOUNCE_DELAY);
        });
    }

    #initCross() {
        this.#createCross();
        this.#madeCrossDraggable();
        this.#madeCrossResizable();
        this.#createBorder();
        this.#createTools();
        this.extraWidth = 0;
        this.extraHeight = 0;
        this.defaultTools = $("#defaultTools_" + this.id);
        if(this.isSign) {
            // this.#createColorPicker();
        } else {
            this.#toggleMinimalTools();
        }
        if(this.restore) {
            // if(localStorage.getItem("zoom") != null) {
            //     this.signScale = parseFloat(localStorage.getItem("zoom"));
            // }
            if(localStorage.getItem("addWatermark") == null) {
                this.#toggleWatermark()
            }
        }
        if(this.isVisa || this.isSign) {
            this.signHeight = 0;
            if(this.isVisa) {
                this.addWatermark = false;
                this.extraText = "";
                this.extraType = true;
                this.extraName = true;
                this.extraDate = false;
                this.isExtraText = true;
                this.addExtra = false;
                this.#toggleMinimalTools();
                this.#toggleExtra();
                this.#toggleName();
                this.#toggleDate();
                this.#toggleType();
                this.#toggleText();
                this.#refreshExtraDiv();
                this.#updateSize();
                this.addWatermark = false;
                this.#toggleWatermark();
                this.addImage = false;
                this.#toggleImage();
                this.extraOnTop = false;
                this.#toggleExtraOnTop();
            }
        }
        if(this.restore && this.isSign) {
            if (JSON.parse(localStorage.getItem('signNumber')) != null) {
                this.fireEvent("nextSign", localStorage.getItem('signNumber'));
            }
            if (!this.isVisa && localStorage.getItem('addExtra') != null) {
                if (localStorage.getItem('addExtra') === "true") {
                    this.addExtra = false;
                    this.#toggleExtra();
                }
            }
            if (!this.isVisa) {
                this.#restoreUserParams();
            }
        }
        if(this.isShare && this.isSign) {
            if(this.signColorPicker != null) {
                // this.signColorPicker.spectrum("destroy");
                this.signColorPicker.hide();
            }
            this.addWatermark = true;
            this.#toggleWatermark();
            this.addExtra = false;
            this.#toggleExtra();
            this.isExtraText = false;
            this.#toggleText();
            $("#signExtra_" + this.id).hide();
            $("#extraType_" + this.id).hide();
            $("#extraName_" + this.id).hide();
            this.extraName = true;
            this.#toggleName();
            this.extraType = true;
            this.#toggleType();
            this.savedText = this.userName + "\nP.O.\n" + this.authUserName;
            this.extraText = this.savedText;
            this.textareaExtra.val(this.savedText);
            this.#refreshExtraDiv();
            this.#updateSize();
            this.textareaExtra.attr("readonly", true);
        }

        if(this.isOtp && this.isSign){
            this.#toggleExtra();
            this.#toggleText();
            if(this.userName.length < 2) {
                this.#toggleName();
            }
            if(this.phone != null) {
                $("#extraTypeDiv_" + this.id).html("<span>Signature OTP : " + this.phone + "<br></span>");
            } else {
                $("#extraTypeDiv_" + this.id).html("<span>Signature OTP<br></span>");
            }
            // $("#extraTools_" + this.id).remove();
            $("#crossTools_" + this.id).css("top", "-45px");
            if(this.globalProperties.externalSignatureParams != null) {
                this.addWatermark = !this.globalProperties.externalSignatureParams.addWatermark;
                this.#toggleWatermark();
                this.extraDate = !this.globalProperties.externalSignatureParams.extraDate;
                this.#toggleDate();
                this.extraType = !this.globalProperties.externalSignatureParams.extraType;
                this.#toggleType();
                this.extraName = !this.globalProperties.externalSignatureParams.extraName;
                this.#toggleName();
                this.addExtra = !this.globalProperties.externalSignatureParams.addExtra;
                this.#toggleExtra();
                this.isExtraText = (this.globalProperties.externalSignatureParams.extraText === null);
                this.#toggleText();
                if(this.globalProperties.externalSignatureParams.extraText != null) {
                    this.extraText = this.globalProperties.externalSignatureParams.extraText;
                    this.textareaExtra.val(this.globalProperties.externalSignatureParams.extraText);
                }
                this.extraOnTop = !this.globalProperties.externalSignatureParams.extraOnTop;
                this.#toggleExtraOnTop();
                // $("#displayMoreTools_" + this.id).remove();
            }
        }
        this.cross.attr("page", this.signPageNumber);
    }

    #initLight() {
        this.cross = $("#cross_" + this.id);
        this.border = $("#borders_" + this.id);
        this.tools = $("#crossTools_" + this.id);
        this.canvas = $("#canvas_" + this.id);
        this.originalWidth = 200;
        this.originalHeight = 100;
        this.#restoreFromFavorite();
    }

    #initSpot() {
        console.log("init spot");
        this.#createCross();
        this.#madeCrossDraggable();
        this.#madeCrossResizable();
        this.#createBorder();
        this.#createTools();
        this.#updateSize();
        this.#toggleMinimalTools();
        this.cross.append("<div class='text-black overflow-hidden' style='font-weight: bold; width: 100%; height: 100%;font-size: "+ 6 * this.currentScale +"px;'>Positionner le champ de signature et cliquer sur enregistrer</div>");
        this.cross.css("width", Math.round(this.signWidth * this.signScale * this.currentScale) + "px");
        this.cross.css("height", Math.round(this.signHeight * this.signScale * this.currentScale) + "px");
        this.cross.css("font-size", Math.round(10 * this.signScale * this.currentScale)  + "px");
        this.cross.append("<button id='delete-add-spot' type='button' class='btn btn-sm btn-danger position-absolute' style='z-index: 4; bottom:10px; left: 10px;'><i class='fa-solid fa-xmark'></i></button>");
        this.cross.append("<button id='submit-add-spot' type='button' class='btn btn-sm btn-success position-absolute' style='z-index: 4; bottom:10px; right: 10px;'><i class='fa-solid fa-save'></i></button>");
        this.border.remove();
        this.tools.remove();
        this.submitAddSpotBtn = $("#submit-add-spot");
        this.submitAddSpotBtn.on("click", function () {
            $("#spot-modal").modal("show");
        });
        $("#delete-add-spot").on("click", function (){
            const url = new URL(window.location.href);
            url.searchParams.set("annotation", "");
            window.location.href = url.toString();
        });
        this.saveSpotButton = $("#save-spot-button")
        this.saveSpotButton.unbind();
        this.saveSpotButton.on('click', e => this.#saveSpot(e));
    }

    #saveSpot() {
        $(window).unbind("beforeunload");
        this.spotStepNumber = $("#spotStepNumber").val();
        if(this.spotStepNumber == null || this.spotStepNumber === "") {
            alert("Merci de selectionner une étape");
        } else {
            let commentUrlParams = "comment=" + encodeURIComponent($("#spotComment").val() ?? "") +
                "&commentPosX=" + Math.round(this.xPos * this.getBrowserZoom()) +
                "&commentPosY=" + Math.round(this.yPos * this.getBrowserZoom()) +
                "&commentScale=" + this.signScale / this.getBrowserZoom() +
                "&commentPageNumber=" + this.signPageNumber +
                "&spotStepNumber=" + this.spotStepNumber +
                "&" + this.csrf.parameterName + "=" + this.csrf.token;
            this.signRequestId = $("#save-spot-button").attr("data-es-signrequest-id");
            let url = "/user/signrequests/comment/" + this.signRequestId + "?" + commentUrlParams;
            if (this.signType === "form") {
                url = "/" + this.userName + "/forms/add-spot/" + this.signRequestId + "?" + commentUrlParams;
            }
            $.ajax({
                method: 'POST',
                url: url,
                success: function (result) {
                    const url = new URL(window.location.href);
                    url.searchParams.set("annotation", "");
                    window.location.href = url.toString();
                }
            });
        }
    }

    #restoreFromFavorite() {
        let text = this.extraText;
        this.addExtra = !this.addExtra;
        this.#toggleExtra();
        if(this.divExtra != null) {
            this.extraType = !this.extraType;
            this.#toggleType();
            this.extraName = !this.extraName;
            this.#toggleName();
            this.extraDate = !this.extraDate;
            this.#toggleDate();
            this.extraText = text;
            this.isExtraText = !(this.extraText !== "" && this.extraText !== null);
            this.#toggleText();
            this.textareaExtra.val(text);
            if(!this.extraOnTop) {
                this.extraOnTop = !this.extraOnTop;
                this.#toggleExtraOnTop();
            }
        } else {
            this.extraType = false;
            this.extraName = false;
            this.extraDate = false;
            this.isExtraText = false;
        }
        this.addWatermark = !this.addWatermark;
        this.#toggleWatermark();
    }

    #createCross() {
        let divName = "cross_" + this.id;
        let div = "";
        if(this.isSign) {
            div = "<div id='" + divName + "' class='cross'>" +
                "<canvas id='canvas_" + this.id + "' style='z-index:9 !important; position: absolute; bottom: " + (this.padMargin + 2) + "px; background-color: rgba(236,236,236,0.5);border: 1px solid black; display: none;'></canvas>" +
                "</div>";
            $("#pdf").prepend(div);
            this.cross = $("#" + divName);
            this.cross.css("width", "200");
            this.canvas = $("#canvas_" + this.id);
            this.canvas.css("width", 200);
        } else {
            div = "<div id='" + divName + "' class='cross'>" +
                "</div>"+
                "<input type='hidden' id='canvas_" + this.id + "'/>"
            ;
            $("#pdf").prepend(div);
            this.cross = $("#" + divName);
            this.canvas = $("#canvas_" + this.id);
            this.canvasBtn = $("#canvas_" + this.id);

        }

        this.cross.css("position", "absolute");
        this.cross.css("z-index", "1028");
        this.cross.attr("data-id", this.id);

    }

    #enableCanvas() {
        this.cross.draggable("disable");
        this.canvas.show();
        this.canvas.css("cursor", "pointer");
        this.cross.css("background-image", "");
        this.userSignaturePad = new UserSignaturePad("canvas_" + this.id);
        this.userSignaturePad.signImageBase64 = $("#signImageBase64_" + this.id, 1, 2);
    }

    #disableCanvas() {
        this.cross.draggable("enable");
        this.canvas.hide();
        if(this.userSignaturePad != null) {
            this.userSignaturePad.signImageBase64 = null;
            this.userSignaturePad.destroy();
            this.userSignaturePad = null;
        }
    }

    #createTools() {
        let tools = this.#getTools()
        tools.removeClass("d-none");
        this.cross.prepend(tools);
        this.tools = tools;
    }

    #createBorder() {
        let border = "<div id='border_" + this.id + "' class='static-border' style='width: 100%; height: 100%;'>" +

            "</div>"
        this.cross.prepend(border);
        this.border = $("#border_" + this.id);
    }

    #restoreUserParams() {
        if (JSON.parse(localStorage.getItem('addWatermark')) != null) {
            if(JSON.parse(localStorage.getItem('addWatermark')) === true) {
                this.addWatermark = false;
                this.#toggleWatermark();
            }
        }
        if(this.addExtra) {
            if (JSON.parse(localStorage.getItem('extraOnTop')) != null) {
                if (JSON.parse(localStorage.getItem('extraOnTop')) === false) {
                    if (this.divExtra != null && this.extraOnTop) {
                        this.#toggleExtraOnTop();
                    }
                }
            }
            if (JSON.parse(localStorage.getItem('extraType')) != null) {
                if (JSON.parse(localStorage.getItem('extraType')) === true) {
                    this.#toggleType();
                }
            }
            if (JSON.parse(localStorage.getItem('extraName')) != null) {
                if (JSON.parse(localStorage.getItem('extraName')) === true) {
                    this.#toggleName();
                }
            }
            if (JSON.parse(localStorage.getItem('extraText')) != null) {
                if (JSON.parse(localStorage.getItem('extraText')) === true) {
                    this.#toggleText();
                }
            }
            if (JSON.parse(localStorage.getItem('extraDate')) != null) {
                if (JSON.parse(localStorage.getItem('extraDate')) === true) {
                    this.#toggleDate();
                }
            }
        }
        if (JSON.parse(localStorage.getItem('addImage')) != null && this.signImages.length > 0) {
            if(JSON.parse(localStorage.getItem('addImage')) === false) {
                this.addImage = true;
                this.#toggleImage();
            } else {
                let signImageBtn = $("#signImage_" + this.id);
                if(!this.addImage) {
                    signImageBtn.addClass("btn-outline-dark");
                }
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

    #getNewScale(ui) {
        if (!this.addExtra || this.extraOnTop) {
            return Math.round((ui.size.width / (this.currentScale)) / (this.originalWidth) * 100) / 100;
        } else {
            return Math.round((ui.size.height / (this.currentScale)) / (this.originalHeight) * 100) / 100;
        }
    }

    #madeCrossDraggable() {
        let self = this;
        this.cross.draggable({
            containment: "#pdf",
            snap: ".pdf-page",
            snapMode: "inner",
            snapTolerance: 10 / this.getBrowserZoom(),
            refreshPositions:true,
            scroll: true,
            drag: function(event, ui) {
                if(self.firstLaunch) {
                    self.firstLaunch = false;
                    self.cross.css("background-color", "rgba(220, 220, 220, 0.5)");
                }
                self.tools.addClass("d-none");
            },
            stop: function(event, ui) {
                const dragRect = this.getBoundingClientRect();
                self.#checkInside(dragRect, self);
                self.tools.removeClass("d-none");
                if($(event.originalEvent.target).attr("id") != null && $("#border_" + $(event.originalEvent.target).attr("id").split("_")[1]).hasClass("cross-warning") && self.firstCrossAlert) {
                    self.firstCrossAlert = false;
                    bootbox.alert("Attention votre signature superpose un autre élément du document cela pourrait nuire à sa lecture. Vous pourrez tout de même la valider même si elle est de couleur orange", null);
                }
                self.#afterDropRefresh(ui);
                let signLaunchButton = $("#signLaunchButton");
                if(signLaunchButton.length) {
                    signLaunchButton.focus();
                    signLaunchButton.addClass("pulse-success");
                }
            }
        });
    }

    #checkInside(dragRect, self) {
        this.inside = false;
        $(".pdf-page").each(function () {
            const pageRect = this.getBoundingClientRect();
            if (
                dragRect.left + 10 >= pageRect.left &&
                dragRect.top + 10 >= pageRect.top &&
                dragRect.right - 10 <= pageRect.right &&
                dragRect.bottom - 10 <= pageRect.bottom
            ) {
                self.inside = true;
                return;
            }
        });

        if (!this.inside) {
            console.log("La signature n'est pas entièrement dans une page !");
            $("#signLaunchButton").attr("disabled", "disabled");
        } else {
            $("#signLaunchButton").removeAttr("disabled");
        }
        this.#computeBgColor();
    }

    #computeBgColor() {
        if(this.signImages === 999999) {
            return;
        }
        if (!this.inside) {
            this.cross.css("background-color", "rgba(255, 151, 151, 0.5)");
            return;
        }
        if(this.signSpace != null && this.signSpace.ready) {
            this.cross.css("background-color", "rgba(220, 250, 220, 1)");
        } else {
            this.cross.css("background-color", "rgba(255, 255, 255, 0.8)");
        }
    }

    #enableCrossResizable() {
        if(!this.dropped) {
            this.cross.resizable("enable");
        }
    }

    #madeCrossResizable() {
        let self = this;
        this.cross.resizable({
            handles: "n, e, s, w, ne, se, sw, nw",
            aspectRatio: true,
            containment: "#pdf",
            start: function(event, ui) {
                window.__isResizingCross = true;
                // Sauvegarder la position initiale
                self.initialPosition = {
                    left: ui.position.left,
                    top: ui.position.top
                };
                // Sauvegarder la taille initiale
                self.initialSize = {
                    width: ui.size.width,
                    height: ui.size.height
                };
            },
            resize: function(event, ui) {
                let maxWidth = ((self.originalWidth + self.extraWidth / self.signScale) * 1.5 * self.currentScale);
                let maxHeight = ((self.originalHeight + self.extraHeight / self.signScale) * 1.5 * self.currentScale);
                let minWidth = ((self.originalWidth + self.extraWidth / self.signScale) * .3 * self.currentScale);
                let minHeight = ((self.originalHeight + self.extraHeight / self.signScale) * .3 * self.currentScale);

                // Sauvegarder les valeurs originales avant correction
                let originalWidth = ui.size.width;
                let originalHeight = ui.size.height;
                let originalTop = ui.position.top;
                let originalLeft = ui.position.left;

                // Corriger la largeur
                if (ui.size.width > maxWidth) {
                    ui.size.width = maxWidth;
                }
                if (ui.size.width < minWidth) {
                    ui.size.width = minWidth;
                }

                // Corriger la hauteur
                if (ui.size.height > maxHeight) {
                    ui.size.height = maxHeight;
                }
                if (ui.size.height < minHeight) {
                    ui.size.height = minHeight;
                }

                // Corriger la position si la taille a été limitée (pour les handles n, w, nw, ne, sw)
                // Si on a redimensionné depuis le haut ou la gauche et qu'on a atteint une limite
                if (originalWidth !== ui.size.width) {
                    // La largeur a été corrigée
                    let widthDelta = originalWidth - ui.size.width;
                    // Réajuster left si on redimensionne depuis la gauche
                    if (originalLeft !== ui.originalPosition.left) {
                        ui.position.left = originalLeft + widthDelta;
                    }
                }

                if (originalHeight !== ui.size.height) {
                    // La hauteur a été corrigée
                    let heightDelta = originalHeight - ui.size.height;
                    // Réajuster top si on redimensionne depuis le haut
                    if (originalTop !== ui.originalPosition.top) {
                        ui.position.top = originalTop + heightDelta;
                    }
                }

                if(self.textareaPart != null) {
                    self.signScale = self.#getNewScale(ui);
                    self.#resizeText();
                    self.signWidth = parseInt(self.textareaPart.css("width")) / self.currentScale;
                    self.extraWidth = self.extraWidth / self.signScale;
                    self.canvas.css("width", (self.signWidth * self.currentScale * self.signScale));
                    self.canvas.css("height", (self.signHeight * self.currentScale * self.signScale));
                } else {
                    self.resize(ui);
                }
            },
            stop: function(event, ui) {
                console.log(ui);
                self.signScale = self.#getNewScale(ui);
                localStorage.setItem("zoom", self.signScale);
                if (ui.position.left !== self.initialPosition.left || ui.position.top !== self.initialPosition.top) {
                    self.#afterDropRefresh(ui);
                }
                const dragRect = this.getBoundingClientRect();
                self.#checkInside(dragRect, self);
                window.__isResizingCross = false;
                // self.#simulateDrag(1, 1);
                // self.#simulateDrag(-1, -1);
            }
        });
    }

    #afterDropRefresh(ui) {
        this.signPageNumber = this.cross.attr("page");
        this.xPos = Math.round(ui.position.left / (this.currentScale * this.getBrowserZoom()));
        const deltaTop = $("#page_" + this.signPageNumber).offset().top - $("#page_1").offset().top;
        this.yPos = Math.round((ui.position.top - deltaTop) / (this.currentScale * this.getBrowserZoom()));
        if (this.yPos < 0) this.yPos = 0;
        console.log("x : " + this.xPos + ", y : " + this.yPos);
        if(this.textareaPart != null) {
            this.#resizeText();
        }
    }

    #deleteSign() {
        let self = this;
        this.cross.attr("remove", "true");
        self.cross.remove();
        let signSpaceId = null;
        if(self.signSpace != null) {
            signSpaceId = self.signSpace.attr("id").split("_")[1];
        }
        self.fireEvent("delete", [signSpaceId]);
        $("#addSpotButton").attr("disabled", false);
        $("#addCommentButton").attr("disabled", false);
        $('#insert-btn').removeAttr('disabled');
        if(self.signSpace != null) {
            self.signSpace.removeData("locked");
            self.signSpace.addClass("sign-field");
            self.signSpace.removeClass("sign-field-dropped");
            self.ready = false;
            self.signSpace.text("Vous devez placer une signature ici");
            self.signSpace.css("pointer-events", "auto");
            self.signSpace = null;
        }
    }

    #getTools() {
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

    #wantUnlock() {
        this.fireEvent("unlock", ["ok"]);
        this.#unlock();
    }

    #handleKeydown(event) {
        const activeElement = document.activeElement;

        if (
            (event.key === "Delete" || event.keyCode === 46) &&
            activeElement.tagName !== "INPUT" &&
            activeElement.tagName !== "TEXTAREA"
        ) {
            this.#deleteSign();
        }
    }

    #unlock() {
        this.cross.removeClass("hide-handles");
        this.tools.removeClass("d-none");
        if(this.textareaExtra != null) {
            this.textareaExtra.removeClass("sign-textarea-lock");
        }
        $(document).on('keydown', e => this.#handleKeydown(e));
        this.#computeBgColor();
    }

    #nextSignImage() {
        if(this.signImageNumber < this.signImages.length - 1) {
            this.changeSignImage(parseInt(this.signImageNumber) + 1);
        } else {
            if(this.signImages.length > 0) {
                this.changeSignImage(0);
            }
        }
    }

    #prevSignImage() {
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
            if(this.isSign) {
                this.originalWidth = Math.round((200));
                this.originalHeight = Math.round((100));
            }
            this.signWidth = Math.round(this.originalWidth * this.signScale) + this.extraWidth;
            this.signHeight = Math.round(this.originalHeight * this.signScale) + this.extraHeight;
            this.cross.css('width', (this.signWidth * this.currentScale));
            this.cross.css('height', (this.signHeight * this.currentScale));
            this.canvas.css("width", ((this.signWidth - this.extraWidth - this.padMargin) * this.currentScale));
            this.canvas.css("height", ((this.signHeight - this.extraHeight - this.padMargin) * this.currentScale));
            if(this.addImage) {
                this.cross.css('background-size', (this.signWidth - this.extraWidth) * this.currentScale);
            }
            this.#updateSize();
        } else {
            this.signWidth = Math.round(parseInt(this.cross.css("width"))/ (this.currentScale));
            this.signHeight = Math.round(parseInt(this.cross.css("height"))/ (this.currentScale));
        }
        this.fireEvent("sizeChanged", ['ok']);
    }

    #DEPRECATED_show() {
        this.cross.css('opacity', '1');
        this.cross.draggable("enable");
        this.cross.css("z-index", 1028);
    }

    #DEPRECATED_hide() {
        this.cross.css('opacity', '0');
        this.cross.draggable("disable");
        this.cross.css("z-index", -1);
    }

    #simulateDrag(x, y) {
        console.log("simulate drag : (" + x + ", " + y + ")");
        this.cross.simulate("drag", {
            handle: "corner",
            moves: 1,
            dx: x,
            dy: y
        });
    }

    #displayMoreTools() {
        $("#extraTools_" + this.id).toggleClass("d-none");
        if(!this.light) {
            this.cross.resizable("disable");
        }
    }

    // #createColorPicker() {
    //     this.signColorPicker = $('#signColorPicker_' + this.id);
    //     this.signColorPicker.spectrum({
    //         type: "color",
    //         showPaletteOnly: true,
    //         hideAfterPaletteSelect: true,
    //         preferredFormat: "hex",
    //         change: color => this.#changeSignColor(color)
    //     });
    // }

    #toggleAllPages() {
        if(this.allPages) {
            $("#allPages_" + this.id).removeClass("btn-outline-dark");
            this.allPages = false;
        } else {
            $("#allPages_" + this.id).addClass("btn-outline-dark");
            this.allPages = true;
        }
    }

    #toggleWatermark(e) {
        if(e != null) {
            e.stopPropagation();
        }
        if(this.addWatermark) {
            $("#watermark_" + this.id).removeClass("btn-outline-dark");
            this.cross.removeClass("watermark-width");
            this.cross.removeClass("watermark-height");
            this.addWatermark = false;
        } else {
            $("#watermark_" + this.id).addClass("btn-outline-dark");
            if(this.extraOnTop) {
                this.cross.addClass("watermark-width");
            } else {
                this.cross.addClass("watermark-height");
            }
            this.addWatermark = true;
        }
        if(!this.firstLaunch && !this.isShare) {
            localStorage.setItem('addWatermark', this.addWatermark);
        }
    }

    #toggleImage() {
        console.log("toggle sign image");
        if(this.addImage) {
            if(this.addExtra) {
                if(!this.light) {
                    if(this.extraOnTop) {
                        this.restoreExtraOnTop = true;
                        this.#toggleExtraOnTop();
                    }
                }
                this.canvas.hide();
                this.divExtra.removeClass("div-extra-right");
                this.divExtra.addClass("div-extra-top");
                this.signImageNumber = 0;
                this.extraWidth = 0;
                this.signWidth = this.signWidth / 2;
                this.cross.css('background-size', 0);
                $("#signImage_" + this.id).addClass("btn-outline-dark");
                $("#signExtra_" + this.id).attr("disabled", true);
                $("#signExtraOnTop_" + this.id).attr("disabled", true);
                $("#signPrevImage_" + this.id).attr("disabled", true);
                $("#signNextImage_" + this.id).attr("disabled", true);
                if(!this.isShare) {
                    localStorage.setItem('addImage', false);
                }
                this.addImage = !this.addImage;
                this.#refreshExtraDiv()
                this.#updateSize();
            }
        } else {
            console.log(this.canvas);
            if(this.canvas.css("touch-action") === "none") {
                this.canvas.show();
            }
            if(!this.extraOnTop) {
                this.divExtra.removeClass("div-extra-top");
                this.divExtra.addClass("div-extra-right");
            }
            this.signImageNumber = 0;
            this.extraWidth = this.signWidth;
            this.signWidth = this.signWidth * 2;
            this.cross.css('background-size', (this.signWidth - this.extraWidth) * this.currentScale);
            $("#signImage_" + this.id).removeClass("btn-outline-dark");
            $("#signExtra_" + this.id).attr("disabled", false);
            $("#signExtraOnTop_" + this.id).attr("disabled", false);
            $("#signPrevImage_" + this.id).attr("disabled", false);
            $("#signNextImage_" + this.id).attr("disabled", false);
            if(this.restoreExtraOnTop) {
                this.restoreExtraOnTop = false;
                this.#toggleExtraOnTop();
            }
            if(!this.isShare) {
                localStorage.setItem('addExtra', true);
                localStorage.setItem('addImage', true);
            }
            this.addImage = !this.addImage;
            this.#refreshExtraDiv()
            this.#updateSize();
        }
    }

    #toggleExtra() {
        this.addExtra = !this.addExtra;
        let self = this;
        if(this.addExtra) {
            $("#signExtra_" + this.id).addClass("btn-outline-dark");
            $("#signImage_" + this.id).attr("disabled", false);
            if(!this.addImage) {
                $("#signImage_" + this.id).addClass("btn-outline-dark");
            }
            $("#signExtraOnTop_" + this.id).removeAttr("disabled");
            if(this.extraOnTop) {
                $("#signExtraOnTop_" + this.id).addClass("btn-outline-dark");
            }
            if(this.divExtra == null) {
                this.typeSign = "Signature";
                if (this.isVisa) this.typeSign = "Visa";
                let divExtraHtml = "<div id='divExtra_" + this.id + "' class='div-extra div-extra-top' style='position: absolute;z-index: 5;'></div>";
                this.cross.prepend(divExtraHtml);
                this.divExtra = $("#divExtra_" + this.id);
                this.divExtra.append("<span id='extraTypeDiv_"+ this.id +"' style='display: none;'>" + this.typeSign + "<br/></span>");
                this.divExtra.append("<span id='extraNameDiv_"+ this.id +"' style='display: none;'>" + this.userName + "<br/></span>");
                this.divExtra.append("<span id='extraDateDiv_"+ this.id +"' style='display: none;'>le " + moment().format('DD/MM/YYYY HH:mm:ss Z') + "<br/></span>");
                setInterval(function() {
                    self.#refreshDate();
                }, 1000);
                this.#addTextArea();
            } else {
                this.divExtra.removeClass("d-none");
            }
            this.#refreshExtraDiv();
            this.extraHeight = Math.round(parseInt(this.divExtra.css("height"))/ (this.currentScale * this.getBrowserZoom()));
            this.signHeight += this.extraHeight;
            // if(!this.restoreExtra && this.restore && !this.isVisa) {
            //     this.#restoreUserParams();
            //     this.restoreExtra = true;
            // }
        } else {
            if(!this.extraOnTop) {
                this.#toggleExtraOnTop();
            }
            $("#signImage_" + this.id).attr("disabled", true);
            $("#signImage_" + this.id).removeClass("btn-outline-dark");
            $("#signExtra_" + this.id).removeClass("btn-outline-dark");
            $("#signExtraOnTop_" + this.id).attr("disabled", true);
            if(this.divExtra != null) {
                this.divExtra.addClass("d-none");
            }
            // $("#extraTools_" + this.id).addClass("d-none");
            // $("#crossTools_" + this.id).css("top", "-45px");
            this.signHeight -= this.extraHeight;
            this.extraHeight = 0;
        }
        this.#updateSize();
        if(!this.firstLaunch && !this.isShare) {
            localStorage.setItem('addExtra', this.addExtra);
        }

    }

    #toggleExtraOnTop() {
        if(this.divExtra != null) {
            if(!this.extraOnTop) {
                if(this.addWatermark) {
                    this.cross.removeClass("watermark-height")
                    this.cross.addClass("watermark-width")
                }
                this.divExtra.addClass("d-none");
                this.signWidth -= this.extraWidth;
                this.extraWidth = 0;
                this.divExtra.removeClass("d-none");
                this.extraOnTop = true;
                this.#refreshExtraDiv();
                this.extraHeight = Math.round(parseInt(this.divExtra.css("height"))/ (this.currentScale * this.getBrowserZoom()));
                this.signHeight = this.originalHeight * this.signScale + this.extraHeight
                if(this.light == null || !this.light) {
                    this.canvas.css("width", (this.signWidth - this.extraWidth - this.padMargin) * this.currentScale + "px");
                    this.canvas.css("height", (this.signHeight - this.extraHeight - this.padMargin) * this.currentScale + "px");
                    this.cross.css("width", this.signWidth * this.currentScale + "px");
                    this.cross.css("height", this.signHeight * this.currentScale + "px");
                } else {
                    this.cross.css("width", this.originalWidth * this.currentScale + "px");
                    this.cross.css("height", (this.originalHeight + this.extraHeight) * this.currentScale + "px");
                    this.canvas.css("height", this.originalHeight + "px")
                }
                this.divExtra.addClass("div-extra-top");
                this.divExtra.removeClass("div-extra-right");
                $("#signExtraOnTop_" + this.id).addClass("btn-outline-dark");
                $("#signExtraOnTop_" + this.id).children().next().text("Au dessus");

            } else {
                if(this.addWatermark) {
                    this.cross.addClass("watermark-height")
                    this.cross.removeClass("watermark-width")
                }
                $("#signExtraOnTop_" + this.id).removeClass("btn-outline-dark");
                $("#signExtraOnTop_" + this.id).children().next().text("À droite");
                this.divExtra.addClass("d-none");
                this.signHeight -= this.extraHeight;
                this.extraHeight = 0;
                // this.#updateSize();
                this.divExtra.removeClass("d-none");
                this.extraOnTop = false;
                this.#refreshExtraDiv();
                this.signWidth = parseInt(this.cross.css("width"))/ (this.currentScale * this.getBrowserZoom()) * 2;
                this.extraWidth = this.signWidth / 2;
                if(this.light == null || !this.light) {
                    this.cross.css("width", this.signWidth * this.currentScale + "px");
                    this.cross.css("height", this.signHeight * this.currentScale + "px");
                    this.canvas.css("width", (this.signWidth - this.extraWidth - this.padMargin) * this.currentScale + "px");
                    this.canvas.css("height", (this.signHeight - this.extraHeight - this.padMargin) * this.currentScale + "px");
                } else {
                    this.cross.css("width", 400 * this.currentScale + "px");
                    this.cross.css("height", 100 * this.currentScale + "px");
                }
                this.divExtra.addClass("div-extra-right");
                this.divExtra.removeClass("div-extra-top");
            }
            if(!this.firstLaunch && !this.isShare) {
                localStorage.setItem('extraOnTop', this.extraOnTop);
            }
        }
        // if(!this.firstLaunch) {
        //     this.#simulateDrag(this.xPos/ (this.currentScale * this.getBrowserZoom()), this.yPos/ (this.currentScale * this.getBrowserZoom()) / 2);
        // }
    }

    #refreshDate() {
        $("#extraDateDiv_" + this.id).html("le " + moment().format('DD/MM/YYYY HH:mm:ss Z') + "<br/>");
    }

    #toggleType() {
        if(!this.extraName && !this.extraDate && !this.isExtraText && !this.addImage){
            return;
        }
        if(this.extraType) {
            if(!this.extraName && !this.extraDate && !this.isExtraText && this.signImages) {
                this.addExtra = true;
                this.#toggleExtra();
            }
            $("#extraTypeDiv_" + this.id).hide();
            $("#extraType_" + this.id).removeClass("btn-outline-dark");
        } else {
            if(this.addExtra === false) {
                this.#toggleExtra();
            }
            $("#extraTypeDiv_" + this.id).show();
            $("#extraType_" + this.id).addClass("btn-outline-dark");
        }
        this.extraType = !this.extraType;
        this.#updateSize();
        this.#refreshExtraDiv();
        if(!this.firstLaunch && !this.isShare) {
            localStorage.setItem('extraType', this.extraType);
        }
    }

    #toggleName() {
        if(!this.extraType && !this.extraDate && !this.isExtraText && !this.addImage){
            return;
        }
        if(this.extraName) {
            if(this.extraName && !this.extraDate && !this.isExtraText && !this.extraType) {
                this.addExtra = true;
                this.#toggleExtra();
            }
            $("#extraNameDiv_" + this.id).hide();
            $("#extraName_" + this.id).removeClass("btn-outline-dark");
        } else {
            if(this.addExtra === false) {
                this.#toggleExtra();
            }
            $("#extraNameDiv_" + this.id).show();
            $("#extraName_" + this.id).addClass("btn-outline-dark");
        }
        this.extraName = !this.extraName;
        this.#updateSize();
        this.#refreshExtraDiv();
        if(!this.firstLaunch && !this.isShare) {
            localStorage.setItem('extraName', this.extraName);
        }
    }

    #toggleDate() {
        if(!this.extraName && !this.extraType && !this.isExtraText && !this.addImage){
            return;
        }
        if(this.extraDate) {
            if(!this.extraName && this.extraDate && !this.isExtraText && !this.extraType) {
                this.addExtra = true;
                this.#toggleExtra();
            }
            $("#extraDateDiv_" + this.id).hide();
            $("#extraDate_" + this.id).removeClass("btn-outline-dark");
        } else {
            if(this.addExtra === false) {
                this.#toggleExtra();
            }
            $("#extraDateDiv_" + this.id).show();
            $("#extraDate_" + this.id).addClass("btn-outline-dark");
        }
        this.extraDate = !this.extraDate;
        this.#updateSize();
        this.#refreshExtraDiv();
        if(!this.firstLaunch && !this.isShare) {
            localStorage.setItem('extraDate', this.extraDate);
        }
    }

    #toggleText() {
        if(!this.extraName && !this.extraDate && !this.extraType && !this.addImage){
            return;
        }
        let textExtra = $("#textExtra_" + this.id);
        if(this.isExtraText) {
            if(!this.extraName && !this.extraDate && this.isExtraText && !this.extraType) {
                this.addExtra = true;
                this.#toggleExtra();
            }
            $("#extraText_" + this.id).removeClass("btn-outline-dark");
            textExtra.hide();
            this.savedText = this.textareaExtra.val();
            this.textareaExtra.val("");
            this.extraText = "";
        } else {
            if(this.addExtra === false) {
                this.#toggleExtra();
            }
            textExtra.show();
            $("#extraText_" + this.id).addClass("btn-outline-dark");
            this.extraText = this.savedText;
            this.textareaExtra.val(this.savedText);
        }
        this.isExtraText = !this.isExtraText;
        this.#updateSize();
        this.#refreshExtraDiv();
        if(!this.firstLaunch && !this.isShare) {
            localStorage.setItem('extraText', this.isExtraText);
        }
    }

    #updateSize() {
        if(this.extraOnTop) {
            this.signHeight -= this.extraHeight;
            this.extraHeight = 0;
            if(this.divExtra != null) {
                this.extraHeight = Math.round(parseInt(this.divExtra.css("height"))/ (this.currentScale));
            }
            this.signHeight += this.extraHeight;
            this.cross.css("height", this.signHeight * this.currentScale + "px");
            this.canvas.css("height", (this.signHeight - this.extraHeight - this.padMargin) * this.currentScale + "px");
        } else {
            this.signWidth -= this.extraWidth;
            if(this.addImage) {
                this.extraWidth = Math.round(this.originalWidth * this.signScale);
            }
            this.signWidth += this.extraWidth;
            this.cross.css("width", this.signWidth * this.currentScale + "px");
            this.canvas.css("width", (this.signWidth - this.extraWidth - this.padMargin) * this.currentScale + "px");
        }
    }

    #addTextArea() {
        let divExtraHtml = "<textarea id='textExtra_" + this.id + "' tabindex='0' class='sign-textarea align-top' style='display: none;line-height: 1.3 !important;' rows='1' cols='30'></textarea>";
        this.divExtra.append(divExtraHtml);
        this.textareaExtra = $("#textExtra_" + this.id);
        this.textareaExtra.css('width', '100%');
        this.textareaExtra.attr('cols', '30');
        this.textareaExtra.attr('rows', '1');
        this.textareaExtra.on("input", e => this.#refreshExtraDiv());
        document.getElementById("textExtra_" + this.id).addEventListener('touchstart', function(event) {
            event.preventDefault();
            this.focus();
        });
    }

    #refreshExtraDiv() {
        if(this.divExtra != null && !this.light) {
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
                ctx.font = fontSize + "px";
                let txt = lines[i];
                if(ctx.measureText(txt).width < (parseInt(this.textareaExtra.css("width")))) {
                    text += txt;
                    this.stringLength = txt.length;
                } else {
                    console.log("text length : " + ctx.measureText(txt).width + " " + this.textareaExtra.css("width"));
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
                this.#updateSize();
            }
            if(this.light) {
                this.divExtra.css("font-size", "unset");
            }
        }
    }

    #toggleMinimalTools() {
        $("#signPrevImage_" + this.id).hide();
        $("#signNextImage_" + this.id).hide();
        if(!this.isVisa) {
            $("#signExtra_" + this.id).hide();
            $("#signImage_" + this.id).hide();
            $("#watermark_" + this.id).hide();
        }
        $("#signExtraOnTop_" + this.id).hide();
        $("#allPages_" + this.id).hide();
        if(this.signColorPicker != null) {
            // this.signColorPicker.spectrum("destroy");
            this.signColorPicker.hide();
        }
        $("#signColorPicker_" + this.id).hide();
        $("#canvasBtn_" + this.id).remove();
        $("#displayMoreTools_" + this.id).remove();

    }

    #resizeText() {
        const minCols = 10;
        const extraPx = 6;
        let fontSize = this.fontSize * this.currentScale;
        const roundedFontSize = Math.round(fontSize);
        this.textareaPart.css("font-size", roundedFontSize + "px");

        const text = this.textareaPart.val() || "";
        const lines = text.split(/\r\n|\r|\n/);

        if (lines[0] && lines[0].length > 0) {
            this.textareaPart.css("width", "");
        }

        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");
        const fontFamily = this.textareaPart.css("font-family") || "sans-serif";
        ctx.font = `${roundedFontSize}px ${fontFamily}`;

        const sample = "W";
        const avgCharWidth = ctx.measureText(sample).width / sample.length;

        const maxLen = lines.reduce((m, l) => Math.max(m, l.length), 0);

        let finalPxWidth;
        if (maxLen <= minCols) {
            finalPxWidth = Math.ceil(avgCharWidth * minCols) + extraPx;
        } else {
            let maxWidth = 0;
            for (const l of lines) {
                const w = ctx.measureText(l).width;
                if (w > maxWidth) maxWidth = w;
            }
            finalPxWidth = Math.ceil(maxWidth) + extraPx;
        }

        this.textareaPart.css("width", finalPxWidth + "px");
        this.textareaPart.attr("rows", Math.max(lines.length, 1));
        this.textareaPart.attr("cols", Math.max(minCols, Math.ceil(finalPxWidth / avgCharWidth)));

        this.signWidth = Math.round(parseInt(this.textareaPart.css("width"))/ (this.currentScale * this.getBrowserZoom()));
        this.signHeight = Math.round(parseInt(this.textareaPart.css("height"))/ (this.currentScale * this.getBrowserZoom()));

        this.cross.css("width", this.textareaPart.css("width"));
        this.cross.css("height", this.textareaPart.css("height"));
    }

    #getImageDimensions(file) {
        return new Promise (function (resolved) {
            if(file != null) {
                let i = new Image();
                i.onload = function(){
                    resolved({w: i.width / 3, h: i.height / 3})
                };
                i.src = file
            } else {
                resolved({w: 200, h: 100})
            }
        })
    }

    #changeSignColor(color) {
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

    #convertImgToBase64URL(url, callback, outputFormat){
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

    #toggleSignModal(e) {
        if (this.userUI == null) {
            this.userUI = new UserUi();
        }
        $("#add-sign-image").modal("show");
    }

    resize(ui) {
        let newScale = this.#getNewScale(ui);
        let ratio = newScale / this.signScale;
        this.extraWidth = this.extraWidth * ratio;
        this.extraHeight = this.extraHeight * ratio;

        this.signScale = newScale;
        this.signWidth = Math.round(this.originalWidth * this.signScale) + this.extraWidth;
        this.signHeight = Math.round(this.originalHeight * this.signScale) + this.extraHeight;

        if (this.addExtra) {
            if (!this.extraOnTop) {
                this.divExtra.css('width', Math.round(this.extraWidth * this.currentScale) + "px");
            } else {
                this.divExtra.css('width', Math.round(this.originalWidth * this.signScale * this.currentScale) + "px");
            }
        }

        if (this.addImage) {
            this.cross.css('background-size', Math.round(ui.size.width - this.extraWidth * this.currentScale) + "px");
        }
        if (this.addExtra) {
            this.#refreshExtraDiv();
        }
        this.canvas.css("width", (this.signWidth - this.extraWidth - this.padMargin) * this.currentScale);
        this.canvas.css("height", (this.signHeight - this.extraHeight - this.padMargin) * this.currentScale);
    }

    applyCurrentSignRequestParams(offset) {
        this.cross.css('top', Math.round(this.yPos * this.currentScale * this.getBrowserZoom() + offset) + 'px');
        this.cross.css('left', Math.round(this.xPos * this.currentScale * this.getBrowserZoom()) + 'px');
    }

    updateScale(scale) {
        this.currentScale = scale;
        this.cross.css("width", this.signWidth * scale + "px");
        this.cross.css("height", this.signHeight * scale + "px");
        this.cross.css("left", this.xPos * scale + 'px');
        let offset = $("#page_" + this.signPageNumber).offset().top - $("#page_1").offset().top;
        this.cross.css("top", this.yPos * scale + offset + 'px');
        this.canvas.css("width", (this.signWidth * scale - this.extraWidth) + "px");
        this.canvas.css("height", (this.signHeight * scale - this.extraHeight) + "px");
        if(this.addImage) {
            if(this.extraOnTop) {
                this.cross.css('background-size', this.signWidth * scale);
            } else {
                this.cross.css('background-size', this.signWidth * scale / 2);
            }
        }
        if(this.addExtra) {
            this.divExtra.css("width", this.extraWidth * scale + "px");
            this.divExtra.css("font-size", Math.round(10 * scale * this.signScale) + "px");
        }
        if(this.divExtra != null) {
            this.#refreshExtraDiv();
        }
        if(this.textareaPart != null) {
            this.#resizeText();
        }
    }

    lock() {
        if(this.textareaPart == null) {
            this.#enableCrossResizable();
        }
        $("#extraTools_" + this.id).addClass("d-none");
        this.cross.draggable("enable");
        this.cross.addClass("hide-handles");
        this.tools.addClass("d-none");
        if(this.userSignaturePad != null) {
            this.userSignaturePad.signaturePad.off();
            this.canvas.css("cursor", "move");
        }
        if(!this.firstLaunch) {
            this.canvasBtn.show();
        }
        if(this.textareaExtra != null) {
            this.textareaExtra.addClass("sign-textarea-lock");
        }
        $(document).unbind('keydown');
        this.canvasBtn.removeClass("d-none");
        this.#computeBgColor();
    }

    getBrowserZoom() {
        return 1 || 1;
    }


    simulateDrop() {
        if(this.firstLaunch) {
            let x = Math.round(this.xPos * this.currentScale * this.getBrowserZoom());
            let y = Math.round(this.yPos * this.currentScale  * this.getBrowserZoom() + $("#page_" + this.signPageNumber).offset().top - $("#page_1").offset().top);
            let self = this;
            this.cross.on("dragstop", function () {
                let test = self.scrollTop + $(window).height();
                if (y > test) {
                    window.scrollTo(0, y);
                }
                $(this).unbind("dragstop");
            });
            this.#simulateDrag(x, y);
        }
    }

    turnToText() {
        $("#signImage_" + this.id).remove();
        let self = this;
        let divExtraHtml = "<textarea id='textPart_" + this.id + "' class='sign-textarea align-top' rows='1' style='overflow: hidden'></textarea>";
        this.cross.append(divExtraHtml);
        this.textareaPart = $("#textPart_" + this.id);
        this.textareaPart.css('width', '100%');
        this.border.remove();
        this.fontSize = 10;
        this.textareaPart.on("input", function () {
            self.textPart = $(this).val();
            self.#resizeText();
        });
        this.#resizeText();
        this.cross.resizable("destroy");
        let textGrow = $("#textGrow_" + this.id);
        let textReduce = $("#textReduce_" + this.id);
        textGrow.show();
        textReduce.show();
        textGrow.on("click", function () {
            self.fontSize = self.fontSize + 1
            self.#resizeText();
        });
        textReduce.on("click", function () {
            self.fontSize = self.fontSize - 1
            self.#resizeText();
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

    changeSignImage(imageNum) {
        this.#disableCanvas();
        if(imageNum != null && imageNum >= 0) {
            if(this.signImages != null) {
                if(imageNum > this.signImages.length - 1 && imageNum !== 999998 && imageNum !== 999997) {
                    imageNum = 0;
                }
                this.signImageNumber = imageNum;
                console.debug("debug - " + "change sign image to " + imageNum);
                let img = null;
                if(this.signImages[imageNum] != null) {
                    img = "data:image/jpeg;charset=utf-8;base64, " + this.signImages[imageNum];
                    this.cross.css("background-image", "url('" + img + "')");
                    let sizes = this.#getImageDimensions(img);
                    sizes.then(result => this.changeSignSize(result));
                    if(imageNum !== 999999) {
                        localStorage.setItem('signNumber', imageNum);
                    }
                } else {
                    let self = this;
                    let url = "/ws-secure/users/get-default-image-base64";
                    if(imageNum === 999997) {
                        url = "/ws-secure/users/get-default-paraphe-base64";
                    }
                    $.get({
                        url: url,
                        success: function(data) {
                            img = "data:image/PNG;charset=utf-8;base64, " + data;
                            self.cross.css("background-image", "url('" + img + "')");
                            let sizes = self.#getImageDimensions(img);
                            sizes.then(result => self.changeSignSize(result));
                            if(imageNum !== 999999) {
                                localStorage.setItem('signNumber', imageNum);
                            }
                        }
                    });
                }
            }
        } else if(imageNum < 0) {
            this.signImageNumber = imageNum;
            let self = this;
            this.#convertImgToBase64URL('/images/' + this.faImages[Math.abs(imageNum) - 1] + '.png', function(img) {
                self.cross.css("background-image", "url('" + img + "')");
                let sizes = self.#getImageDimensions(img);
                sizes.then(result => self.changeSignSize(result));
            });
            this.addExtra = true;
            this.extraOnTop = true;
            this.#toggleExtra();
        }
    }

}