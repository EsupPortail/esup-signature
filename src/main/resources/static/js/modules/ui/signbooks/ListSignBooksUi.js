import {CsrfToken} from "../../../prototypes/CsrfToken.js?version=@version@";
import {Nexu} from "../signrequests/Nexu.js?version=@version@";

export class ListSignBooksUi {

    constructor(signRequests, statusFilter, recipientsFilter, workflowFilter, creatorFilter, docTitleFilter, dateFilter, infiniteScrolling, csrf, mode, options = {}) {
        console.info("Starting list sign UI");
        this.signRequests = signRequests;
        this.mode = mode;
        this.preferenceStorageKey = options.preferenceStorageKey ?? null;
        this.toggleSelector = options.toggleSelector ?? null;
        this.infiniteScrolling = Boolean(infiniteScrolling);
        this.infiniteScrolling = this.resolveInfiniteScrollingPreference();
        this.statusFilter = "";
        this.recipientsFilter = "";
        this.workflowFilter = "";
        this.docTitleFilter = "";
        this.creatorFilter = "";
        this.dateFilter = "";
        if(statusFilter != null) {
            this.statusFilter = statusFilter;
        }
        if(recipientsFilter != null) {
            this.recipientsFilter = recipientsFilter;
        }
        if(workflowFilter != null) {
            this.workflowFilter = workflowFilter;
        }
        if(docTitleFilter != null) {
            this.docTitleFilter = docTitleFilter;
        }
        if(creatorFilter != null) {
            this.creatorFilter = creatorFilter;
        }
        if(dateFilter != null) {
            this.dateFilter = dateFilter;
        }
        this.csrf = new CsrfToken(csrf);
        this.signRequestTable = $("#signRequestTable");
        this.listSignRequestTable = $('#listSignRequestTable');
        this.page = 0;
        this.launchMassSignButtonHide = true;
        this.rowHeight = null;
        this.certTypeSelect = $("#certType");
        this.isLoadingPage = false;
        this.initialLoadPending = true;
        $("#password").hide();
        new Nexu(null, null, null, null, null);
        $(document).ready(() => {
            this.initListeners();
            this.ensureSealCertificateSelection();
            if(this.infiniteScrolling) {
                this.detectEndDiv();
            } else {
                this.waitForHeaderStabilization().then(() => this.finishInitialLoading());
            }
        });
        $("#sealChoose").addClass('d-none');
    }

    waitForHeaderStabilization() {
        return new Promise(resolve => {
            if (this.isHeaderReadyForDisplay()) {
                this.finishInitialLoadingAfterPaint(resolve);
                return;
            }

            let resolved = false;
            let intervalId = null;
            let timeoutId = null;

            const cleanup = () => {
                document.removeEventListener('globalUiReady', onGlobalUiReady);
                if (intervalId != null) {
                    window.clearInterval(intervalId);
                }
                if (timeoutId != null) {
                    window.clearTimeout(timeoutId);
                }
            };

            const finalize = () => {
                if (resolved) {
                    return;
                }
                resolved = true;
                cleanup();
                this.finishInitialLoadingAfterPaint(resolve);
            };

            const checkReadyState = () => {
                if (this.isHeaderReadyForDisplay()) {
                    finalize();
                }
            };

            const onGlobalUiReady = () => checkReadyState();

            document.addEventListener('globalUiReady', onGlobalUiReady);
            intervalId = window.setInterval(checkReadyState, 50);
            timeoutId = window.setTimeout(() => finalize(), 2500);
            checkReadyState();
        });
    }

