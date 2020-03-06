//import {Sign} from "./sign";

export class WorkspacePdf {

    url;
    pdf = document.getElementById("pdf");
    posX;
    posY;
    signPageNumber;
    currentSignType;
    signable;
    postits;
    mode = 'read';
    dateActive = false;
    visualActive = true;
    pointItEnable = true;
    pdfDoc = null;
    pageNum = 1;
    pageRendering = false;
    pageNumPending = null;
    oldscale = 1;
    scale = 1;
    rotation = 0;
    canvas = document.getElementById('pdf');
    ctx = this.canvas.getContext('2d');
    cross = $('#cross');
    borders = $('#borders');
    currentSignRequestParams;

    constructor(url, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits) {
        this.currentSignRequestParams = currentSignRequestParams;
        this.postits = postits;
        this.url = url;
        this.signable = signable;
        this.init();
    }

    init() {
        pdfjsLib.disableWorker = true;
        pdfjsLib.GlobalWorkerOptions.workerSrc = '/js/pdf.worker.js';
        pdfjsLib.getDocument(this.url).promise.then(pdf => this.start(pdf));
        document.getElementById('prev').addEventListener('click', this.prevPage());
        document.getElementById('next').addEventListener('click', this.nextPage());
        document.getElementById('zoomin').addEventListener('click', this.zoomIn());
        document.getElementById('zoomout').addEventListener('click', this.zoomOut());
        document.getElementById('rotateleft').addEventListener('click', this.rotateLeft());
        document.getElementById('rotateright').addEventListener('click', this.rotateRight());
        window.addEventListener("touchmove", e => this.touchmove(e));
        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e));
        canvas.on('mouseup', e => this.action());
        canvas.on('mousemove', e => this.point(e));

        this.resetSign();

        if(localStorage.getItem('mode') != null && localStorage.getItem('mode') !== "") {
            this.mode = localStorage.getItem('mode');
        } else {
            if(signable === 'ok') {
                localStorage.setItem('mode', 'sign');
                this.mode = 'sign';
            } else {
                localStorage.setItem('mode', 'read');
                this.mode = 'read';

                $('#readButton').removeClass('d-none');
            }
        }
        if(this.mode === 'sign') {
            this.pageNum = this.signPageNumber;
            this.enableSignMode();
        } else if(mode === 'comment') {
            this.enableCommentMode();
        } else if(mode === 'refuse') {
            this.enableRefuseMode();
        } else {
            this.enableSignMode();
        }
        if(this.signable === 'ok' && this.currentSignType === 'visa') {
            if(mode === 'sign') {
                toggleVisual();
                pageNum = 1;
            }
        }
    }

    start(pdf) {
        this.pdfDoc = pdf;
        document.getElementById('page_count').textContent = this.pdfDoc.numPages;
        this.renderPage(this.pageNum);
    }


    renderPage(num) {
        if(this.pdfDoc != null) {
            document.getElementById('signPageNumber').value = num;
            this.pageRendering = true;
            this.pdfDoc.getPage(num).then(page => this.renderTask(page));
            this.pageNum = num;
            document.getElementById('page_num').textContent = num;
            document.getElementById('zoom').textContent = 100 * this.scale;
            this.refreshSign();
            if(this.pdfDoc.numPages === 1) {
                $('#prev').prop('disabled', true);
                $('#next').prop('disabled', true);
            }

            this.postits.forEach((postit, index) => {
                if(postit.pageNumber === num && mode === 'comment') {
                    $('#' + postit.id).show();
                    $('#' + postit.id).css("background-color", "#FFC");
                } else {
                    $('#' + postit.id).hide();
                    $('#' + postit.id).css("background-color", "#EEE");
                }
            });
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
        if (pageNumPending !== null) {
            this.renderPage(pageNumPending);
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
        if (this.pageNum >= this.pdfDoc.numPages) {
            return;
        }
        this.pageNum++;
        this.queueRenderPage(pageNum);
        window.scrollTo(0, 0);
    }

    zoomIn() {
        if (scale >= 2.5) {
            return;
        }
        $(".circle").each(function( index ) {
            var left = Math.round($(this).css('left').replace("px", "") / scale * (scale + 0.25));
            var top = Math.round((($(this).css('top').replace("px", "") / scale) - 20) * (scale + 0.25)) + 20 * (scale + 0.25);
            console.log(top);
            $(this).css('left', left);
            $(this).css('top', top);
        });
        scale = scale + 0.25;
        //textDate = document.getElementById("textDate");
        $('#textDate').css('font-size', 8 * scale + 'px')
        $('#borders').css('line-height', 8 * scale + 'px')
       this.queueRenderPage(pageNum);
    }


    zoomOut() {
        if (scale <= 0.75) {
            return;
        }
        $(".circle").each(function( index ) {
            var left = Math.round($(this).css('left').replace("px", "") / scale * (scale - 0.25));
            var top = Math.round((($(this).css('top').replace("px", "") / scale) - 20) * (scale - 0.25)) + 20 * (scale - 0.25);
            console.log(top);
            $(this).css('left', left);
            $(this).css('top', top);
        });
        scale = scale - 0.25;
        $('#textDate').css('font-size', 8 * scale + 'px')
        $('#borders').css('line-height', 8 * scale + 'px')
        this.queueRenderPage(pageNum);
    }


    rotateLeft() {
        if (this.rotation < -90) {
            return;
        }
        this.rotation = this.rotation - 90;
        this.queueRenderPage(pageNum);
    }

    rotateRight() {
        if (this.rotation > 90) {
            return;
        }
        this.rotation = this.rotation + 90;
        this.queueRenderPage(pageNum);
    }

    action() {
        if(mode === 'sign') {
            this.savePosition();
        } else if(mode === 'comment') {
            this.displayComment();
        }
    }

    displayComment() {
        if(this.mode !== 'comment') {
            return;
        }
        this.pointItEnable = false;
        document.getElementById("postit").style.left = posX + "px";
        document.getElementById("postit").style.top = posY + "px";
        $("#postit").show();
        //document.getElementById("postit").style.display = "block";
    }

    hideComment() {
        if(this.mode !== 'comment') {
            return;
        }
        this.pointItEnable = true;
        $("#postit").hide();
    }

    enableReadMode() {
        //$('#pdf').awesomeCursor('comment-alt');
        this.disableAllModes();
        this.mode = 'read';
        localStorage.setItem('mode', 'read');
        this.pointItEnable = false;
        scale = 1.75;
        $('#readButton').toggleClass('btn-light btn-secondary');
        $('#rotateleft').prop('disabled', false);
        $('#rotateright').prop('disabled', false);
        $('#stepscard').show();
        this.queueRenderPage(1);
    }

    enableCommentMode() {
        console.log("enable comments");
        this.disableAllModes();
        this.mode = 'comment';
        localStorage.setItem('mode', 'comment');
        this.pointItEnable = true;
        this.scale = 1;
        $('#workspace').toggleClass('alert-warning alert-secondary');
        $('#commentButton').toggleClass('btn-light btn-warning');
        $('#commentsTools').show();
        $('#infos').show();
        this.queueRenderPage(1);
    }

    enableSignMode() {
        this.disableAllModes();
        this.mode = 'sign';
        localStorage.setItem('mode', 'sign');
        this.pointItEnable = false;
        $('#workspace').toggleClass('alert-success alert-secondary');
        $(".circle").each(function( index ) {
            $(this).hide();
        });
        document.getElementById("xPos").value = Math.round(this.posX);
        document.getElementById("yPos").value = Math.round(this.posY);
        $('#signButton').toggleClass('btn-light btn-success');
        $('#signtools').show();
        $('#stepscard').show();
        $('#infos').show();
        if(this.visualActive) {
            $('#pen').removeClass('btn-outline-secondary').addClass('btn-outline-success');
            $('#cross').show();
        }
        this.rotation = 0;
        this.scale = 1;
        this.queueRenderPage(this.signPageNumber);

    }

    enableRefuseMode() {
        this.disableAllModes();
        this.mode = 'refuse';
        localStorage.setItem('mode', 'refuse');
        $('#workspace').toggleClass('alert-danger alert-secondary');
        $('#refuseButton').toggleClass('btn-light btn-danger');
        $('#refusetools').show();
        $('#infos').show();
        this.queueRenderPage(pageNum);
    }

    disableAllModes() {
        this.mode = 'sign';
        $('#workspace').removeClass('alert-danger').removeClass('alert-warning').removeClass('alert-success').addClass('alert-secondary');
        $('#commentButton').addClass('btn-light').removeClass('btn-warning');
        $('#signButton').addClass('btn-light').removeClass('btn-success');
        $('#refuseButton').addClass('btn-light').removeClass('btn-danger');
        $('#readButton').addClass('btn-light').removeClass('btn-secondary');
        $('#commentsTools').hide();
        $('#stepscard').hide();
        $('#signtools').hide();
        $('#cross').hide();
        $('#infos').hide();
        $('#postit').hide();
        $('#refusetools').hide();
        $('#rotateleft').prop('disabled', true);
        $('#rotateright').prop('disabled', true);
        $('#pdf').css('cursor', 'default');
    }


    toggleVisual() {
        if(this.visualActive) {
            this.visualActive = false;
            $('#clock').prop('disabled', true);
        } else {
            this.visualActive = true;
            $('#clock').prop('disabled', false);
        }
        $('#cross').toggle();
        $('#pen').toggleClass('btn-outline-success btn-outline-dark').children().toggleClass('fa-eye-slash fa-eye');
    }

    toggleDate() {
        $('#clock').toggleClass('btn-outline-success btn-outline-dark');
        var textDate;
        if(!this.dateActive) {
            this.dateActive = true;
            this.cross.style.width = 200;
            this.cross.style.height = cross.offsetHeight + 20;
            this.borders.style.width = 200;
            this.borders.style.height = borders.offsetHeight + 20;
            this.borders.insertAdjacentHTML("beforeend", "<span id='textDate' class='align-top' style='font-size:" + 8 * this.scale + "px;'>Le "+ moment().format('DD/MM/YYYY HH:mm') +"</span>");
        } else {
            this.dateActive = false;
            this.cross.style.width = 100;
            this.cross.style.height = cross.offsetHeight - 20;
            this.borders.style.width = 100;
            this.borders.style.height = borders.offsetHeight - 20;
            textDate = document.getElementById("textDate");
            textDate.remove();
        }
    }

    computeWhellEvent(event) {
        if(event.ctrlKey === true) {
            if (this.detectMouseWheelDirection(event) === 'down'){
                this.zoomOut();
            } else {
                this.zoomIn();
            }
        } else {
            if (this.detectMouseWheelDirection(event) === 'down' && $(window).scrollTop() + $(window).height() === $(document).height()) {
                if(pageNum < pdfDoc.numPages) {
                    this.nextPage();
                }
            } else if (this.detectMouseWheelDirection(event) === 'up' && window.scrollY === 0) {
                if(pageNum > 1) {
                    this.prevPage();
                    window.scrollTo(0, document.body.scrollHeight);
                }
            }
        }
    }

    detectMouseWheelDirection(e) {
        var delta = null,
            direction = false;
        // if ( !e ) {
        //     e = window.event;
        // }
        var e_delta = (e.deltaY || -e.wheelDelta || e.detail);
        if ( e_delta ) {
            delta = e_delta  / 60;
        } else if ( e.detail ) {
            delta = -e.detail / 2;
        }
        if ( delta !== null ) {
            direction = delta > 0 ? 'down' : 'up';
        }
        return direction;
    }
    
}