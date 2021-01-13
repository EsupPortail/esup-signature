import {PdfViewer} from "../../utils/PdfViewer.js";
import {SignPosition} from "./SignPosition.js";
import {SignRequestParams} from "../../../prototypes/SignRequestParams.js";
import {WheelDetector} from "../../utils/WheelDetector.js";

export class WorkspacePdf {

    constructor(isPdf, id, currentSignRequestParams, currentSignType, signable, postits, currentStepNumber, signImages, userName, signType, fields) {
        console.info("Starting workspace UI ");
        this.isPdf = isPdf;
        this.currentSignRequestParams =  [ new SignRequestParams(currentSignRequestParams) ];
        this.currentSignType = currentSignType;
        this.postits = postits;
        this.signable = signable;
        this.signRequestId = id;
        this.signPosition = new SignPosition(
            signType,
            this.currentSignRequestParams[0].xPos,
            this.currentSignRequestParams[0].yPos,
            this.currentSignRequestParams[0].signPageNumber,
            signImages,
            userName);
        if(this.isPdf) {
            this.pdfViewer = new PdfViewer('/user/signrequests/get-last-file/' + id, signable, currentStepNumber);
        }
        //this.signPageNumber = document.getElementById('signPageNumber');
        this.mode = 'sign';
        this.xmlHttpMain = new XMLHttpRequest();
        this.wheelDetector = new WheelDetector();
        this.initListeners();
        this.initDataFields(fields);
    }

    initListeners() {
        if(this.isPdf) {
            document.getElementById('prev').addEventListener('click', e => this.pdfViewer.prevPage());
            document.getElementById('next').addEventListener('click', e => this.pdfViewer.nextPage());
            document.getElementById('saveCommentButton').addEventListener('click', e => this.saveComment());
            this.signPosition.addEventListener("startDrag", e => this.hideAllPostits());
            this.signPosition.addEventListener("stopDrag", e => this.showAllPostits());
            this.pdfViewer.addEventListener('ready', e => this.initWorkspace());
            this.pdfViewer.addEventListener('scaleChange', e => this.refreshWorkspace());
            this.pdfViewer.addEventListener('pageChange', e => this.refreshAfterPageChange());
            this.pdfViewer.addEventListener('render', e => this.initForm());
            if (document.getElementById('commentModeButton') != null) {
                document.getElementById('commentModeButton').addEventListener('click', e => this.toggleCommentMode());
                if (this.signable) {
                    document.getElementById('signModeButton').addEventListener('click', e => this.toggleSignMode());
                    let visualButton = document.getElementById('visualButton')
                    if (this.currentSignType !== "pdfImageStamp") {
                        visualButton.classList.remove("d-none");
                        visualButton.addEventListener('click', e => this.signPosition.toggleVisual());
                    }
                }
                document.getElementById('hideComment').addEventListener('click', e => this.hideComment());
            }

            this.wheelDetector.addEventListener("zoomin", e => this.pdfViewer.zoomIn());
            this.wheelDetector.addEventListener("zoomout", e => this.pdfViewer.zoomOut());
            this.wheelDetector.addEventListener("pagetop", e => this.pageTop());
            this.wheelDetector.addEventListener("pagebottom", e => this.pageBottom());

            this.pdfViewer.canvas.addEventListener('mouseup', e => this.clickAction());

            // this.pdfViewer.canvas.addEventListener('mousemove', e => this.moveAction(e));
            $('#pdf').mousemove(e => this.moveAction(e));

            $(".postit-global-close").on('click', function () {
                $(this).parent().toggleClass("postit-small");
            });

            this.postits.forEach((postit, index) => {
                let postitButton = $('#postit' + postit.id);
                postitButton.on('click', e => this.focusComment(postit));
            });
        }
        $("#visaLaunchButton").on('click', e => this.launchSignModal());
        $("#signLaunchButton").on('click', e => this.launchSignModal());
        //$("#signForm").on('submit', e => this.validateForm(e));
    }

    initDataFields(fields) {
        if(this.pdfViewer) {
            this.pdfViewer.setDataFields(fields);
            if (this.pdfViewer.dataFields.length > 0 && this.pdfViewer.dataFields[0].defaultValue != null) {
                for (let i = 0 ; i < this.pdfViewer.dataFields.length ; i++) {
                    this.pdfViewer.savedFields.set(this.pdfViewer.dataFields[i].name, this.pdfViewer.dataFields[i].defaultValue);
                }
            }
        }
    }

