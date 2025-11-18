import {EventBus} from "../../customs/ui_utils.js?version=@version@";
import {EventFactory} from "./EventFactory.js?version=@version@";
import {DataField} from "../../prototypes/DataField.js?version=@version@";

export class PdfViewer extends EventFactory {

    constructor(url, signable, editable, currentStepNumber, forcePageNum, fields, disableAllFields) {
        super();
        console.info("Starting PDF Viewer, signable : " + signable);
        this.timer = null;
        this.viewed = false;
        this.url= url;
        this.interval = null;
        this.initialOffset = 0;
        this.pages = [];
        this.signable = signable;
        this.editable = editable;
        this.currentStepNumber = currentStepNumber;
        this.saveScrolling = 0;
        this.pageNum = 1;
        if(forcePageNum != null) {
            this.pageNum = forcePageNum;
        }
        let jsFields = [];
        if(fields) {
            fields.forEach(function (e){
                jsFields.push(new DataField(e));
            });
        }
        this.disableAllFields = disableAllFields;
        this.scale = 1;
        if(localStorage.getItem('scale')) {
            this.scale = parseFloat(localStorage.getItem('scale'));
        }
        this.zoomStep = 0.1;
        this.pdfDiv = $("#pdf");
        this.pdfDoc = null;
        this.numPages = 1;
        this.page = null;
        this.dataFields = jsFields;
        this.savedFields = new Map();
        this.events = {};
        this.rotation = 0;
        this.renderedPages = 0
        this.initListeners();
        let self = this;
        $(document).ready(function() {
            if (!globalThis.pdfjsLib || !Promise.withResolvers) {
                bootbox.alert("Votre navigateur ne support pas pdfJs pour l’affichage des PDF.<br>Version minimales : Firefox 121, Chrome 119, Safari 17.4", function () {
                    document.location = "https://www.mozilla.org/fr/firefox/new/"
                });
            } else {
                globalThis.pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
                    '/webjars/pdfjs-dist/4.6.82/legacy/build/pdf.worker.min.mjs',
                    import.meta.url
                ).toString();
                let loadingTask = globalThis.pdfjsLib.getDocument(self.url);
                loadingTask.promise.then(function(pdf) {
                    self.startRender(pdf)
                });
            }
        });
    }

    initListeners() {
        $('#zoomin').on('click', e => this.zoomIn());
        $('#zoomout').on('click', e => this.zoomOut());
        $('#fullwidth').on('click', e => this.fullWidth());
        $('#fullheight').on('click', e => this.fullHeight());
        $('#rotateleft').on('click', e => this.rotateLeft());
        $('#rotateright').on('click', e => this.rotateRight());
        // $(window).on('resize', e => this.adjustZoom(e));
        $(window).on('resize', e => {
            if (window.__isResizingCross) return;
            this.adjustZoom();
        });
        // this.addEventListener("renderFinished", e => this.listenToSearchCompletion());
        // this.addEventListener("ready", e => this.restoreScrolling());
        $('#page_num').on('change', e => this.scrollToPage(e.target.value));
    }

    restoreScrolling() {
        window.scrollTo({
            top: this.saveScrolling * this.scale,
            left: 0,
            behavior: 'instant',
        });
    }

    listenToSearchCompletion() {
        let controller = new AbortController();
        let signal = controller.signal;
        console.info("listen to search autocompletion");
        $(".search-completion").each(function () {
            let serviceName = $(this).attr("search-completion-service-name");
            let searchType = $(this).attr("search-completion-type");
            let searchReturn = $(this).attr("search-completion-return");
            $(this).autocomplete({
                delay: 500,
                source: function( request, response ) {
                    if(request.term.length > 2) {
                        controller.abort();
                        controller = new AbortController()
                        signal = controller.signal;
                        $.ajax({
                            url: "/ws-secure/users/search-extvalue?searchType=" + searchType + "&searchString=" + request.term + "&serviceName=" + serviceName + "&searchReturn=" + searchReturn,
                            dataType: "json",
                            signal: signal,
                            data: {
                                q: request.term
                            },
                            success: function (data) {
                                console.debug("debug - " + "search user " + request.term);
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
            $(this).droppable({
                tolerance: "touch",
                drop: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).addClass("cross-warning");
                    }
                },
                over: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).addClass("cross-warning");
                    }
                },
                out: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).removeClass("cross-warning");
                    }
                }
            });
        });
    }

    annotationLinkRemove() {
        $('.linkAnnotation').each(function (){
            $(this).css("opacity", 0);
            $(this).click(function(e) {
                e.preventDefault();
            });
        });
    }

    checkCurrentPage(e) {
        if(this.renderedPages < this.numPages) return;
        let numPages = this.pdfDoc.numPages;
        for(let i = 1; i < numPages + 1; i++) {
            if(e > $("#page_" + i).offset().top - 250) {
                this.pageNum = i;
                document.getElementById('page_num').value = this.pageNum;
                if((this.pageNum === this.numPages || this.numPages === 1) && !this.viewed) {
                    this.viewed = true;
                    this.fireEvent('reachEnd', ['ok'])
                }
            }
        }
    }

    adjustZoom() {
        let newScale = 1;
        if (localStorage.getItem('scale')) {
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
            console.info("adjust zoom to screen wide " + window.innerWidth);
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.fireEvent('scaleChange', ['in']);
        }
    }

    startRender(pdf) {
        // this.pdfDiv.css('opacity', 0);
        this.saveScrolling = window.scrollY / this.scale;
        $(".pdf-page").each(function(e) {
            $(this).remove();
        });
        if(this.pdfDoc == null) {
            this.pdfDoc = pdf;
        }
        this.numPages = this.pdfDoc.numPages;
        document.getElementById('page_count').textContent = this.pdfDoc.numPages;
        this.renderedPages = 0;
        this.pages = [];
        this.disableScrollBtn();
        this.resetProgress();
        $("#pdf-progress-bar").css("opacity", 1);
        this.startProgress();
        this.render();
        this.refreshTools();
        this.fireEvent("ready", ['ok']);
    }

    render() {
        this.renderedPages++;
        let self = this;
        this.pdfDoc.getPage(this.renderedPages).then(page => this.renderTask(page, this.renderedPages).then(function (){
            if(self.renderedPages < self.numPages) {
                self.render();
            } else {
                self.initialOffset = parseInt($("#page_1").offset().top);
                self.fireEvent('renderFinished', ['ok']);
                $(document).trigger("renderFinished");
                if(self.pages.length === self.numPages) {
                    self.stopProgress();
                    self.postRenderAll();
                    $("#pdf-progress-bar").css("opacity", 0);
                    self.enableScrollBtn();
                }
            }
        }));
    }

    scrollToPage(num) {
        let self = this;
        let page = $("#page_" + num);
        self.disableScrollBtn();
        if(page.length) {
            let scrollTo = page.offset().top - self.initialOffset;
            $([document.documentElement, document.body]).animate({
                scrollTop: scrollTo
            }, 500, function (){
                self.enableScrollBtn();
            });
        }
    }

    enableScrollBtn() {
        $('#prev').prop('disabled', false);
        $('#next').prop('disabled', false);
        $('#page_num').prop('disabled', false);
    }

    disableScrollBtn() {
        $('#prev').prop('disabled', true);
        $('#next').prop('disabled', true);
        $('#page_num').prop('disabled', true);
    }

    refreshTools() {
        document.getElementById('page_num').value = this.pageNum;
        document.getElementById('zoom').textContent = Math.round(100 * this.scale);
        if(this.pdfDoc.numPages === 1) {
            if((this.pageNum === this.numPages || this.numPages === 1) && !this.viewed) {
                this.viewed = true;
                this.fireEvent('reachEnd', ['ok'])
            }
            this.disableScrollBtn();
        }
        // this.pdfDoc.getPage(this.pageNum).then(page => this.renderTask(page));
    }

    renderTask(page, i) {
        return new Promise((resolve, reject) => {
            console.info("launch render task scaled to : " + this.scale);
            let container = document.createElement("div");
            $(container).attr("id", "page_" + i);
            $(container).attr("page-num", i);
            $(container).addClass("drop-shadows");
            $(container).addClass("pdf-page");
            this.pdfDiv.append(container);
            $(container).droppable({
                drop: function (event, ui) {
                    ui.helper.attr("page", i)
                }
            });
            this.page = page;
            let self = this;
            localStorage.setItem('scale', this.scale.toPrecision(2) + "");
            let viewport = page.getViewport({scale: this.scale, rotation: this.rotation});
            let dispatchToDOM = false;
            let globalEventBus = new EventBus({dispatchToDOM});
            let pdfPageView = new pdfjsViewer.PDFPageView({
                eventBus: globalEventBus,
                container: container,
                id: this.pageNum,
                scale: this.scale,
                rotation: this.rotation,
                defaultViewport: viewport,
                useOnlyCssZoom: false,
                defaultZoomDelay: 0,
                textLayerMode: 1,
                renderer: "canvas",
            });
            pdfPageView.setPdfPage(page);
            pdfPageView.eventBus.on("annotationlayerrendered", function () {
                const annotationLayer = container.querySelector('.annotationLayer');
                if (annotationLayer) {
                    const viewport = pdfPageView.viewport;
                    annotationLayer.style.width = Math.floor(viewport.width) + "px";
                    annotationLayer.style.height = Math.floor(viewport.height) + "px";
                    annotationLayer.style.transform = `scale(${pdfPageView.outputScale.sx}, ${pdfPageView.outputScale.sy})`;
                    annotationLayer.style.transformOrigin = '0 0';
                }
                self.pages.push(page);
                resolve("ok");
            });

            pdfPageView.draw();
        });
    }

    postRenderAll() {
        for(let i = 0; i < this.numPages; i++) {
            this.postRender(this.pages[i]);
        }
    }

    postRender(page) {
        this.promiseRenderForm(false, page).then(e => this.promiseRestoreValue());
        console.groupEnd();
        this.annotationLinkTargetBlank();
        this.restoreScrolling();
    }

    promiseRenderForm(isField, page) {
        return new Promise((resolve, reject) => {
            page.getAnnotations().then(items => this.renderPdfFormWithFields(items));
            resolve("Réussite");
        });
    }

    promiseToggleFields(enable) {
        if(this.pdfDoc != null) {
            for (let i = 1; i < this.pdfDoc.numPages + 1; i++) {
                this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.toggleItems(items, enable)));
            }
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

    promiseSaveValues() {
        console.log("save");
        return new Promise((resolve, reject) => {
            console.info("launch save values");
            for (let i = 1; i < this.pdfDoc.numPages + 1; i++) {
                this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.saveValues(items)));
            }
            resolve();
        });
    }

    saveValues(items) {
        console.log("saving " + items.length + " fields");
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
            let inputName = item.fieldName;
            let inputField = $("[name='" + $.escapeSelector(inputName) + "']");
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
                        let radio = $('input[name=\'' + inputField.attr("name") + '\']');
                        let self = this;
                        radio.each(function() {
                            if ($(this).prop("checked")) {
                                self.savedFields.set(item.fieldName, $(this).val());
                            }
                        });
                        return;
                    }
                    if (inputField.is('select')) {
                        let value = inputField.val();
                        this.savedFields.set(item.fieldName, value);
                        return;
                    }
                    let value = inputField.val();
                    this.savedFields.set(item.fieldName, value);
                }
            }
        }
    }

    promiseRestoreValue() {
        if(this.savedFields.size === 0) {
            this.promiseSaveValues();
        }
        for(let i = 1; i < this.pdfDoc.numPages + 1; i++) {
            this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.restoreValues(items)));
        }
        this.fireEvent('render', ['end']);
    }

    restoreValues(items) {
        console.log("set fields " + items.length);
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldName != null) {
                let inputName = items[i].fieldName.split(/\$|#|!/)[0];
                let savedValue = this.savedFields.get(items[i].fieldName);
                let inputField = $('[name="' + inputName + '"]');
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
                if (inputField.is('select')) {
                    $("#" + inputName + " option[value='" + savedValue + "']").prop('selected', true);
                    inputField.val(savedValue);
                    continue;
                }
                let selectField = $('select[name=\'' + inputName + '\']');
                if (selectField.val() != null) {
                    let savedFields = this.savedFields;
                    $('#' + inputName + ' option').each(function() {
                        let fieldName = items[i].fieldName;
                        let value = $(this).val();
                        if(savedFields.get(fieldName) === value) {
                            $(this).prop("selected", true);
                        }
                    });
                }
            }
        }
    }

    renderPdfFormWithFields(items) {
        let self = this;
        let datePickerIndex = 40;
        console.debug("debug - " + "rending pdfForm items");
        let signFieldNumber = 0;
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldType === undefined) {
                if(items[i].title && items[i].title.toLowerCase().includes('sign')) {
                    signFieldNumber = signFieldNumber + 1;
                    $('.popupWrapper').remove();
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
            let canvasField = $('section[data-annotation-id=' + items[i].id + '] > canvas');
            if (canvasField.length) {
                canvasField.remove();
            }
            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            if (inputField.length) {
                inputField.addClass("field-type-text");
                inputField.on('input', function(e) {
                    clearTimeout(self.timer);
                    self.timer = setTimeout(e => self.fireEvent('change', ['checked']), 500);

                });
                inputField.removeAttr("hidden");
                if(dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                if(this.disableAllFields) continue;
                let section = $('section[data-annotation-id=' + items[i].id + ']');
                inputField.attr('name', inputName);
                inputField.attr('placeholder', " ");
                inputField.removeAttr("maxlength");
                inputField.attr('id', inputName);
                inputField.attr('title', dataField.description);
                if (dataField.favorisable && !$("#div_" + inputField.attr('id')).length) {
                    let sendField = inputField;
                    $.ajax({
                        type: "GET",
                        url: '/ws-secure/users/get-favorites/' + dataField.id,
                        success: response => this.autocomplete(response, sendField)
                    });
                }
                if (dataField.editable) {
                    inputField.val(items[i].fieldValue);
                    if (dataField.defaultValue != null) {
                        // inputField.attr("es-data", dataField.defaultValue);
                        inputField.val(dataField.defaultValue);
                    }
                    this.enableInputField(inputField, dataField)
                } else {
                    inputField.val(items[i].fieldValue);
                }

                if (dataField.searchServiceName) {
                    inputField.addClass("search-completion");
                    inputField.attr("search-completion-service-name", dataField.searchServiceName);
                    inputField.attr("search-completion-return", dataField.searchReturn);
                    inputField.attr("search-completion-type", dataField.searchType);
                }

                if (dataField.type === "number") {
                    inputField.get(0).type = "number";
                }

                if (dataField.type === "radio") {
                    inputField.addClass("field-type-radio");
                    if (this.isFieldEnable(dataField)) {
                        if (dataField.required) {
                            inputField.parent().addClass('required-field');
                        }
                    }
                    inputField.val(items[i].buttonValue);
                    inputField.attr("id", dataField.name + items[i].buttonValue);
                    if (dataField.defaultValue === items[i].buttonValue) {
                        inputField.attr("checked", "checked");
                        inputField.prop("checked", true);
                    }
                    inputField.unbind();
                    inputField.on('click', e => this.fireEvent('change', ['checked']));
                }
                if (dataField.type === 'checkbox') {
                    inputField.addClass("field-type-checkbox");
                    inputField.val('on');
                    if (dataField.defaultValue === 'on') {
                        inputField.attr("checked", "checked");
                        inputField.prop("checked", true);
                    }
                    inputField.unbind();
                    inputField.on('click', e => this.fireEvent('change', ['checked']));
                }
                if (dataField.type === "date") {
                    datePickerIndex--;
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
                            clear: 'fa-solid fa-trash-alt',
                            close: 'fa fa-check'
                        },
                        toolbarPlacement: 'bottom',
                        showClear: true,
                        showClose: true,
                        keepOpen: true
                    });
                    inputField.on("focus", function () {
                        section.css("z-index", datePickerIndex + 2000);
                    });
                    inputField.on("focusout", function () {
                        section.css("z-index", 4);
                    });
                    inputField.off('dp.change');
                    inputField.on('dp.change', e => this.fireEvent('change', ['date']));
                }
                if (dataField.type === "time") {
                    datePickerIndex--;
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
                            clear: 'fa-solid fa-trash-alt',
                            close: 'fa fa-check'
                        },
                        toolbarPlacement: 'bottom',
                        showClear: true,
                        showClose: true,
                        keepOpen: true,
                    });
                    inputField.on("focus", function () {
                        section.css("z-index", datePickerIndex + 2000);
                    });
                    inputField.on("focusout", function () {
                        section.css("z-index", datePickerIndex);
                    });
                    inputField.off('dp.change');
                    inputField.on('dp.change', e => this.fireEvent('change', ['time']));
                }
            }

            inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
            if (inputField.length) {
                inputField.addClass("field-type-textarea");
                inputField.on('input', function(e) {
                    clearTimeout(self.timer);
                    self.timer = setTimeout(e => self.fireEvent('change', ['checked']), 500);
                });
                inputField.removeAttr("hidden");
                if(dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                if(this.disableAllFields) continue;
                let sendField = inputField;
                if (dataField.favorisable) {
                    $.ajax({
                        type: "GET",
                        url: '/ws-secure/users/get-favorites/' + dataField.id,
                        success: response => this.autocomplete(response, sendField)
                    });
                }
                inputField.attr('name', inputName);
                inputField.attr('placeholder', " ");
                inputField.removeAttr("maxlength");
                inputField.attr('id', inputName);
                if (this.isFieldEnable(dataField)) {
                    inputField.val(dataField.defaultValue);
                    this.enableInputField(inputField, dataField)
                }
            }

            inputField = $('section[data-annotation-id=' + items[i].id + '] > select');
            if (inputField.length) {
                inputField.addClass("field-type-select");
                inputField.on('change', e => this.fireEvent('change', ['checked']));
                if(dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                inputField.removeAttr("hidden");
                if(this.disableAllFields) continue;
                inputField.removeAttr('size');
                inputField.attr('name', inputName);
                inputField.attr('id', inputName);
                if (dataField.editable) {
                    inputField.val(dataField.defaultValue);
                    this.enableInputField(inputField, dataField)
                }
            }
        }
        console.debug("debug - " + ">>End compute field");
        $(".annotationLayer").each(function() {
            $(this).removeClass("d-none");
        });
        this.listenToSearchCompletion();
    }

    isFieldEnable(dataField) {
        return dataField.editable && !dataField.readOnly;
    }

    enableInputField(inputField, dataField) {
        if (!dataField.required) {
            inputField.prop('required', false);
            inputField.removeClass('required-field');
        } else {
            inputField.prop('required', true);
            inputField.addClass('required-field');
        }
        if (!dataField.readOnly) {
            inputField.prop('disabled', false);
            inputField.removeClass('disabled-field disable-selection');
        }
        inputField.attr('title', dataField.description);
    }

    disableInput(inputField, dataField, readOnly) {
        if (readOnly || dataField == null || dataField.readOnly || this.disableAllFields || !this.isFieldEnable(dataField)) {
            inputField.addClass('disabled-field disable-selection');
            inputField.prop('disabled', true);
            inputField.prop('required', false);
            inputField.parent().addClass('disable-div-selection');
        }
    }

    prevPage() {
        this.fireEvent('beforeChange', ['prev']);
        if (!this.isFirstPage()) {
            this.pageNum--;
        }
        this.scrollToPage(this.pageNum);
        return true;
    }

    nextPage() {
        if (this.isLastPage()) {
            return false;
        }
        this.pageNum++;
        this.scrollToPage(this.pageNum);
        return true;
    }

    isFirstPage() {
        return this.pageNum <= 1;
    }

    isLastPage() {
        return this.pageNum >= this.numPages;
    }

    zoomInit(e) {
        this.scale = 1.2;
        console.info('zoom in, scale = ' + this.scale);
        this.fireEvent('scaleChange', ['in']);
    }

    zoomIn(e) {
        if (this.scale >= 1.9) {
            return;
        }
        this.scale = Math.round((this.scale + this.zoomStep) * 10) / 10;
        console.info('zoom in, scale = ' + this.scale);
        this.fireEvent('scaleChange', ['in']);
    }

    zoomOut(e) {
        if (this.scale <= 0.4) {
            return;
        }
        this.scale = this.scale - this.zoomStep;
        console.info('zoom out, scale = ' + this.scale);
        this.fireEvent('scaleChange', ['out']);
    }


    fullWidth() {
        console.info("full width " + window.innerWidth);
        let newScale = (Math.round(window.innerWidth / 100) / 10);
        if (newScale !== this.scale) {
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.fireEvent('scaleChange', ['in']);
        }
    }

    fullHeight() {
        console.info("full height " + window.innerHeight);
        let newScale = (Math.round((window.innerHeight - 200) / 100) / 10) - 0.1;
        if (newScale !== this.scale) {
            this.scale = newScale;
            console.info('zoom in, scale = ' + this.scale);
            this.fireEvent('scaleChange', ['in']);
        }
    }

    rotateLeft() {
        console.info('rotate left');
        if (this.rotation < -90) {
            return;
        }
        this.rotation = this.rotation - 90;
        this.startRender()
        this.fireEvent('rotate', ['left']);
    }

    rotateRight() {
        console.info('rotate right' + this.rotation);
        if (this.rotation > 90) {
            return;
        }
        this.rotation = this.rotation + 90;
        this.startRender()
        this.fireEvent('rotate', ['right']);
    }

    autocomplete(response, inputField) {
        let id = inputField.attr('id');
        let div = "<div class='custom-autocompletion' id='div_" + id +"'></div>";
        $(div).insertAfter(inputField);
        inputField.autocomplete({
            delay: 500,
            source: response,
            appendTo: "#div_" + id,
            minLength:0
        }).bind('focus', function(){ $(this).autocomplete("search"); } );
    }

    checkForm() {
        return new Promise((resolve, reject) => {
            let formData = new Map();
            console.info("check data name");
            let self = this;
            let resolveOk = "ok";
            let warningFields = [];
            $(self.dataFields).each(function (e, item) {
                let savedField = self.savedFields.get(item.name)
                formData[item.name] = savedField;
                if (item.required && self.isFieldEnable(item) &&
                    (!savedField || (savedField === "off" && item.type === "checkbox"))) {
                    let addWarning = true;
                    for(let i = 0; i < warningFields.length; i++) {
                        if(warningFields[i].name === item.name) {
                            addWarning = false;
                        }
                    }
                    if(addWarning) {
                        warningFields.push($(this)[0]);
                    }
                }
            });
            if (warningFields.length > 0) {
                warningFields.sort((a, b) => a.compareByPage(b))
                let text = "Certain champs requis n'ont pas été remplis dans ce formulaire";
                if (warningFields.length < 2 && warningFields[0].name != null) {
                    if (warningFields[0].description != null && warningFields[0].description !== "") {
                        text = "Le champ " + warningFields[0].description + " n'est pas rempli en page " + warningFields[0].page;
                    } else {
                        text = "Le champ " + warningFields[0].name + " n'est pas rempli en page " + warningFields[0].page;
                    }
                } else {
                    warningFields.forEach(function (field) {
                        if (field.description != null && field.description !== "") {
                            text += "<li>" + field.description;
                            if(field.page != null) {
                                text += " (en page " + field.page + ")";
                            }
                            text +="</li>";
                        } else {
                            text += "<li>" + field.name;
                            if(field.page != null) {
                                text += " (en page " + field.page + ")";
                            }
                            text +="</li>";
                        }
                    });
                }
                bootbox.alert(text, function () {
                    let field = $('#' + warningFields[0].name);
                    setTimeout(function () {
                        self.focusField(field)
                    }, 100);
                });
                resolveOk = $(this)[0].name;
                $('#sendModal').modal('hide');
            }
            resolve(resolveOk);
        });
    }

    focusField(field) {
        if(field.attr("type") === "radio") {
            this.highlightRadio(field);
        }
        field.focus();
        let offset = field.offset();
        if(offset != null) {
            $('html, body').animate({
                scrollTop: offset.top - 170,
                scrollLeft: offset.left
            });
        }
    }

    highlightRadio(field) {
        $("[name='" + field.attr('name') + "']").each(function() {
            let radio = $(this);
            let i = 0;
            let flashInterval = setInterval(
                function() {
                    radio.toggleClass('highlight');
                    if(i > 4) {
                        clearInterval(flashInterval);
                        radio.removeClass('highlight');
                    }
                    i++;
                },
                1000
            );
        });
    }

    startProgress() {
        let self = this;
        this.interval = setInterval(function() {
            let progress = Math.round(self.renderedPages / self.numPages * 100)
            $(".progress-bar")
                .css("width", progress + "%")
                .attr("aria-valuenow", progress)
                .text("Chargement de la page " + self.renderedPages + "/" + self.numPages);

        }, 100);
    }

    stopProgress(){
        $(".progress-bar").css("width","100%").attr("aria-valuenow", 100).text("Chargement terminé");
        clearInterval(this.interval);
    }

    resetProgress() {
        $(".progress-bar").css("width","0%").attr("aria-valuenow", 0);
        clearInterval(this.interval);
    }
}