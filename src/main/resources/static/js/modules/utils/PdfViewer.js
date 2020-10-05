import {EventBus} from "../../customs/ui_utils.js";
import {EventFactory} from "./EventFactory.js";
import {DataField} from "../../prototypes/DataField.js";

export class PdfViewer extends EventFactory {

    constructor(url, signable, currentStepNumber) {
        super();
        console.info("Starting PDF Viewer, signable : " + signable);
        this.url= url;
        this.pdfPageView = null;
        this.currentStepNumber = currentStepNumber;
        this.scale = 0.6;
        this.zoomStep = 0.10;
        this.canvas = document.getElementById('pdf');
        this.pdfDoc = null;
        this.pageNum = 1;
        this.numPages = 1;
        this.page = null;
        this.dataFields = [];
        this.savedFields = new Map();
        this.signable = signable;
        this.events = {};
        this.rotation = 0;
        pdfjsLib.getDocument(this.url).promise.then(pdf => this.startRender(pdf));
        this.initListeners();
    }

    initListeners() {
        document.getElementById('prev').addEventListener('click', e => this.prevPage());
        document.getElementById('next').addEventListener('click', e => this.nextPage());
        document.getElementById('zoomin').addEventListener('click', e => this.zoomIn());
        document.getElementById('zoomout').addEventListener('click', e => this.zoomOut());
        document.getElementById('fullwidth').addEventListener('click', e => this.fullWidth());
        document.getElementById('fullheight').addEventListener('click', e => this.fullHeight());
        document.getElementById('rotateleft').addEventListener('click', e => this.rotateLeft());
        document.getElementById('rotateright').addEventListener('click', e => this.rotateRight());
        window.addEventListener('resize', e => this.adjustZoom());
        this.addEventListener("render", e => this.listenToSearchCompletion());
   }

    listenToSearchCompletion() {
        console.info("listen to search autocompletion");
        $(".search-completion").each(function () {
            let serviceName = $(this).attr("search-completion-service-name");
            let searchReturn = $(this).attr("search-completion-return");
            let url =
            $(this).autocomplete({
                source: function( request, response ) {
                    $.ajax({
                        url: "/user/user-ws/search-extvalue/?searchType=user&searchString=" + request.term + "&serviceName=" + serviceName + "&searchReturn=" + searchReturn,
                        dataType: "json",
                        data: {
                            q: request.term
                        },
                        success: function( data ) {
                            response($.map(data, function (item) {
                                console.log(item);
                                return {
                                    label: item.text,
                                    value: item.value
                                };
                            }));
                        }
                    });
                }
            });
        });
    }

    fullWidth() {
        console.info("full width " + window.innerWidth);
        let newScale = (Math.round(window.innerWidth / 100) / 10);
        if (newScale !== this.scale) {
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.renderPage(this.pageNum);
            this.fireEvent('scaleChange', ['in']);
        }
    }

    fullHeight() {
        console.info("full height " + window.innerHeight);
        let newScale = (Math.round((window.innerHeight - 200) / 100) / 10) - 0.1;
        if (newScale !== this.scale) {
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.renderPage(this.pageNum);
            this.fireEvent('scaleChange', ['in']);
        }
    }

    adjustZoom() {
        console.info("adjust zoom to screen wide " + window.innerWidth);
        let newScale = 1;
        if (window.innerWidth < 1200) {
            newScale = 0.9;
        }
        if (window.innerWidth < 992) {
            newScale = 0.8;
        }
        if (window.innerWidth < 768) {
            newScale = 0.7;
        }
        if (window.innerWidth < 576) {
            newScale = 0.5;
        }
        if (newScale !== this.scale) {
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.renderPage(this.pageNum);
            this.fireEvent('scaleChange', ['in']);
        }
    }

    startRender(pdf) {
        this.pdfDoc = pdf;
        this.numPages = this.pdfDoc.numPages;
        document.getElementById('page_count').textContent = this.pdfDoc.numPages;
        this.fireEvent("ready", ['ok']);
    }

    renderPage(num) {
        console.group("Start render");
        console.debug("render page " + num + ", scale : " + this.scale + ", signable : " + this.signable + " ,step : " + this.currentStepNumber);
        this.pageNum = num;
        if(this.pdfDoc != null) {
            if(this.page != null) {
                this.page.getAnnotations().then(e => this.promizeSaveValues()).then(e => this.initRender());
            } else {
                this.initRender()
            }
        }
    }

