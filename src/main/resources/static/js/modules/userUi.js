export default class UserUi {

    constructor(lastSign, signWidth, signHeight) {
        console.log('Starting user UI');
        this.emailAlertFrequencySelect = document.getElementById("_emailAlertFrequency_id");
        this.emailAlertDay = document.getElementById("emailAlertDay");
        this.emailAlertHour = document.getElementById("emailAlertHour");
        this.userSignaturePad = new UserSignaturePad(lastSign, signWidth, signHeight);
        this.userSignatureCrop =  new UserSignatureCrop();
        this.checkAlertFrequency();
        this.initListeners();
    }

    initListeners() {
        this.userSignatureCrop.addEventListener('started', e => this.userSignaturePad.clear());
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
        $("#userParamsFormSubmit").on('click', e => this.checkSignatureUpdate());
    }

    checkSignatureUpdate() {
        if(!this.signaturePad.isEmpty()) {
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


export class UserSignatureCrop {

    constructor() {
        console.info("Starting user signature crop tool");
        this.cropDiv = document.getElementById('crop-div');
        this.zoomLevel = 1;
        this.signPad = document.getElementById('signPad');
        this.signPadLabel = document.getElementById('signPadLabel');
        this.zoomInButton = document.getElementById('zoomin');
        this.zoomOutButton = document.getElementById('zoomout');
        this.vanillaUpload = document.getElementById('vanilla-upload');
        this.vanillaRotate = document.querySelector('.vanilla-rotate');
        this.vanillaCrop = document.getElementById('vanilla-crop');
        this.vanillaCroppie = new Croppie(this.vanillaCrop, {
            viewport : {
                width : 600,
                height : 300
            },
            boundary : {
                width : 604,
                height : 304
            },
            enableExif : true,
            enableOrientation : true,
            enableResize : true,
            enforceBoundary : false,
            mouseWheelZoom: true
        });
        this.events = {};
        this.initListeners();
    }

    initListeners() {
        this.vanillaRotate.click(e => this.rotate(this.vanillaCroppie));
        this.zoomInButton.addEventListener('click', e => this.zoomIn());
        this.zoomOutButton.addEventListener('click', e => this.zoomOut());
        this.vanillaUpload.addEventListener('change', e=> this.readFile(this.vanillaUpload));
        this.vanillaCrop.addEventListener('update', e => this.update());
    }

    addEventListener(name, handler) {
        if (this.events.hasOwnProperty(name))
            this.events[name].push(handler);
        else
            this.events[name] = [handler];
    };

    removeEventListener(name, handler) {
        if (!this.events.hasOwnProperty(name))
            return;

        let index = this.events[name].indexOf(handler);
        if (index !== -1)
            this.events[name].splice(index, 1);
    };

    fireEvent(name, args) {
        if (!this.events.hasOwnProperty(name))
            return;

        if (!args || !args.length)
            args = [];

        let evs = this.events[name], l = evs.length;
        for (let i = 0; i < l; i++) {
            evs[i].apply(null, args);
        }
    };

    update() {
        let result = this.getResult();
        result.then(this.saveVanilla);
    }

    rotate(elem) {
        this.vanillaCroppie.rotate(parseInt($(elem).data('deg')));
    }

    zoomIn() {
        this.zoomLevel = this.zoomLevel + 0.05;
        this.zoom();
    }

    zoomOut() {
        this.zoomLevel = this.zoomLevel - 0.05;
        this.zoom();
    }

    zoom()  {
        this.vanillaCroppie.setZoom(this.zoomLevel);
    }

    saveVanilla(result) {
        $("#signImageBase64").val(result);
    }

    getResult() {
        return this.vanillaCroppie.result('base64');
    }

    bind(e) {
        this.vanillaCrop.classList.add('good');
        this.vanillaCroppie.bind({
            url : e.target.result,
            orientation : 1,
            currentZoom : 1
        });
    }

    readFile(input) {
        this.cropDiv.style.display = "block";
        if (input.files) {
            if (input.files[0]) {
                let reader = new FileReader();
                reader.addEventListener('load', e => this.bind(e));
                reader.readAsDataURL(input.files[0]);
                this.signPad.style.display = 'none';
                this.signPadLabel.style.display = 'none';
                this.fireEvent('started', ['ok']);
            }
        }
    }
}