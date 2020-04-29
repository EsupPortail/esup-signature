import {PdfViewer} from "./pdfViewer.js";

export class CreateDataUi {

    constructor(documentId, fields) {
        this.pdfViewer = new PdfViewer('/user/documents/getfile/' + documentId, true, 0);
        this.pdfViewer.setDataFields(fields);
        this.pdfViewer.scale = 0.70;
        this.pdfViewer.addEventListener('ready', e => this.startRender());
        this.initListeners();
    }


    initListeners() {
        document.getElementById('saveButton').addEventListener('click', e => this.saveData(e));
        document.getElementById('saveForm').addEventListener('submit', e => this.saveData(e));
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
        this.pdfViewer.adjustZoom();
    }

}