    initRender() {
        document.getElementById('page_num').textContent = this.pageNum;
        document.getElementById('zoom').textContent = Math.round(100 * this.scale);
        if(this.pdfDoc.numPages === 1) {
            $('#prev').prop('disabled', true);
            $('#next').prop('disabled', true);
        }
        this.pdfDoc.getPage(this.pageNum).then(page => this.renderTask(page));
    }

    renderTask(page) {
        console.info("launch render task");
        this.page = page;
        let scale = this.scale;
        let rotation = this.rotation;
        let viewport = page.getViewport({scale, rotation});
        if(this.pdfPageView == null) {
            let dispatchToDOM = false;
            let globalEventBus = new EventBus({ dispatchToDOM });
            this.pdfPageView = new pdfjsViewer.PDFPageView({
                eventBus: globalEventBus,
                container: this.canvas,
                id: this.pageNum,
                defaultViewport: viewport,
                annotationLayerFactory:
                    new pdfjsViewer.DefaultAnnotationLayerFactory(),
                renderInteractiveForms: true,
            });
        }
        this.pdfPageView.scale = this.scale;
        this.pdfPageView.rotation = this.rotation;
        this.pdfPageView.setPdfPage(page);
        this.pdfPageView.draw().then(e => this.postRender());
    }

    postRender() {
        this.promiseRenderForm(false).then(e => this.promiseRenderForm(true)).then(e => this.promizeRestoreValue());
        this.canvas.style.width = Math.round(this.pdfPageView.viewport.width) +"px";
        this.canvas.style.height = Math.round(this.pdfPageView.viewport.height) + "px";
        console.groupEnd();
    }

    promiseRenderForm(isField) {
        return new Promise((resolve, reject) => {
            if (this.page != null) {
                if (isField) {
                    if (this.dataFields != null) {
                        console.info("render fields");
                        this.page.getAnnotations().then(items => this.renderPdfFormWithFields(items));
                    }
                } else {
                    this.page.getAnnotations().then(items => this.renderPdfForm(items));
                }
                resolve("Réussite");
            }
        });
    }
    
    promizeToggleFields(enable) {
        if(this.page != null) {
            this.page.getAnnotations().then(items => this.toggleItems(items, enable));
        }
    }

    toggleItems(items, enable) {
        console.log("toggle fields " + items.length);
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldName != null) {
                let inputField = $('input[name=\'' + items[i].fieldName.split(/\$|#|!/)[0] + '\']');
                if (enable) {
                    inputField.prop("disabled", false);
                } else {
                    inputField.prop("disabled", true);
                }
            }
        }
    }

    promizeSaveValues() {
        this.page.getAnnotations().then(items => this.saveValues(items));
    }

