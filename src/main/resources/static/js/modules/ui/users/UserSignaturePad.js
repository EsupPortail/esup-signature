export class UserSignaturePad {

    constructor(name) {
        console.info("Starting user signature pad tool");
        this.canvas = $('#' + name);
        this.signImageBase64 = $("#signImageBase64");
        this.signImageBase64Val = null;
        this.signaturePad = new SignaturePad(this.canvas[0], {
            minWidth: 1,
            maxWidth: 4
        });
        this.firstClear = true;
        this.lastSign = null;
        this.initListeners();
        this.resizeCanvas();
        this.canvas.mousedown();
    }

    initListeners() {
        this.canvas.on('mousedown', e => this.firstClearSignaturePad());
        this.canvas.on('touchstart', e => this.firstClearSignaturePad());
        $('#erase').click(e => this.clear());
        // $('#validate').click(e => this.saveSignaturePad());
        // $('#reset').click(e => this.resetSignaturePad());
        window.addEventListener("resize", e => this.resizeCanvas());
        $(document).ready(e => this.resizeCanvas());
    }

    checkSignatureUpdate() {
        if (!this.signaturePad.isEmpty()) {
            this.save();
        }
    }

    setLastSign() {
        this.lastSign = this.signaturePad.toDataURL("image/png");
        //this.ratio = Math.max(window.devicePixelRatio || 1, 1);
        // if(this.ratio === 1) {

        // } else {
        //     this.firstClearSignaturePad();
        // }
    }

    firstClearSignaturePad() {
        if (this.firstClear) {
            this.clear();
            this.firstClear = false;
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
    }

    // resetSignaturePad() {
    //     console.info("reset pad");
    //     // this.canvas.css("backgroundColor", "rgba(255, 255, 255, 1)");
    //     this.signaturePad.clear();
    //     this.signaturePad.fromDataURL(this.lastSign);
    //     this.firstClear = true;
    // }

    resizeCanvas() {
        console.info("resize sign pad");
        let ratio = Math.max(window.devicePixelRatio || 1, 1);
        this.canvas[0].width = this.canvas[0].offsetWidth * ratio;
        this.canvas[0].height = this.canvas[0].offsetHeight * ratio;
        this.canvas[0].getContext("2d").scale(ratio, ratio);
        //this.resetSignaturePad();
    }
}