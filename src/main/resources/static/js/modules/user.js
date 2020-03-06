export class User {

    static signImageBase64 = $('#signImageBase64');
    emailAlertFrequencySelect = document.getElementById("_emailAlertFrequency_id");
    emailAlertDay = document.getElementById("emailAlertDay");
    emailAlertHour = document.getElementById("emailAlertHour");
    userSignaturePad;
    userSignatureCrop;

    constructor(lastSign) {
        console.log('const user');
        this.checkAlertFrequency();
        this.userSignaturePad = new UserSignaturePad(lastSign);
        this.userSignatureCrop = new UserSignatureCrop();
    }

    checkAlertFrequency() {
        var selectedValue = this.emailAlertFrequencySelect.options[this.emailAlertFrequencySelect.selectedIndex].value;
        if(selectedValue === 'daily') {
            this.emailAlertDay.style.display = "none";
            this.emailAlertHour.style.display = "flex";
        } else if(selectedValue === 'weekly') {
            this.emailAlertDay.style.display = "flex";
            this.emailAlertHour.style.display = "none";
        } else {
            this.emailAlertDay.style.display = "none";
            this.emailAlertHour.style.display = "none";
        }
    }
}

export class UserSignaturePad {
    lastSign;
    canvas = $("#canvas");
    signaturePad = new SignaturePad(document.querySelector("canvas"));
    firstClear = true;

    constructor(lastSign) {
        this.setLastSign(lastSign);
        this.init();
    }

    init() {
        this.canvas.mousedown(e => this.firstClearSignaturePad());
        $('#erase').click(e => this.clearSignaturePad());
        $('#validate').click(e =>this.saveSignaturePad());
        $('#reset').click(e => this.resetSignaturePad());
    }

    setLastSign(lastSign) {
        this.lastSign = lastSign;
        this.ratio =  Math.max(window.devicePixelRatio || 1, 1);
        if(this.ratio === 1) {
            this.signaturePad.fromDataURL(this.lastSign);
        } else {
            this.firstClearSignaturePad();
        }
    }

    firstClearSignaturePad() {
        if(this.firstClear) {
            this.clearSignaturePad();
            this.firstClear = false;
        }
    }

    saveSignaturePad() {
        console.log(this.signaturePad.toDataURL("image/png"));
        User.signImageBase64.val(this.signaturePad.toDataURL("image/png"));
        this.canvas.css("backgroundColor", "rgba(0, 255, 0, .5)");
    }

    clearSignaturePad() {
        this.canvas.css("backgroundColor", "rgba(255, 255, 255, 1)");
        this.signaturePad.clear();
        User.signImageBase64.val(this.lastSign);
    }

    resetSignaturePad() {
        this.canvas.css("backgroundColor", "rgba(255, 255, 255, 1)");
        this.signaturePad.clear();
        this.signaturePad.fromDataURL(this.lastSign);
        User.signImageBase64.val(this.lastSign);
        this.firstClear = true;
    }

}

export class UserSignatureCrop {
    vanillaUpload = document.getElementById('vanilla-upload');
    vanillaRotate = document.querySelector('.vanilla-rotate');
    vanillaCrop = document.getElementById('vanilla-crop')
    vanillaCroppie;

    constructor() {
        this.vanillaCroppie = new Croppie(this.vanillaCrop, {
            viewport : {
                width : 200,
                height : 150
            },
            boundary : {
                width : 400,
                height : 300
            },
            enableExif : true,
            enableOrientation : true,
            enableResize : true,
            enforceBoundary : false

        });
        this.init();
    }

    init() {
        this.vanillaRotate.click(e => this.rotate(this.vanillaCroppie));
        this.vanillaCrop.addEventListener('update', e => this.update());
        this.vanillaUpload.addEventListener('change', e=> this.readFile(this.vanillaUpload));
    }

    update() {
        var result = this.getResult();
        result.then(this.saveVanilla);
    }

    rotate(elem) {
        this.vanillaCroppie.rotate(parseInt($(elem).data('deg')));
    }

    saveVanilla(result) {
        User.signImageBase64.value = result;
    }

    getResult() {
        return this.vanillaCroppie.result('base64');
    }

    bind(e) {
        this.vanillaCrop.classList.add('good');
        this.vanillaCroppie.bind({
            url : e.target.result,
            orientation : 1
        });
    }

    readFile(input) {
        if (input.files) {
            if (input.files[0]) {
                let reader = new FileReader();
                reader.addEventListener('load', e => this.bind(e));
                reader.readAsDataURL(input.files[0]);
            }
        }
    }
}