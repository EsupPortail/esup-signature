import {PdfViewer} from "./pdfViewer.js";

export class CreateDataUi {

    constructor(documentId, fields) {
        this.pdfViewer = new PdfViewer('/user/documents/getfile/' + documentId, null);
        this.pdfViewer.setDataFields(fields);
        this.pdfViewer.scale = 1.5;
        this.pdfViewer.addEventListener('ready', e => this.startRender());
        this.initListeners();
    }

    initListeners() {
        document.getElementById('saveForm').addEventListener('submit', e => this.saveData(e));
    }

    saveData(e) {
        e.preventDefault();
        let tempName = document.getElementById('tempMame');
        if (tempName.checkValidity()) {
            document.getElementById('name').value = tempName.value;
            document.getElementById('newDataSubmit').click();
        } else {
            tempName.focus();
            document.getElementById('tempMame');
        }
    }

    startRender() {
        this.pdfViewer.renderPage(1);
    }

}