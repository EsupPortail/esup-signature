import {PdfViewer} from "./pdfViewer.js";
import {SignPosition} from "./signPosition.js";
import {SignRequestParams} from "../prototypes/signRequestParams.js";

export class WorkspacePdf {

    constructor(id, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits, currentStepNumber, signImages) {
        console.info("Starting workspace UI");
        this.currentSignRequestParams =  new SignRequestParams(currentSignRequestParams);
        this.currentSignType = currentSignType;
        this.postits = postits;
        this.signable = signable;
        this.signRequestId = id;
        this.signPosition = new SignPosition(this.currentSignRequestParams.xPos, this.currentSignRequestParams.yPos, signWidth, signHeight, this.currentSignRequestParams.signPageNumber, signImages);
        this.pdfViewer = new PdfViewer('/user/signrequests/get-last-file/' + id, signable, currentStepNumber);
        //this.signPageNumber = document.getElementById('signPageNumber');
        this.mode = 'sign';
        this.xmlHttpMain = new XMLHttpRequest();
        this.initListeners();
    }

    initListeners() {
        this.pdfViewer.addEventListener('ready', e => this.initWorkspace());
        this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
        this.pdfViewer.addEventListener('pageChange', e => this.refreshComments());
        this.pdfViewer.addEventListener('render', e => this.initForm());
        document.getElementById('saveCommentButton').addEventListener('click', e => this.saveComment());
        document.getElementById('commentModeButton').addEventListener('click', e => this.toggleCommentMode());
        if(this.signable) {
            document.getElementById('signModeButton').addEventListener('click', e => this.toggleSignMode());
            if(this.currentSignType !== "pdfImageStamp") {
                document.getElementById('visualButton').addEventListener('click', e => this.signPosition.toggleVisual());
            }
            document.getElementById('dateButton').addEventListener('click', e => this.signPosition.toggleDate());
        }
        document.getElementById('hideComment').addEventListener('click', e => this.hideComment());

        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e));

        this.pdfViewer.canvas.addEventListener('mouseup', e => this.clickAction());

        // this.pdfViewer.canvas.addEventListener('mousemove', e => this.moveAction(e));
        $(document).mousemove(e => this.moveAction(e));

        this.postits.forEach((postit, index) => {
            let postitButton = $('#postit' + postit.id);
            postitButton.on('click', e => this.focusComment(postit));
        });
        console.log("init listener workspace");
        $("#visaLaunchButton").on('click', e => this.launchSignModal(e));
        $("#signLaunchButton").on('click', e => this.launchSignModal(e));
        //$("#signForm").on('submit', e => this.validateForm(e));
    }

    launchSignModal(e) {
        console.log("test form");
        if(WorkspacePdf.validateForm()) {
            $("#signModal").modal('toggle');
        }
    }

    static validateForm() {
        let valid = true;
        $("#signForm :input").each(function() {
            let input = $(this).get(0);
            if (!input.checkValidity()) {
                valid = false;
            }
        });
        if(!valid) {
            $("#checkDataSubmit").click();
        }
        return valid;
    }

    disableForm() {
        $("#signForm :input").each(function(i, e) {
            console.log("disable ");
            console.log(e);
            e.disabled = true;
        });
    }

    initWorkspace() {
        console.info("init workspace");
        if(localStorage.getItem('mode') == null) {
            localStorage.setItem('mode', this.mode);
        }
        console.info("init to " + this.mode + " mode");
        if(localStorage.getItem('mode') === 'comment') {
            this.enableCommentMode();
        } else {
            this.enableSignMode();
            if(this.signable && this.currentSignType === 'visa') {
                if(this.mode === 'sign') {
                    this.signPosition.toggleVisual();
                }
            }
        }

        // this.refreshComments();
        // if(this.signable) {
        //     this.signPosition.resetSign();
        // }
//        this.signPosition.updateSignSize(this.pdfViewer.scale);

        this.pdfViewer.adjustZoom();
        this.pdfViewer.removeEventListener('ready');

    }

    initForm(e) {
        console.info("init form");
        $("#signForm :input").each(function () {
            $(this).on('change', e => WorkspacePdf.launchValidate());
        });
        if(this.mode === 'read' || this.mode === 'comment') {
            this.disableForm();
        }

    }

    static launchValidate() {
        if(!WorkspacePdf.validateForm()) {
            $("#visaLaunchButton").attr('disabled', true);
        } else {
            $("#visaLaunchButton").attr('disabled', false);
        }

    }

    refreshWorkspace() {
        console.info("refresh workspace");
        this.signPosition.updateScale(this.pdfViewer.scale);
        this.refreshComments();
    }

    clickAction() {
        if(this.mode === 'sign') {
            this.signPosition.stopDragSignature();
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

    saveComment() {
        let csrf = document.getElementsByName("_csrf")[0];
        let commentUrlParams = "comment=" + document.getElementById("comment").value +
            "&commentPosX=" + document.getElementById("commentPosX").value +
            "&commentPosY=" + document.getElementById("commentPosY").value +
            "&commentPageNumber=" + document.getElementById("commentPageNumber").value +
            "&" + csrf.name + "=" + csrf.value;
        this.xmlHttpMain.addEventListener('readystatechange', function () {document.location.reload()});
        this.xmlHttpMain.open('POST', '/user/signrequests/comment/' + this.signRequestId, true);
        this.xmlHttpMain.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        // this.xmlHttpMain.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        this.xmlHttpMain.send(commentUrlParams);

    }

    focusComment(postit) {
        this.pdfViewer.renderPage(postit.pageNumber)
        this.refreshComments();
    }

    refreshComments() {
        console.debug("refresh comments " + this.pdfViewer.pageNum);
        this.signPosition.signPageNumber = this.pdfViewer.pageNum;
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
        document.getElementById("postit").style.left = $('#commentPosX').val() + "px";
        document.getElementById("postit").style.top = $('#commentPosY').val() + "px";
        $("#postit").show();
        $("#comment").removeAttr('disabled');
        $("#saveCommentButton").removeAttr('disabled');
    }

    hideComment() {
        if(this.mode !== 'comment') {
            return;
        }
        this.signPosition.pointItEnable = true;
        $("#postit").hide();
    }

    enableReadMode() {
        console.info("enable read mode");
        this.disableAllModes();
        this.mode = 'read';
        this.signPosition.pointItEnable = false;
        this.pdfViewer.scale = 1;
        $('#readModeButton').toggleClass('btn-outline-secondary');
        $('#rotateleft').prop('disabled', false);
        $('#rotateright').prop('disabled', false);
        this.pdfViewer.renderForm = false;
        this.pdfViewer.renderPage(1);
    }

    toggleCommentMode() {
        if(this.mode === 'comment') {
            this.enableReadMode();
            return;
        }
        this.enableCommentMode()
    }

    enableCommentMode() {
        console.info("enable comments mode");
        localStorage.setItem('mode', 'comment');
        this.disableAllModes();
        this.mode = 'comment';
        this.signPosition.pointItEnable = true;
        $('#workspace').toggleClass('alert-warning alert-secondary');
        $('#commentModeButton').toggleClass('btn-outline-warning');
        $('#commentsTools').show();
        $('#infos').show();
        this.pdfViewer.renderPage(1);
        this.pdfViewer.promizeToggleFields(false);
        this.refreshComments();
    }

    toggleSignMode() {
        if(this.mode === 'sign') {
            this.enableReadMode();
            return;
        }
        this.enableSignMode();
    }

    enableSignMode() {
        console.info("enable sign mode");
        localStorage.setItem('mode', 'sign');
        this.disableAllModes();
        this.mode = 'sign';
        this.signPosition.pointItEnable = false;
        $('#workspace').toggleClass('alert-success alert-secondary');
        $(".circle").each(function( index ) {
            $(this).hide();
        });
        $('#signButtons').removeClass('d-none');
        $('#signZoomIn').removeClass('d-none');
        $('#signZoomOut').removeClass('d-none');
        $('#signNextImage').removeClass('d-none');
        $('#signPrevImage').removeClass('d-none');
        $('#signModeButton').toggleClass('btn-outline-success');
        $('#signTools').show();

        $('#infos').show();
        if(this.signPosition.visualActive) {
            $('#pen').removeClass('btn-outline-secondary').addClass('btn-outline-success');
            this.signPosition.cross.show();
        }
        this.pdfViewer.rotation = 0;
        this.pdfViewer.renderPage(this.currentSignRequestParams.signPageNumber);
        this.signPosition.updateScale(this.pdfViewer.scale);
        //this.pdfViewer.promizeToggleFields(false);
    }

    disableAllModes() {
        //this.mode = 'sign';
        $('#workspace').removeClass('alert-danger').removeClass('alert-warning').removeClass('alert-success').addClass('alert-secondary');
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        $('#signButtons').addClass('d-none');
        $('#signZoomIn').addClass('d-none');
        $('#signZoomOut').addClass('d-none');
        $('#signNextImage').addClass('d-none');
        $('#signPrevImage').addClass('d-none');
        $('#commentsTools').hide();

        $('#signTools').hide();
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