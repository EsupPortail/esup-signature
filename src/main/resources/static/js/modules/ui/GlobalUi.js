import {default as SelectUser} from "../utils/SelectUser.js";
import {SseSubscribe} from "../utils/SseSubscribe.js";
import {CsrfToken} from "../../prototypes/CsrfToken.js";
import {WizUi} from "./WizUi.js";

export class GlobalUi {

    constructor(authUserEppn, csrf) {
        console.info("Starting global UI");
        this.checkBrowser();
        this.csrf = csrf;
        this.sideBarStatus = localStorage.getItem('sideBarStatus');
        this.sideBar = $('#sidebar');
        this.sideBar2 = $('#sidebar2');
        this.sideBarLabels = $('.sidebar-label');
        this.content = $('#content');
        this.breadcrumb = $('#breadcrumb');
        this.inputFiles = $(".custom-file-input");
        this.clickableRow = $(".clickable-row");
        this.clickableTd = $(".clickable-td");
        this.autoHide = $('.auto-hide');
        this.markAsReadButtons = $('button[id^="markAsReadButton_"]');
        this.markHelpAsReadButtons = $('button[id^="markHelpAsReadButton_"]');
        if(authUserEppn != null) {
            sessionStorage.setItem("sseId", this.csrf.token)
            this.sseSubscribe = new SseSubscribe(this.csrf.token);
        }
        this.initListeners();
        this.initBootBox();
        this.initSideBar();
        this.checkCurrentPage();
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
        this.inputFiles.on('change', e => this.changeFileInputName(e));
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

        $("#sendPendingButton").on('click', e => this.checkUserCertificate());

        let csrf = this.csrf;
        $("#startWizardCustomButton").on('click', function(e) {
            let wizUi = new WizUi("", $("#wizFrameCustom"), "Demande personnalisée", csrf);
            wizUi.startByDocs();
        });

        $(".startWizardWorkflowButton").each(function() {
            $(this).on('click', function(e) {
                let wizUi = new WizUi($(this).attr('data-workflow-id'), $("#wizFrameWorkflow"), $(this).attr('data-workflow-name'), csrf);
                wizUi.startByDocs();
            });
        });

        $("#startWizardButton").on('click', function(e) {
            let wizUi = new WizUi("", $("#wizFrame"), "Circuit personnalisé", csrf);
            wizUi.startByRecipients();
        });

        this.bindKeyboardKeys();
    }

    initBootBox() {
        bootbox.setDefaults({
            locale: "fr",
            show: true,
                backdrop: true,
            closeButton: true,
            animate: true,
            className: "my-modal"

        });
    }

    checkBrowser() {
        let ua = window.navigator.userAgent;
        let msie = ua.indexOf("MSIE ");
        let isIE = /*@cc_on!@*/false || !!document.documentMode;
        if (isIE || msie > 0) {
            document.body.innerHTML = "";
            alert("Votre navigateur n'est pas compatible");
            window.location.href="https://www.google.com/intl/fr_fr/chrome/";
        }
    }

    checkUserCertificate() {
        if ($('#signType2').val() === 'certSign') {
            let csrf = new CsrfToken(this.csrf);
            $.ajax({
                url: "/user/users/check-user-certificate?" + csrf.parameterName + "=" + csrf.token,
                type: 'POST',
                contentType: "application/json",
                dataType: 'json',
                data: JSON.stringify($('#recipientsEmails').find(`[data-check='true']`).prevObject[0].slim.selected()),
                success: response => this.submitSendPending(response)
            });
        } else {
            $("#pending").val(true);
            $("#sendButton").click();
        }
    }

    submitSendPending(data) {
        let stringChain = "Les utilisateurs suivants n'ont pas de certificats électroniques : ";
        for (let i = 0; i < data.length ; i++) {
            stringChain += data[i].firstname + " " + data[i].name + " ";
        }
        stringChain += "Confirmez-vous l'envoie de la demande ? "
        if (data.length < 1 || bootbox.confirm(stringChain)) {
            $("#pending").val(true);
            $("#sendButton").click();
        }
    }

    // showSendPendingModal() {
    //     $('#sendPending').modal('toggle');
    //     $('#sendSignRequestModal').modal('toggle');
    // }

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
            console.debug("auto adjust : display");
            let url = window.location.pathname;
            if(this.sideBarStatus === 'on') {
                this.showSideBar();
            } else {
                this.hideSideBar();
            }
            if(!url.match("/user/users+[\\w\\W]+")
                && !url.match("/admin/+[\\w\\W]+")
                && !url.match("^/user/$")
                && !url.match("^/user/signrequests$")
                && !url.match("/user/signrequests/+[\\w\\W]+")) {
                console.info("auto display side bar : show");
                this.hideSideBar();
                this.disableSideBarButton();
            }
            if(url.match("^/user/workflows/+[\\w\\W]+")
                || url.match("^/user/signbooks/+[\\w\\W]+")
                || url.match("^/user/signrequests$")
                || url.match("^/user/signrequests/$")
                || url.match("/user/signrequests/+[\\w\\W]+")) {
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
            new SelectUser(selectId, null, $(this).attr('data-signrequest-id'));
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

        $(".slim-select-simple").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select-simple for : " + selectName);
            let allowDeselect = new Boolean($(this).attr('data-allow-deselect'));
            new SlimSelect({
                select: '#' + selectName,
                showSearch: false,
                searchHighlight: false,
                hideSelectedOption: false,
                allowDeselect: allowDeselect.valueOf(),
                placeholder: $(this).attr('data-placeholder'),
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

    bindKeyboardKeys() {
        $(window).bind('keydown', function(event) {
            console.info('push ' + event.which + ' key');
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
                switch (event.which) {
                    case 39:
                        let nextSignRequestButton = $("#nextSignRequestButton");
                        if(nextSignRequestButton.length) {
                            event.preventDefault();
                            location.href = nextSignRequestButton.attr('href');
                        }
                        break;
                    case 37:
                        event.preventDefault();
                        let prevSignRequestButton = $("#prevSignRequestButton");
                        if(prevSignRequestButton.length) {
                            location.href = prevSignRequestButton.attr('href');
                        }
                        break;
                }
            } else {
                if(event.which === 13) {
                    let signLaunchButton = $("#signLaunchButton");
                    if(signLaunchButton.length && $(".bootbox-alert").length === 0) {
                        signLaunchButton.click();
                    }
                    let saveCommentButton = $("#saveCommentButton");
                    if(saveCommentButton.length && $("#postitComment").val() !== '') {
                        saveCommentButton.click();
                    }

                }
                if(event.which === 27) {
                    let hideCommentButton = $("#hideCommentButton");
                    if(hideCommentButton.length) {
                        hideCommentButton.click();
                    }
                }
            }
        });
    }

}