    finishInitialLoadingAfterPaint(callback) {
        window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => callback());
        });
    }

    isHeaderReadyForDisplay() {
        return this.isGlobalUiReady() && this.areFilterWidgetsReady();
    }

    isGlobalUiReady() {
        return document.documentElement?.dataset.globalUiReady === 'true';
    }

    areFilterWidgetsReady() {
        return this.isFilterWidgetReady('#docTitleFilter')
            && this.isFilterWidgetReady('#workflowFilter')
            && this.isFilterWidgetReady('#creatorFilter')
            && this.isFilterWidgetReady('#recipientsFilter');
    }

    isFilterWidgetReady(selector) {
        const element = document.querySelector(selector);
        if (element == null) {
            return true;
        }

        if (element.classList.contains('auto-select-users')) {
            return element.slim != null;
        }

        if (element.classList.contains('slim-select-filter') || element.classList.contains('slim-select-filter-search') || element.classList.contains('slim-select')) {
            return element.slim != null || document.querySelector(selector + ' + .ss-main') != null;
        }

        return true;
    }

    finishInitialLoading() {
        if (!this.initialLoadPending) {
            return;
        }
        this.initialLoadPending = false;
        document.body?.classList.remove('signbooks-list-loading');
    }

    initListeners() {
        this.initDisplayModeToggle();
        $("#refresh-certType").on('click', e => this.checkSignOptions());
        $("#certType").on("change", e => this.checkAfterChangeSignType());
        $('#checkValidateSignButtonEnd').on('click', e => this.launchMassSign());
        $('#workflowFilter').on('change', e => this.buildUrlFilter());
        this.initFilterSelect('#creatorFilter');
        this.initFilterSelect('#recipientsFilter', true);
        $('#docTitleFilter').on('change', e => this.buildUrlFilter());
        $('#statusFilter').on('change', e => this.buildUrlFilter());
        $('#dateFilter').on('change', e => this.buildUrlFilter());
        $('#deleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#restoreMultipleButton').on("click", e => this.restoreMultiple());
        $('#menuDeleteMultipleButton').on("click", e => this.deleteMultiple());
        $('#menuRestoreMultipleButton').on("click", e => this.restoreMultiple());
        $('#downloadMultipleButton').on("click", e => this.downloadMultiple());
        $('#downloadMultipleButtonWithReport').on("click", e => this.downloadMultipleWithReport());
        $('#menuDownloadMultipleButton').on("click", e => this.downloadMultiple());
        $('#menuDownloadMultipleButtonWithReport').on("click", e => this.downloadMultipleWithReport());
        this.listSignRequestTable.on('scroll', e => this.detectEndDiv(e));
        $(document).on('wheel', e => {
            let delta = e.originalEvent.deltaY;
            let scrollAmount = delta > 0 ? 50 : -50;
            this.listSignRequestTable.scrollTop(this.listSignRequestTable.scrollTop() + scrollAmount);
        });
        $('#selectAllButton').on("click", e => this.selectAllCheckboxes());
        $('#unSelectAllButton').on("click", e => this.unSelectAllCheckboxes());
        this.refreshListeners();
        document.addEventListener("massSign", e => this.updateWaitModal(e));
        document.addEventListener("sign", e => this.updateErrorWaitModal(e));
        $("#more-sign-request").on("click", e => this.addToPage());
    }

    resolveInfiniteScrollingPreference() {
        const storedMode = this.getStoredDisplayMode();
        if (storedMode === 'infinite') {
            return true;
        }
        if (storedMode === 'pagination') {
            return false;
        }
        return this.infiniteScrolling;
    }

    getStoredDisplayMode() {
        if (this.preferenceStorageKey == null) {
            return null;
        }

        try {
            const storedMode = window.localStorage.getItem(this.preferenceStorageKey);
            if (storedMode === 'infinite' || storedMode === 'pagination') {
                return storedMode;
            }
        } catch (e) {
            console.debug('LocalStorage indisponible pour la préférence d’affichage des signbooks', e);
        }

        return null;
    }

    initDisplayModeToggle() {
        if (this.toggleSelector == null) {
            return;
        }

        const toggle = $(this.toggleSelector);
        if (toggle.length === 0) {
            return;
        }

        toggle.prop('checked', this.infiniteScrolling);
        toggle.off('change.listSignBooksUi').on('change.listSignBooksUi', e => {
            this.changeDisplayMode($(e.currentTarget).is(':checked'));
        });
    }

    changeDisplayMode(infiniteScrolling) {
        this.persistDisplayModePreference(infiniteScrolling);
        this.navigateWithDisplayMode(infiniteScrolling);
    }

    persistDisplayModePreference(infiniteScrolling) {
        if (this.preferenceStorageKey == null) {
            return;
        }

        try {
            window.localStorage.setItem(this.preferenceStorageKey, infiniteScrolling ? 'infinite' : 'pagination');
        } catch (e) {
            console.debug('Impossible d’enregistrer la préférence d’affichage des signbooks', e);
        }
    }

    navigateWithDisplayMode(infiniteScrolling) {
        const currentParams = new URLSearchParams(window.location.search);
        currentParams.set('infiniteScrolling', String(infiniteScrolling));
        currentParams.delete('page');

        const queryString = currentParams.toString();
        const targetUrl = window.location.pathname + (queryString !== '' ? '?' + queryString : '');
        const currentUrl = window.location.pathname + window.location.search;

        if (targetUrl !== currentUrl) {
            window.location.href = targetUrl;
        }
    }

    initFilterSelect(selector, updatePlaceholder = false, remainingAttempts = 20) {
        let select = document.querySelector(selector);
        if (select == null) {
            return;
        }

        if (select.slim == null) {
            if (remainingAttempts > 0) {
                window.setTimeout(() => this.initFilterSelect(selector, updatePlaceholder, remainingAttempts - 1), 100);
            } else {
                $(select)
                    .off('change.listSignBooksUi')
                    .on('change.listSignBooksUi', () => this.buildUrlFilter());
            }
            return;
        }

        $(select)
            .off('change.listSignBooksUi')
            .on('change.listSignBooksUi', () => this.buildUrlFilter());

        const placeholder = $(select).attr("data-placeholder");
        if (placeholder != null && select.slim.settings != null) {
            select.slim.settings.placeholderText = placeholder;
        }
        if (updatePlaceholder) {
            let placeholderElement = document.querySelector(selector + ' + div .ss-placeholder');
            if (placeholderElement != null && placeholder != null) {
                placeholderElement.textContent = placeholder;
            }
        }

        const previousAfterChange = select.slim.events?.afterChange;
        if (select.slim.events == null) {
            select.slim.events = {};
        }
        select.slim.events.afterChange = (...args) => {
            if (typeof previousAfterChange === 'function') {
                previousAfterChange(...args);
            }
            this.buildUrlFilter();
        };
    }

    checkSignOptions() {
        console.info("check sign options");
        this.ensureSealCertificateSelection();
        new Nexu(null, null, null, null, null);
        $("#certType").focus();
    }

    ensureSealCertificateSelection() {
        const sealCertificatSelect = $("#sealCertificat");
        if (!sealCertificatSelect.length) {
            return;
        }

        const currentValue = sealCertificatSelect.val();
        if (currentValue != null && currentValue !== "") {
            return;
        }

        const firstOptionValue = sealCertificatSelect.find("option:first").val();
        if (firstOptionValue != null && firstOptionValue !== "") {
            sealCertificatSelect.val(firstOptionValue);
        }
    }

    checkAfterChangeSignType() {
        let value = this.certTypeSelect.val();
        $("#alert-sign-present").hide();
        if(value === "userCert") {
            $("#password").show();
        } else {
            $("#password").hide();
        }
        if(value === "nexuCert") {
            $("#nexuCheck").removeClass('d-none');
        } else {
            $("#nexuCheck").addClass('d-none');
        }
        if(value === "imageStamp") {
            $("#alert-sign-present").show();
        }
        if(value === "sealCert") {
            this.ensureSealCertificateSelection();
            $("#sealChoose").removeClass('d-none');
        } else {
            $("#sealChoose").addClass('d-none');
        }
    }

    refreshListeners() {
        $('.sign-requests-ids').on("change", e => this.checkNbCheckboxes());
        this.initParticipantStepSelects();
        $("button[id^='menu-toggle']").each(function() {
           $(this).on("click", function (){
               $("div[id^='menu-']").each(function() {
                   $(this).collapse('hide');
               });
           }) ;
        });
        $("div[id^='menu-']").each(function() {
            $(this).on('shown.bs.collapse', function (e) {
                let id = $(this).attr('id').split("-")[1];
                let menu = $("#menu-toggle_" + id);
                let div = $("#listSignRequestTable");
                let divHeight = div.height();
                let menuTop = menu.offset().top;
                if(divHeight < menuTop) {
                    div.scrollTop(div.scrollTop() + 150);
                }
            });
        });
    }

    initParticipantStepSelects() {
        if (typeof SlimSelect === 'undefined') {
            return;
        }

        document.querySelectorAll('select.participant-step-select').forEach(select => {
            if (select.dataset.participantStepInit === 'true' || select.id == null || select.id === '') {
                return;
            }

            const initialSelectedValue = Array.from(select.options).find(option => option.selected)?.value
                || Array.from(select.options)[0]?.value
                || '';

            const slimData = Array.from(select.options).map(option => ({
                text: option.text,
                value: option.value,
                html: this.renderParticipantStepOption(option),
                selected: option.selected || false,
                disabled: option.disabled || false,
                placeholder: option.dataset.placeholder === 'true'
            }));

            const slim = new SlimSelect({
                select: '#' + select.id,
                data: slimData,
                settings: {
                    showSearch: false,
                    searchHighlight: false,
                    hideSelectedOption: false,
                    closeOnSelect: true,
                    allowDeselect: false,
                },
                events: {
                    afterChange: newValue => {
                        const selectedValue = Array.isArray(newValue) && newValue.length > 0
                            ? (typeof newValue[0] === 'string' ? newValue[0] : newValue[0]?.value)
                            : null;
                        if (selectedValue != null && selectedValue !== select.dataset.readonlyValue) {
                            window.requestAnimationFrame(() => {
                                slim.setSelected(select.dataset.readonlyValue);
                            });
                        }
                    }
                },
                ajax: function (search, callback) {
                    callback(false)
                }
            });

            select.dataset.readonlyValue = initialSelectedValue;
            select.dataset.participantStepInit = 'true';
            const slimContainer = $('#' + select.id).next('.ss-main');
            slimContainer.addClass('participant-step-select-ui participant-step-select-readonly');
        });
    }

    renderParticipantStepOption(option) {
        const stepNumber = this.escapeHtml(option.dataset.stepNumber || option.value || '');
        const summary = this.escapeHtml(option.dataset.summary || option.text || '');
        const statusIcon = this.escapeHtml(option.dataset.statusIcon || '');
        const statusTitle = this.escapeHtml(option.dataset.statusTitle || '');
        const isCurrentStep = option.dataset.currentStep === 'true';
        const statusIconHtml = statusIcon !== ''
            ? '<i class="fi ' + statusIcon + ' participant-step-status-icon" title="' + statusTitle + '"></i>'
            : '';

        return '<div class="participant-step-option d-flex align-items-center gap-2 ' + (isCurrentStep ? 'participant-step-option-current' : '') + '">' +
            '<span class="step-vertical-icon participant-step-badge ' + (isCurrentStep ? 'participant-step-badge-current' : '') + '">' + stepNumber + '</span>' +
            '<div class="participant-step-meta min-w-0">' +
            '<span class="participant-step-users">' + summary + '</span>' +
            '</div>' +
            statusIconHtml +
            '</div>';
    }

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    hasMassSignableSelection(checkboxes) {
        for (let i = 0; i < checkboxes.length; i++) {
            let checkbox = checkboxes.eq(i);
            if (checkbox.attr("data-es-signrequest-status") === 'pending' && checkbox.attr("data-es-signrequest-deleted") !== 'true') {
                return true;
            }
        }

        return false;
    }

    hasRestorableSelection(checkboxes) {
        for (let i = 0; i < checkboxes.length; i++) {
            if (checkboxes.eq(i).attr("data-es-signrequest-deleted") === 'true') {
                return true;
            }
        }

        return false;
    }

    checkNbCheckboxes() {
        let idDom = $('.sign-requests-ids:checked');
        if (idDom.length > 0) {
            $('#deleteMultipleButton').removeClass('d-none');
            $('#downloadMultipleButton').removeClass('d-none');
            $('#downloadMultipleButtonWithReport').removeClass('d-none');
            $('#menuDeleteMultipleButton').removeClass('d-none');
            $('#menuDownloadMultipleButton').removeClass('d-none');
            $('#menuDownloadMultipleButtonWithReport').removeClass('d-none');
        } else {
            $('#deleteMultipleButton').addClass('d-none');
            $('#downloadMultipleButton').addClass('d-none');
            $('#downloadMultipleButtonWithReport').addClass('d-none');
            $('#menuDeleteMultipleButton').addClass('d-none');
            $('#menuDownloadMultipleButton').addClass('d-none');
            $('#menuDownloadMultipleButtonWithReport').addClass('d-none');
        }

        if (this.hasRestorableSelection(idDom)) {
            $('#restoreMultipleButton').removeClass('d-none');
            $('#menuRestoreMultipleButton').removeClass('d-none');
        } else {
            $('#restoreMultipleButton').addClass('d-none');
            $('#menuRestoreMultipleButton').addClass('d-none');
        }

        const shouldShowMassSignButton = idDom.length > 1 && (this.statusFilter !== 'deleted' || this.hasMassSignableSelection(idDom));
        if (shouldShowMassSignButton && this.launchMassSignButtonHide) {
            $('#massSignModalButton').removeClass('d-none');
            this.launchMassSignButtonHide = false;
        } else if (!shouldShowMassSignButton && !this.launchMassSignButtonHide) {
            $('#massSignModalButton').addClass('d-none');
            this.launchMassSignButtonHide = true;
        }
    }

    selectAllCheckboxes() {
        $("input[name^='ids']").each(function() {
            $(this).prop("checked", true).change();
        });
    }

    unSelectAllCheckboxes() {
        $("input[name^='ids']").each(function() {
            $(this).prop("checked", false).change();
        });
    }

    detectEndDiv(e) {
        if ( e== null || ($(e.target).scrollTop() + $(e.target).innerHeight() + 1 >= $(e.target)[0].scrollHeight && (this.infiniteScrolling != null && this.infiniteScrolling))) {
            this.addToPage();
        }
    }

    deleteMultiple() {
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-es-signbook-id");
            i++;
        });

        if(ids.length > 0) {
            let self = this;
            bootbox.confirm("Attention, les demandes au statut 'Supprimé' seront définitivement perdues. Les autres seront placées dans la corbeille.<br/>Confirmez vous l'opération ?",
                function(result) {
                    if(result) {
                        bootbox.dialog({
                            closeButton : false,
                            message : "<h5>Suppression en cours</h5>" +
                                "<div class=\"text-center\">" +
                                "<div id=\"signSpinner\" class=\"justify-content-center mx-auto\">\n" +
                                "   <div class=\"spinner-border mx-auto\" role=\"status\" style=\"width: 3rem; height: 3rem;\">\n" +
                                "       <span class=\"sr-only\">En cours...</span>\n" +
                                "   </div>\n" +
                                "</div> " +
                                "</div> "
                        });
                        $.ajax({
                            url: "/" + self.mode + "/signbooks/delete-multiple?" + self.csrf.parameterName + "=" + self.csrf.token,
                            type: 'POST',
                            dataType: 'json',
                            contentType: "application/json",
                            data: JSON.stringify(ids),
                            success: function () {
                                location.reload();
                            }
                        });
                    }
                }
            );
        }
    }

    restoreMultiple() {
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function () {
            if ($(this).attr("data-es-signrequest-deleted") === 'true') {
                ids[i] = $(this).attr("data-es-signbook-id");
                i++;
            }
        });

        if(ids.length > 0) {
            let self = this;
            bootbox.confirm("Confirmez vous la restauration de la sélection ?",
                function(result) {
                    if(result) {
                        bootbox.dialog({
                            closeButton : false,
                            message : "<h5>Restauration en cours</h5>" +
                                "<div class=\"text-center\">" +
                                "<div id=\"signSpinner\" class=\"justify-content-center mx-auto\">\n" +
                                "   <div class=\"spinner-border mx-auto\" role=\"status\" style=\"width: 3rem; height: 3rem;\">\n" +
                                "       <span class=\"sr-only\">En cours...</span>\n" +
                                "   </div>\n" +
                                "</div> " +
                                "</div> "
                        });
                        $.ajax({
                            url: "/" + self.mode + "/signbooks/restore-multiple?" + self.csrf.parameterName + "=" + self.csrf.token,
                            type: 'POST',
                            dataType: 'json',
                            contentType: "application/json",
                            data: JSON.stringify(ids),
                            success: function () {
                                location.reload();
                            }
                        });
                    }
                }
            );
        }
    }

    downloadMultiple() {
        console.info("launch download multiple");
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-es-signbook-id");
            i++;
        });
        if (ids.length > 0) {
            fetch(`/${this.mode}/signbooks/download-multiple?ids=${ids}`)
                .then(response => {
                    // Vérifier si la réponse est une erreur HTTP
                    if (!response.ok) {
                        return response.json().then(data => {
                            throw new Error(data.text || 'Erreur lors du téléchargement');
                        });
                    }
                    return response.blob().then(blob => ({
                        blob: blob,
                        filename: this.extractFilenameFromHeader(response)
                    }));
                })
                .then(({blob, filename}) => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = filename;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                    window.URL.revokeObjectURL(url);
                })
                .catch(error => {
                    bootbox.alert(error.message);
                });
        }
    }

    extractFilenameFromHeader(response) {
        const contentDisposition = response.headers.get('content-disposition');
        if (contentDisposition) {
            const matches = contentDisposition.match(/filename[^;=\n]*=(['"]*)(.*?)\1(?:;|$)/);
            if (matches && matches[2]) {
                return matches[2];
            }
        }
        return 'documents.zip'; // fallback
    }

    downloadMultipleWithReport() {
        console.info("launch download multiple");
        let ids = [];
        let i = 0;
        $("input[name='ids[]']:checked").each(function (e) {
            ids[i] = $(this).attr("data-es-signbook-id");
            i++;
        });
        if (ids.length > 0) {
            const a = document.createElement("a");
            a.href = `/${this.mode}/signbooks/download-multiple-with-report?ids=${ids}`;
            a.download = "";
            document.body.appendChild(a);
            a.click();
            a.remove();
        }
    }

    addToPage() {
        if (this.isLoadingPage) return;
        this.isLoadingPage = true;
        console.info("Add to page");
        let self = this;
        const urlParams = new URLSearchParams(window.location.search);
        let sortParam = "";
        const sort = urlParams.get("sort");

        if (sort) {
            sortParam = `&sort=${sort}`;
        }
        $("#loader").show();
        $.get("/" + this.mode + "/signbooks/list-ws?statusFilter=" + this.statusFilter + sortParam + "&recipientsFilter=" + this.recipientsFilter + "&workflowFilter=" + this.workflowFilter + "&docTitleFilter=" + this.docTitleFilter + "&creatorFilter=" + this.creatorFilter + "&dateFilter=" + this.dateFilter + "&" + this.csrf.parameterName + "=" + this.csrf.token + "&page=" + this.page + "&size=15")
            .done(function (data) {
                if(typeof data === 'string' && data.trim().length > 0) {
                    self.listSignRequestTable.unbind('scroll');
                    self.listSignRequestTable.addClass("wait");
                    self.page++;
                    self.signRequestTable.append(data);
                    let clickableRows = $(".clickable-row");
                    clickableRows.off('click').on('click', function (e) {
                        if ($(e.target).closest('.no-row-navigation, .participant-step-select, .participant-step-select-ui, .ss-main, .ss-content').length > 0) {
                            return;
                        }
                        let url = $(this).closest('tr').attr('data-href');
                        if (e.ctrlKey || e.metaKey) {
                            window.open(url, '_blank');
                        } else {
                            window.location = url;
                        }
                    });
                    $(document).trigger("refreshClickableTd");
                    self.listSignRequestTable.removeClass("wait");
                    self.refreshListeners();
                    self.listSignRequestTable.on('scroll', e => self.detectEndDiv(e));
                } else {
                    self.listSignRequestTable.unbind('scroll');
                    self.signRequestTable.parent().children('tfoot').remove();
                }
            })
            .always(function () {
                $("#loader").hide();
                self.isLoadingPage = false;
                if (self.initialLoadPending) {
                    window.requestAnimationFrame(() => self.finishInitialLoading());
                }
            });
    }

    buildUrlFilter() {
        let currentParams = new URLSearchParams(window.location.search);
        let filters = $('select.sign-request-filter');
        for (let i = 0 ; i < filters.length ; i++) {
            currentParams.set(filters.eq(i).attr('id'), filters.eq(i).val());
        }
        filters = $('input.sign-request-filter');
        for (let i = 0 ; i < filters.length ; i++) {
            currentParams.set(filters.eq(i).attr('id'), filters.eq(i).val());
        }

        if (!this.hasEffectiveFilterChange(currentParams)) {
            return;
        }

        const queryString = currentParams.toString();
        const targetUrl = "/" + this.mode + "/signbooks" + (queryString !== "" ? "?" + queryString : "");
        const currentUrl = window.location.pathname + window.location.search;
        if (targetUrl !== currentUrl) {
            document.location.href = targetUrl;
        }
    }

    hasEffectiveFilterChange(targetParams) {
        const currentParams = new URLSearchParams(window.location.search);
        const filterNames = new Set();
        $('select.sign-request-filter, input.sign-request-filter').each(function () {
            const filterName = $(this).attr('id');
            if (filterName != null && filterName !== '') {
                filterNames.add(filterName);
            }
        });

        for (const filterName of filterNames) {
            if (this.normalizeFilterValue(currentParams.get(filterName)) !== this.normalizeFilterValue(targetParams.get(filterName))) {
                return true;
            }
        }

        return false;
    }

    normalizeFilterValue(value) {
        if (value == null || value === '' || value === 'all' || value === '%') {
            return '';
        }
        return value;
    }

    launchMassSign() {
        $('#massSignModal').modal('hide');
        let signRequestIds = $('.sign-requests-ids:checked');
        let ids = [];
        let nbNotViewed = 0;
        for (let i = 0; i < signRequestIds.length ; i++) {
            let checkbox = signRequestIds.eq(i);
            if(checkbox.attr("data-es-signrequest-status") === 'pending' && checkbox.attr("data-es-signrequest-deleted") !== 'true') {
                ids.push(signRequestIds.eq(i).val());
            }
            if(checkbox.attr("data-es-viewed") === 'false') {
                nbNotViewed++;
            }
        }
        let self = this;
        if(ids.length > 0) {
            if(nbNotViewed > 0) {
                bootbox.confirm({
                    message: "Vous êtes sur le point de signer " + nbNotViewed + " documents sans consultation préalable.<br/>Cette action est irrevocable !<br/>Voulez-vous continuer ?",
                    buttons: {
                        cancel: {
                            label: 'Annuler',
                        },
                        confirm: {
                            label: 'Confirmer la signature',
                            className: 'btn-success'
                        }
                    },
                    callback: function (result) {
                        if (result) {
                            self.massSign(ids);
                        }
                    }
                });
            } else {
                self.massSign(ids);
            }
        } else {
            bootbox.alert("Aucune demande à signer dans la selection", function (){});
        }
    }

    massSign(ids) {
        let waitModal = $("#wait");
        waitModal.modal('show');
        waitModal.modal({backdrop: 'static', keyboard: false});
        this.ensureSealCertificateSelection();
        let signRequestUrlParams;
        signRequestUrlParams = {
            "ids" : JSON.stringify(ids),
            "signWith" : $("#certType").val(),
            "password" : $("#password").val(),
            "sealCertificat" : $("#sealCertificat").val()
        };
        this.reset();
        let self = this;
        $.ajax({
            url: "/" + this.mode + "/signbooks/mass-sign?" + self.csrf.parameterName + "=" + self.csrf.token,
            type: 'POST',
            data: signRequestUrlParams,
            success: function(e) {
                document.location.reload();
            },
            error: function(e) {
                if(e.responseText === "initNexu") {
                    document.location.href="/nexu-sign/start?ids=" + ids;
                } else {
                    bootbox.alert("La signature s'est terminée, d'une façon inattendue. La page va s'actualiser", function () {
                        location.href = "/" + self.mode + "/reports";
                    });
                }
            }
        });
    }

    updateWaitModal(e) {
        console.info("update wait modal");
        let message = e.detail
        console.info(message);
        this.percent = this.percent + 1;
        let bar = $("#bar");
        let barText = $("#bar-text");
        if(message.type === "end") {
            console.info("mass-sign end");
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            barText.after(cloneBarText);
            cloneBarText.before("<br>");
            bar.removeClass("progress-bar-animated");
            barText.html(message.text);
            bar.css("width", 100 + "%");
            $("#closeModal").show();
            $("#validModal").show();
        } else if(message.type === "nextSuccess") {
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            cloneBarText.css("color", "var(--bs-green)");
            barText.after(cloneBarText);
            cloneBarText.before("<br>");
            // barText.html("");
        } else if(message.type === "nextError") {
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            cloneBarText.css("color", "var(--bs-red)");
            barText.after(cloneBarText);
            barText.css("color", "var(--bs-red)");
            cloneBarText.before("<br>");
            if(message.text !== "") {
                cloneBarText.html(message.text);
            }
        } else {
            console.debug("debug - " + "update bar");
            bar.css("display", "block");
            bar.css("width", this.percent + "%");
            barText.html(message.text);
        }
    }

    updateErrorWaitModal(e) {
        console.info("update wait modal");
        let message = e.detail
        console.info(message);
        this.percent = this.percent + 1;
        let barText = $("#bar-text");
        if(message.type === "sign_system_error" || message.type === "not_authorized") {
            console.error("sign error : system error");
            let cloneBarText = barText.clone();
            cloneBarText.attr("id", "");
            cloneBarText.css("color", "var(--bs-red)");
            barText.after(cloneBarText);
            cloneBarText.before("<br>");
            cloneBarText.html(message.text);
        }
    }

    reset() {
        this.percent = 0;
        $("#passwordError").hide();
        $("#signError").hide();
        $("#closeModal").hide();
        $("#validModal").hide();
        let bar = $("#bar");
        bar.hide();
        bar.addClass("progress-bar-animated");
    }

}
