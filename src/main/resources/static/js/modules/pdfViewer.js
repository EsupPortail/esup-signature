export class PdfViewer {

    constructor(url, signable, currentStepNumber) {
        console.info("Starting PDF Viewer");
        this.url= url;
        this.pdfPageView = null;
        this.currentStepNumber = currentStepNumber;
        this.scale = 0.75;
        this.canvas = document.getElementById('pdf');
        this.pdfDoc = null;
        this.pageNum = 1;
        this.numPages = 1;
        this.page = null;
        this.dataFields = null;
        this.formRender = false;
        this.signable = signable;
        this.events = {};
        pdfjsLib.disableWorker = true;
        pdfjsLib.GlobalWorkerOptions.workerSrc = '/js/pdf.worker.js';
        pdfjsLib.getDocument(this.url).promise.then(pdf => this.startRender(pdf));
        this.initListeners();
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

    initListeners() {
        document.getElementById('prev').addEventListener('click', e => this.prevPage());
        document.getElementById('next').addEventListener('click', e => this.nextPage());
        document.getElementById('zoomin').addEventListener('click', e => this.zoomIn());
        document.getElementById('zoomout').addEventListener('click', e => this.zoomOut());
        document.getElementById('rotateleft').addEventListener('click', e => this.rotateLeft());
        document.getElementById('rotateright').addEventListener('click', e => this.rotateRight());
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
        if(this.pdfDoc != null) {
            //document.getElementById('signPageNumber').value = num;
            document.getElementById('page_num').textContent = num;
            document.getElementById('zoom').textContent = 100 * this.scale;
            if(this.pdfDoc.numPages === 1) {
                $('#prev').prop('disabled', true);
                $('#next').prop('disabled', true);
            }
            this.pdfDoc.getPage(num).then(page => this.renderTask(page));
            this.pageNum = num;
        }
    }

    renderTask(page) {
        console.info("launch render task");
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
        this.pdfPageView.scale = this.scale;
        this.pdfPageView.rotation = this.rotation;
        this.pdfPageView.setPdfPage(page);
        if(this.dataFields != null) {
            console.debug("enable render form");
            this.pdfPageView.renderInteractiveForms = true;
            this.pdfPageView.annotationLayerFactory = new pdfjsViewer.DefaultAnnotationLayerFactory();
        } else {
            console.debug("disable render form");
            this.pdfPageView.renderInteractiveForms = false;
        }
        this.pdfPageView.draw();
        if(this.dataFields != null) {
            this.page.getAnnotations().then(items => this.renderPdfForm(items));
        }
        this.canvas.style.width = Math.round(this.pdfPageView.viewport.width) +"px";
        this.canvas.style.height = Math.round(this.pdfPageView.viewport.height) + "px";
        console.groupEnd();
    }

    renderPdfForm(items) {
        console.debug("rending pdfForm items");
        let signFieldNumber = 0;
        let visaFieldNumber = 0;
        console.debug(this.dataFields);
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
            if(items[i].fieldType === undefined && items[i].title.toLowerCase().startsWith('visa')) {
                console.debug("found sign field");
                visaFieldNumber = visaFieldNumber + 1;
                $('.popupWrapper').remove();
                let signField = $('section[data-annotation-id=' + items[i].id + '] > div');
                signField.append('Champ visa ' + visaFieldNumber + '<br>');
                //signField.append('Vous pourrez signer le document après avoir lancé le processus de signature');
                signField.addClass("sign-field");
                signField.addClass("d-none");
                signField.parent().remove();
            }

            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            if(inputField.length && dataField != null) {
                console.debug(items[i]);
                console.debug(inputField);
                console.debug(dataField);
                inputField.attr('name', items[i].fieldName.split(/\$|#|!/)[0]);
                if(!dataField.stepNumbers.includes("" + this.currentStepNumber) || !this.signable) {
                    //TODO debug
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
                        inputField.attr('name', items[i].fieldName.split(/\$|#|!/)[0]);
                        inputField.val(items[i].fieldValue);
                        if (dataField != null) {
                            inputField.val(dataField.defaultValue);
                        }
                        inputField.prop('disabled', false);
                        if (dataField.required) {
                            inputField.prop('required', true);
                            inputField.addClass('required-field');
                        }
                    }

                }
            }
            console.debug(">>End compute field");
            this.fireEvent('render', ['end']);
        }
    }

    // setValues() {
    //     for (let i = 0; i < this.dataFields.length; i++) {
    //         if(this.dataFields[i] != null) {
    //             let inputField = $('input[name=\'' + this.dataFields[i].name + '\']');
    //             if (inputField.val() != null) {
    //                 this.dataFields[i].defaultValue = inputField.val();
    //                 if(inputField.is(':checkbox')) {
    //                     if(!inputField[0].checked) {
    //                         this.dataFields[i].defaultValue = 'off';
    //                     }
    //                 }
    //             }
    //         }
    //     }
    // }

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
        console.info('zoom in');
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
        console.info('zoom out');
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
        this.formRender = true;
    }

    printPdf() {
        this.pdfPageView.eventBus.dispatch('print', {
            source: self
        });

    }

}