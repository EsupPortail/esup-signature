/**
 * DashboardFilter.js — AFPI-ACM
 * Parsing des tags [APP:][TYPE:][LIEU:][EXPEDITEUR:] dans les sujets de demandes.
 * Rétrocompatibilité avec l'ancien format [CACES:][CENTRE:].
 */
export class DashboardFilter {

    constructor() {
        this.appColorMap  = {};
        this.colorPalette = [
            '#5A8DEE','#39DA8A','#FDAC41','#FF5B5C',
            '#00CFDD','#7B61E4','#475F7B','#F77F00',
            '#2D9CDB','#00B09B'
        ];
        this.colorIdx = 0;
        this.init();
    }

    /* ── Couleur stable par nom d'application ── */
    getAppColor(app) {
        if (!this.appColorMap[app]) {
            this.appColorMap[app] = this.colorPalette[this.colorIdx % this.colorPalette.length];
            this.colorIdx++;
        }
        return this.appColorMap[app];
    }

    /* ── Extraction d'un tag [TAG:valeur] ── */
    extractTag(subject, tag) {
        if (!subject) return '';
        const m = subject.match(new RegExp(`\\[${tag}:([^\\]]+)\\]`, 'i'));
        return m ? m[1].trim() : '';
    }

    /* ── Parse subject → { app, type, lieu, expediteur, cleanDesc } ── */
    parseSubject(subject, workflowName) {
        let app        = this.extractTag(subject, 'APP');
        let type       = this.extractTag(subject, 'TYPE');
        let lieu       = this.extractTag(subject, 'LIEU');
        let expediteur = this.extractTag(subject, 'EXPEDITEUR');

        // Rétrocompat CACES
        if (!app) {
            const caces = this.extractTag(subject, 'CACES');
            if (caces) { app = workflowName || 'CACES'; type = caces; }
        }
        if (!lieu)             lieu = this.extractTag(subject, 'CENTRE');
        if (!app && workflowName) app = workflowName;

        // Description nettoyée (sans les blocs [TAG:val])
        const cleanDesc = subject
            ? subject.replace(/\[[^\]:]+:[^\]]+\]/g, '').trim()
            : '';

        return { app, type, lieu, expediteur, cleanDesc };
    }

    /* ── HTML des badges ── */
    badgeApp(app) {
        const color = this.getAppColor(app);
        return `<span class="afpi-badge-app" style="background:${color}">${this.escapeHtml(app)}</span>`;
    }
    hexToRgba(hex, alpha) {
        hex = hex.replace('#', '');
        let r = parseInt(hex.substring(0, 2), 16);
        let g = parseInt(hex.substring(2, 4), 16);
        let b = parseInt(hex.substring(4, 6), 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }

    getDarkerColor(hex, factor = 0.8) {
        hex = hex.replace('#', '');
        let r = Math.round(parseInt(hex.substring(0, 2), 16) * factor);
        let g = Math.round(parseInt(hex.substring(2, 4), 16) * factor);
        let b = Math.round(parseInt(hex.substring(4, 6), 16) * factor);
        return `rgb(${r}, ${g}, ${b})`;
    }

    badgeType(type, app) {
        if (app) {
            const color = this.getAppColor(app);
            const bgColor = this.hexToRgba(color, 0.12);
            const textColor = this.getDarkerColor(color, 0.75);
            return `<span class="afpi-badge-type" style="background:${bgColor} !important; color:${textColor} !important; border: 1px solid ${this.hexToRgba(color, 0.25)} !important; box-shadow: none !important;">${this.escapeHtml(type)}</span>`;
        }
        return `<span class="afpi-badge-type">${this.escapeHtml(type)}</span>`;
    }

    escapeHtml(s) {
        if (!s) return '';
        return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    /* ── Peuple un <select> existant (tableau de bord) ── */
    populateSelect(id, values) {
        const sel = document.getElementById(id);
        if (!sel) return;
        const sortedVals = [...values].sort();
        if (sel.slim) {
            try {
                const placeholder = sel.getAttribute('data-placeholder') || '';
                sel.slim.setData([
                    { placeholder: true, text: placeholder, value: '' },
                    { text: 'Tout', value: 'all' },
                    ...sortedVals.map(v => ({ text: v, value: v }))
                ]);
            } catch(_) {}
        } else {
            const opts = sortedVals.map(v =>
                `<option value="${this.escapeHtml(v)}">${this.escapeHtml(v)}</option>`
            ).join('');
            sel.innerHTML = `<option value="" data-placeholder="true"></option><option value="all">Tout</option>${opts}`;
        }
    }

    /* ── Construit un <select> inline (accueil) ── */
    buildSelectHtml(id, label, values) {
        const opts = [...values].sort().map(v =>
            `<option value="${this.escapeHtml(v)}">${this.escapeHtml(v)}</option>`
        ).join('');
        return `
        <div class="afpi-filter-group">
            <select id="${id}" class="dashboard-filter-select" data-placeholder="${label}" data-allow-deselect="true">
                <option value="" data-placeholder="true"></option>
                <option value="">Tout</option>
                ${opts}
            </select>
        </div>`;
    }

    /* ── Badge de statut pour l'accueil ── */
    statusBadgeHtml(status) {
        const labels = {
            uploading: "En cours d'upload", draft: 'Brouillon', pending: 'En cours',
            checked: 'Visé', signed: 'Signé', refused: 'Refusé',
            completed: 'Terminé', exported: 'Exporté', deleted: 'À la corbeille'
        };
        const colors = {
            uploading: 'text-bg-secondary', draft: 'text-bg-secondary',
            pending: 'text-bg-warning', checked: 'text-bg-info',
            signed: 'text-bg-success', refused: 'text-bg-danger',
            completed: 'text-bg-success', exported: 'text-bg-success',
            deleted: 'text-bg-danger'
        };
        const label = labels[status] || status;
        const cls   = colors[status]  || 'text-bg-secondary';
        return `<span class="badge rounded-pill badge-status ${cls}">${this.escapeHtml(label)}</span>`;
    }

    /* ── Initialise SlimSelect sur les filtres accueil ── */
    initSlimSelects(filterBar, tableEl, filterId) {
        if (typeof window.SlimSelect === 'undefined') return;
        filterBar.querySelectorAll('.dashboard-filter-select').forEach(sel => {
            try {
                const ss = new window.SlimSelect({
                    select: sel,
                    settings: {
                        showSearch: false,
                        allowDeselect: true,
                        placeholderText: sel.getAttribute('data-placeholder') || 'Tout'
                    }
                });
                sel._slimSelect = ss;
            } catch(_) {}
        });
    }

    /* ── Thead accueil ── */
    buildTheadHtml(tableEl) {
        return `<tr class="afpi-thead-row">
            <th class="afpi-th d-none d-md-table-cell" style="width: 25px;"></th>
            <th class="afpi-th d-none d-md-table-cell" style="width: 25px;"></th>
            <th class="afpi-th" style="width: 15px;"></th>
            <th class="afpi-th d-none d-md-table-cell">Statut</th>
            <th class="afpi-th d-none d-xl-table-cell">Application</th>
            <th class="afpi-th d-none d-xl-table-cell">Type</th>
            <th class="afpi-th d-none d-xxl-table-cell">Lieu</th>
            <th class="afpi-th">Titre du document</th>
            <th class="afpi-th d-none d-xxl-table-cell">Expéditeur</th>
            <th class="afpi-th">Date</th>
            <th class="afpi-th d-none d-md-table-cell">Participants</th>
            <th class="afpi-th d-none d-xxl-table-cell">Circuit</th>
        </tr>`;
    }

    /* ── Réordonne les cellules (tableau de bord) ── */
    reorderDashboardRow(tr) {
        const checkboxTd     = tr.querySelector('td input.sign-requests-ids')?.closest('td') || null;
        const iconsTd        = checkboxTd ? checkboxTd.nextElementSibling : null;
        const statusTd       = tr.querySelector('.col-status');
        const appTd          = tr.querySelector('.tag-col-app');
        const typeTd         = tr.querySelector('.tag-col-type');
        const lieuTd         = tr.querySelector('.tag-col-lieu');
        const titleTd        = tr.querySelector('.col-title');
        const expediteurTd   = tr.querySelector('.tag-col-expediteur');
        const dateTd         = tr.querySelector('.col-date');
        const participantsTd = tr.querySelector('.col-participants');
        const circuitTd      = tr.querySelector('.col-circuit');
        const actionsTd      = tr.querySelector('.col-actions');

        [checkboxTd, iconsTd, statusTd, appTd, typeTd, lieuTd,
         titleTd, expediteurTd, dateTd, participantsTd, circuitTd, actionsTd]
            .filter(Boolean)
            .forEach(td => tr.appendChild(td));
    }

    /* ── Réordonne les cellules (accueil) ── */
    reorderHomeRow(tr) {
        const cells = Array.from(tr.querySelectorAll(':scope > td'));
        if (!cells.length) return;

        /* cells[0]=postit, cells[1]=expand(+), cells[2]=fileIcon, cells[3]=circuit(d-xxl) */
        const iconTd         = cells[0] || null;   /* indicateur vue / postit */
        const statusTd       = tr.querySelector('.tag-col-status') || null;
        const appTd          = tr.querySelector('.tag-col-app');
        const typeTd         = tr.querySelector('.tag-col-type');
        const lieuTd         = tr.querySelector('.tag-col-lieu');
        const titleTd        = tr.querySelector('td.w-30');
        const expediteurTd   = tr.querySelector('.tag-col-expediteur');
        const participantsTd = tr.querySelector('td.w-20.d-none.d-md-table-cell');
        const dateTd         = tr.querySelector('td.w-20:not(.d-none)') || cells[cells.length - 1] || null;
        const circuitTd      = tr.querySelector('td.d-none.d-xxl-table-cell');

        [iconTd, statusTd, appTd, typeTd, lieuTd,
         titleTd, expediteurTd, dateTd, participantsTd, circuitTd]
            .filter(Boolean)
            .forEach(td => tr.appendChild(td));
    }

    /* ── Applique les filtres ── */
    applyFilters(tableEl, filterId) {
        const isDashboard = filterId === 'filter-dashboard';
        const filterBar   = document.getElementById(filterId);

        const get = (id) => document.getElementById(id);
        const selApp  = isDashboard ? get('appFilter')      : filterBar?.querySelector(`#${filterId}-app`);
        const selType = isDashboard ? get('typeFilter')     : filterBar?.querySelector(`#${filterId}-type`);
        const selLieu = isDashboard ? get('locationFilter') : filterBar?.querySelector(`#${filterId}-lieu`);
        const selExp  = isDashboard ? get('senderFilter')   : filterBar?.querySelector(`#${filterId}-exp`);

        const norm  = v => (v === 'all' ? '' : (v || ''));
        const fApp  = norm(selApp?.value);
        const fType = norm(selType?.value);
        const fLieu = norm(selLieu?.value);
        const fExp  = norm(selExp?.value);

        tableEl.querySelectorAll('tbody > tr').forEach(tr => {
            if (tr.querySelector('.collapse')) return;
            const ok = (!fApp  || tr.dataset.filterApp        === fApp)
                    && (!fType || tr.dataset.filterType       === fType)
                    && (!fLieu || tr.dataset.filterLieu       === fLieu)
                    && (!fExp  || tr.dataset.filterExpediteur === fExp);
            tr.style.display = ok ? '' : 'none';
        });
    }

    /* ── Instrumente une table ── */
    instrumentTable(tableEl, filterId) {
        if (tableEl.dataset.dashboardFilterInstrumented === 'true') return;
        const rows = tableEl.querySelectorAll('tbody > tr:not(.collapse-row)');
        if (!rows.length) return;

        tableEl.dataset.dashboardFilterInstrumented = 'true';
        const isDashboard = filterId === 'filter-dashboard';

        const apps = new Set(), types = new Set(),
              lieus = new Set(), expediteurs = new Set();

        rows.forEach(tr => {
            const subject      = tr.dataset.subject      || '';
            const workflowName = tr.dataset.workflowName || '';
            const parsed       = this.parseSubject(subject, workflowName);

            tr.dataset.filterApp        = parsed.app;
            tr.dataset.filterType       = parsed.type;
            tr.dataset.filterLieu       = parsed.lieu;
            tr.dataset.filterExpediteur = parsed.expediteur;

            if (parsed.app)        apps.add(parsed.app);
            if (parsed.type)       types.add(parsed.type);
            if (parsed.lieu)       lieus.add(parsed.lieu);
            if (parsed.expediteur) expediteurs.add(parsed.expediteur);

            /* Éviter la double-instrumentation d'une même ligne */
            if (tr.dataset.dfInstrumented === 'true') return;
            tr.dataset.dfInstrumented = 'true';

            const tdApp = Object.assign(document.createElement('td'), {
                className: 'd-none d-xl-table-cell tag-col-app afpi-tag-cell',
                innerHTML: parsed.app  ? this.badgeApp(parsed.app)   : ''
            });
            const tdType = Object.assign(document.createElement('td'), {
                className: 'd-none d-xl-table-cell tag-col-type afpi-tag-cell',
                innerHTML: parsed.type ? this.badgeType(parsed.type, parsed.app) : ''
            });
            const tdLieu = Object.assign(document.createElement('td'), {
                className: 'd-none d-xxl-table-cell tag-col-lieu afpi-tag-cell afpi-meta',
                textContent: parsed.lieu
            });
            const tdExp = Object.assign(document.createElement('td'), {
                className: 'd-none d-xxl-table-cell tag-col-expediteur afpi-tag-cell afpi-meta',
                textContent: parsed.expediteur
            });

            /* Badge de statut pour l'accueil */
            if (!isDashboard) {
                const status = tr.dataset.status || '';
                const tdStatus = Object.assign(document.createElement('td'), {
                    className: 'd-none d-md-table-cell tag-col-status afpi-tag-cell',
                    innerHTML: this.statusBadgeHtml(status)
                });
                tr.insertBefore(tdStatus, tr.firstChild.nextSibling || tr.firstChild);
            }

            const titleTd = tr.querySelector('.col-title') || tr.querySelector('td.w-30');
            const dateTd  = tr.querySelector('.col-date')  || tr.querySelector('td:last-child');

            if (titleTd) {
                tr.insertBefore(tdApp,  titleTd);
                tr.insertBefore(tdType, titleTd);
                tr.insertBefore(tdLieu, titleTd);
                tr.insertBefore(tdExp,  dateTd && dateTd !== titleTd ? dateTd : titleTd);
            } else if (dateTd) {
                [tdApp, tdType, tdLieu, tdExp].forEach(td => tr.insertBefore(td, dateTd));
            } else {
                tr.append(tdApp, tdType, tdLieu, tdExp);
            }

            /* Nettoyage du titre : suppression des tags [TAG:val] */
            if (titleTd && parsed.cleanDesc) {
                titleTd.setAttribute('title', subject);
                if (isDashboard && titleTd.childElementCount === 0) {
                    titleTd.textContent = parsed.cleanDesc;
                } else if (!isDashboard && titleTd.childElementCount === 1) {
                    const span = titleTd.querySelector('span');
                    if (span) span.textContent = parsed.cleanDesc;
                }
            }

            isDashboard ? this.reorderDashboardRow(tr) : this.reorderHomeRow(tr);
        });

        /* Badge de comptage dans l'en-tête de carte (accueil) */
        if (!isDashboard) {
            const headerDiv = tableEl.closest('.card')?.querySelector('.card-header');
            if (headerDiv && !headerDiv.querySelector('.afpi-card-count')) {
                const h5 = headerDiv.querySelector('h5');
                if (h5) {
                    const badge = document.createElement('span');
                    badge.className = 'afpi-card-count';
                    badge.textContent = rows.length;
                    h5.prepend(badge);
                }
            }
        }

        if (isDashboard) {
            this.populateSelect('appFilter',      apps);
            this.populateSelect('typeFilter',     types);
            this.populateSelect('locationFilter', lieus);
            this.populateSelect('senderFilter',   expediteurs);

            ['appFilter','typeFilter','locationFilter','senderFilter'].forEach(id => {
                const el = document.getElementById(id);
                if (el && !el.dataset.dashboardFilterBound) {
                    el.dataset.dashboardFilterBound = 'true';
                    el.addEventListener('change', () => this.applyFilters(tableEl, filterId));
                }
            });
        } else {
            let thead = tableEl.querySelector('thead');
            if (!thead) { thead = document.createElement('thead'); tableEl.prepend(thead); }
            thead.innerHTML = this.buildTheadHtml(tableEl);

            const filterBar = document.getElementById(filterId);
            if (filterBar && (apps.size || types.size || lieus.size || expediteurs.size)) {
                filterBar.innerHTML = `
                <div class="afpi-filter-bar">
                    <span class="afpi-filter-label-main"><i class="fa-solid fa-filter"></i> Filtrer :</span>
                    ${apps.size        ? this.buildSelectHtml(filterId+'-app',  'Application', apps)        : ''}
                    ${types.size       ? this.buildSelectHtml(filterId+'-type', 'Type',        types)       : ''}
                    ${lieus.size       ? this.buildSelectHtml(filterId+'-lieu', 'Lieu',        lieus)       : ''}
                    ${expediteurs.size ? this.buildSelectHtml(filterId+'-exp',  'Expéditeur',  expediteurs) : ''}
                    <button class="afpi-filter-reset" id="${filterId}-reset">
                        <i class="fa-solid fa-xmark"></i> Effacer
                    </button>
                </div>`;

                filterBar.querySelectorAll('.dashboard-filter-select')
                    .forEach(s => s.addEventListener('change', () => this.applyFilters(tableEl, filterId)));

                this.initSlimSelects(filterBar, tableEl, filterId);

                const resetBtn = document.getElementById(filterId + '-reset');
                if (resetBtn) {
                    resetBtn.addEventListener('click', () => {
                        filterBar.querySelectorAll('.dashboard-filter-select').forEach(s => {
                            if (s._slimSelect) { try { s._slimSelect.setSelected(''); } catch(_) {} }
                            s.value = '';
                        });
                        this.applyFilters(tableEl, filterId);
                    });
                }
                filterBar.classList.remove('d-none');
            }
        }
    }

    /* ── MutationObserver pour le chargement AJAX du tableau de bord ── */
    watchDashboardTable() {
        const container = document.querySelector('#listSignRequestTable');
        if (!container) return;

        let timer;
        const self = this;
        const observer = new MutationObserver(() => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                const tbl = container.querySelector('table');
                if (!tbl) return;
                tbl.removeAttribute('data-dashboard-filter-instrumented');
                self.instrumentTable(tbl, 'filter-dashboard');
            }, 120);
        });

        const tbody = container.querySelector('tbody');
        if (tbody) {
            observer.observe(tbody, { childList: true });
        } else {
            observer.observe(container, { childList: true, subtree: true });
        }
    }

    /* ── Point d'entrée ── */
    init() {
        document.addEventListener('DOMContentLoaded', () => {
            const toSignTable  = document.querySelector('#to-sign-list table');
            const pendingTable = document.querySelector('#pendingList table');

            if (toSignTable)  this.instrumentTable(toSignTable,  'filter-to-sign');
            if (pendingTable) this.instrumentTable(pendingTable, 'filter-pending');

            this.watchDashboardTable();
        });
    }
}
