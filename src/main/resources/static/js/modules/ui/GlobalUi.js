import {default as SelectUser} from "../utils/SelectUser.js?version=@version@";
import {WizUi} from "./WizUi.js?version=@version@";

export class GlobalUi {

    constructor(authUserEppn, csrf, applicationEmail, maxSize, maxInactiveInterval) {
        console.info("Starting global UI");
        this.checkBrowser();
        this.checkOS();
        this.csrf = csrf;
        this.maxSize = maxSize;
        this.maxInactiveInterval = maxInactiveInterval;
        this.applicationEmail = applicationEmail;
        this.sideBarStatus = localStorage.getItem('sideBarStatus');
        this.sideBar = $('#sidebar');
        this.sideBar2 = $('#sidebar2');
        this.sideBarLabels = $('.sidebar-label');
        this.content = $('#content');
        this.newDiv = $('#new-div');
        this.breadcrumb = $('#breadcrumb');
        this.inputFiles = $(".custom-file-input");
        this.clickableRow = $(".clickable-row");
        this.clickableTd = $(".clickable-td");
        this.markAsReadButtons = $('button[id^="markAsReadButton_"]');
        this.markHelpAsReadButtons = $('button[id^="markHelpAsReadButton_"]');
        this.initListeners();
        this.initBootBox();
        this.initSideBar();
        this.checkCurrentPage();
        this.initTooltips();
        this.lastWidth = window.innerWidth;
        this.lastHeight = window.innerHeight;
        window.__isResizingCross = false;
    }

