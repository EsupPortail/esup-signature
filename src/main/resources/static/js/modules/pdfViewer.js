export class PdfViewer {

    constructor(url, signPosition) {
        console.info("Starting PDF Viewer");
        this.url= url;
        this.signPosition = signPosition;
        this.pdfPageView = null;
        this.pageRendering = false;
        this.scale = 0.75;
        this.canvas = document.getElementById('pdf');
        this.pdfDoc = null;
        this.pageNum = 1;
        this.numPages = 1;
        this.page = null;
        this.dataFields = null;
        this.formRender = false;
        this.events = {};
        pdfjsLib.disableWorker = true;
        pdfjsLib.GlobalWorkerOptions.workerSrc = '/js/pdf.worker.js';
        pdfjsLib.getDocument(this.url).promise.then(pdf => this.startRender(pdf));
        this.init();
    }

    addEventListener(name, handler) {
        if (this.events.hasOwnProperty(name))
            this.events[name].push(handler);
        else
            this.events[name] = [handler];
    };

    removeEventListener(name, handler) {
        if (!this.events.hasOwnProperty(name))
            return;

        let index = this.events[name].indexOf(handler);
        if (index !== -1)
            this.events[name].splice(index, 1);
    };

    fireEvent(name, args) {
        if (!this.events.hasOwnProperty(name))
            return;

        if (!args || !args.length)
            args = [];

        let evs = this.events[name], l = evs.length;
        for (let i = 0; i < l; i++) {
            evs[i].apply(null, args);
        }
    };

    init() {
        document.getElementById('prev').addEventListener('click', e => this.prevPage());
        document.getElementById('next').addEventListener('click', e => this.nextPage());
        document.getElementById('zoomin').addEventListener('click', e => this.zoomIn());
        document.getElementById('zoomout').addEventListener('click', e => this.zoomOut());
        document.getElementById('rotateleft').addEventListener('click', e => this.rotateLeft());
        document.getElementById('rotateright').addEventListener('click', e => this.rotateRight());
    }

    startRender(pdf) {
        console.group("Start render");
        this.pdfDoc = pdf;
        this.numPages = this.pdfDoc.numPages;
        document.getElementById('page_count').textContent = this.pdfDoc.numPages;
        this.fireEvent("ready", ['ok']);
    }

    renderPage(num) {
        console.debug("render page " + num + ", scale : " + this.scale);
        if(this.dataFields != null) {
            this.setValues();
            this.formRender = true;
        }
        if(this.pdfDoc != null) {
            //document.getElementById('signPageNumber').value = num;
            document.getElementById('page_num').textContent = num;
            document.getElementById('zoom').textContent = 100 * this.scale;
            if(this.pdfDoc.numPages === 1) {
                $('#prev').prop('disabled', true);
                $('#next').prop('disabled', true);
            }
            this.pageRendering = true;
            this.pdfDoc.getPage(num).then(page => this.renderTask(page));
            this.pageNum = num;
        }
    }

    renderTask(page) {
        console.debug("launch render task");
        this.page = page;
        let scale = this.scale;
        let rotation = this.rotation;
        let viewport = page.getViewport({scale, rotation});
        if(this.pdfPageView == null) {
            this.pdfPageView = new pdfjsViewer.PDFPageView({
                container: this.canvas,
                id: this.pageNum,
                scale: this.scale,
                defaultViewport: viewport
            });
        }
        if(this.formRender) {
            this.pdfPageView.renderInteractiveForms = true;
            this.pdfPageView.annotationLayerFactory = new pdfjsViewer.DefaultAnnotationLayerFactory();
        }
        this.pdfPageView.scale = this.scale;
        this.pdfPageView.rotation = this.rotation;
        this.pdfPageView.setPdfPage(page);
        this.pdfPageView.draw();
        this.pageRendering = false;
        if(this.dataFields != null) {
            console.info("render form");
            this.page.getAnnotations().then(items => this.renderPdfForm(items));
        }
        this.canvas.style.width = Math.round(this.pdfPageView.viewport.width) +"px";
        this.canvas.style.height = Math.round(this.pdfPageView.viewport.height) + "px";
        this.fireEvent('render', ['end']);
        console.groupEnd();
    }

    renderPdfForm(items) {
        console.log("rending pdfForm items");
        let signFieldNumber = 0;
        for (let i = 0; i < items.length; i++) {
            let dataField = this.dataFields.filter(obj => {
                return obj.name === items[i].fieldName
            })[0];
            console.debug(items[i]);
            if(items[i].fieldType === undefined && items[i].title.toLowerCase().startsWith('sign')) {
                console.debug("found sign field");
                signFieldNumber = signFieldNumber + 1;
                $('.popupWrapper').remove();
                let signField = $('section[data-annotation-id=' + items[i].id + '] > div');
                signField.append('Champ signature ' + signFieldNumber + ' : <br>');
                signField.append('Vous pourrez signer le document après avoir lancé le processus de signature');
                signField.addClass("sign-field");

            }
            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            if(inputField.length > 0) {
                inputField.attr('name', items[i].fieldName);
                if (dataField != null) {
                    inputField.val(dataField.defaultValue);
                    if (dataField.required) {
                        inputField.prop('required', true);
                        inputField.addClass('required-field');
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
                }
            } else {
                let inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
                if(inputField.length > 0) {
                    inputField.attr('name', items[i].fieldName);
                    if (dataField != null) {
                        inputField.val(dataField.defaultValue);
                    }
                }
            }
        }
    }

    setValues() {
        for (let i = 0; i < this.dataFields.length; i++) {
            if(this.dataFields[i] != null) {
                let inputField = $('input[name=\'' + this.dataFields[i].name + '\']');
                if (inputField.val() != null) {
                    this.dataFields[i].defaultValue = inputField.val();
                    if(inputField.is(':checkbox')) {
                        if(!inputField[0].checked) {
                            this.dataFields[i].defaultValue = 'off';
                        }
                    }
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
        console.group('zoom in');
        if (this.scale >= 2) {
            return;
        }
        // $(".circle").each(function( index ) {
        //     let left = Math.round($(this).css('left').replace("px", "") / this.scale * (this.scale + 0.25));
        //     let top = Math.round((($(this).css('top').replace("px", "") / this.scale) - 20) * (this.scale + 0.25)) + 20 * (this.scale + 0.25);
        //     console.log(top);
        //     $(this).css('left', left);
        //     $(this).css('top', top);
        // });
        this.scale = this.scale + 0.25;
        this.renderPage(this.pageNum);
        this.fireEvent('scaleChange', ['in']);
    }


    zoomOut() {
        console.group('zoom out');
        if (this.scale <= 0.50) {
            return;
        }
        // $(".circle").each(function( index ) {
        //     let left = Math.round($(this).css('left').replace("px", "") / this.scale * (this.scale - 0.25));
        //     let top = Math.round((($(this).css('top').replace("px", "") / this.scale) - 20) * (this.scale - 0.25)) + 20 * (this.scale - 0.25);
        //     console.log(top);
        //     $(this).css('left', left);
        //     $(this).css('top', top);
        // });
        this.scale = this.scale - 0.25;
        this.renderPage(this.pageNum);
        this.fireEvent('scaleChange', ['out']);
    }


    rotateLeft() {
        console.group('rotate left');
        if (this.rotation < -90) {
            return;
        }
        this.rotation = this.rotation - 90;
        this.renderPage(this.pageNum);
        this.fireEvent('rotate', ['left']);
    }

    rotateRight() {
        console.group('rotate right');
        if (this.rotation > 90) {
            return;
        }
        this.rotation = this.rotation + 90;
        this.renderPage(this.pageNum);
        this.fireEvent('rotate', ['right']);
    }


    setDataFields(dataFields) {
        this.dataFields = dataFields;
    }

}