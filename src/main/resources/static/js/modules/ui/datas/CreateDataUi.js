import {PdfViewer} from "../../utils/PdfViewer.js";
import {SseDispatcher} from "../../utils/SseDispatcher.js";
import {Message} from "../../../prototypes/Message.js";
import {WheelDetector} from "../../utils/WheelDetector.js";

export class CreateDataUi {

    constructor(id, action, data, fields, csrf) {
        console.info("Starting data UI for :" + data.id);
        console.log(fields);
        this.data = data;
        if(data) {
            this.pdfViewer = new PdfViewer('/user/datas/get-model/' + id, true, 0, null, false, fields, false);
        }
        this.action = action;
        this.actionEnable = 0;
        this.formId = id;
        this.csrf = csrf;
        if (this.pdfViewer.dataFields[0].defaultValue != null) {
            for (let i = 0 ; i < this.pdfViewer.dataFields.length ; i++) {
                this.pdfViewer.savedFields.set(this.pdfViewer.dataFields[i].name, this.pdfViewer.dataFields[i].defaultValue);
            }
        }
        this.nextCommand = "none";
        this.wheelDetector = new WheelDetector();
        this.sseDispatcher = new SseDispatcher();
        this.changeControl = false;
        this.initListeners();
    }

    initListeners() {
        if(this.pdfViewer) {
            this.pdfViewer.addEventListener('ready', e => this.startRender());
            this.pdfViewer.addEventListener('render', e => this.initChangeControl());
            this.pdfViewer.addEventListener('change', e => this.saveData());
            this.wheelDetector.addEventListener("zoomin", e => this.pdfViewer.zoomIn());
            this.wheelDetector.addEventListener("zoomout", e => this.pdfViewer.zoomOut());
            this.wheelDetector.addEventListener("pagetop", e => this.simulateSave("prev"));
            this.wheelDetector.addEventListener("pagebottom", e => this.simulateSave("next"));
            $('#prev').on('click', e => this.simulateSave("prev"));
            $('#next').on('click', e => this.simulateSave("next"));
        }
        if (document.getElementById('sendModalButton') != null) {
            document.getElementById('sendModalButton').addEventListener('click', e => this.openSendModal());
        }
        // $('#saveButton').on('click', e => this.submitForm());
        $('#newData').on('submit', function (e){
            e.preventDefault();
        });
        // let self = this;
        // $("input").each(function(){
        //     $(this).on("change", e => self.launchSave());
        // });
    }

    openSendModal() {
        this.pdfViewer.checkForm().then(function(result) {
            if(result === "ok") {
                $('#sendModal').modal('show');
            }
        });
    }

    // launchSave() {
    //     if(this.nextCommand === "none") {
    //         this.saveData();
    //     } else {
    //         this.pushData(false);
    //     }
    // }

    initChangeControl() {
        console.info("init change control")
        let inputs = $("#newData :input");
        $.each(inputs, (index, e) => this.listenForChange(e));
        if($("#sendModalButton").length && !this.changeControl) {
            this.changeControl = true;
            let saveButton = $('#saveButton');
            saveButton.addClass('disabled');
        }
        if(this.action) {
            this.initFormAction();
        }
    }

    listenForChange(input) {
        $(input).change(e => this.saveData());
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
        this.pdfViewer.page.getAnnotations().then(items => this.pdfViewer.saveValues(items)).then(e => this.pushData(false));
    }

    // submitForm() {
    //     this.nextCommand = "none";
    //     this.launchSave();
    // }

    simulateSave(command) {
        if((command === "next" && !this.pdfViewer.isLastpage()) || (command === "prev" && !this.pdfViewer.isFirstPage())) {
            this.nextCommand = command;
            this.pdfViewer.promizeSaveValues().then(e => this.afterSimulate(command));
        } else {
            this.nextCommand = "none";
        }
    }

    afterSimulate(command) {
        //$('#simulateDataSubmit').click();
        this.executeNextCommand();
        if(command === "prev") window.scrollTo(0, document.body.scrollHeight);
    }

    pushData(redirect) {
        let formData  = new Map();
        console.info("check data name");
        let pdfViewer = this.pdfViewer;

        pdfViewer.dataFields.forEach(function(dataField){
            let savedField = pdfViewer.savedFields.get(dataField.name)
            formData[dataField.name]= savedField;
            // if(dataField.required && (savedField === "" || savedField == null)) {
            //     alert("Un champ n'est pas rempli en page " + dataField.page);
            //     redirect = false;
            //     pdfViewer.renderPage(dataField.page);
            // }
        })
        let dispatcher = this.sseDispatcher;
        if(redirect || this.data.id != null) {
            let json = JSON.stringify(formData);
            let dataId = $('#dataId');
            $.ajax({
                data: {'formData': json},
                type: 'POST',
                url: '/user/datas/form/' + this.formId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&dataId=' + dataId.val(),
                success: function (response) {
                    let message = new Message();
                    message.type = "success";
                    message.text = "Modifications enregistrées";
                    message.object = null;
                    dispatcher.dispatchEvent("user", message);
                    dataId.val(response);
                    if(redirect) {
                       location.href = "/user/datas/" + response + "/update";
                    }
                }
            });
        }
        this.executeNextCommand();
    }

    executeNextCommand() {
        if(this.nextCommand === "next") {
            this.pdfViewer.nextPage();
        } else if(this.nextCommand === "prev") {
            this.pdfViewer.prevPage()
        }
        this.nextCommand = "none";
    }

    startRender() {
        this.pdfViewer.renderPage(1);
        this.pdfViewer.adjustZoom();
    }

}