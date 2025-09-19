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
        this.lastSign = null;
        this.initListeners();
        this.resizeCanvas();
        this.canvas.mousedown();
        this.cachedWidth = null;
        this.cachedImage = null;
    }

    initListeners() {
        this.canvas.on('mousedown', e => this.firstClearSignaturePad());
        this.canvas.on('touchstart', e => this.firstClearSignaturePad());
        this.canvas.on('click', e => this.firstClearSignaturePad());
        $('#erase').click(e => this.clear());
        // $('#validate').click(e => this.saveSignaturePad());
        // $('#reset').click(e => this.resetSignaturePad());
        window.addEventListener("resize", e => this.resizeCanvas());
        $(document).ready(e => this.resizeCanvas());
        // window.addEventListener("resize", e => this.resizeCanvas());

    }

    destroy() {
        this.signaturePad.off();
        this.signaturePad.clear();
    }

    resizeCanvas() {
        if(this.canvas[0].offsetWidth !== this.cachedWidth ) {
            if (typeof this.signaturePad != 'undefined') {
                this.cachedImage = this.signaturePad.toDataURL("image/png");
            }
            this.cachedWidth = this.canvas[0].offsetWidth;  
            let ratio = Math.max(window.devicePixelRatio || 1, 1);
            this.canvas[0].width = this.canvas[0].offsetWidth * ratio;
            this.canvas[0].height = this.canvas[0].offsetHeight * ratio;
            this.canvas[0].getContext("2d").scale(ratio, ratio);
            if (typeof this.signaturePad != 'undefined' && !this.signaturePad.isEmpty()) {
                this.signaturePad.fromDataURL(this.cachedImage);
            }
        }
    }

    checkSignatureUpdate() {
        if (!this.signaturePad.isEmpty()) {
            this.save();
        }
    }

    setLastSign() {
        this.lastSign = this.signaturePad.toDataURL("image/png");
    }

    firstClearSignaturePad() {
        if (this.firstClear) {
            this.clear();
            this.firstClear = false;
        } else {
            $("#signPadTip").hide();
        }
        this.setLastSign();
    }

    save() {
        let imageBase64 = this.signaturePad.toDataURL("image/png");
        this.signImageBase64.val(imageBase64);
        this.signImageBase64Val = imageBase64;
        this.canvas.css("background", "rgba(0, 255, 0, .5)");
    }

    clear() {
        console.info("clear sign pad");
        this.signaturePad.clear();
        $("#signPadTip").show();
    }

}