import {default as SelectUser} from "./selectUser.js";

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
    }

    initListeners() {
        $('#sidebarCollapse').on('click', e => this.toggleSideBarAction());

        $("#closeUserInfo").on('click', function() {
            $("#user-toggle").click();
        });
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

        $(document).click(e => this.hideMenus(e));

        $('#returnButton').click(function () {
            window.history.back();
        });

        window.addEventListener('resize', e => this.adjustUi());
        $(document).ready(e => this.onDocumentLoad());
    }

    hideMenus(event) {
        var clickover = $(event.target);
        $("div[id^='menu-']").each(function() {
            var _opened = $(this).hasClass("collapse show");
            if (_opened === true && !clickover.hasClass("toggle-mini-menu")) {
                let id = $(this).attr('id').split('-')[1];
                $("#menu-toggle-" + id).click();
            }
        });
        var container = document.getElementsByClassName('user-infos')[0];
        var _opened = $("#user-infos").hasClass("collapse show");
        if (_opened === true && container !== event.target && !container.contains(event.target)) {
            $("#user-toggle").click();
        }
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

    changeFileInputName(e) {
        let inputFile = e.target;
        let fileName = inputFile.files[0].name;
        let label = $('label[for='+  inputFile.id  +']');
        label[0].innerHTML = fileName;
        console.info("change name to " + fileName);
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

    checkSelectUser() {
        $(".select-users").each(function () {
            let selectId = $(this).attr('id');
            console.info("auto enable select-user for : " + selectId);
            new SelectUser(selectId);
        });
    }

    checkSlimSelect() {
        $(".slim-select").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select for : " + selectName);
            new SlimSelect({
                select: '#' + selectName
            });
            $(this).addClass("slim-select-hack");
        })

        $(".slim-select-simple").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select-simple for : " + selectName);
            new SlimSelect({
                select: '#' + selectName,
                showSearch: false,
                searchHighlight: false,
                hideSelectedOption: true,
                closeOnSelect: true,
                ajax: function (search, callback) {
                    callback(false)
                }
            });
            $(this).addClass("slim-select-hack");
        })
    }

    enableSummerNote() {
        $('.summer-note').each(function() {
            console.info("auto enable summer note for " + $(this).attr('id'));
            $(this).summernote({
                tabsize: 2,
                height: 250,
                toolbar: [
                    ['style', ['style']],
                    ['font', ['bold', 'underline', 'clear']],
                    ['color', ['color']],
                    ['para', ['ul', 'ol', 'paragraph']],
                    ['table', ['table']],
                    ['insert', ['link', 'picture', 'video']],
                    ['view', ['fullscreen', 'codeview', 'help']]
                ]
            });
        });
    }

    displayToasts() {
        $('.toast').each(function() {
            console.info("display toast : " + $(this).attr('id'));
            $(this).toast('show');
        });
    }

    onDocumentLoad() {
        this.checkSelectUser();
        this.checkSlimSelect();
        this.enableSummerNote();
        this.adjustUi();
        this.displayToasts();
    }

}