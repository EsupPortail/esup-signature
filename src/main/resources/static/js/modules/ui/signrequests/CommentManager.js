export class CommentManager {

    constructor(state, options = {}) {
        this.state = state;
        this.positionLocked = false;
        this.options = {
            eventNamespace: options.eventNamespace ?? ".commentManager",
            postitNamespace: options.postitNamespace ?? ".commentManagerPostit",
            commentDialogNamespace: options.commentDialogNamespace ?? ".commentManagerDialog",
            getPdfViewer: options.getPdfViewer ?? (() => null),
            getComments: options.getComments ?? (() => []),
            isEditable: options.isEditable ?? (() => false),
            isDisplayComments: options.isDisplayComments ?? (() => false),
            setDisplayComments: options.setDisplayComments ?? (() => {}),
            isAddSpotEnabled: options.isAddSpotEnabled ?? (() => false),
            setAddSpotEnabled: options.setAddSpotEnabled ?? (() => {}),
            isAddCommentEnabled: options.isAddCommentEnabled ?? (() => false),
            setAddCommentEnabled: options.setAddCommentEnabled ?? (() => {}),
            setToolsDisabled: options.setToolsDisabled ?? (() => {}),
            setSignSpacesDroppableEnabled: options.setSignSpacesDroppableEnabled ?? (() => {}),
            setCommentAddButtonsState: options.setCommentAddButtonsState ?? (() => {}),
            lockSigns: options.lockSigns ?? (() => {}),
            setPointItEnabled: options.setPointItEnabled ?? (() => {}),
            selectChangeMode: options.selectChangeMode ?? (() => {}),
            showAllPostits: options.showAllPostits ?? (() => {}),
            hideAllPostits: options.hideAllPostits ?? (() => {}),
            reloadPage: options.reloadPage ?? (() => document.location.reload()),
            restoreAddSpotButton: options.restoreAddSpotButton ?? (() => {}),
            signRequestId: options.signRequestId ?? (() => null),
            isOtp: options.isOtp ?? (() => false),
            csrf: options.csrf ?? (() => null),
            status: options.status ?? (() => null)
        };
    }

    bind(eventNamespace) {
        $(".toggleComments").each((_, el) => {
            $(el).off("click" + eventNamespace).on("click" + eventNamespace, () => {
                if (this.options.isDisplayComments()) {
                    this.hideComments();
                } else {
                    this.showComments();
                }
            });
        });

        const comments = this.options.getComments();
        if (!Array.isArray(comments)) {
            return;
        }
        comments.forEach(postit => {
            let postitButton = $('#annotation_' + postit.id);
            postitButton.off('click' + eventNamespace).on('click' + eventNamespace, () => this.focusComment(postit));
            postitButton.off('mouseover' + eventNamespace).on('mouseover' + eventNamespace, function () {
                $('#inDocComment_' + postit.id).addClass('circle-background');
                postitButton.addClass('circle-border');
            });
            postitButton.off('mouseout' + eventNamespace).on('mouseout' + eventNamespace, function () {
                $('#inDocComment_' + postit.id).removeClass('circle-background');
                postitButton.removeClass('circle-border');
            });
        });
    }

    pointIt2(e) {
        const pdfViewer = this.options.getPdfViewer();
        let target = e.target;
        let page = $(target).closest('.pdf-page');
        if (!page.length) {
            page = $("#page_" + pdfViewer.pageNum);
        }
        if (!page.length) {
            return;
        }

        let pageNumber = page.attr("page-num") || page.attr("id")?.split("_")[1] || pdfViewer.pageNum;
        const pageRect = page.get(0).getBoundingClientRect();
        $('#commentPageNumber').val(pageNumber);
        let xPos = Math.round(e.clientX - pageRect.left);
        let yPos = Math.round(e.clientY - pageRect.top);
        $("#commentPosX").val(xPos);
        $('#commentPosY').val(yPos);
        console.debug("debug - mouse pos : " + xPos + ", " + yPos);
    }

    saveComment() {
        this.positionLocked = true;
        const addSpotEnabled = this.options.isAddSpotEnabled();
        let spotStepNumberVal = $("#spotStepNumber");
        if (addSpotEnabled && spotStepNumberVal.val() === "") {
            spotStepNumberVal.attr("required", true);
            $("#submitPostit").click();
            return;
        }
        let postitComment = $("#postitComment");
        if (!addSpotEnabled && postitComment.val() === '') {
            $("#submitPostit").click();
            return;
        }
        let xPos = parseInt($("#commentPosX").val());
        let yPos = parseInt($("#commentPosY").val());
        let spotStepNumber = "";
        if(addSpotEnabled) {
            spotStepNumber = spotStepNumberVal.val();
        }
        const csrf = this.options.csrf();
        let commentUrlParams =
            "comment=" + encodeURIComponent(postitComment.val()) +
            "&commentPosX=" + Math.round(xPos) +
            "&commentPosY=" + Math.round(yPos) +
            "&commentPageNumber=" + $("#commentPageNumber").val() +
            "&spotStepNumber=" + spotStepNumber +
            "&" + csrf.parameterName + "=" + csrf.token;
        let postitDiv = $("#postit");
        if(postitDiv.length) {
            postitDiv.html("<div class=\"spinner-border\" role=\"status\">\n" +
                "  <span class=\"visually-hidden\">Enregistrement</span>\n" +
                "</div>");
        }
        let mode = this.options.isOtp() ? "otp" : "user";
        $.ajax({
            method: 'POST',
            url: "/" + mode + "/signrequests/comment/" + this.options.signRequestId() + "?" + commentUrlParams,
            success: () => this.options.reloadPage()
        });
    }

    focusComment(postit) {
        this.refresh();
        this.options.getPdfViewer().animateScrollToPosition(parseInt($('#inDocComment_' + postit.id).css('top').replace('px', ''), 10));
    }

    getPageOffsets(pageNum) {
        const pdfViewer = this.options.getPdfViewer();
        const normalizedPage = Number.parseInt(pageNum, 10) || 1;
        const page = $("#page_" + normalizedPage);
        if (page.length) {
            return {
                top: Math.round(page.position()?.top ?? 0),
                left: Math.round(page.position()?.left ?? 0)
            };
        }
        return {
            top: pdfViewer.getPageTopInPdf(normalizedPage),
            left: pdfViewer.getPageLeftInPdf(normalizedPage)
        };
    }

    getCommentDialogCssPosition(pageNum, renderedX, renderedY) {
        const normalizedPage = Number.parseInt(pageNum, 10) || 1;
        const page = $("#page_" + normalizedPage);
        const commentDiv = $("#comment-div");
        if (!page.length || !commentDiv.length) {
            return {
                left: Math.round(renderedX || 0),
                top: Math.round(renderedY || 0)
            };
        }
        const pageOffset = page.offset() || {left: 0, top: 0};
        const parentOffset = commentDiv.offsetParent().offset() || {left: 0, top: 0};
        return {
            left: Math.round(pageOffset.left - parentOffset.left + (renderedX || 0)),
            top: Math.round(pageOffset.top - parentOffset.top + (renderedY || 0))
        };
    }

    refresh() {
        const pdfViewer = this.options.getPdfViewer();
        const comments = this.options.getComments();
        const editable = this.options.isEditable();
        const displayComments = this.options.isDisplayComments();
        const postitNamespace = this.options.postitNamespace;
        const status = this.options.status();
        const csrf = this.options.csrf();
        const signRequestId = this.options.signRequestId();

        if (Array.isArray(comments)) {
            comments.forEach(comment => {
                if(comment.stepNumber == null) {
                    let postitDiv = $('#inDocComment_' + comment.id);
                    let postitButton = $('#annotation_' + comment.id);
                    if (editable || displayComments) {
                        postitDiv.show();
                        const pageOffsets = this.getPageOffsets(comment.pageNumber);
                        postitDiv.css('left', ((parseInt(comment.posX) * pdfViewer.scale) + pageOffsets.left) + "px");
                        postitDiv.css('top', ((parseInt(comment.posY) * pdfViewer.scale) - 48 + pageOffsets.top) + "px");
                        postitDiv.width(postitDiv.width() * pdfViewer.scale);
                        postitButton.css("background-color", "var(--bs-warning-bg-subtle)");
                        postitDiv.off('mouseup' + postitNamespace);
                        if(status === "draft" || status === "pending") {
                            let deletable = postitDiv.attr('es-comment-delete') === "true";
                            let buttons = {
                                cancel: {
                                    label: 'Fermer',
                                    className: 'btn-secondary',
                                    callback: function () {
                                    }
                                }
                            };
                            if(deletable) {
                                buttons = {
                                    confirm: {
                                        label: 'Supprimer',
                                        className: 'btn-danger',
                                        callback: () => {
                                            bootbox.confirm('Confirmer la suppression', result2 => {
                                                if(result2) {
                                                    $.ajax({
                                                        method: 'DELETE',
                                                        url: "/ws-secure/global/delete-comment/" + signRequestId + "/" + comment.id + "?" + csrf.parameterName + "=" + csrf.token,
                                                        success: () => {
                                                            this.options.reloadPage();
                                                            this.options.restoreAddSpotButton();
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    },
                                    cancel: {
                                        label: 'Fermer',
                                        className: 'btn-secondary',
                                        callback: function () {
                                        }
                                    }
                                };
                            }
                            postitDiv.on('mouseup' + postitNamespace, e => {
                                e.stopPropagation();
                                bootbox.dialog({
                                    title: postitDiv.attr("es-comment-title"),
                                    message: postitDiv.attr("es-comment-text"),
                                    buttons: buttons
                                }).find('.modal-content').css({'background-color': 'var(--bs-warning-bg-subtle)'});
                            });
                        }

                    } else {
                        postitDiv.hide();
                        postitButton.css("background-color", "var(--color-e9ecef)");
                        postitDiv.off('mouseup' + postitNamespace);
                    }
                }
            });
        }

        let postitForm = $("#comment-div");
        if (postitForm.is(':visible')) {
            const commentPageNumber = $("#commentPageNumber").val();
            const renderedX = (parseInt($("#commentPosX").val(), 10) || 0) * pdfViewer.scale;
            const renderedY = (parseInt($("#commentPosY").val(), 10) || 0) * pdfViewer.scale;
            const cssPos = this.getCommentDialogCssPosition(commentPageNumber, renderedX, renderedY);
            postitForm.css('left', cssPos.left + "px");
            postitForm.css('top', cssPos.top + "px");
            $("#comment-div :input").each(function () {
                $(this).removeAttr('disabled');
            });
            postitForm.children('select[name="spotStepNumber"]').each(function () {
                $(this).removeAttr('disabled');
            });
        }
    }

    displayDialogBox() {
        const pdfViewer = this.options.getPdfViewer();
        this.positionLocked = true;
        $('#pdf').off('mousemove' + this.options.commentDialogNamespace);
        let comment = $("#comment-div");
        if (comment.is(':visible')) {
            return;
        }
        this.options.setPointItEnabled(false);
        $('#pdf').css('cursor', 'default');
        let commentPosX = $("#commentPosX");
        let commentPosY = $('#commentPosY');
        let commentPageNumber = $("#commentPageNumber").val();
        const clickedRenderedX = parseInt(commentPosX.val(), 10) || 0;
        const clickedRenderedY = parseInt(commentPosY.val(), 10) || 0;
        let xPos = clickedRenderedX / pdfViewer.scale;
        let yPos = clickedRenderedY / pdfViewer.scale;
        commentPosX.val(Math.round(xPos));
        commentPosY.val(Math.round(yPos));
        const cssPos = this.getCommentDialogCssPosition(commentPageNumber, clickedRenderedX, clickedRenderedY);
        comment.css('left', cssPos.left + "px");
        comment.css('top', cssPos.top + "px");
        $("#postitComment").removeAttr("disabled");
        $("#spotStepNumber").removeAttr("disabled");
        comment.show();
        this.options.lockSigns();
    }

    hideComment(e) {
        e.stopPropagation();
        this.exitCommentAddMode();
    }

    showComments() {
        $("#postit").removeClass("d-none");
        $("#commentHelp").removeClass("d-none");
        this.options.setDisplayComments(true);
        this.options.setPointItEnabled(true);
        this.options.selectChangeMode("comment");
        $('#commentsBar').show();
        this.refresh();
        $(".circle").each(function () {
            $(this).show();
            $(this).css('width', '0px');
        });
        this.options.showAllPostits();
    }

    hideComments() {
        $("#postit").addClass("d-none");
        $("#commentHelp").addClass("d-none");
        this.options.setDisplayComments(false);
        this.options.setPointItEnabled(false);
        this.options.selectChangeMode(null);
        $('#commentsBar').hide();
        $(".circle").each(function () {
            $(this).hide();
            $(this).css('width', '');
        });
        this.options.hideAllPostits();
    }

    activateAddCommentMode() {
        let postit = $("#postit");
        postit.removeClass("alert-success");
        postit.addClass("alert-warning");
        this.options.setAddCommentEnabled(true);
        const png = '/images/icons/rr-comment-32.png';
        $('body *').css('pointer-events', 'none');
        this.options.getPdfViewer().pdfDiv.css({
            'pointer-events': 'auto',
            'cursor': `url("${png}"), auto`
        });
        $("#addCommentButton").css('pointer-events', 'auto');
        $("#comment-div, #comment-div *").css('pointer-events', 'auto');
        $("#divSpotStepNumber").hide();
        $("#postitComment").attr("required", true);
        $(".textLayer").each(function () {
            $(this).addClass("text-disable-selection");
        });
    }

    deactivateAddCommentMode() {
        this.options.setAddCommentEnabled(false);
        this.options.getPdfViewer().pdfDiv.css('cursor', 'default');
        $('body *').css('pointer-events', 'auto');
        $("#divSpotStepNumber").show();
        $(".textLayer").each(function () {
            $(this).removeClass("text-disable-selection");
        });
    }

    disableAddComment() {
        return this.deactivateAddCommentMode();
    }

    enableAddComment() {
        return this.activateAddCommentMode();
    }

    enableCommentAdd() {
        this.positionLocked = false;
        if (this.options.isAddCommentEnabled()) {
            this.exitCommentAddMode();
            return;
        }

        this.exitCommentAddMode();
        this.options.setToolsDisabled(true);
        this.options.setSignSpacesDroppableEnabled(false);
        this.activateAddCommentMode();
        this.options.setAddSpotEnabled(false);

        let last = 0;
        $('#pdf')
            .off(this.options.commentDialogNamespace)
            .on('click' + this.options.commentDialogNamespace, e => this.handlePdfClick(e))
            .on('mousemove' + this.options.commentDialogNamespace, e => {
                const now = performance.now();
                if (now - last < 50) return;
                last = now;
                this.handlePdfMove(e);
            });

        $(document)
            .off('keydown' + this.options.commentDialogNamespace)
            .on('keydown' + this.options.commentDialogNamespace, e => {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    this.exitCommentAddMode();
                }
            });

        this.options.setCommentAddButtonsState(true);

        $('#saveCommentButton')
            .off('click' + this.options.commentDialogNamespace)
            .on('click' + this.options.commentDialogNamespace, () => this.saveComment());
        $('#hideCommentButton')
            .off('click' + this.options.commentDialogNamespace)
            .on('click' + this.options.commentDialogNamespace, e => this.hideComment(e));
    }

    exitCommentAddMode() {
        this.positionLocked = false;
        $('#pdf').off(this.options.commentDialogNamespace);
        $(document).off('keydown' + this.options.commentDialogNamespace);
        $('#saveCommentButton').off('click' + this.options.commentDialogNamespace);
        $('#hideCommentButton').off('click' + this.options.commentDialogNamespace);
        $("#comment-div").hide();
        this.options.setToolsDisabled(false);
        this.options.setSignSpacesDroppableEnabled(true);
        this.deactivateAddCommentMode();
        this.options.setAddSpotEnabled(false);
        this.options.setCommentAddButtonsState(false);
    }

    handlePdfClick(e) {
        if (this.positionLocked) {
            return;
        }
        this.options.lockSigns();
        if (this.options.isAddSpotEnabled() || this.options.isAddCommentEnabled()) {
            this.displayDialogBox();
        }
    }

    handlePdfMove(e) {
        if (this.positionLocked) {
            return;
        }
        if (this.options.isAddSpotEnabled() || this.options.isAddCommentEnabled()) {
            this.pointIt2(e);
        }
    }

    destroy() {
        $('#pdf').off(this.options.commentDialogNamespace);
        $(document).off('keydown' + this.options.commentDialogNamespace);
        $('#saveCommentButton').off('click' + this.options.commentDialogNamespace);
        $('#hideCommentButton').off('click' + this.options.commentDialogNamespace);
        $(".toggleComments").off("click" + this.options.eventNamespace);
    }

}




