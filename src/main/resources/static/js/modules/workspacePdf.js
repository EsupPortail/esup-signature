import {PdfViewer} from "/js/modules/pdfViewer.js";
import {SignPosition} from "/js/modules/signPosition.js";

export class WorkspacePdf {

    signPageNumber = document.getElementById('signPageNumber');
    currentSignType;
    signable;
    postits;
    mode = 'read';
    visualActive = true;
    pointItEnable = true;
    pageNum = 1;
    scale = 1;
    rotation = 0;
    currentSignRequestParams;
    pdfViewer;
    signPosition;

    constructor(url, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits) {
        this.currentSignRequestParams = currentSignRequestParams;
        this.postits = postits;
        this.signable = signable;
        this.signPosition = new SignPosition(currentSignRequestParams.xpos, currentSignRequestParams.ypos);
        this.pdfViewer = new PdfViewer(url, this.signPosition);
        this.init();
    }

    init() {

        this.pdfViewer.addEventListener('scale', e => this.signPosition.resetSign());
        document.getElementById('commentButton').addEventListener('click', e => this.enableCommentMode());
        document.getElementById('signButton').addEventListener('click', e => this.enableSignMode());
        document.getElementById('visualButton').addEventListener('click', e => this.signPosition.toggleVisual());
        document.getElementById('dateButton').addEventListener('click', e => this.signPosition.toggleDate());

        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e));

        this.pdfViewer.canvas.addEventListener('mouseup', e => this.signPosition.action());

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

        this.signPosition.resetSign();

        // this.postits.forEach((postit, index) => {
        //     let postitDiv = $('#' + postit.id);
        //     if(postit.pageNumber === num && mode === 'comment') {
        //         postitDiv.show();
        //         postitDiv.css("background-color", "#FFC");
        //     } else {
        //         postitDiv.hide();
        //         postitDiv.css("background-color", "#EEE");
        //     }
        // });
    }

    action() {
        if(this.mode === 'sign') {
            this.signPosition.savePosition();
        } else if(this.mode === 'comment') {
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
        this.scale = 1.75;
        $('#readButton').toggleClass('btn-light btn-secondary');
        $('#rotateleft').prop('disabled', false);
        $('#rotateright').prop('disabled', false);
        $('#stepscard').show();
        this.pdfViewer.queueRenderPage(1);
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
        this.pdfViewer.queueRenderPage(1);
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
        document.getElementById("xPos").value = Math.round(this.signPosition.posX);
        document.getElementById("yPos").value = Math.round(this.signPosition.posY);
        $('#signButton').toggleClass('btn-light btn-success');
        $('#signtools').show();
        $('#stepscard').show();
        $('#infos').show();
        if(this.visualActive) {
            $('#pen').removeClass('btn-outline-secondary').addClass('btn-outline-success');
            this.signPosition.cross.show();
        }
        this.rotation = 0;
        this.scale = 1;
        this.pdfViewer.queueRenderPage(parseInt(this.signPageNumber.value, 10));

    }

    enableRefuseMode() {
        this.disableAllModes();
        this.mode = 'refuse';
        localStorage.setItem('mode', 'refuse');
        $('#workspace').toggleClass('alert-danger alert-secondary');
        $('#refuseButton').toggleClass('btn-light btn-danger');
        $('#refusetools').show();
        $('#infos').show();
        this.pdfViewer.queueRenderPage(pageNum);
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
        this.signPosition.cross.hide();
        $('#infos').hide();
        $('#postit').hide();
        $('#refusetools').hide();
        $('#rotateleft').prop('disabled', true);
        $('#rotateright').prop('disabled', true);
        $('#pdf').css('cursor', 'default');
    }


    computeWhellEvent(event) {
        console.log("wheel event");
        if(event.ctrlKey === true) {
            if (this.detectMouseWheelDirection(event) === 'down'){
                this.pdfViewer.zoomOut();
            } else {
                this.pdfViewer.zoomIn();
            }
        } else {
            if (this.detectMouseWheelDirection(event) === 'down' && $(window).scrollTop() + $(window).height() === $(document).height()) {
                if(this.pageNum < this.pdfViewer.pdfDoc.numPages) {
                    this.pdfViewer.nextPage();
                }
            } else if (this.detectMouseWheelDirection(event) === 'up' && window.scrollY === 0) {
                if(this.pageNum > 1) {
                    this.pdfViewer.prevPage();
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