    initListeners() {
        let applicationEmail = this.applicationEmail;
        window.onerror = function (msg, url, lineNo, columnNo, error) {
            let clientSideError = {
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
            alert("Une erreur s'est produite au niveau de l'affichage.\n" +
                "Merci de contacter le gestionnaire de cette application : \n" +
                applicationEmail + "\n" +
                "Détails : " +
                "\n #url :" + url +
                "\n #error : " + error +
                "\n #ligne : " + lineNo +
                "\n #colonne : " + columnNo);
            return false;
        };

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Tab') {
                const active = document.activeElement;
                if (active === document.body) {
                    e.preventDefault();
                    document.querySelector('#link-accueil')?.focus();
                }
            }
        }, { once: true });

        document.addEventListener('shown.bs.modal', function (e) {
            const modal = e.target;
            const focusable = Array.from(
                modal.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')
            ).filter(el => !el.disabled && el.offsetParent !== null && el.hasAttribute("autofocus"));

            if (focusable) {
                const last = focusable.at(-1);
                if (last) {
                    last.focus();
                }
            }
            function escHandler(event) {
                if (event.key === 'Escape' || event.key === 'Esc') {
                    const instance = bootstrap.Modal.getOrCreateInstance(modal);
                    instance.hide();
                }
            }
            modal._escHandler = escHandler;
            document.addEventListener('keydown', escHandler);
        });

        document.querySelector('#shortcuts')
            ?.querySelectorAll('a[role="button"]')
            .forEach(el => {
                el.addEventListener('keydown', e => {
                    if (e.code === 'Space') {
                        e.preventDefault();
                        e.stopPropagation();
                        el.click();
                    }
                });
            });

        document.addEventListener('hidden.bs.modal', function (e) {
            const modal = e.target;
            if (modal._escHandler) {
                document.removeEventListener('keydown', modal._escHandler);
                delete modal._escHandler;
            }
        });

        $(document).on("refreshClickableTd", e => this.refreshClickableTd());
        this.markAsReadButtons.each((index, e) => this.listenMarkAsReadButton(e));
        this.markHelpAsReadButtons.each((index, e) => this.listenHelpMarkAsReadButton(e));
        $('#sidebarCollapse').unbind('click').on('click', e => this.toggleSideBarAction());

        $("#closeUserInfo").on('click', function() {
            $("#user-toggle").click();
        });
        this.clickableRow.off('click').on('click', function(e) {
            let url = $(this).closest('tr').attr('data-href');
            if (e.ctrlKey || e.metaKey) {
                window.open(url, '_blank');
            } else {
                window.location = url;
            }
        });
        this.refreshClickableTd();
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

        $("#display-side-btn").on('click', function(e) {
           $("#sidebar").toggleClass("sidebar-mobile");
        });
        $(window).on("resize", (e) => {
            const w = window.innerWidth;
            const h = window.innerHeight;
            const deltaW = w - self.lastWidth;
            const deltaH = h - self.lastHeight;
            if (deltaW === 0 && deltaH === 0) return;
            if(e.target.tagName == null) {
                self.adjustUi();
            }
            self.lastWidth = w;
            self.lastHeight = h;
        });

        $(document).ready(e => this.onDocumentLoad());
        let self = this;
        let newSelfSign =$("#new-self-sign");
        newSelfSign.on('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.target.click();
            }
        });
        newSelfSign.on('click', function(e) {
            let wizUi = new WizUi("", $("#wiz-self-sign-div"), self.csrf, self.maxSize);
            wizUi.selfSignStart();
        });

        let newFastSign = $("#new-fast-sign")
        newFastSign.on('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.target.click();
            }
        });
        newFastSign.on('click', function(e) {
            let wizUi = new WizUi("", $("#wiz-fast-sign-div"), self.csrf, self.maxSize);
            wizUi.fastStartSign();
        });

        let startWizardCustomButton = $("#start-wizard-custom-button");
        startWizardCustomButton.on('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.target.click();
            }
        });
        startWizardCustomButton.on('click', function(e) {
            let wizUi = new WizUi("", $("#wiz-custom-sign-div"), self.csrf, self.maxSize);
            wizUi.workflowSignStart();
        });

        $(".start-wizard-buttons").each(function(){
            $(this).on('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.target.click();
                }
            });
            $(this).on('click', function(e) {
                let wizUi = new WizUi("", $("#wiz-div"), self.csrf, self.maxSize);
                wizUi.wizardWorkflowStart();
            });
        });

        let startFormButton = $(".start-form-button");
        startFormButton.on('keydown', function(e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.target.click();
            }
        });
        startFormButton.on('click', function() {
            let wizUi = new WizUi("", $("#wiz-start-form-div"), self.csrf, self.maxSize);
            wizUi.wizardFormStart($(this).attr("data-es-form-id"));
        });

        $(".start-wizard-workflow-button").each(function() {
            let menuToggle = $(this).children('button').first();
            menuToggle.on("click", function(e) {
                e.stopPropagation();
                e.preventDefault();
            });
            $(this).on('keydown', function(e) {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.target.click();
                }
            });
            $(this).on('click', function(e) {
                let wizUi = new WizUi($(this).attr('data-es-workflow-id'), $("#wiz-workflow-sign-div"), self.csrf, self.maxSize);
                wizUi.workflowSignStart();
            });
        });

        $('.toggle-mini-menu').each(function(e) {
            $(this).on('click', function(e) {
                e.stopPropagation();
            })
        });

        $('.prevent').each(function(e) {
            $(this).on('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
            })
        });

        $('.workflow-delete-button').each(function(e) {
            $(this).on('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                $('#deleteWorkflow_' + this.getAttribute('data-id')).submit();
            })
        });

        $('.workflow-update-button').each(function(e) {
            $(this).on('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                location.href = "/user/workflows/" + this.getAttribute('data-id');
            })
        });

        $("#user-toggle").on("click", function (e){
            e.stopPropagation();
        });
        this.bindKeyboardKeys();
    }

    initTooltips() {
        $("#new-scroll").tooltip({
            disabled: false,
            show: { effect: "fade", duration: 500 },
            hide: { effect: "fade", duration: 500 }
        });
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
        bootbox.addLocale("fr", {
            OK : 'Fermer',
            CANCEL : 'Annuler',
            CONFIRM : 'Confirmer'
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

    checkOS() {
        let userAgent = window.navigator.userAgent,
            platform = window.navigator?.userAgentData?.platform || window.navigator.platform,
            macosPlatforms = ['Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
            windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE'],
            iosPlatforms = ['iPhone', 'iPad', 'iPod'],
            os = null;

        if (macosPlatforms.indexOf(platform) !== -1) {
            os = 'Mac OS';
        } else if (iosPlatforms.indexOf(platform) !== -1) {
            os = 'iOS';
        } else if (windowsPlatforms.indexOf(platform) !== -1) {
            os = 'Windows';
            document.styleSheets[document.styleSheets.length - 1].addRule("html", `scrollbar-width: thin;`);
            document.styleSheets[document.styleSheets.length - 1].addRule(".scrollbar-lite scrollbar-style", `scrollbar-width: thin;`);
            document.styleSheets[document.styleSheets.length - 1].addRule(".table-fix-head", `scrollbar-width: thin;`);
        } else if (/Android/.test(userAgent)) {
            os = 'Android';
        } else if (/Linux/.test(platform)) {
            os = 'Linux';
        }
        console.info("detected os : " + os);
        return os;
    }

    disableSendButton(e) {
        $("#send-pending-button").unbind();
    }

    checkCurrentPage() {
        let url = window.location.pathname;
        if(!url.match("/user/signrequests/+[\\w\\W]+")) {
            this.resetMode();
        }
    }

    listenHelpMarkAsReadButton(btn) {
        console.debug("debug - " + "listen to" + btn);
        $(btn).on('click', e => this.markHelpAsRead(e));
    }

    markHelpAsRead(e) {
        let id = e.target.id.split('_')[1];
        console.info("mark help as read message " + id);
        $.get("/user/users/mark-help-as-read/" + id);
    }

    listenMarkAsReadButton(btn) {
        console.debug("debug - " + "listen to" + btn);
        $(btn).on('click', e => this.markAsRead(e));
    }

    markAsRead(e) {
        let id = e.target.id.split('_')[1];
        console.info("mark as read message " + id);
        $.get("/user/users/mark-as-read/" + id);
    }

    hideMenus(event) {
        $("#mega-result").modal('hide');
        $("#second-tools").collapse('hide');
        var clickover = $(event.target);
        if(clickover.attr("id") !== "display-side-btn" && clickover.parent().attr("id") !== "display-side-btn" && clickover.parent().parent().attr("id") !== "display-side-btn") {
            $("#sidebar").removeClass("sidebar-mobile");
        }
        $("div[id^='menu-']").each(function() {
            var _opened = $(this).hasClass("collapse show");
            if (_opened === true && !clickover.hasClass("toggle-mini-menu")) {
                let id = $(this).attr('id').split('-')[1];
                $("#menu-toggle_" + id).click();
            }
        });
        var container = document.getElementsByClassName('user-infos')[0];
        var _opened = $("#user-infos").hasClass("collapse show");
        if (_opened === true && container !== event.target && !container.contains(event.target)) {
            $("#user-infos").collapse('hide');
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
            console.debug("debug - " + "auto adjust : display");
            let url = window.location.pathname;
            if(this.sideBarStatus === 'on') {
                this.showSideBar();
            } else {
                this.hideSideBar();
            }
            if(!url.match("/user/users+[\\w\\W]+")
                && !url.match("/user/users")
                && !url.match("/admin")
                && !url.match("/admin/+[\\w\\W]+")
                && !url.match("/manager/+[\\w\\W]+")
                && !url.match("^/user$")
                && !url.match("^/user/$")
                && !url.match("^/user/signrequests$")
                && !url.match("/user/signrequests/+[\\w\\W]+")
                && !url.match("^/otp/signrequests$")
                && !url.match("/otp/signrequests/+[\\w\\W]+")
                && !url.match("^/user/signbooks$")
                && !url.match("/user/signbooks/+[\\w\\W]+")) {
                console.info("auto display side bar : show");
                this.hideSideBar();
                this.disableSideBarButton();
            }
            if(url.match("^/user/workflows/+[\\w\\W]+")
                || url.match("^/user/signbooks$")
                || url.match("^/user/signbooks/$")
                || url.match("/user/signbooks/+[\\w\\W]+")
                || url.match("^/user/signrequests$")
                || url.match("^/user/signrequests/$")
                || url.match("/user/signrequests/+[\\w\\W]+")
                || url.match("^/otp/signrequests$")
                || url.match("^/otp/signrequests/$")
                || url.match("/otp/signrequests/+[\\w\\W]+")) {
                console.info("auto display side bar : hide");
                this.showSideBar();
                this.disableSideBarButton();
            }
        }
    }

    disableSideBarButton() {
        console.debug("debug - " + "disable side button");
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
        console.debug("debug - " + "show side");
        this.sideBar.removeClass('active');
        this.sideBar2.removeClass('d-none');
        this.sideBarLabels.removeClass('d-none');
        this.content.removeClass('content-full');
        this.newDiv.removeClass('new-width-full');
        this.breadcrumb.removeClass('breadcrumb-nav-full');
    }

    hideSideBar() {
        console.debug("debug - " + "hide side");
        this.sideBar.addClass('active');
        this.sideBar2.addClass('d-none');
        this.sideBarLabels.addClass('d-none');
        this.content.addClass('content-full');
        this.newDiv.addClass('new-width-full');
        this.breadcrumb.addClass('breadcrumb-nav-full');
    }

    checkSelectUser() {
        let csrf = this.csrf;
        $("select").each(function () {
            if($(this).hasClass("auto-select-users")) {
                let selectId = $(this).attr('id');
                console.info("auto enable select-user for : " + selectId);
                let limit = null;
                if ($(this).attr("maxLength") != null) {
                    limit = parseInt($(this).attr("maxLength"));
                }
                new SelectUser(selectId, limit, $(this).attr('data-signrequest-id'), csrf);
            }
        });
    }

    checkSlimSelect() {
        let self = this;
        $("select[class='slim-select']").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select for : " + selectName);
            new SlimSelect({
                select: '#' + selectName,
            });
            self.slimSelectHack($(this))
        })

        $(".intl-phone").each(function() {
            intlTelInput(this, {
                validationNumberTypes: "FIXED_LINE_OR_MOBILE",
                strictMode: true,
                separateDialCode: false,
                nationalMode: true,
                countryOrder: ["fr"],
                initialCountry: "auto",
                geoIpLookup: callback => {
                    callback(navigator.language.split('-')[0]);
                },
                customPlaceholder: (selectedCountryPlaceholder, selectedCountryData) => "Saisir un numéro",
                searchPlaceholder: "Rechercher",
            });
        });

        $(".slim-select-filter").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select-filter for : " + selectName);
            let select = $("#" + selectName);
            new SlimSelect({
                select: '#' + selectName,
                settings: {
                    hideSelectedOption: false,
                    placeholderText: $(this).attr('data-placeholder'),
                    closeOnSelect: true,
                },
                events: {
                    searchFilter: (option, search) => {
                        return option.text.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, "").indexOf(search.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, "")) !== -1
                    }
                }
            });
            select.removeClass("spinner-border");
            self.slimSelectHack($(this))
        })

        $(".slim-select-filter-search").each(function () {
            let selectName = $(this).attr('id');
            let url = $(this).attr('es-search-url');
            let placeholderText = $(this).attr('es-search-text');
            let addable = null;
            if($(this).attr('es-search-addable') === "true") {
                addable = function (value) {
                    return value
                };
            }
            console.info("auto enable slim-select-filter-search for : " + selectName);
            let select = $("#" + selectName);
            new SlimSelect({
                select: '#' + selectName,
                settings: {
                    placeholderText: placeholderText,
                    searchText: 'Aucun résultat',
                    searchingText: 'Recherche en cours',
                    searchPlaceholder: 'Rechercher',
                    searchHighlight: false,
                    hideSelectedOption: true,
                    closeOnSelect: true,
                    maxValuesShown: 40,
                },
                events: {
                    addable: addable,
                    searchFilter: (option, search) => {
                        return true;
                    },
                    search: (search, currentData) => {
                        return new Promise((resolve, reject) => {
                            if (search.length < 3) {
                                return reject('Merci de saisir au moins 3 caractères');
                            } else {
                                fetch(url + '?searchString=' + search, {
                                    method: 'get',
                                })
                                .then((response) => {
                                    return response.json()
                                })
                                .then((json) => {$
                                    console.log(json);
                                    let data = []
                                    for (let i = 0; i < json.length; i++) {
                                        data.push({
                                            text: json[i],
                                            value: json[i]
                                        });
                                    }
                                    if (data.length > 0) {
                                        return resolve(data);
                                    } else {
                                        return reject("Pas de résultat");
                                    }
                                })
                                .catch(function () {
                                    return reject("Recherche en cours");
                                });
                            }
                        });
                    }
                }
            });
            $(".ss-search > input").on("click" , function (e) {
                e.stopPropagation();
            });
            select.removeClass("spinner-border");
            self.slimSelectHack($(this));
        })

        $(".slim-select-simple").each(function () {
            let selectName = $(this).attr('id');
            console.info("auto enable slim-select-simple for : " + selectName);
            let allowDeselect = Boolean($(this).attr('data-allow-deselect'));
            new SlimSelect({
                select: '#' + selectName,
                settings: {
                    showSearch: false,
                    searchHighlight: false,
                    hideSelectedOption: false,
                    allowDeselect: allowDeselect.valueOf(),
                    placeholderText: $(this).attr('data-placeholder'),
                    closeOnSelect: true,
                },
                ajax: function (search, callback) {
                    callback(false)
                }
            });
            self.slimSelectHack($(this))
        });
    }

    slimSelectHack(slim) {
        slim.css("display", "block");
        slim.css("position", "absolute");
        slim.css("height", 38);
        slim.css("opacity", 0);
        slim.css("z-index", -1);
    }

    enableSummerNote() {
        $('.summer-note').each(function() {
            console.info("auto enable summer note for " + $(this).attr('id'));
            let summernote = $(this).summernote({
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
            $('form').on('submit',function(){
                if (summernote.summernote('isEmpty')) {
                    summernote.val('');
                }else if(summernote.val()==='<p><br></p>'){
                    summernote.val('');
                }
            });
            $('workflow').on('submit',function(){
                if (summernote.summernote('isEmpty')) {
                    summernote.val('');
                }else if(summernote.val()==='<p><br></p>'){
                    summernote.val('');
                }
            });
        });
    }

    enableSpectrum() {
        $(".tag-color").each(function () {
            $(this).spectrum({
                type: "flat",
                showPalette: false,
                showPaletteOnly: true,
                togglePaletteOnly: true,
                showInput: true,
                showAlpha: false
            });
        });
    }

    onDocumentLoad() {
        console.info("global on load");
        // $.fn.modal.Constructor.prototype.enforceFocus = function () {};
        this.checkSelectUser();
        this.checkSlimSelect();
        this.enableSummerNote();
        this.enableSpectrum();
        this.adjustUi();
        this.sessionTimeout();
    }

    sessionTimeout() {
        setInterval(function(){
            $("#timeoutModal").modal("show");
        }, this.maxInactiveInterval * 1000);
        $("#timeoutModal").on('hidden.bs.modal', function(){
            if(window.location.pathname.includes("otp")) {
                window.location.href = "/otp-access/session-expired";
            } else {
                location.reload();
            }
        });
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
                    case 13:
                        event.preventDefault();
                        let signLaunchButton = $("#signLaunchButton");
                        if(signLaunchButton.length && $(".bootbox-alert").length === 0) {
                            signLaunchButton.click();
                        }
                        break;
                }
            } else {
                if(event.which === 13 && event.target.nodeName !== "TEXTAREA") {
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

    refreshClickableTd() {
        this.clickableTd = $(".clickable-td");
        this.clickableTd.off('click').on('click', function(e) {
            let test = $(".card.show").length > 0;

            if (!test) {
                let url = $(this).closest('tr').attr('data-href');
                if (e.ctrlKey || e.metaKey) {
                    window.open(url, '_blank');
                } else {
                    window.location = url;
                }
            }
        });
    }
}
