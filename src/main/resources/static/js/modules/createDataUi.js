import {PdfViewer} from "./pdfViewer.js";

export class CreateDataUi {

    constructor(documentId, fields) {
        this.pdfViewer = new PdfViewer('/user/documents/getfile/' + documentId, true, 0);
        this.pdfViewer.setDataFields(fields);
        this.pdfViewer.scale = 0.75;
        this.pdfViewer.addEventListener('ready', e => this.startRender());
        this.initListeners();
    }


    initListeners() {
        document.getElementById('saveButton').addEventListener('click', e => this.saveData(e));
        document.getElementById('saveForm').addEventListener('submit', e => this.saveData(e));
        let delay = 0;
        let offset = 300;

        document.addEventListener('invalid', function(e){
            $(e.target).addClass("invalid");
            $('html, body').animate({scrollTop: $($(".invalid")[0]).offset().top - offset }, delay);
        }, true);
        document.addEventListener('change', function(e){
            $(e.target).removeClass("invalid")
        }, true);
    }

    saveData(e) {
        e.preventDefault();
        console.info("check data name");
        let tempName = document.getElementById('tempName');
        if (tempName.checkValidity()) {
            console.info("submit form");
            document.getElementById('name').value = tempName.value;
            document.getElementById('newDataSubmit').click();
        } else {
            tempName.focus();
            document.getElementById('tempName');
        }
    }

    startRender() {
        this.pdfViewer.renderPage(1);
    }

}