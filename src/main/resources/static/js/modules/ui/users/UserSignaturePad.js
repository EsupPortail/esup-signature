let userSignaturePadInstanceCounter = 0;

export class UserSignaturePad {

    constructor(name, minWidth, maxWidth) {
        console.info("Starting user signature pad tool");
        this.eventNamespace = ".userSignaturePad-" + (++userSignaturePadInstanceCounter);
        this.canvas = $('#' + name);
        this.signImageBase64 = $("#signImageBase64");
        this.signImageBase64Val = null;
        this.resizeHandler = () => this.resizeCanvas();
        this.signaturePad = new SignaturePad(this.canvas[0], {
            minWidth: minWidth,
            maxWidth: maxWidth
        });
        this.firstClear = true;
        this.cachedData = [];
        this.pendingDirtyState = false;
        this.drawingLocked = false;
        this.initListeners();
        this.cachedWidth = null;
        this.cachedHeight = null;
        this.resizeCanvas();
    }

    initListeners() {
        this.canvas.on('pointerdown' + this.eventNamespace, () => this.startSignatureInteraction());
        this.canvas.on('pointerup' + this.eventNamespace, () => this.endSignatureInteraction());
        this.canvas.on('pointerleave' + this.eventNamespace, () => this.cancelSignatureInteraction());
        this.canvas.on('pointercancel' + this.eventNamespace, () => this.cancelSignatureInteraction());
        $('#erase').on('click' + this.eventNamespace, () => this.clear());
        // $('#validate').click(e => this.saveSignaturePad());
        // $('#reset').click(e => this.resetSignaturePad());
        $(window).on("resize" + this.eventNamespace, this.resizeHandler);
        $(document).ready(() => this.resizeCanvas());
        // window.addEventListener("resize", e => this.resizeCanvas());

    }

    destroy() {
        this.canvas.off(this.eventNamespace);
        $('#erase').off(this.eventNamespace);
        $(window).off(this.eventNamespace);
        this.signaturePad.off();
        this.signaturePad.clear();
    }

    setDrawingLocked(locked) {
        if (this.drawingLocked === locked) {
            return;
        }

        this.drawingLocked = locked;
        if (locked) {
            this.signaturePad.off();
            this.canvas.css({
                cursor: "move",
                pointerEvents: "none"
            });
            return;
        }

        this.signaturePad.on();
        this.canvas.css({
            cursor: "crosshair",
            pointerEvents: ""
        });
    }

    resizeCanvas() {
        if(this.canvas[0].offsetWidth !== this.cachedWidth || this.canvas[0].offsetHeight !== this.cachedHeight) {
            if (typeof this.signaturePad != 'undefined') {
                this.cachedData = this.signaturePad.toData();
            }
            this.cachedWidth = this.canvas[0].offsetWidth;
            this.cachedHeight = this.canvas[0].offsetHeight;
            let ratio = Math.max(window.devicePixelRatio || 1, 1);
            this.canvas[0].width = this.canvas[0].offsetWidth * ratio;
            this.canvas[0].height = this.canvas[0].offsetHeight * ratio;
            this.canvas[0].getContext("2d").scale(ratio, ratio);
            if (typeof this.signaturePad != 'undefined') {
                this.signaturePad.clear();
                if (Array.isArray(this.cachedData) && this.cachedData.length > 0) {
                    this.signaturePad.fromData(this.cachedData);
                    this.hidePlaceholder();
                } else if (this.signImageBase64Val) {
                    this.hidePlaceholder();
                } else {
                    this.showPlaceholder();
                }
            }
        }
    }

    checkSignatureUpdate() {
        if (!this.signaturePad.isEmpty()) {
            this.save();
        }
    }

    startSignatureInteraction() {
        if (this.drawingLocked) {
            return;
        }
        this.firstClearSignaturePad();
        this.markPendingDirtyState();
    }

    endSignatureInteraction() {
        if (this.drawingLocked) {
            return;
        }
        this.checkSignatureUpdate();
    }

    cancelSignatureInteraction() {
    }

    firstClearSignaturePad() {
        if (this.firstClear) {
            this.clear();
            this.firstClear = false;
        }
        this.hidePlaceholder();
    }

    save() {
        let imageBase64 = this.signaturePad.toDataURL("image/png");
        this.pendingDirtyState = false;
        this.setCanvasBackground('');
        this.updateSignImage(imageBase64);
        this.hidePlaceholder();
    }

    clear() {
        console.info("clear sign pad");
        this.setDrawingLocked(false);
        this.pendingDirtyState = false;
        this.signaturePad.clear();
        this.cachedData = [];
        this.setCanvasBackground('');
        this.updateSignImage('');
        this.showPlaceholder();
    }

    reset() {
        this.firstClear = true;
        this.updateSignImage('');
        this.canvas.css("background", "");
        this.clear();
    }

    hasPendingDirtyState() {
        return this.pendingDirtyState;
    }

    markPendingDirtyState() {
        if (this.pendingDirtyState) {
            return;
        }

        this.pendingDirtyState = true;
        this.dispatchDirtyRefresh();
    }

    dispatchDirtyRefresh() {
        if (this.signImageBase64 == null || typeof this.signImageBase64.get !== 'function') {
            return;
        }
        const signImageBase64Element = this.signImageBase64.get(0);
        if (signImageBase64Element) {
            signImageBase64Element.dispatchEvent(new Event('input', {bubbles: true}));
            signImageBase64Element.dispatchEvent(new Event('change', {bubbles: true}));
        }
    }

    updateSignImage(value) {
        this.signImageBase64Val = value || null;
        if (this.signImageBase64 == null || typeof this.signImageBase64.val !== 'function') {
            return;
        }
        this.signImageBase64.val(value);
        this.dispatchDirtyRefresh();
    }

    setCanvasBackground(imageBase64) {
        if (imageBase64) {
            this.canvas.css({
                backgroundImage: `url("${imageBase64}")`,
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat',
                backgroundSize: 'contain'
            });
            return;
        }

        this.canvas.css({
            backgroundImage: '',
            backgroundPosition: '',
            backgroundRepeat: '',
            backgroundSize: '',
            background: ''
        });
    }

    loadImage(imageBase64) {
        if (!imageBase64) {
            this.clear();
            this.firstClear = true;
            return;
        }

        this.pendingDirtyState = false;
        this.firstClear = false;
        this.cachedData = [];
        this.signaturePad.clear();
        this.setCanvasBackground(imageBase64);
        this.updateSignImage(imageBase64);
        this.hidePlaceholder();
        this.setDrawingLocked(true);
    }

    getPlaceholderElements() {
        return $("#signPadTip, #signPad .signature-pad-background");
    }

    showPlaceholder() {
        this.getPlaceholderElements().show();
    }

    hidePlaceholder() {
        this.getPlaceholderElements().hide();
    }

}
