import {PdfViewer} from "../../utils/PdfViewer.js";

export class CreateDataUi {

    constructor(id, action, documentId, fields, csrf) {
        console.info("Starting data UI");
        if(documentId) {
            this.pdfViewer = new PdfViewer('/user/documents/getfile/' + documentId, true, 0);
            this.pdfViewer.setDataFields(fields);
            this.pdfViewer.scale = 0.70;
        }
        this.action = action;
        this.actionEnable = 0;
        this.initListeners();
        this.formId = id;
        this.csrf = csrf;
        if (this.pdfViewer.dataFields[0].defaultValue != null) {
            for (let i = 0 ; i < this.pdfViewer.dataFields.length ; i++) {
                this.pdfViewer.savedFields.set(this.pdfViewer.dataFields[i].name, this.pdfViewer.dataFields[i].defaultValue);
            }
        }
    }


    initListeners() {
        if(this.pdfViewer) {
            this.pdfViewer.addEventListener('ready', e => this.startRender());
            this.pdfViewer.addEventListener('render', e => this.initChangeControl());
            this.pdfViewer.addEventListener('change', e => this.enableSave());
        }
        document.getElementById('saveButton').addEventListener('click', e => this.saveData(e));
        document.getElementById('saveForm').addEventListener('submit', e => this.saveData(e));
    }

    initChangeControl() {
        console.info("init change control")
        let inputs = $("#newData :input");
        $.each(inputs, (index, e) => this.listenForChange(e));
        if($("#sendModalButton").length) {
            let saveButton = $('#saveButton');
            saveButton.addClass('disabled');
        }
        if(this.action) {
            this.initFormAction();
        }
    }

    listenForChange(input) {
        $(input).change(e => this.enableSave());
    }

    enableSave() {
        let sendModalButton = $('#sendModalButton');
        sendModalButton.addClass('disabled');
        sendModalButton.removeAttr('data-target');
        let saveButton = $('#saveButton');
        saveButton.removeClass('disabled');
    }

    initFormAction() {
        if(this.actionEnable === 1) {
            console.info("eval : " + this.action);
            jQuery.globalEval(this.action);
            this.actionEnable = true
        }
        this.actionEnable++;
    }

    async saveData(e) {
        e.preventDefault();
        await this.pdfViewer.page.getAnnotations().then(items => this.pdfViewer.saveValues(items));
        Promise.resolve(this.pdfViewer.page.getAnnotations());
        var formData  = new Map();
        console.info("check data name");
        let tempName = document.getElementById('tempName');
        if (tempName.checkValidity()) {
            this.pdfViewer.savedFields.forEach(function (value, key, map){
                formData[key]= value;
            })
            var json = JSON.stringify(formData);
            $.ajax({
                data: {'formData': json},
                type: 'POST',
                url: '/user/datas/form/' + this.formId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&dataId=' + $('#dataId').val(),
                success: function (response){
                    window.location.href = response;
                }
            });
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