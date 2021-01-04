export class UserSignaturePad {

    constructor(lastSign, signWidth, signHeight) {
        console.info("Starting user signature pad tool");
        this.canvas = document.getElementById("canvas");
        this.signImageBase64 = $("#signImageBase64");
        this.signaturePad = new SignaturePad(this.canvas, {
            minWidth: 1,
            maxWidth: 2,
            throttle: 0,
            minDistance: 5,
            velocityFilterWeight: 0.1
        });

        this.firstClear = true;
        this.lastSign = null;
        this.initListeners();
        this.resizeCanvas();
    }

    initListeners() {
        this.canvas.addEventListener('mousedown', e => this.firstClearSignaturePad());
        this.canvas.addEventListener('touchstart', e => this.firstClearSignaturePad());
        $('#erase').click(e => this.clear());
        // $('#validate').click(e => this.saveSignaturePad());
        // $('#reset').click(e => this.resetSignaturePad());
        window.addEventListener("resize", e => this.resizeCanvas());
        $("#saveButton").on('click', e => this.checkSignatureUpdate());
    }

    checkSignatureUpdate() {
        if (!this.signaturePad.isEmpty()) {
            this.save();
        }
        $("#userParamsForm").submit();
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
        this.signImageBase64.val(this.signaturePad.toDataURL("image/png"));
        this.canvas.style.background = "rgba(0, 255, 0, .5)";
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
        this.canvas.width = this.canvas.offsetWidth * ratio;
        this.canvas.height = this.canvas.offsetHeight * ratio;
        this.canvas.getContext("2d").scale(ratio, ratio);
        //this.resetSignaturePad();
    }
}