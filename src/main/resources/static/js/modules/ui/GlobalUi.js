import {default as SelectUser} from "../utils/SelectUser.js";
import {SseSubscribe} from "../utils/SseSubscribe.js";

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
        this.markAsReadButtons = $('button[id^="markAsReadButton_"]');
        this.markHelpAsReadButtons = $('button[id^="markHelpAsReadButton_"]');
        this.initListeners();
        this.initSideBar();
        this.checkCurrentPage();
        this.sseSubscribe = new SseSubscribe();
    }

    initListeners() {
        window.onerror = function (msg, url, lineNo, columnNo, error) {
            var clientSideError = {
                msg: msg,
                url: url,
                lineNumber: lineNo,
                columnNumber: columnNo,
                error: error
            };

            $.ajax({
                type: 'POST',
                contentType : 'application/json; charset=utf-8',
                url: "/log",
                dataType: "json",
                data: JSON.stringify(clientSideError)
            });
            return false;
        };

        this.markAsReadButtons.each((index, e) => this.listenMarkAsReadButton(e));
        this.markHelpAsReadButtons.each((index, e) => this.listenHelpMarkAsReadButton(e));
        $('#sidebarCollapse').unbind('click').on('click', e => this.toggleSideBarAction());

        $("#closeUserInfo").on('click', function() {
            $("#user-toggle").click();
        });
        this.clickableRow.on('click',  function() {
            window.location = $(this).closest('tr').attr('data-href');
        });
        this.clickableTd.on('click',  function() {
            let test = false;
            $(".card").each(function (index, e) {
                if(e.classList.contains("show")) {
                    test = true;
                }
            });
            if(!test) {
                window.location = $(this).closest('tr').attr('data-href');
            }
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

        $("#sendPendingButton").on('click', e => this.showSendPendingModal());
        $("#submitSendPending").on('click', e => this.submitSendPending());

        $(window).bind('keydown', function(event) {
            if (event.ctrlKey || event.metaKey) {
                switch (String.fromCharCode(event.which).toLowerCase()) {
                    case 's':
                        event.preventDefault();
                        let saveButton = $("#saveButton");
                        if(saveButton) {
                            saveButton.click();
                        }
                        break;
                }
            }
        });
    }

    submitSendPending() {
        $("#pending").val(true);
        $("#sendSignRequestForm").submit();
    }

    showSendPendingModal() {
        $('#sendPending').modal('toggle');
        $('#sendSignRequestModal').modal('toggle');
    }

    checkCurrentPage() {
        let url = window.location.pathname;
        if(!url.match("/user/signrequests/+[\\w\\W]+")) {
            this.resetMode();
        }
    }

    listenHelpMarkAsReadButton(btn) {
        console.debug("listen to" + btn);
        $(btn).on('click', e => this.markHelpAsRead(e));
    }

    markHelpAsRead(e) {
        let id = e.target.id.split('_')[1];
        console.info("mark help as read message " + id);
        $.get("/user/users/mark-help-as-read/" + id);
    }

    listenMarkAsReadButton(btn) {
        console.debug("listen to" + btn);
        $(btn).on('click', e => this.markAsRead(e));
    }

    markAsRead(e) {
        let id = e.target.id.split('_')[1];
        console.info("mark as read message " + id);
        $.get("/user/users/mark-as-read/" + id);
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
            console.info("auto adjust : hide");
            this.hideSideBar();
        } else {
            console.info("auto adjust : display");
            let url = window.location.pathname;
            console.info("auto display side bar : " + url);
            if(this.sideBarStatus === 'on') {
                this.showSideBar();
            } else {
                this.hideSideBar();
            }
            if(!url.match("/user/users+[\\w\\W]+") && !url.match("/admin/+[\\w\\W]+") && !url.match("^/user/$") && !url.match("^/user/signrequests$") && !url.match("/user/signrequests/+[\\w\\W]+")) {
                console.info("auto display side bar : show");
                this.hideSideBar();
                this.disableSideBarButton();
            }
            if(url.match("^/user/signrequests$") || url.match("^/user/signrequests/$") || url.match("/user/signrequests/+[\\w\\W]+")) {
                console.info("auto display side bar : hide");
                this.showSideBar();
                this.disableSideBarButton();
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
            this.showSideBar();
        }
    }

    toggleSideBarAction() {
        console.info("toggle side bar");
        this.sideBarStatus = localStorage.getItem('sideBarStatus');

        if(this.sideBarStatus === 'on') {
            this.hideSideBar();
            localStorage.setItem('sideBarStatus', 'off');
            this.sideBarStatus = 'off';
        } else {
            this.showSideBar()
            localStorage.setItem('sideBarStatus', 'on');
            this.sideBarStatus = 'on';
        }
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
        $("select[class='select-users']").each(function () {
            let selectId = $(this).attr('id');
            console.info("auto enable select-user for : " + selectId);
            new SelectUser(selectId);
        });
    }

    checkSlimSelect() {
        $("select[class='slim-select']").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select for : " + selectName);
            new SlimSelect({
                select: '#' + selectName
            });
            $(this).addClass("slim-select-hack");
        })

        $("select[class='slim-select-simple']").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select-simple for : " + selectName);
            new SlimSelect({
                select: '#' + selectName,
                showSearch: false,
                searchHighlight: false,
                hideSelectedOption: false,
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

    onDocumentLoad() {
        console.info("global on load");
        this.checkSelectUser();
        this.checkSlimSelect();
        this.enableSummerNote();
        this.adjustUi();
    }

}