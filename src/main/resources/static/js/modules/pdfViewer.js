export class PdfViewer {

    pageRendering = false;
    pageNumPending;
    oldscale = 1;
    scale = 1;
    canvas = document.getElementById('pdf');
    ctx = this.canvas.getContext('2d');
    url;
    pdfDoc;
    pageNum = 1;
    numPages = 1;
    signPosition;

    events = {};

    addEventListener(name, handler) {
        if (this.events.hasOwnProperty(name))
            this.events[name].push(handler);
        else
            this.events[name] = [handler];
    };

    removeEventListener(name, handler) {
        /* This is a bit tricky, because how would you identify functions?
           This simple solution should work if you pass THE SAME handler. */
        if (!this.events.hasOwnProperty(name))
            return;

        var index = this.events[name].indexOf(handler);
        if (index != -1)
            this.events[name].splice(index, 1);
    };

    fireEvent(name, args) {
        if (!this.events.hasOwnProperty(name))
            return;

        if (!args || !args.length)
            args = [];

        var evs = this.events[name], l = evs.length;
        for (var i = 0; i < l; i++) {
            evs[i].apply(null, args);
        }
    };

    constructor(url, signPosition) {
        this.url= url;
        this.signPosition = signPosition;
        this.init();
    }

    init() {
        pdfjsLib.disableWorker = true;
        pdfjsLib.GlobalWorkerOptions.workerSrc = '/js/pdf.worker.js';
        pdfjsLib.getDocument(this.url).promise.then(pdf => this.startRender(pdf));

        document.getElementById('prev').addEventListener('click', e => this.prevPage());
        document.getElementById('next').addEventListener('click', e => this.nextPage());
        document.getElementById('zoomin').addEventListener('click', e => this.zoomIn());
        document.getElementById('zoomout').addEventListener('click', e => this.zoomOut());
        document.getElementById('rotateleft').addEventListener('click', e => this.rotateLeft());
        document.getElementById('rotateright').addEventListener('click', e => this.rotateRight());

        this.canvas.addEventListener('mousemove', e => this.signPosition.point(e));
    }


    startRender(pdf) {
        this.pdfDoc = pdf;
        this.numPages = this.pdfDoc.numPages;
        document.getElementById('page_count').textContent = this.pdfDoc.numPages;
        this.renderPage(this.pageNum);
    }

    renderPage(num) {
        console.log("render page " + num);
        if(this.pdfDoc != null) {
            document.getElementById('signPageNumber').value = num;
            this.pageRendering = true;
            this.pdfDoc.getPage(num).then(page => this.renderTask(page));
            this.pageNum = num;
            document.getElementById('page_num').textContent = num;
            document.getElementById('zoom').textContent = 100 * this.scale;
            //parent.signPosition.refreshSign();
            if(this.pdfDoc.numPages === 1) {
                $('#prev').prop('disabled', true);
                $('#next').prop('disabled', true);
            }


            this.oldscale = this.scale;
        }
    }

    renderTask(page) {
        let scale = this.scale;
        let rotation = this.rotation;
        let viewport = page.getViewport({scale, rotation});
        this.canvas.height = viewport.height;
        this.canvas.width = viewport.width;
        let renderContext = {
            canvasContext: this.ctx,
            viewport: viewport
        };
        let renderTask = page.render(renderContext);
        renderTask.promise.then(e => this.saveRender());
    }

    saveRender() {
        this.pageRendering = false;
        if (this.pageNumPending !== null) {
            //this.renderPage(this.pageNumPending);
            this.pageNumPending = null;
        }
    }

    queueRenderPage(num) {
        if (this.pageRendering) {
            this.pageNumPending = num;
        } else {
            this.renderPage(num);
        }
    }

    prevPage() {
        if (this.pageNum <= 1) {
            return;
        }
        this.pageNum--;
        this.queueRenderPage(this.pageNum);
        window.scrollTo(0, 0);

    }

    nextPage() {
        if (this.pageNum >= this.numPages) {
            return;
        }
        this.pageNum++;
        this.queueRenderPage(this.pageNum);
        window.scrollTo(0, 0);
    }

    zoomIn() {
        console.log('zoomin');
        if (this.scale >= 2.5) {
            return;
        }
        // $(".circle").each(function( index ) {
        //     var left = Math.round($(this).css('left').replace("px", "") / this.scale * (this.scale + 0.25));
        //     var top = Math.round((($(this).css('top').replace("px", "") / this.scale) - 20) * (this.scale + 0.25)) + 20 * (this.scale + 0.25);
        //     console.log(top);
        //     $(this).css('left', left);
        //     $(this).css('top', top);
        // });
        this.scale = this.scale + 0.25;
        //textDate = document.getElementById("textDate");
        $('#textDate').css('font-size', 8 * this.scale + 'px')
        $('#borders').css('line-height', 8 * this.scale + 'px')
        this.queueRenderPage(this.pageNum);
        this.fireEvent('scale', ['in']);
    }


    zoomOut() {
        if (this.scale <= 0.75) {
            return;
        }
        $(".circle").each(function( index ) {
            var left = Math.round($(this).css('left').replace("px", "") / this.scale * (this.scale - 0.25));
            var top = Math.round((($(this).css('top').replace("px", "") / this.scale) - 20) * (this.scale - 0.25)) + 20 * (this.scale - 0.25);
            console.log(top);
            $(this).css('left', left);
            $(this).css('top', top);
        });
        this.scale = this.scale - 0.25;
        $('#textDate').css('font-size', 8 * this.scale + 'px')
        $('#borders').css('line-height', 8 * this.scale + 'px')
        this.queueRenderPage(this.pageNum);
        this.fireEvent('scale', ['out']);
    }


    rotateLeft() {
        if (this.rotation < -90) {
            return;
        }
        this.rotation = this.rotation - 90;
        this.queueRenderPage(this.pageNum);
    }

    rotateRight() {
        if (this.rotation > 90) {
            return;
        }
        this.rotation = this.rotation + 90;
        this.queueRenderPage(this.pageNum);
    }

}