    saveValues(items) {
        console.log("save fields " + items.length);
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldName != null) {
                let inputField = $('input[name=\'' + items[i].fieldName.split(/\$|#|!/)[0] + '\']');
                if (inputField.val() != null) {
                    if (inputField.is(':checkbox')) {
                        if (!inputField[0].checked) {
                            this.savedFields.set(items[i].fieldName, 'off');
                        } else {
                            this.savedFields.set(items[i].fieldName, 'on');
                        }
                        continue;
                    }
                    if (inputField.is(':radio')) {
                        let radio = $('input[name=\'' + items[i].fieldName.split(/\$|#|!/)[0] + '\'][value=\'' + items[i].buttonValue + '\']');
                        if (radio.prop("checked")) {
                            this.savedFields.set(items[i].fieldName, radio.val());
                        }
                        continue;
                    }
                    this.savedFields.set(items[i].fieldName, inputField.val());
                }
            }
        }
    }

    promizeRestoreValue() {
        if(this.savedFields.size > 0) {
            this.page.getAnnotations().then(items => this.restoreValues(items));
        }
        this.fireEvent('render', ['end']);
    }

    restoreValues(items) {
        console.log("set fields " + items.length);
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldName != null) {
                let inputField = $('input[name=\'' + items[i].fieldName.split(/\$|#|!/)[0] + '\']');
                if (inputField.val() != null) {
                    if (inputField.is(':checkbox')) {
                        if(this.savedFields.get(items[i].fieldName) === 'on') {
                            inputField.prop( "checked", true);
                        } else {
                            inputField.prop( "checked", false);
                        }
                        continue;
                    }
                    if (inputField.is(':radio')) {
                        let radio = $('input[name=\'' + items[i].fieldName.split(/\$|#|!/)[0] + '\'][value=\'' + items[i].buttonValue + '\']');
                        console.log("test "+ items[i].fieldName + " " + this.savedFields.get(items[i].fieldName) + " = " + inputField.val());
                        if (this.savedFields.get(items[i].fieldName) === radio.val()) {
                            radio.prop("checked", true);
                        }
                        continue;
                    }
                    inputField.val(this.savedFields.get(items[i].fieldName));
                }
            }
        }
    }

    renderPdfFormWithFields(items) {
        console.debug("rending pdfForm items with fields" + items);
        let signFieldNumber = 0;
        let visaFieldNumber = 0;
        for (let i = 0; i < items.length; i++) {
            console.debug(">>Start compute field");
            let dataField;
            if(this.dataFields != null && items[i].fieldName != null) {
                dataField = this.dataFields.filter(obj => {
                    return obj.name === items[i].fieldName.split(/\$|#|!/)[0]
                })[0];
            }
            if(items[i].fieldType === undefined && items[i].title.toLowerCase().startsWith('sign')) {
                console.debug("found sign field");
                signFieldNumber = signFieldNumber + 1;
                $('.popupWrapper').remove();
                let signField = $('section[data-annotation-id=' + items[i].id + '] > div');
                signField.append('Champ signature ' + signFieldNumber + '<br>');
                //signField.append('Vous pourrez signer le document après avoir lancé le processus de signature');
                signField.addClass("sign-field");
                signField.addClass("d-none");
                signField.parent().remove();
            }
            // if(items[i].fieldType === undefined && items[i].title.toLowerCase().startsWith('visa')) {
            //     console.debug("found sign field");
            //     visaFieldNumber = visaFieldNumber + 1;
            //     $('.popupWrapper').remove();
            //     let signField = $('section[data-annotation-id=' + items[i].id + '] > div');
            //     signField.append('Champ visa ' + visaFieldNumber + '<br>');
            //     //signField.append('Vous pourrez signer le document après avoir lancé le processus de signature');
            //     signField.addClass("sign-field");
            //     signField.addClass("d-none");
            //     signField.parent().remove();
            // }

            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            if(inputField.length && dataField != null) {
                console.debug(items[i]);
                console.debug(inputField);
                console.debug(dataField);
                inputField.attr('name', items[i].fieldName.split(/\$|#|!/)[0]);
                inputField.attr('id', items[i].fieldName.split(/\$|#|!/)[0]);
                if(!dataField.stepNumbers.includes("" + this.currentStepNumber) || !this.signable) {
                    inputField.val(items[i].fieldValue);
                    if(dataField.defaultValue != null) {
                        inputField.val(dataField.defaultValue);
                    }
                    inputField.prop('disabled', true);
                    inputField.prop('required', false);
                    inputField.addClass('disabled-field disable-selection');
                    inputField.parent().addClass('disable-div-selection');
                } else {
                    // if(dataField.stepNumbers.includes("" + this.currentStepNumber)) {
                        inputField.val(items[i].fieldValue);
                        if(dataField.defaultValue != null) {
                            inputField.val(dataField.defaultValue);
                        }
                    // }
                    inputField.prop('disabled', false);
                    if (dataField.required) {
                        inputField.prop('required', true);
                        inputField.addClass('required-field');
                    }
                }
                if(dataField.searchServiceName) {
                    $(inputField).addClass("search-completion");
                    $(inputField).attr("search-completion-service-name", dataField.searchServiceName);
                    $(inputField).attr("search-completion-return", dataField.searchReturn);
                    $(inputField).attr("search-completion-type", dataField.searchType);
                }

                if (dataField.type === "number") {
                    inputField.get(0).type = "number";
                }
                if (dataField.type === "radio") {
                    inputField.val(items[i].buttonValue);
                    if (dataField.defaultValue === items[i].buttonValue) {
                        inputField.prop("checked", true);
                    }
                }
                if (dataField.type === 'checkbox') {
                    inputField.val('on');
                    if (dataField.defaultValue === 'on') {
                        inputField.attr("checked", "checked");
                        inputField.prop("checked", true);
                    }
                }
                if (dataField.type === "date") {
                    inputField.datetimepicker({
                        format: 'DD/MM/YYYY',
                        locale: 'fr',
                        icons: {
                            time: 'fa fa-time',
                            date: 'fa fa-calendar',
                            up: 'fa fa-chevron-up',
                            down: 'fa fa-chevron-down',
                            previous: 'fa fa-chevron-left',
                            next: 'fa fa-chevron-right',
                            today: 'fa fa-screenshot',
                            clear: 'fas fa-trash-alt',
                            close: 'fa fa-check'
                        },
                        toolbarPlacement: 'bottom',
                        showClear: true,
                        showClose: true,
                        keepOpen: false,
                        widgetPositioning: {
                            horizontal: 'right',
                            vertical: 'top'
                        },
                    });
                }
                if (dataField.type === "time") {
                    inputField.datetimepicker({
                        format: 'LT',
                        locale: 'fr',
                        stepping: 5,
                        icons: {
                            time: 'fa fa-time',
                            date: 'fa fa-calendar',
                            up: 'fa fa-chevron-up',
                            down: 'fa fa-chevron-down',
                            previous: 'fa fa-chevron-left',
                            next: 'fa fa-chevron-right',
                            today: 'fa fa-screenshot',
                            clear: 'fas fa-trash-alt',
                            close: 'fa fa-check'
                        },
                        toolbarPlacement: 'bottom',
                        showClear: true,
                        showClose: true,
                        keepOpen: false,
                        widgetPositioning: {
                            horizontal: 'right',
                            vertical: 'top'
                        },
                    });
                }
            } else {
                let inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
                if(inputField.length > 0) {
                    if(!dataField.stepNumbers.includes("" + this.currentStepNumber) || !this.signable) {
                        inputField.prop('disabled', true);
                        inputField.prop('required', false);
                        inputField.addClass('disabled-field disable-selection');
                        inputField.parent().addClass('disable-div-selection');
                    } else {
                        inputField.val(dataField.defaultValue);
                        inputField.attr('name', items[i].fieldName.split(/\$|#|!/)[0]);
                        inputField.prop('disabled', false);
                        if (dataField.required) {
                            inputField.prop('required', true);
                            inputField.addClass('required-field');
                        }
                    }

                }
            }
        }
        console.debug(">>End compute field");
    }

    renderPdfForm(items) {
        console.debug("rending pdfForm items");
        let signFieldNumber = 0;
        for (let i = 0; i < items.length; i++) {
            console.debug(">>Start compute item");
            if (items[i].fieldType === undefined) {
                console.debug("sign field found");
                signFieldNumber = signFieldNumber + 1;
                $('.popupWrapper').remove();
                let signField = $('section[data-annotation-id=' + items[i].id + '] > div');
                signField.append('Champ signature ' + signFieldNumber + '<br>');
                //signField.append('Vous pourrez signer le document après avoir lancé le processus de signature');
                signField.addClass("sign-field");
                signField.addClass("d-none");
                signField.parent().remove();
            }
            console.debug(items[i]);
            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            console.debug(inputField);
            if (inputField.length) {
                inputField.attr('name', items[i].fieldName.split(/\$|#|!/)[0]);
                if (inputField.is(':radio')) {
                    inputField.val(items[i].buttonValue);
                }
            } else {
                inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
                if (inputField.length > 0) {
                    inputField.attr('name', items[i].fieldName.split(/\$|#|!/)[0]);
                    inputField.val(items[i].fieldValue);
                }
            }
        }
    }

    prevPage() {
        if (this.pageNum <= 1) {
            return;
        }
        this.pageNum--;
        this.renderPage(this.pageNum);
        window.scrollTo(0, 0);
        this.fireEvent('pageChange', ['prev']);

    }

    nextPage() {
        if (this.pageNum >= this.numPages) {
            return;
        }
        this.pageNum++;
        this.renderPage(this.pageNum);
        window.scrollTo(0, 0);
        this.fireEvent('pageChange', ['next']);
    }

    zoomIn() {
        if (this.scale >= 2) {
            return;
        }
        this.scale = this.scale + this.zoomStep;
        console.info('zoom in, scale = ' + this.scale);
        this.renderPage(this.pageNum);
        this.fireEvent('scaleChange', ['in']);
    }


    zoomOut() {
        if (this.scale <= 0.50) {
            return;
        }
        this.scale = this.scale - this.zoomStep;
        console.info('zoom out, scale = ' + this.scale);
        this.renderPage(this.pageNum);
        this.fireEvent('scaleChange', ['out']);
    }


    rotateLeft() {
        console.info('rotate left');
        if (this.rotation < -90) {
            return;
        }
        this.rotation = this.rotation - 90;
        this.renderPage(this.pageNum);
        this.fireEvent('rotate', ['left']);
    }

    rotateRight() {
        console.info('rotate right' + this.rotation);
        if (this.rotation > 90) {
            return;
        }
        this.rotation = this.rotation + 90;
        this.renderPage(this.pageNum);
        this.fireEvent('rotate', ['right']);
    }


    setDataFields(dataFields) {
        dataFields.forEach(e => this.addDataField(e));
    }

    addDataField(dataField) {
        this.dataFields.push(new DataField(dataField));
    }


    printPdf() {
        this.pdfPageView.eventBus.dispatch('print', {
            source: self
        });
    }
}