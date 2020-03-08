export class GlobalUi {

    sideBarStatus = localStorage.getItem('sideBarStatus');
    sideBar = $('#sidebar');
    sideBar2 = $('#sidebar2');
    sideBarLabels = $('.sidebar-label');
    content = $('#content');
    breadcrumb = $('#breadcrumb');
    inputFile = $(".custom-file-input");
    clickableRow = $(".clickable-row");
    autoHide = $('.auto-hide');

    constructor() {
        this.init();
    }

    init() {
        console.info("Starting global UI");
        $(document).on('click', e => this.closeUserMenu(e));
        $(document).on('click', e => this.scrollToHash());
        this.initSideBar();
        this.clickableRow.on('click',  e => this.gotoRowHref());
        this.inputFile.on('change', e => this.changeFileInputName(e));
    }

    gotoRowHref() {
        window.location = this.clickableRow.data("href");
    }

    changeFileInputName(e) {
        let inputFile = e.target;
        let fileName = inputFile.files[0].name;
        let label = $('label[for='+  inputFile.id  +']');
        label[0].innerHTML = fileName;
    }

    scrollToHash() {
        if(window.location.hash) {
            var element_to_scroll_to = document.getElementById(window.location.hash.substring(1));
            element_to_scroll_to.scrollIntoView();
        }
    }

    closeUserMenu(event) {
        var clickover = $(event.target);
        var _opened = $("#user-infos").hasClass("user-infos collapse show");
        if (_opened === true && !clickover.hasClass("user-toggle")) {
            $("#user-toggle").click();
        }
    }

    initSideBar() {
        $('#sidebarCollapse').on('click', e => this.toggleSideBarAction());
        if(this.sideBarStatus == null) {
            localStorage.setItem('sideBarStatus', 'off');
            this.sideBarStatus = localStorage.getItem('sideBarStatus');
        }

        if(this.sideBarStatus === 'off' && !this.sideBar.hasClass('active')) {
            this.toggleSideBar();
        }

        if(this.sideBarStatus === 'on' && this.sideBar.hasClass('active')) {
            this.toggleSideBar();
        }
        this.autoHide.on('mouseover', e => this.autoShowSideBar());
        this.autoHide.on('mouseout', e => this.hideSideBar());
    }

    toggleSideBarAction() {
        this.sideBarStatus = localStorage.getItem('sideBarStatus');
        this.toggleSideBar();
        if(this.sideBarStatus === 'on') {
            localStorage.setItem('sideBarStatus', 'off');
        } else {
            localStorage.setItem('sideBarStatus', 'on');
        }
    }

    toggleSideBar() {
        this.sideBar.toggleClass('active');
        this.sideBar2.toggleClass('d-none');
        this.sideBarLabels.toggleClass('d-none');
        this.content.toggleClass('content content2');
        this.breadcrumb.toggleClass('breadcrumb-nav breadcrumb-nav2');
    }

    autoShowSideBar() {
        this.sideBar.removeClass('active');
        this.sideBar2.removeClass('d-none');
        this.sideBarLabels.removeClass('d-none');
    }

    showSideBar() {
        this.sideBar.removeClass('active');
        this.sideBar2.removeClass('d-none');
        this.sideBarLabels.removeClass('d-none');
        this.content.addClass('content').removeClass('content2');
        this.breadcrumb.addClass('breadcrumb-nav').removeClass('breadcrumb-nav2');
    }

    hideSideBar() {
        if(localStorage.getItem('sideBarStatus') === 'off') {
            this.sideBar.addClass('active');
            this.sideBar2.addClass('d-none');
            this.sideBarLabels.addClass('d-none');
            this.content.removeClass('content').addClass('content2');
            this.breadcrumb.removeClass('breadcrumb-nav').addClass('breadcrumb-nav2');
        }
    }
}