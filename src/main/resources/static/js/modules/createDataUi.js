import {PdfViewer} from "./pdfViewer.js";

export class CreateDataUi {

    constructor(documentId, fields) {
        this.pdfViewer = new PdfViewer('/user/documents/getfile/' + documentId, null);
        this.pdfViewer.setDataFields(fields);
        this.pdfViewer.scale = 1.5;
        this.pdfViewer.addEventListener('ready', e => this.startRender());
    }

    startRender() {
        this.pdfViewer.renderPage(1);
    }

}