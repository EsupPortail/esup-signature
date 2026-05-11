export class UserSignaturePad {

    constructor(name, minWidth, maxWidth) {
        console.info("Starting user signature pad tool");
        this.canvas = $('#' + name);
        this.signImageBase64 = $("#signImageBase64");
        this.signImageBase64Val = null;
        this.signaturePad = new SignaturePad(this.canvas[0], {
            minWidth: minWidth,
            maxWidth: maxWidth
        });
        this.firstClear = true;
        this.cachedData = [];
        this.pendingDirtyState = false;
        this.initListeners();
        this.resizeCanvas();
        this.cachedWidth = null;
    }

    initListeners() {
        this.canvas.on('pointerdown', () => this.startSignatureInteraction());
        this.canvas.on('pointerup', () => this.endSignatureInteraction());
        this.canvas.on('pointerleave', () => this.cancelSignatureInteraction());
        this.canvas.on('pointercancel', () => this.cancelSignatureInteraction());
        $('#erase').click(() => this.clear());
        // $('#validate').click(e => this.saveSignaturePad());
        // $('#reset').click(e => this.resetSignaturePad());
        window.addEventListener("resize", () => this.resizeCanvas());
        $(document).ready(() => this.resizeCanvas());
        // window.addEventListener("resize", e => this.resizeCanvas());

    }

    destroy() {
        this.signaturePad.off();
        this.signaturePad.clear();
    }

    resizeCanvas() {
        if(this.canvas[0].offsetWidth !== this.cachedWidth ) {
            if (typeof this.signaturePad != 'undefined') {
                this.cachedData = this.signaturePad.toData();
            }
            this.cachedWidth = this.canvas[0].offsetWidth;  
            let ratio = Math.max(window.devicePixelRatio || 1, 1);
            this.canvas[0].width = this.canvas[0].offsetWidth * ratio;
            this.canvas[0].height = this.canvas[0].offsetHeight * ratio;
            this.canvas[0].getContext("2d").scale(ratio, ratio);
            if (typeof this.signaturePad != 'undefined') {
                this.signaturePad.clear();
                if (Array.isArray(this.cachedData) && this.cachedData.length > 0) {
                    this.signaturePad.fromData(this.cachedData);
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
        this.firstClearSignaturePad();
        this.markPendingDirtyState();
    }

    endSignatureInteraction() {
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
        this.updateSignImage(imageBase64);
        this.hidePlaceholder();
    }

    clear() {
        console.info("clear sign pad");
        this.pendingDirtyState = false;
        this.signaturePad.clear();
        this.cachedData = [];
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
        const signImageBase64Element = this.signImageBase64.get(0);
        if (signImageBase64Element) {
            signImageBase64Element.dispatchEvent(new Event('input', {bubbles: true}));
            signImageBase64Element.dispatchEvent(new Event('change', {bubbles: true}));
        }
    }

    updateSignImage(value) {
        this.signImageBase64.val(value);
        this.signImageBase64Val = value || null;
        this.dispatchDirtyRefresh();
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