export default class UserUi {

    constructor(lastSign) {
        console.log('Starting user UI');
        this.emailAlertFrequencySelect = document.getElementById("_emailAlertFrequency_id");
        this.emailAlertDay = document.getElementById("emailAlertDay");
        this.emailAlertHour = document.getElementById("emailAlertHour");
        this.userSignaturePad = new UserSignaturePad(lastSign);
        this.userSignatureCrop = new UserSignatureCrop();
        this.checkAlertFrequency();
    }

    checkAlertFrequency() {
        let selectedValue = this.emailAlertFrequencySelect.options[this.emailAlertFrequencySelect.selectedIndex].value;
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

    constructor(lastSign) {
        this.canvas = $("#canvas");
        this.signImageBase64 = $("#signImageBase64");
        this.signaturePad = new SignaturePad(document.querySelector("canvas"));
        this.firstClear = true;
        this.lastSign = null;
        this.setLastSign(lastSign);
        this.initListeners();
    }

    initListeners() {
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
        this.signImageBase64.val(this.signaturePad.toDataURL("image/png"));
        this.canvas.css("backgroundColor", "rgba(0, 255, 0, .5)");
    }

    clearSignaturePad() {
        this.canvas.css("backgroundColor", "rgba(255, 255, 255, 1)");
        this.signaturePad.clear();
        this.signImageBase64.val(this.lastSign);
    }

    resetSignaturePad() {
        this.canvas.css("backgroundColor", "rgba(255, 255, 255, 1)");
        this.signaturePad.clear();
        this.signaturePad.fromDataURL(this.lastSign);
        this.signImageBase64.val(this.lastSign);
        this.firstClear = true;
    }

}

export class UserSignatureCrop {

    constructor() {
        console.info("Starting user signature crop tool");
        this.signImageBase64 = $("#signImageBase64");
        this.vanillaUpload = document.getElementById('vanilla-upload');
        this.vanillaRotate = document.querySelector('.vanilla-rotate');
        this.vanillaCrop = document.getElementById('vanilla-crop')
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
        this.initListeners();
    }

    initListeners() {
        this.vanillaRotate.click(e => this.rotate(this.vanillaCroppie));
        this.vanillaUpload.addEventListener('change', e=> this.readFile(this.vanillaUpload));
        this.vanillaCrop.addEventListener('update', e => this.update());
    }

    update() {
        let result = this.getResult();
        result.then(this.saveVanilla);
    }

    rotate(elem) {
        this.vanillaCroppie.rotate(parseInt($(elem).data('deg')));
    }

    saveVanilla(result) {
        console.log(Promise.resolve(result));
        $("#signImageBase64").val(result);
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