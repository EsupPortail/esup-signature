import {PdfViewer} from "./pdfViewer.js";
import {SignPosition} from "./signPosition.js";
import {SignRequestParams} from "../prototypes/signRequestParams.js";

export class WorkspacePdf {

    signPageNumber = document.getElementById('signPageNumber');
    mode = 'read';
    signable;
    currentSignType;
    currentSignRequestParams;
    postits;
    signPosition;
    pdfViewer;

    constructor(url, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits, isPdf) {
        console.info("Starting workspace UI");
        this.currentSignRequestParams =  new SignRequestParams(currentSignRequestParams);
        this.currentSignType = currentSignType;
        this.postits = postits;
        this.signable = signable;
        if(signHeight != null) {
            this.signPosition = new SignPosition(this.currentSignRequestParams.xPos, this.currentSignRequestParams.yPos, signWidth, signHeight, this.signPageNumber);
            this.pdfViewer = new PdfViewer(url, this.signPosition);
        }
        this.initListeners();
    }

    initListeners() {
        this.pdfViewer.addEventListener('ready', e => this.initWorkspace());
        this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
        this.pdfViewer.addEventListener('pageChange', e => this.refreshComments());

        document.getElementById('commentButton').addEventListener('click', e => this.enableCommentMode());
        if(this.signable) {
            document.getElementById('signButton').addEventListener('click', e => this.enableSignMode());
            if(this.currentSignType !== "pdfImageStamp") {
                document.getElementById('visualButton').addEventListener('click', e => this.signPosition.toggleVisual());
            }
            document.getElementById('dateButton').addEventListener('click', e => this.signPosition.toggleDate());
        }
        document.getElementById('hideComment').addEventListener('click', e => this.hideComment());

        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e));

        this.pdfViewer.canvas.addEventListener('mouseup', e => this.clickAction());
        this.pdfViewer.canvas.addEventListener('mousemove', e => this.moveAction(e));
        this.postits.forEach((postit, index) => {
            let postitButton = $('#postit' + postit.id);
            postitButton.on('click', e => this.focusComment(postit));
        });
    }

    initWorkspace() {
        if(localStorage.getItem('mode') != null && localStorage.getItem('mode') !== "") {
            this.mode = localStorage.getItem('mode');
        } else {
            if(this.signable) {
                localStorage.setItem('mode', 'sign');
                this.mode = 'sign';
            } else {
                localStorage.setItem('mode', 'read');
                this.mode = 'read';
                $('#readButton').removeClass('d-none');
            }
        }
        console.info("init to " + this.mode + " mode");
        if(this.mode === 'sign') {
            this.enableSignMode();
        } else if(this.mode === 'comment') {
            this.enableCommentMode();
        } else if(this.mode === 'refuse') {
            this.enableRefuseMode();
        } else {
            this.enableSignMode();
        }

        if(this.signable && this.currentSignType === 'visa') {
            if(this.mode === 'sign') {
                this.signPosition.toggleVisual();
            }
        }
        this.refreshComments();
        this.signPosition.resetSign();
        this.pdfViewer.removeEventListener('ready');
    }

    refreshWorkspace() {
        this.signPosition.refreshSign(this.pdfViewer.scale / 0.75);
        this.refreshComments();
    }

    clickAction() {
        if(this.mode === 'sign') {
            this.signPosition.savePosition();
        } else if(this.mode === 'comment') {
            this.displayComment();
        }
    }

    moveAction(e) {
        console.debug('move');
        if(this.mode === 'sign') {
            this.signPosition.pointIt(e);
        } else if(this.mode === 'comment') {
            this.displayCommentPointer();
            this.signPosition.pointIt2(e);
        }
    }

    focusComment(postit) {
        this.pdfViewer.renderPage(postit.pageNumber)
        this.refreshComments();
    }

    refreshComments() {
        this.postits.forEach((postit, index) => {
            let postitDiv = $('#' + postit.id);
            let postitButton = $('#postit' + postit.id);
            if(postit.pageNumber === this.pdfViewer.pageNum && this.mode === 'comment') {
                postitDiv.show();
                postitDiv.css('left', postit.posX * this.pdfViewer.scale);
                postitDiv.css('top', postit.posY * this.pdfViewer.scale);
                postitDiv.width(postitDiv.width() * this.pdfViewer.scale);
                postitButton.css("background-color", "#FFC");
            } else {
                postitDiv.hide();
                postitButton.css("background-color", "#EEE");
            }
        });
    }

    getCommentPointer() {
        let pointerCanvas = document.createElement("canvas");
        pointerCanvas.width = 24;
        pointerCanvas.height = 24;
        let pointerCtx = pointerCanvas.getContext("2d");
        pointerCtx.fillStyle = "#000000";
        pointerCtx.font = "24px FontAwesome";
        pointerCtx.textAlign = "center";
        pointerCtx.textBaseline = "middle";
        pointerCtx.fillText("\uf075", 12, 12);
        return pointerCanvas.toDataURL('image/png');
    }

    displayCommentPointer() {
        this.pdfViewer.canvas.style.cursor = 'url(' + this.getCommentPointer() + '), auto';
    }

    displayComment() {
        if(this.mode !== 'comment') {
            return;
        }
        this.signPosition.pointItEnable = false;
        document.getElementById("postit").style.left = this.signPosition.posX + "px";
        document.getElementById("postit").style.top = this.signPosition.posY + "px";
        $("#postit").show();
        //document.getElementById("postit").style.display = "block";
    }

    hideComment() {
        if(this.mode !== 'comment') {
            return;
        }
        this.signPosition.pointItEnable = true;
        $("#postit").hide();
    }

    enableReadMode() {
        //$('#pdf').awesomeCursor('comment-alt');
        console.log("enable read mode");
        this.disableAllModes();
        this.mode = 'read';
        localStorage.setItem('mode', 'read');
        this.signPosition.pointItEnable = false;
        this.pdfViewer.scale = 1.75;
        $('#readButton').toggleClass('btn-light btn-secondary');
        $('#rotateleft').prop('disabled', false);
        $('#rotateright').prop('disabled', false);
        $('#stepscard').show();
        this.pdfViewer.renderPage(1);
    }

    enableCommentMode() {
        console.log("enable comments mode");
        this.disableAllModes();
        this.mode = 'comment';
        localStorage.setItem('mode', 'comment');
        this.signPosition.pointItEnable = true;
        this.pdfViewer.scale = 0.75;
        $('#workspace').toggleClass('alert-warning alert-secondary');
        $('#commentButton').toggleClass('btn-light btn-warning');
        $('#commentsTools').show();
        $('#infos').show();
        this.pdfViewer.renderPage(1);
    }

    enableSignMode() {
        console.log("enable sign mode");
        this.disableAllModes();
        this.mode = 'sign';
        localStorage.setItem('mode', 'sign');
        this.signPosition.pointItEnable = false;
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
        if(this.signPosition.visualActive) {
            $('#pen').removeClass('btn-outline-secondary').addClass('btn-outline-success');
            this.signPosition.cross.show();
        }
        this.pdfViewer.rotation = 0;
        this.pdfViewer.scale = 0.75;
        this.pdfViewer.renderPage(this.currentSignRequestParams.signPageNumber);

    }

    enableRefuseMode() {
        this.disableAllModes();
        this.mode = 'refuse';
        localStorage.setItem('mode', 'refuse');
        $('#workspace').toggleClass('alert-danger alert-secondary');
        $('#refuseButton').toggleClass('btn-light btn-danger');
        $('#refusetools').show();
        $('#infos').show();
        this.pdfViewer.renderPage(pageNum);
    }

    disableAllModes() {
        //this.mode = 'sign';
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
        console.debug("wheel event");
        if(event.ctrlKey === true) {
            if (this.detectMouseWheelDirection(event) === 'down'){
                console.debug("wheel down zoom out");
                this.pdfViewer.zoomOut();
            } else {
                console.debug("wheel up zoom in");
                this.pdfViewer.zoomIn();
            }
        } else {
            if (this.detectMouseWheelDirection(event) === 'down' && $(window).scrollTop() + $(window).height() === $(document).height()) {
                console.debug("wheel down next page");
                if(this.pdfViewer.pageNum < this.pdfViewer.pdfDoc.numPages) {
                    this.pdfViewer.nextPage();
                }
            } else if (this.detectMouseWheelDirection(event) === 'up' && window.scrollY === 0) {
                console.debug("wheel up prev page");
                if(this.pdfViewer.pageNum > 1) {
                    this.pdfViewer.prevPage();
                    window.scrollTo(0, document.body.scrollHeight);
                }
            }
        }
    }

    detectMouseWheelDirection(e) {
        let delta = null,
            direction = false;
        let e_delta = (e.deltaY || -e.wheelDelta || e.detail);
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