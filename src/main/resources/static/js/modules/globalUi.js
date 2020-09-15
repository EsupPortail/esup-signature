export class GlobalUi {

    constructor() {
        console.info("Starting global UI");
        this.sideBarStatus = localStorage.getItem('sideBarStatus');
        this.sideBar = $('#sidebar');
        this.sideBar2 = $('#sidebar2');
        this.sideBarLabels = $('.sidebar-label');
        this.content = $('#content');
        this.breadcrumb = $('#breadcrumb');
        this.inputFile = $(".custom-file-input");
        this.clickableRow = $(".clickable-row");
        this.clickableTd = $(".clickable-td");
        this.autoHide = $('.auto-hide');
        this.initListeners();
        this.initSideBar();
        this.adjustUi();
    }

    initListeners() {
        $('#sidebarCollapse').on('click', e => this.toggleSideBarAction());

        $("#closeUserInfo").on('click', function() {
            $("#user-toggle").click();
        });
        this.sideBar.on('mouseover', e => this.disableBodyScroll());
        this.sideBar.on('mouseout', e => this.enableBodyScroll());
        this.clickableRow.on('click',  function() {
            window.location = $(this).closest('tr').attr('data-href');
        });
        this.clickableTd.on('click',  function() {
            window.location = $(this).closest('tr').attr('data-href');
        });
        this.inputFile.on('change', e => this.changeFileInputName(e));
        let delay = 0;
        let offset = 300;

        document.addEventListener('invalid', function(e){
            $(e.target).addClass("invalid");
            $('html, body').animate({scrollTop: $($(".invalid")[0]).offset().top - offset }, delay);
        }, true);

        $(document).click(function (event) {
            var container = document.getElementsByClassName('user-infos')[0];
            var _opened = $("#user-infos").hasClass("collapse show");
            if (_opened === true && container !== event.target && !container.contains(event.target)) {
                $("#user-toggle").click();
            }
        });

        $('#returnButton').click(function () {
            window.history.back();
        });
        window.addEventListener('resize', e => this.adjustUi());
        $(document).ready(e => this.adjustUi());
    }

    resetMode() {
        localStorage.setItem('mode', 'sign');
    }

    adjustUi() {
        if (window.innerWidth < 992) {
            this.hideSideBar();
        } else {
            if(this.sideBarStatus === 'on') {
                this.showSideBar();
            }
        }
    }

    disableSideBarButton() {
        console.debug("disable side button");
        $("#sidebarCollapse").attr("disabled", true).addClass('d-none');

        $('#returnButton').removeClass('d-none');
    }

    disableBodyScroll() {
        //$('body').addClass('disable-body-scrollbar');
    }

    enableBodyScroll() {
        //$('body').removeClass('disable-body-scrollbar');
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
        let clickover = $(event.target);
        let _opened = $("#user-infos").hasClass("show");
        if (_opened === true && !clickover.hasClass("user-toggle")) {
            $("#user-toggle").click();
        }
    }

    initSideBar() {
        console.info("init side bar : " + this.sideBarStatus);
        if(this.sideBarStatus == null) {
            localStorage.setItem('sideBarStatus', 'on');
            this.sideBarStatus = localStorage.getItem('sideBarStatus');
        }
        if(this.sideBarStatus === 'off' && !this.sideBar.hasClass('active')) {
            this.hideSideBar();
        }

        if(this.sideBarStatus === 'on' && this.sideBar.hasClass('active')) {
            this.toggleSideBar();
        }
        // this.autoHide.on('mouseover', e => this.autoShowSideBar());
        // this.autoHide.on('mouseout', e => this.hideSideBar());
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
        console.info("toggle side bar");
        this.sideBar.toggleClass('active');
        this.sideBar2.toggleClass('d-none');
        this.sideBarLabels.toggleClass('d-none');
        this.content.toggleClass('content-full');
        this.breadcrumb.toggleClass('breadcrumb-nav-full');
    }

    autoShowSideBar() {
        this.sideBar.removeClass('active');
        this.sideBar2.removeClass('d-none');
        this.sideBarLabels.removeClass('d-none');
    }

    showSideBar() {
        console.debug("show side");
        this.sideBar.removeClass('active');
        this.sideBar2.removeClass('d-none');
        this.sideBarLabels.removeClass('d-none');
        this.content.removeClass('content-full');
        this.breadcrumb.removeClass('breadcrumb-nav-full');
    }

    hideSideBar() {
        console.debug("hide side");
        this.sideBar.addClass('active');
        this.sideBar2.addClass('d-none');
        this.sideBarLabels.addClass('d-none');
        this.content.addClass('content-full');
        this.breadcrumb.addClass('breadcrumb-nav-full');
    }

}