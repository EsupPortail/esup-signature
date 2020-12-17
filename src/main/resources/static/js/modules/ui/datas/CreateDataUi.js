import {PdfViewer} from "../../utils/PdfViewer.js";

export class CreateDataUi {

    constructor(id, action, documentId, fields, csrf) {
        console.info("Starting data UI");
        console.log(fields);
        if(documentId) {
            this.pdfViewer = new PdfViewer('/user/datas/get-model/' + id, true, 0);
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
        this.newData = $('#newData');
        this.nextCommand = "none";
    }

    initListeners() {
        if(this.pdfViewer) {
            document.getElementById('prev').addEventListener('click', e => this.simulateSave("prev"));
            document.getElementById('next').addEventListener('click', e => this.simulateSave("next"));
            this.pdfViewer.addEventListener('ready', e => this.startRender());
            this.pdfViewer.addEventListener('render', e => this.initChangeControl());
            this.pdfViewer.addEventListener('change', e => this.enableSave());
        }
        document.getElementById('saveButton').addEventListener('click', e => this.submitForm());
        document.getElementById('newData').addEventListener('submit', e => this.launchSave(e));
    }

    launchSave(e) {
        e.preventDefault()
        if(this.nextCommand === "none") {
            this.saveData();
        } else {
            this.pushData(false);
        }
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

    saveData() {
        this.pdfViewer.page.getAnnotations().then(items => this.pdfViewer.saveValues(items)).then(e => this.pushData(true));
    }

    submitForm() {
        this.nextCommand = "none";
        $('#realDataSubmit').click();
    }

    simulateSave(command) {
        this.nextCommand = command;
        $('#simulateDataSubmit').click();
    }

    pushData(redirect) {
        let formData  = new Map();
        console.info("check data name");
        let pdfViewer = this.pdfViewer;
        pdfViewer.savedFields.forEach(function (value, key, map){
            formData[key]= value;
        })
        if(redirect) {
            let json = JSON.stringify(formData);
            let dataId = $('#dataId');
            $.ajax({
                data: {'formData': json},
                type: 'POST',
                url: '/user/datas/form/' + this.formId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&dataId=' + dataId.val(),
                success: function (response) {
                    dataId.val(response);
                    location.href = "/user/datas/" + response + "/update";
                }
            });
        } else {
            let command = this.nextCommand;
            if(command === "next") {
                pdfViewer.nextPage();
            } else if(command === "prev") {
                pdfViewer.prevPage()
            }
        }
    }

    startRender() {
        this.pdfViewer.renderPage(1);
        this.pdfViewer.adjustZoom();
    }

}