export class PdfNavigationController {

    constructor(viewer) {
        this.viewer = viewer;
    }

    getWorkspaceElement() {
        return document.getElementById('workspace');
    }

    getScrollTop() {
        const workspace = this.getWorkspaceElement();
        return workspace ? workspace.scrollTop : window.scrollY;
    }

    getViewportHeight() {
        const workspace = this.getWorkspaceElement();
        return workspace ? workspace.clientHeight : window.innerHeight;
    }

    getScrollLeft() {
        const workspace = this.getWorkspaceElement();
        return workspace ? workspace.scrollLeft : window.scrollX;
    }

    scrollToPosition(top, behavior = 'auto') {
        const workspace = this.getWorkspaceElement();
        if (workspace) {
            workspace.scrollTo({
                top: Math.max(0, top),
                left: 0,
                behavior: behavior,
            });
            return;
        }
        window.scrollTo({
            top: Math.max(0, top),
            left: 0,
            behavior: behavior,
        });
    }

    animateScrollToPosition(top) {
        const targetTop = Math.max(0, top);
        const workspace = this.getWorkspaceElement();
        if (workspace) {
            $(workspace).stop().animate({
                scrollTop: targetTop
            }, 100);
            return;
        }
        $('html, body').stop().animate({
            scrollTop: targetTop
        }, 100);
    }

    getPageRelativeTop(pageNum) {
        const page = $("#page_" + pageNum);
        if (!page.length) {
            return 0;
        }
        const firstPage = $("#page_1");
        const pageTop = page.position()?.top ?? 0;
        if (!firstPage.length) {
            return Math.round(pageTop);
        }
        const firstPageTop = firstPage.position()?.top ?? 0;
        return Math.round(pageTop - firstPageTop);
    }

    getPageTopInPdf(pageNum) {
        const page = $("#page_" + pageNum);
        if (!page.length) {
            return 0;
        }
        return Math.round(page.position()?.top ?? 0);
    }

    getPageLeftInPdf(pageNum) {
        const page = $("#page_" + pageNum);
        if (!page.length) {
            return 0;
        }
        return Math.round(page.position()?.left ?? 0);
    }

    getVisiblePages() {
        const visiblePages = [];
        const scrollTop = this.getScrollTop();
        const scrollBottom = scrollTop + this.getViewportHeight();

        for (let i = 1; i <= this.viewer.numPages; i++) {
            const pageElement = document.getElementById(`page_${i}`);
            if (pageElement) {
                const elementTop = this.getPageRelativeTop(i);
                const elementBottom = elementTop + pageElement.offsetHeight;
                if (elementBottom > scrollTop && elementTop < scrollBottom) {
                    visiblePages.push(i);
                }
            }
        }
        return visiblePages;
    }

    restoreScrolling() {
        let newScrolling = Math.round(this.viewer.saveScrolling * this.viewer.scale);
        this.scrollToPosition(newScrolling);
    }

    checkCurrentPage(e) {
        let numPages = this.viewer.pdfDoc.numPages;

        for(let i = 1; i < numPages + 1; i++) {
            let pagePos = this.getPageRelativeTop(i);

            if(e > pagePos - 250) {
                this.viewer.pageNum = i;
                document.getElementById('page_num').value = this.viewer.pageNum;
                if((this.viewer.pageNum === this.viewer.numPages || this.viewer.numPages === 1) && !this.viewer.viewed) {
                    this.viewer.viewed = true;
                    this.viewer.fireEvent("reachEnd", ['ok'])
                }
            }
        }
    }

    scrollToPage(num) {
        let page = $("#page_" + num);
        if(page.length) {
            let scrollTo = this.getPageRelativeTop(num);
            this.animateScrollToPosition(scrollTo);
        }
    }

    prevPage() {
        this.viewer.fireEvent("beforeChange", ['prev']);
        if (!this.isFirstPage()) {
            this.viewer.pageNum--;
        }
        this.scrollToPage(this.viewer.pageNum);
        return true;
    }

    nextPage() {
        if (this.isLastPage()) {
            return false;
        }
        this.viewer.pageNum++;
        this.scrollToPage(this.viewer.pageNum);
        return true;
    }

    isFirstPage() {
        return this.viewer.pageNum <= 1;
    }

    isLastPage() {
        return this.viewer.pageNum >= this.viewer.numPages;
    }

    focusField(field) {
        if(field.attr("type") === "radio") {
            this.highlightRadio(field);
        }
        field.focus();
        let offset = field.offset();
        if(offset != null) {
            const workspace = this.getWorkspaceElement();
            if (workspace) {
                const workspaceOffset = $(workspace).offset();
                if (workspaceOffset != null) {
                    $(workspace).animate({
                        scrollTop: offset.top - workspaceOffset.top + workspace.scrollTop - 170,
                        scrollLeft: 0
                    });
                    return;
                }
            }
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
}
