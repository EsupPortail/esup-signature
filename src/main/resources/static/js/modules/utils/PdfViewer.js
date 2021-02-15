import {EventBus} from "../../customs/ui_utils.js";
import {EventFactory} from "./EventFactory.js";
import {DataField} from "../../prototypes/DataField.js";

export class PdfViewer extends EventFactory {

    constructor(url, signable, currentStepNumber, forcePageNum, fields, disableAllFields) {
        super();
        console.info("Starting PDF Viewer, signable : " + signable);
        this.url= url;
        this.pdfPageView = null;
        this.currentStepNumber = currentStepNumber;
        this.scale = 1;
        if(localStorage.getItem('scale')) {
            this.scale = parseFloat(localStorage.getItem('scale'));
        }
        this.zoomStep = 0.1;
        this.canvas = document.getElementById('pdf');
        this.pdfDoc = null;
        this.pageNum = 1;
        if(forcePageNum != null) {
            this.pageNum = forcePageNum;
        }
        this.numPages = 1;
        this.page = null;
        let jsFields = [];
        if(fields) {
            fields.forEach(function (e){
                jsFields.push(new DataField(e));
            });
        }
        this.dataFields = jsFields;
        this.savedFields = new Map();
        this.signable = signable;
        this.events = {};
        this.rotation = 0;
        this.disableAllFields = disableAllFields;
        this.pdfJs = pdfjsLib.getDocument(this.url).promise.then(pdf => this.startRender(pdf));
        this.initListeners();
    }

    initListeners() {
        $('#zoomin').on('click', e => this.zoomIn());
        $('#zoomout').on('click', e => this.zoomOut());
        $('#fullwidth').on('click', e => this.fullWidth());
        $('#fullheight').on('click', e => this.fullHeight());
        $('#rotateleft').on('click', e => this.rotateLeft());
        $('#rotateright').on('click', e => this.rotateRight());
        $(window).on('resize', e => this.adjustZoom());
        this.addEventListener("render", e => this.listenToSearchCompletion());
   }

    listenToSearchCompletion() {
        console.info("listen to search autocompletion");
        $(".search-completion").each(function () {
            let serviceName = $(this).attr("search-completion-service-name");
            let searchType = $(this).attr("search-completion-type");
            let searchReturn = $(this).attr("search-completion-return");
            $(this).autocomplete({
                source: function( request, response ) {
                    if(request.term.length > 2) {
                        $.ajax({
                            url: "/user/user-ws/search-extvalue/?searchType=" + searchType + "&searchString=" + request.term + "&serviceName=" + serviceName + "&searchReturn=" + searchReturn,
                            dataType: "json",
                            data: {
                                q: request.term
                            },
                            success: function (data) {
                                console.debug("search user " + request.term);
                                response($.map(data, function (item) {
                                    return {
                                        label: item.text,
                                        value: item.value
                                    };
                                }));
                            }
                        });
                    }
                }
            });
        });
    }

    annotationLinkTargetBlank() {
        $('.linkAnnotation').each(function (){
            $(this).children().attr('target', '_blank');
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

    isFloat(n){
        return Number(n) === n && n % 1 !== 0;
    }

    adjustZoom() {
        console.info("adjust zoom to screen wide " + window.innerWidth);
        let newScale = 1;
        if(localStorage.getItem('scale')) {
            newScale = parseFloat(localStorage.getItem('scale'));
        }
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
        console.info("launch render task" + this.scale);
        this.page = page;
        let scale = this.scale;
        localStorage.setItem('scale', scale.toPrecision(2) + "");
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
                renderInteractiveForms: true
            });
        }
        this.pdfPageView.scale = this.scale;
        this.pdfPageView.rotation = this.rotation;
        this.pdfPageView.setPdfPage(page);
        this.pdfPageView.draw().then(e => this.postRender());
    }

    postRender() {
        let self = this;
        this.promiseRenderForm(false).then(e => this.promiseRenderForm(true)).then(e => this.promizeRestoreValue()).then(function(){
            self.fireEvent("renderFinished", ['ok']);
        });
        this.canvas.style.width = Math.round(this.pdfPageView.viewport.width) +"px";
        this.canvas.style.height = Math.round(this.pdfPageView.viewport.height) + "px";
        console.groupEnd();
    }