    launchSignModal() {
        console.info("launch sign modal");
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
        $("#signForm :input").not(':input[type=button], :input[type=submit], :input[type=reset]').each(function(i, e) {
            console.debug("disable ");
            console.debug(e);
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

        // this.refreshAfterPageChange();
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
        this.refreshAfterPageChange();
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
        let commentUrlParams = "comment=" + document.getElementById("postitComment").value +
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
        this.refreshAfterPageChange();
    }

    refreshAfterPageChange() {
        console.debug("refresh comments and sign pos" + this.pdfViewer.pageNum);
        this.signPosition.getCurrentSignParams().signPageNumber = this.pdfViewer.pageNum;
        $("div[id^='sign_']").each((index, e) => this.toggleSign(e));
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

    toggleSign(e) {
        console.log("toggle sign_ " + $(e));
        let signId = $(e).attr("id").split("_")[1];
        let signRequestParams = this.signPosition.signRequestParamses[signId];
        console.log(signRequestParams.signPageNumber + " = " + this.signPosition.getCurrentSignParams().signPageNumber);
        if(signRequestParams.signPageNumber == this.signPosition.getCurrentSignParams().signPageNumber && this.mode === 'sign') {
            $(e).show();
        } else {
            $(e).hide();
        }

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
        $("#postitComment").removeAttr("disabled");
        $("#postit").show();
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
        localStorage.setItem('mode', 'read');
        this.signPosition.pointItEnable = false;
        this.pdfViewer.scale = 0.5;
        if(this.isFloat(localStorage.getItem('scale'))) {
            this.pdfViewer.scale = localStorage.getItem('scale');
        }
        $('#readModeButton').toggleClass('btn-outline-secondary');
        $('#rotateleft').prop('disabled', false);
        $('#rotateright').prop('disabled', false);
        this.pdfViewer.renderForm = false;
        this.pdfViewer.renderPage(1);
        this.showAllPostits();
    }

    isFloat(n){
        return Number(n) === n && n % 1 !== 0;
    }

    toggleCommentMode() {
        if(this.mode === 'comment') {
            this.enableSignMode();
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
        this.refreshAfterPageChange();
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
        $('#signModeButton').toggleClass('btn-outline-success');
        $('#signTools').show();

        $('#infos').show();
        if(this.signPosition.visualActive) {
            $('#pen').removeClass('btn-outline-secondary').addClass('btn-outline-success');
            this.signPosition.cross.show();
        }
        this.pdfViewer.rotation = 0;
        this.pdfViewer.renderPage(this.currentSignRequestParams[0].signPageNumber);
        this.signPosition.updateScale(this.pdfViewer.scale);
        //this.pdfViewer.promizeToggleFields(false);
        this.refreshAfterPageChange();
        this.showAllPostits();
    }

    disableAllModes() {
        //this.mode = 'sign';
        $('#workspace').removeClass('alert-danger').removeClass('alert-warning').removeClass('alert-success').addClass('alert-secondary');
        $('#commentModeButton').removeClass('btn-outline-warning');
        $('#signModeButton').removeClass('btn-outline-success');
        $('#readModeButton').removeClass('btn-outline-secondary');
        $('#signButtons').addClass('d-none');
        this.signPosition.crossTools.addClass('d-none');
        // $('#signZoomIn').addClass('d-none');
        // $('#signZoomOut').addClass('d-none');
        // $('#signNextImage').addClass('d-none');
        // $('#signPrevImage').addClass('d-none');
        $('#commentsTools').hide();

        $('#signTools').hide();
        this.signPosition.cross.hide();

        $('#infos').hide();
        $('#postit').hide();
        $('#refusetools').hide();
        $('#rotateleft').prop('disabled', true);
        $('#rotateright').prop('disabled', true);
        $('#pdf').css('cursor', 'default');

        this.hideAllPostits();
    }

    pageTop() {
        console.debug("prev page");
        if(this.pdfViewer.pageNum > 1) {
            this.pdfViewer.prevPage();
            window.scrollTo(0, document.body.scrollHeight);
        }
    }

    pageBottom() {
        console.debug("wheel down next page");
        if(this.pdfViewer.pageNum < this.pdfViewer.pdfDoc.numPages) {
            this.pdfViewer.nextPage();
        }
    }

    hideAllPostits() {
        $(".postit-global").each(function () {
            $(this).addClass("d-none");
        });
    }

    showAllPostits() {
        $(".postit-global").each(function () {
            $(this).removeClass("d-none");
        });
    }

}