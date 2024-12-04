import {EventFactory} from "../../utils/EventFactory.js?version=@version@";

export class UserSignatureCrop extends EventFactory {

    constructor() {
        super();
        console.info("Starting user signature crop tool");
        this.cropDiv = document.getElementById('crop-div');
        this.zoomLevel = 1;
        this.signPad = document.getElementById('signPad');
        this.signPadLabel = document.getElementById('signPadLabel');
        this.zoomInButton = document.getElementById('zoomin');
        this.zoomOutButton = document.getElementById('zoomout');
        this.vanillaUpload = document.getElementById('vanilla-upload');
        this.vanillaRotate = document.getElementsByClassName('vanilla-rotate');
        this.vanillaCrop = document.getElementById('vanilla-crop');
        this.vanillaCroppie = new Croppie(this.vanillaCrop, {
            viewport: {
                width: 600,
                height: 300
            },
            boundary: {
                width: 604,
                height: 304
            },
            enableExif: true,
            enableOrientation: true,
            enableResize: false,
            enforceBoundary: false,
            mouseWheelZoom: true
        });
        this.events = {};
        this.signImageBase64 = "";
        this.initListeners();
    }

    initListeners() {
        Array.prototype.forEach.call(this.vanillaRotate, e => this.rotateListener(e));
        this.zoomInButton.addEventListener('click', e => this.zoomIn());
        this.zoomOutButton.addEventListener('click', e => this.zoomOut());
        this.vanillaUpload.addEventListener('change', e => this.readFile(this.vanillaUpload));
        this.vanillaCrop.addEventListener('update', e => this.update());
    }

    rotateListener(item) {
        item.addEventListener('click', e => this.rotate(item));
    }

    update() {
        let result = this.getResult();
        result.then(e => this.saveVanilla(e));
    }

    rotate(elem) {
        console.log(elem);
        let rotation = parseInt($(elem).data('deg'));
        console.info('rotate ' + rotation)
        this.vanillaCroppie.rotate(rotation);
    }

    zoomIn() {
        this.zoomLevel = this.zoomLevel + 0.05;
        this.zoom();
    }

    zoomOut() {
        this.zoomLevel = this.zoomLevel - 0.05;
        this.zoom();
    }

    zoom() {
        this.vanillaCroppie.setZoom(this.zoomLevel);
    }

    saveVanilla(result) {
        this.signImageBase64 = result;
        $("#signImageBase64").val(result);
    }

    getResult() {
        return this.vanillaCroppie.result('base64');
    }

    bind(e) {
        this.vanillaCrop.classList.add('good');
        this.vanillaCroppie.bind({
            url: e.target.result,
            orientation: 1,
            currentZoom: 1
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