    promiseRenderForm(isField) {
        return new Promise((resolve, reject) => {
            if (this.page != null) {
                if (isField) {
                    if (this.dataFields != null && !this.disableAllFields) {
                        console.info("render fields");
                        this.page.getAnnotations().then(items => this.renderPdfFormWithFields(items)).then(this.annotationLinkTargetBlank());
                    }
                } else {
                    this.page.getAnnotations().then(items => this.renderPdfForm(items)).then(this.annotationLinkTargetBlank());
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
        console.info("toggle fields " + items.length);
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

    initSavedValues() {

    }

    promizeSaveValues() {
        console.info("launch save values");
        return this.page.getAnnotations().then(items => this.saveValues(items));
    }

    saveValues(items) {
        console.log("save fields " + items.length);
        if(this.dataFields.length > 0) {
            for (let i = 0; i < this.dataFields.length; i++) {
                let dataField = this.dataFields[i];
                let item = items.filter(function (e) {
                    return e.fieldName != null && e.fieldName === dataField.name
                })[0];
                if (item != null && item.fieldName != null) {
                    this.saveValue(item);
                } else {
                    if(this.savedFields.get(dataField.name) == null) {
                        this.savedFields.set(dataField.name, dataField.defaultValue);
                    }
                }
            }
        } else {
            for (let i = 0; i < items.length; i++) {
                this.saveValue(items[i]);
            }
        }
    }

    saveValue(item) {
        if(item != null && item.fieldName != null) {
            let inputName = item.fieldName.split(/\$|#|!/)[0];
            let inputField = $('#' + inputName);
            if (inputField.length > 0) {
                if (inputField.val() != null) {
                    if (inputField.is(':checkbox')) {
                        if (!inputField[0].checked) {
                            this.savedFields.set(item.fieldName, 'off');
                        } else {
                            this.savedFields.set(item.fieldName, 'on');
                        }
                        return;
                    }
                    if (inputField.is(':radio')) {
                        let radio = $('input[name=\'' + inputName + '\'][value=\'' + item.buttonValue + '\']');
                        if (radio.prop("checked")) {
                            this.savedFields.set(item.fieldName, radio.val());
                        }
                        return;
                    }
                    let value = inputField.val();
                    this.savedFields.set(item.fieldName, value);
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
                let inputName = items[i].fieldName.split(/\$|#|!/)[0];
                let savedValue = this.savedFields.get(items[i].fieldName);
                let inputField = $('input[name=\'' + inputName + '\']');
                if (inputField.val() != null) {
                    if(savedValue != null) {
                        if (inputField.is(':checkbox')) {
                            if (savedValue === 'on') {
                                inputField.prop("checked", true);
                            } else {
                                inputField.prop("checked", false);
                            }
                            continue;
                        }
                        if (inputField.is(':radio')) {
                            let radio = $('input[name=\'' + inputName + '\'][value=\'' + items[i].buttonValue + '\']');
                            console.log("test " + items[i].fieldName + " " + savedValue + " = " + inputField.val());
                            if (savedValue === radio.val()) {
                                radio.prop("checked", true);
                            }
                            continue;
                        }
                        inputField.val(savedValue);
                        continue;
                    }
                }
                let textareaField = $('textarea[name=\'' + inputName + '\']');
                if (textareaField.val() != null) {
                    if (savedValue != null) {
                        textareaField.val(savedValue);
                        continue;
                    }
                }
                let selectField = $('select[name=\'' + inputName + '\']');
                if (selectField.val() != null) {
                    $('#' + inputName + ' option').each(function()
                    {
                        if(this.savedFields.get(items[i].fieldName) === $(this).value) {
                            $(this).prop("selected", true);
                        }
                    });
                }
            }
        }
    }

    renderPdfFormWithFields(items) {
        let datePicherIndex = 1040;
        console.debug("rending pdfForm items with fields" + items);
        let signFieldNumber = 0;
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldType === undefined) {
                if(items[i].title && items[i].title.toLowerCase().includes('sign')) {
                    signFieldNumber = signFieldNumber + 1;
                    $('.popupWrapper').remove();
                    let signField = $('section[data-annotation-id=' + items[i].id + '] > div');
                    signField.append('Champ signature ' + signFieldNumber + '<br>');
                    signField.addClass("sign-field");
                    // signField.addClass("d-none");
                    // signField.parent().remove();
                }
                continue;
            }
            let inputName = items[i].fieldName.split(/\$|#|!/)[0];
            let dataField;
            if(this.dataFields != null && items[i].fieldName != null) {
                dataField = this.dataFields.filter(obj => {
                    return obj.name === inputName
                })[0];
            }


            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            if(inputField.length && dataField != null) {
                inputField.attr('name', inputName);
                inputField.attr('id', inputName);
                if(dataField.favorisable && !$("#div_" + inputField.attr('id')).length) {
                    let sendField = inputField;
                    $.ajax({
                        type: "GET",
                        url: '/user/datas/get-favorites/' + dataField.id,
                        success : response => this.autocomplete(response, sendField)
                    });
                }
                if(items[i].readOnly || dataField.readOnly) {
                    inputField.addClass('disabled-field disable-selection');
                    inputField.prop('disabled', true);
                }
                console.debug(dataField);
                if(!dataField.stepNumbers.includes("" + this.currentStepNumber) || !this.signable) {
                    inputField.val(items[i].fieldValue);
                    inputField.prop('required', false);
                    inputField.prop('disabled', true);
                    inputField.addClass('disabled-field disable-selection');
                    inputField.parent().addClass('disable-div-selection');
                } else {
                    inputField.prop('disabled', false);
                    inputField.removeClass('disabled-field disable-selection');
                    inputField.val(items[i].fieldValue);
                    if(dataField.defaultValue != null) {
                        inputField.val(dataField.defaultValue);
                    }
                    if (dataField.required) {
                        inputField.prop('required', true);
                        inputField.addClass('required-field');
                    }
                }
                if(dataField.searchServiceName) {
                    inputField.addClass("search-completion");
                    inputField.attr("search-completion-service-name", dataField.searchServiceName);
                    inputField.attr("search-completion-return", dataField.searchReturn);
                    inputField.attr("search-completion-type", dataField.searchType);
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
                    $('section[data-annotation-id=' + items[i].id + ']').css("z-index", datePicherIndex);
                    datePicherIndex--;
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
                            vertical: 'bottom'
                        },
                    });
                    inputField.off('dp.change');
                    inputField.on('dp.change', e => this.fireEvent('change', ['date']));
                }
                if (dataField.type === "time") {
                    $('section[data-annotation-id=' + items[i].id + ']').css("z-index", datePicherIndex);
                    datePicherIndex--;
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
                            vertical: 'bottom'
                        },
                    });
                    inputField.off('dp.change');
                    inputField.on('dp.change', e => this.fireEvent('change', ['time']));
                }
            }

            inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
            if(inputField.length && dataField != null) {
                let sendField = inputField;
                if(dataField.favorisable) {
                    $.ajax({
                        type: "GET",
                        url: '/user/datas/get-favorites/' + dataField.id,
                        success : response => this.autocomplete(response, sendField)
                    });
                }
                inputField.attr('name', inputName);
                inputField.attr('id', inputName);
                if(items[i].readOnly || dataField.readOnly) {
                    inputField.addClass('disabled-field disable-selection');
                }
                if(!dataField.stepNumbers.includes("" + this.currentStepNumber) || !this.signable) {
                    inputField.prop('required', false);
                    inputField.addClass('disabled-field disable-selection');
                    inputField.parent().addClass('disable-div-selection');
                } else {
                    inputField.val(dataField.defaultValue);
                    if (dataField.required) {
                        inputField.prop('required', true);
                        inputField.addClass('required-field');
                    }
                }
            }
            inputField = $('section[data-annotation-id=' + items[i].id + '] > select');
            if(inputField.length) {
                inputField.attr('name', inputName);
                inputField.attr('id', inputName);
                if(items[i].readOnly || dataField.readOnly) {
                    inputField.addClass('disabled-field disable-selection');
                    // inputField.prop('disabled', true);
                }
                if(!dataField.stepNumbers.includes("" + this.currentStepNumber) || !this.signable) {
                    inputField.prop('required', false);
                    inputField.addClass('disabled-field disable-selection');
                    inputField.prop('disabled', true);
                    inputField.parent().addClass('disable-div-selection');
                } else {
                    inputField.val(dataField.defaultValue);
                    if (dataField.required) {
                        inputField.prop('required', true);
                        inputField.addClass('required-field');
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
            if(items[i].fieldType === undefined) {
                console.log(items[i]);
                // if(items[i].title && items[i].title.toLowerCase().includes('sign')) {
                //     signFieldNumber = signFieldNumber + 1;
                //     $('.popupWrapper').remove();
                //     let signField = $('section[data-annotation-id=' + items[i].id + '] > div');
                //     signField.append('Champ signature ' + signFieldNumber + '<br>');
                //     signField.addClass("sign-field");
                //     // signField.addClass("d-none");
                //     // signField.parent().remove();
                // }
                continue;
            }
            let inputName = items[i].fieldName.split(/\$|#|!/)[0];
            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            console.debug(inputField);
            if (inputField.length) {
                inputField.attr('name', inputName);
                inputField.attr('id', inputName);
                if (inputField.is(':radio')) {
                    inputField.val(items[i].buttonValue);
                }
            } else {
                inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
                if (inputField.length > 0) {
                    inputField.attr('name', inputName);
                    inputField.attr('id', inputName);
                    inputField.val(items[i].fieldValue);
                }
            }
        }
    }

    prevPage() {
        this.fireEvent('beforeChange', ['prev']);
        if (this.isFirstPage()) {
            return false;
        }
        this.pageNum--;
        this.renderPage(this.pageNum);
        window.scrollTo(0, 0);
        this.fireEvent('pageChange', ['prev']);
        return true;
    }

    nextPage() {
        this.fireEvent('beforeChange', ['next']);
        if (this.isLastpage()) {
            return false;
        }
        this.pageNum++;
        this.renderPage(this.pageNum);
        window.scrollTo(0, 0);
        this.fireEvent('pageChange', ['next']);
        return true;
    }

    isFirstPage() {
        return this.pageNum <= 1;
    }

    isLastpage() {
        return this.pageNum >= this.numPages;
    }

    zoomIn() {
        if (this.scale >= 2) {
            return;
        }
        this.scale = Math.round((this.scale + this.zoomStep) * 10) / 10;
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

    printPdf() {
        this.pdfPageView.eventBus.dispatch('print', {
            source: self
        });
    }

    autocomplete(response, inputField) {
        let id = inputField.attr('id');
        let div = "<div class='custom-autocompletion' id='div_" + id +"'></div>";
        $(div).insertAfter(inputField);
        inputField.autocomplete({
            source: response,
            appendTo: "#div_" + id,
            minLength:0
        }).bind('focus', function(){ $(this).autocomplete("search"); } );
    }

    checkForm() {
        let p = new Promise((resolve, reject) => {
            let formData = new Map();
            console.info("check data name");
            let self = this;
            let resolveOk = true;
            $(self.dataFields).each(function() {
                let savedField = self.savedFields.get($(this)[0].name)
                formData[$(this)[0].name] = savedField;
                if ($(this)[0].required && !savedField && !$("#" + $(this)[0].name).val() && $(this)[0].stepNumbers.includes(self.currentStepNumber)) {
                    let page =  $(this)[0].page;
                    bootbox.alert("Un champ n'est pas rempli en page " + page, function () {
                        self.renderPage(page);
                    });
                    resolveOk = false;
                    $('#sendModal').modal('hide');
                    return false;
                }
            })
            if(resolveOk) {
                resolve("ok");
            } else {
                resolve("error");
            }
        });
        return p;
    }

}