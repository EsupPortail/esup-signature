export class HomeUi {

    constructor(bootstrapUrl = '/ws-secure/ui/home') {
        this.bootstrapUrl = bootstrapUrl;
        this.bootstrap = null;
        this.slimselect = null;
        this.startWhenDomReady();
    }

    startWhenDomReady() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => void this.initialize(), {once: true});
        } else {
            void this.initialize();
        }
    }

    setHomeLoadingState(isLoading) {
        document.body.classList.toggle('home-loading', Boolean(isLoading));
    }

    async initialize() {
        console.info('Starting home UI');
        this.setHomeLoadingState(true);
        try {
            this.bootstrap = await this.loadBootstrap();
            this.renderHomeLists();
            this.initWorkflowDeleteForms();
            this.initFavoriteTriggers();
            this.initFavoriteToggles();
            this.initPendingListToggle();
            this.initToSignToggle();
            this.initWarningModals();
            this.initMegaSearch();
            this.handleRequestedStart();
        } finally {
            window.requestAnimationFrame(() => this.setHomeLoadingState(false));
        }
    }

    async loadBootstrap() {
        try {
            return await this.fetchJson(this.buildBootstrapUrl());
        } catch (error) {
            console.debug('Unable to load home bootstrap', error);
            return {
                startFormId: null,
                startWorkflowId: null,
                warningReadUrl: '/ws-secure/ui/warnings/read',
                searchUrl: '/user/search',
                searchTitlesUrl: '/user/search-titles',
                toSignSignBooks: [],
                pendingSignBooks: []
            };
        }
    }

    renderHomeLists() {
        this.renderHomeSignBookList('to-sign-list', this.bootstrap?.toSignSignBooks || [], 'Aucun document à signer pour le moment');
        this.renderHomeSignBookList('pending-list', this.bootstrap?.pendingSignBooks || [], 'Aucun document à signer pour le moment');
    }

    renderHomeSignBookList(containerId, signBooks, emptyMessage) {
        const container = document.getElementById(containerId);
        if (container == null) {
            return;
        }

        if (!Array.isArray(signBooks) || signBooks.length === 0) {
            container.innerHTML = '<div class="alert alert-secondary text-center w-100 mb-0">' + this.escapeHtml(emptyMessage) + '</div>';
            return;
        }

        const rows = signBooks.map(signBook => this.renderHomeSignBookRows(signBook)).join('');
        container.innerHTML = `
            <div class="div-scrollable scrollbar-style rounded-3" style="max-height: 400px; overflow-x: hidden;">
                <div class="d-flex col-12 mb-2">
                    <table class="table table-sm table-borderless table-striped table-hover table-light mb-0">
                        <thead class="table-secondary">
                            <tr>
                                <th style="width: 80px;"></th>
                                <th class="text-break d-none d-md-table-cell" style="width: 40px;"></th>
                                <th class="col-nom" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem);">Nom</th>
                                <th class="d-none d-xxl-table-cell" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem);">Type</th>
                                <th style="font-size: clamp(0.75rem, 1.2vw, 0.875rem);">Émis le</th>
                            </tr>
                        </thead>
                        <tbody>${rows}</tbody>
                    </table>
                </div>
            </div>
        `;

        this.bindRenderedHomeRows(container);
    }

    renderHomeSignBookRows(signBook) {
        if (signBook == null || !Array.isArray(signBook.signRequests) || signBook.signRequests.length === 0) {
            return '';
        }

        const multiple = signBook.signRequests.length > 1;
        const rowId = 'row_' + signBook.id;
        const dropdownId = 'commentButton-' + signBook.id;
        const unreadClass = signBook.viewedByCurrentUser ? '' : 'fw-bold';
        const description = signBook.description || '';
        const subject = signBook.subject || '';
        const workflowName = signBook.workflowName || '';
        const createDateLabel = signBook.createDateLabel || '';
        const listTitle = signBook.listTitle || subject;

        return `
            <tr title="${this.escapeHtml(description)}"
                data-href="/user/signrequests/${encodeURIComponent(signBook.primarySignRequestId)}"
                class="${multiple ? '' : 'clickable-row'}"
                ${multiple ? 'data-bs-toggle="collapse" data-bs-target="#' + this.escapeHtml(rowId) + '"' : ''}
                style="font-size: clamp(0.75rem, 1.2vw, 0.875rem);">
                <td>
                    <div class="d-flex flex-row align-items-center justify-content-between gap-1">
                        ${signBook.viewedByCurrentUser
                            ? '<i class="fi fi-rr-circle text-transparent" title="Le document a été lu jusqu\'à la dernière page"></i>'
                            : '<i class="fi fi-rr-circle text-danger" title="Le document n\'a pas été lu jusqu\'à la dernière page"></i>'}
                        ${signBook.hasAttachments
                            ? '<i class="fi fi-rr-clip text-dark" title="La demande contient des pièces jointes"></i>'
                            : '<i class="fi fi-rr-clip text-transparent"></i>'}
                        ${this.renderPostitButton(signBook, dropdownId)}
                        ${this.renderPostitDropdown(signBook, dropdownId)}
                    </div>
                </td>
                <td class="text-break d-none d-md-table-cell">
                    <i class="${multiple ? 'fi fi-rr-folder-open' : 'fi fi-rr-file'}" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem);"></i>
                </td>
                <td class="text-break ${unreadClass}" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem); min-width: 200px; overflow: hidden; text-overflow: ellipsis;">
                    ${multiple
                        ? '<span>' + this.escapeHtml(listTitle) + ' <i class="fa-solid fa-caret-down"></i></span>'
                        : '<span>' + this.escapeHtml(subject) + '</span>'}
                </td>
                <td class="text-break d-none d-xxl-table-cell ${unreadClass}" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem); max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${this.escapeHtml(workflowName)}</td>
                <td class="text-break text-nowrap ${unreadClass}" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem); min-width: 120px;">${this.escapeHtml(createDateLabel)}</td>
            </tr>
            ${multiple ? this.renderHomeSecondaryRow(signBook, rowId, description) : ''}
        `;
    }

    renderHomeSecondaryRow(signBook, rowId, description) {
        const nestedRows = signBook.signRequests.map(signRequest => `
            <tr class="clickable-row" data-href="/user/signrequests/${encodeURIComponent(signRequest.id)}" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem);">
                <td style="width: 40px;">
                    <i class="fi fi-rr-file" style="font-size: clamp(0.75rem, 1.2vw, 0.875rem);"></i>
                </td>
                <td style="max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${this.escapeHtml(signRequest.title || '')}</td>
                <td style="min-width: 100px;">
                    ${this.renderStatusBadge(signRequest.status)}
                </td>
            </tr>
        `).join('');

        return `
            <tr title="${this.escapeHtml(description)}">
                <td></td>
                <td></td>
                <td colspan="4">
                    <div class="collapse" id="${this.escapeHtml(rowId)}">
                        <table class="table table-hover mb-0">
                            ${nestedRows}
                        </table>
                    </div>
                </td>
            </tr>
        `;
    }

    renderPostitButton(signBook, dropdownId) {
        const description = (signBook.description || '').trim();
        const postits = Array.isArray(signBook.postits) ? signBook.postits : [];
        const badgeCount = postits.length + (description !== '' ? 1 : 0);
        if (badgeCount === 0) {
            return '<button type="button" class="badge bg-postit border-0 opacity-0">0</button>';
        }
        return `
            <button type="button"
                    title="Afficher les postits"
                    class="badge bg-postit border-0 home-postit-toggle"
                    id="${this.escapeHtml(dropdownId)}"
                    data-bs-toggle="dropdown"
                    data-es-stop-propagation="true">${badgeCount}</button>
        `;
    }

    renderPostitDropdown(signBook, dropdownId) {
        const entries = [];
        const description = (signBook.description || '').trim();
        if (description !== '') {
            entries.push(`
                <li class="dropdown-item" data-es-stop-propagation="true">
                    <pre class="me-1 mb-0" style="font-size: clamp(0.7rem, 1vw, 0.8rem);">${this.escapeHtml(description)}</pre>
                </li>
            `);
        }

        (Array.isArray(signBook.postits) ? signBook.postits : []).forEach(postit => {
            if (postit == null) {
                return;
            }
            const label = ((postit.author || '').trim() !== '' ? (postit.author + ' : ') : '') + (postit.text || '');
            entries.push(`
                <li class="dropdown-item" data-es-stop-propagation="true">
                    <pre class="me-1 mb-0" style="font-size: clamp(0.7rem, 1vw, 0.8rem);">${this.escapeHtml(label)}</pre>
                </li>
            `);
        });

        return `<ul class="dropdown-menu striped-list" aria-labelledby="${this.escapeHtml(dropdownId)}" data-es-stop-propagation="true">${entries.join('')}</ul>`;
    }

    renderStatusBadge(status) {
        if (status === 'pending') {
            return '<span class="badge rounded-pill bg-warning text-dark" style="font-size: clamp(0.65rem, 1vw, 0.75rem);"><i class="fa-solid fa-clock"></i> À signer</span>';
        }
        if (status === 'refused') {
            return '<span class="badge rounded-pill bg-danger" style="font-size: clamp(0.65rem, 1vw, 0.75rem);"><i class="fa-solid fa-clock"></i> Refusé</span>';
        }
        return '<span class="badge rounded-pill bg-success" style="font-size: clamp(0.65rem, 1vw, 0.75rem);"><i class="fa-solid fa-check-circle"></i> Terminé</span>';
    }

    bindRenderedHomeRows(container) {
        container.querySelectorAll('.clickable-row').forEach(row => {
            row.addEventListener('click', event => this.handleClickableRow(event));
        });
        container.querySelectorAll('[data-es-stop-propagation="true"]').forEach(element => {
            element.addEventListener('click', event => event.stopPropagation());
        });
    }

    handleClickableRow(event) {
        const row = event.currentTarget?.closest('tr');
        const url = row?.getAttribute('data-href');
        if (url == null || url === '') {
            return;
        }
        if (event.ctrlKey || event.metaKey) {
            window.open(url, '_blank');
            return;
        }
        window.location = url;
    }

    escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    buildBootstrapUrl() {
        const url = new URL(this.bootstrapUrl, window.location.origin);
        const currentUrl = new URL(window.location.href);
        const formId = currentUrl.searchParams.get('formId');
        const workflowId = currentUrl.searchParams.get('workflowId');
        if (formId != null && formId !== '') {
            url.searchParams.set('formId', formId);
        }
        if (workflowId != null && workflowId !== '') {
            url.searchParams.set('workflowId', workflowId);
        }
        return url.toString();
    }

    async fetchJson(url, options = {}) {
        const response = await fetch(url, {
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json',
                ...(options.headers || {})
            },
            ...options
        });
        if (!response.ok) {
            throw new Error('HTTP ' + response.status + ' for ' + url);
        }
        return response.json();
    }

    confirm(message, onConfirm) {
        if (window.bootbox?.confirm) {
            bootbox.confirm(message, result => {
                if (result) {
                    onConfirm();
                }
            });
            return;
        }
        if (window.confirm(message.replace(/<br\s*\/?>/gi, '\n'))) {
            onConfirm();
        }
    }

    showAlert(message) {
        if (window.bootbox?.alert) {
            bootbox.alert(message);
        } else {
            window.alert(message);
        }
    }

    initWorkflowDeleteForms() {
        document.querySelectorAll('[id^="deleteWorkflow_"]').forEach(form => {
            form.addEventListener('submit', event => {
                event.preventDefault();
                this.confirm(
                    'Pour enlever le favori, utiliser le bouton + <br>Sinon, confirmez-vous la suppression définitive de ce circuit ?',
                    () => form.submit()
                );
            });
        });
    }

    initFavoriteTriggers() {
        document.querySelectorAll('.workflow-favorite-trigger').forEach(trigger => {
            trigger.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                const workflowId = trigger.dataset.esWorkflowId;
                document.querySelector('.toggle-workflow-start[data-es-item-id="' + workflowId + '"]')?.click();
            });
        });

        document.querySelectorAll('.form-favorite-trigger').forEach(trigger => {
            trigger.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                const formId = trigger.dataset.esFormId;
                document.querySelector('.toggle-form-start[data-es-item-id="' + formId + '"]')?.click();
            });
        });
    }

    initFavoriteToggles() {
        document.querySelectorAll('.toggle-workflow-start').forEach(button => {
            button.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                void this.toggleFavorite('workflow', button);
            });
        });

        document.querySelectorAll('.toggle-form-start').forEach(button => {
            button.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                void this.toggleFavorite('form', button);
            });
        });
    }

    async toggleFavorite(type, button) {
        const id = button.dataset.esItemId;
        if (id == null || id === '') {
            return;
        }
        try {
            const response = await fetch('/user/toggle-favorite-' + type + '/' + encodeURIComponent(id), {
                method: 'GET',
                credentials: 'same-origin',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }

            const isFavorite = await response.json();
            const icon = button.querySelector('i');
            if (icon != null) {
                icon.classList.toggle('text-warning', Boolean(isFavorite));
                icon.classList.toggle('text-secondary', !Boolean(isFavorite));
            }
            button.title = isFavorite ? 'Supprimer des favoris' : 'Ajouter aux favoris';
            this.syncFavoriteItem(type, id, Boolean(isFavorite));
            this.syncFavoriteEmptyState(type);
        } catch (error) {
            console.error('Erreur lors de la mise en favori :', error);
        }
    }

    syncFavoriteItem(type, id, isFavorite) {
        if (type === 'workflow') {
            this.getFavoriteWorkflowContainer(id)?.classList.toggle('d-none', !isFavorite);
            return;
        }
        const favoriteLink = document.querySelector('#myFavoritesForms [data-es-form-id="' + id + '"]');
        const target = favoriteLink?.closest('.form-button') || favoriteLink;
        target?.classList.toggle('d-none', !isFavorite);
    }

    syncFavoriteEmptyState(type) {
        if (type === 'workflow') {
            const emptyState = document.getElementById('noFavoritesWorkflows');
            const favoritesRoot = document.getElementById('myFavoritesWorkflows');
            const visibleItems = favoritesRoot == null
                ? []
                : Array.from(favoritesRoot.querySelectorAll('[id^="btn-workflow-"]'))
                    .filter(element => !element.classList.contains('d-none'));
            emptyState?.classList.toggle('d-none', visibleItems.length > 0);
            return;
        }
        const emptyState = document.getElementById('noFavoritesForms');
        const visibleItems = Array.from(document.querySelectorAll('#myFavoritesForms [data-es-form-id]'))
            .filter(element => !element.classList.contains('d-none'));
        emptyState?.classList.toggle('d-none', visibleItems.length > 0);
    }

    getFavoriteWorkflowContainer(id) {
        const favoritesRoot = document.getElementById('myFavoritesWorkflows');
        if (favoritesRoot == null) {
            return null;
        }

        return favoritesRoot.querySelector('[id="btn-workflow-' + id + '"]')
            || favoritesRoot.querySelector('[data-es-workflow-id="' + id + '"]')?.closest('[id^="btn-workflow-"]')
            || null;
    }

    initWarningModals() {
        const oldSignRequests = $('#oldSignRequests');
        if (oldSignRequests.length) {
            oldSignRequests.modal('show');
            document.getElementById('warningReaded')?.addEventListener('click', () => void this.markWarningsRead());
        }

        const recipientNotPresentSignRequests = $('#recipientNotPresentSignRequests');
        if (recipientNotPresentSignRequests.length) {
            recipientNotPresentSignRequests.modal('show');
        }
    }

    async markWarningsRead() {
        try {
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
            await fetch(this.bootstrap?.warningReadUrl || '/ws-secure/ui/warnings/read', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    ...(csrfToken && csrfHeader ? {[csrfHeader]: csrfToken} : {})
                }
            });
        } catch (error) {
            console.debug('Unable to mark warnings as read', error);
        }
    }

    handleRequestedStart() {
        const startFormId = this.bootstrap?.startFormId;
        const startWorkflowId = this.bootstrap?.startWorkflowId;

        if (startFormId != null) {
            const formButton = document.getElementById('form-button-' + startFormId)
                || document.querySelector('.start-form-button[data-es-form-id="' + startFormId + '"]');
            if (formButton != null) {
                formButton.click();
            } else {
                this.showAlert("Ce formulaire n'a pas été trouvé. Vérifier si vous avez bien les droits pour accéder à ce formulaire");
            }
        }

        if (startWorkflowId != null) {
            const workflowButton = document.getElementById('workflow-button-' + startWorkflowId)
                || document.querySelector('.start-wizard-workflow-button[data-es-workflow-id="' + startWorkflowId + '"]');
            if (workflowButton != null) {
                workflowButton.click();
            } else {
                this.showAlert("Ce circuit n'a pas été trouvé. Vérifier si vous avez bien les droits pour accéder à ce circuit");
            }
        }
    }

    initPendingListToggle() {
        const toggleBtn = document.getElementById('toggle-pending-list');
        const pendingContainer = document.getElementById('pending-list');
        const toggleIcon = document.getElementById('pending-toggle-icon');
        if (toggleBtn == null || pendingContainer == null || toggleIcon == null) {
            return;
        }

        const applyState = collapsed => {
            pendingContainer.style.display = collapsed ? 'none' : 'block';
            toggleIcon.className = collapsed ? 'fi fi-rr-angle-down' : 'fi fi-rr-angle-up';
        };

        const isCollapsed = localStorage.getItem('pendingListCollapsed') === 'true';
        applyState(isCollapsed);

        toggleBtn.addEventListener('click', () => {
            const newState = !(localStorage.getItem('pendingListCollapsed') === 'true');
            localStorage.setItem('pendingListCollapsed', String(newState));
            applyState(newState);
        });
    }

    initToSignToggle() {
        const toggleBtn = document.getElementById('toggleSizeBtn');
        const toSignDiv = document.getElementById('toSignDiv');
        const toggleIcon = document.getElementById('toggleIcon');
        const myFavorites = document.getElementById('myFavorites');
        if (toggleBtn == null || toSignDiv == null || toggleIcon == null) {
            return;
        }

        const applyState = expanded => {
            if (expanded) {
                toSignDiv.style.flex = '1 1 100%';
                if (myFavorites != null) {
                    myFavorites.style.display = 'none';
                }
                toggleIcon.className = 'fi fi-rr-compress';
                toggleBtn.title = 'Réduire';
                return;
            }

            toSignDiv.style.flex = '1 1 auto';
            if (myFavorites != null) {
                myFavorites.style.display = 'flex';
            }
            toggleIcon.className = 'fi fi-rr-expand';
            toggleBtn.title = 'Agrandir';
        };

        const isExpanded = localStorage.getItem('toSignExpanded') === 'true';
        applyState(isExpanded);

        toggleBtn.addEventListener('click', () => {
            const newState = !(localStorage.getItem('toSignExpanded') === 'true');
            localStorage.setItem('toSignExpanded', String(newState));
            applyState(newState);
        });
    }

    initMegaSearch() {
        const selectEl = document.getElementById('mega-search');
        if (selectEl == null || typeof SlimSelect === 'undefined') {
            return;
        }

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const select = $('#mega-search');
        const megaResult = $('#mega-result');
        const megaBody = $('#mega-result-body');
        const initialOptions = Array.from(selectEl.options).map(option => ({
            text: option.text,
            value: option.value,
            html: option.dataset.html || option.text,
            selected: option.selected || false,
            disabled: option.disabled || false,
        }));
        let addedOptions = [];
        let fetchedOptions = [];
        let savedOptions = [];
        const placeholderText = select.attr('es-search-text');
        const searchUrl = this.bootstrap?.searchUrl || '/user/search';
        const searchTitlesUrl = this.bootstrap?.searchTitlesUrl || '/user/search-titles';

        const observer = new MutationObserver(mutations => {
            for (const mutation of mutations) {
                if (mutation.type === 'attributes' && mutation.attributeName === 'data-id') {
                    const id = selectEl.getAttribute('data-id');
                    const element = $('div[data-id="' + id + '"]');
                    const list = element.find('.ss-list');
                    list.addClass('border border-secondary-subtl rounded-2');
                    list.css('max-height', '170px');
                    element.css('min-height', '80%');
                    if (element.length) {
                        element.append(megaResult);
                    }
                }
            }
        });
        observer.observe(selectEl, {attributes: true});

        const buildSearchInfoMessage = message => {
            megaBody.html('<div class="alert alert-secondary">' + message + '</div>');
        };

        const renderSearchResults = datas => {
            if (!Array.isArray(datas) || datas.length === 0) {
                buildSearchInfoMessage('Aucun résultat');
                return;
            }

            const rows = datas.map(item => {
                const d = item.date ? new Date(item.date) : null;
                const pad = number => String(number).padStart(2, '0');
                const dateLabel = d != null && !Number.isNaN(d.getTime())
                    ? pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear() + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
                    : '';
                return `
                    <a href="${item.url}" class="list-group-item list-group-item-action d-flex flex-row justify-content-between align-items-center gap-5">
                        <i class="${item.icon}" style="width: 50px;"></i><div style="width: 100%;">${item.title}</div><div style="width: 300px;">${dateLabel}</div><div style="width: 300px;">${item.status || ''}</div><div style="width: 300px;">${item.tags || ''}</div>
                    </a>
                `;
            }).join('');

            megaBody.html('<div class="list-group list-group-flush">' + rows + '</div>');
        };

        this.slimselect = new SlimSelect({
            select: '#mega-search',
            settings: {
                placeholderText: placeholderText,
                searchText: 'Aucun résultat',
                searchingText: 'Recherche en cours',
                searchPlaceholder: 'Rechercher',
                searchHighlight: false,
                hideSelectedOption: true,
                closeOnSelect: false,
                allowDeselect: true,
                maxValuesShown: 40,
            },
            events: {
                beforeOpen: () => {
                    megaResult.modal('show');
                    $('.modal-backdrop').each(function () {
                        $(this).css('opacity', '0.2');
                        $(this).css('z-index', '1');
                    });
                },
                beforeClose: () => {
                    const selectedValues = this.slimselect.getSelected();
                    const allOptions = this.deduplicateOptions([
                        ...fetchedOptions,
                        ...addedOptions,
                        ...initialOptions
                    ]);
                    savedOptions = allOptions.filter(option => selectedValues.includes(option.value));
                    megaResult.modal('hide');
                },
                afterChange: newVal => {
                    if (!Array.isArray(newVal) || newVal.length === 0) {
                        buildSearchInfoMessage('Sélectionnez des éléments dans la barre de recherche pour obtenir des résultats.');
                        return Promise.resolve();
                    }

                    return new Promise((resolve, reject) => {
                        const headers = {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        };
                        if (csrfToken != null && csrfHeader != null) {
                            headers[csrfHeader] = csrfToken;
                        }

                        fetch(searchUrl, {
                            method: 'POST',
                            credentials: 'same-origin',
                            headers: headers,
                            body: JSON.stringify(newVal)
                        })
                            .then(response => {
                                if (!response.ok) {
                                    throw new Error('HTTP ' + response.status);
                                }
                                return response.json();
                            })
                            .then(data => {
                                renderSearchResults(data);
                                resolve();
                            })
                            .catch(error => {
                                buildSearchInfoMessage('Erreur de recherche');
                                reject(error);
                            });
                    });
                },
                addable: value => {
                    const existing = this.deduplicateOptions([...fetchedOptions, ...addedOptions]).find(option => option.value === value);
                    if (existing != null) {
                        return existing;
                    }
                    const newOption = {
                        text: value,
                        value: value,
                        html: '<i class="fa-solid fa-magnifying-glass"></i> ' + value
                    };
                    addedOptions.push(newOption);
                    return newOption;
                },
                search: search => {
                    if (search == null || search === '') {
                        const availableOptions = this.deduplicateOptions([...initialOptions, ...savedOptions]);
                        this.slimselect.setData(availableOptions);
                        this.slimselect.setSelected(this.slimselect.getSelected());
                        return Promise.resolve(availableOptions);
                    }

                    return new Promise((resolve, reject) => {
                        fetch(searchTitlesUrl + '?searchString=' + encodeURIComponent(search), {
                            method: 'GET',
                            credentials: 'same-origin',
                            headers: {
                                'Accept': 'application/json'
                            }
                        })
                            .then(response => {
                                if (!response.ok) {
                                    throw new Error('HTTP ' + response.status);
                                }
                                return response.json();
                            })
                            .then(json => {
                                if (!Array.isArray(json) || json.length === 0) {
                                    reject('Pas de résultat');
                                    return;
                                }

                                fetchedOptions = this.deduplicateOptions(json);
                                savedOptions = this.deduplicateOptions([
                                    ...fetchedOptions,
                                    ...addedOptions,
                                    ...initialOptions
                                ]);
                                resolve(fetchedOptions);
                            })
                            .catch(() => reject('Erreur de recherche'));
                    });
                }
            }
        });

        document.querySelectorAll('.ss-search > input').forEach(input => {
            input.addEventListener('click', event => event.stopPropagation());
        });

        document.getElementById('reset-mega-search')?.addEventListener('click', () => {
            this.slimselect.setSelected([]);
            buildSearchInfoMessage('Sélectionnez des éléments dans la barre de recherche pour obtenir des résultats.');
        });
    }

    deduplicateOptions(options) {
        return (options || []).filter((option, index, array) => {
            return option?.value != null && array.findIndex(current => current?.value === option.value) === index;
        });
    }
}