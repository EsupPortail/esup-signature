(() => {
    const navigateWithFilters = (updates) => {
        const currentUrl = new URL(window.location.href);
        const targetUrl = new URL(window.location.href);

        Object.entries(updates).forEach(([key, value]) => {
            if (Array.isArray(value)) {
                if (value.length > 0) {
                    targetUrl.searchParams.set(key, value.join(','));
                } else {
                    targetUrl.searchParams.delete(key);
                }
                return;
            }

            if (value == null || value === '') {
                targetUrl.searchParams.delete(key);
            } else {
                targetUrl.searchParams.set(key, value);
            }
        });

        if (targetUrl.pathname === currentUrl.pathname && targetUrl.search === currentUrl.search) {
            return;
        }

        window.location.assign(targetUrl.pathname + targetUrl.search + targetUrl.hash);
    };

    const showFiltersWhenReady = () => {
        document.querySelectorAll('.admin-list-filters-loading').forEach(filters => {
            const selects = filters.querySelectorAll('select.slim-select-simple');
            let attempts = 0;
            const reveal = () => {
                const ready = Array.from(selects).every(select => select.nextElementSibling?.classList.contains('ss-main'));
                if (ready || attempts >= 60) {
                    filters.classList.remove('admin-list-filters-loading');
                    return;
                }
                attempts++;
                requestAnimationFrame(reveal);
            };
            reveal();
        });
    };

    const normalize = value => value.trim().toLocaleLowerCase();

    document.querySelectorAll('.js-admin-list-filter').forEach(select => {
        select.addEventListener('change', function () {
            const filterName = this.dataset.filterName;
            if (!filterName) {
                return;
            }

            if (this.multiple) {
                navigateWithFilters({[filterName]: Array.from(this.selectedOptions).map(option => option.value)});
                return;
            }

            navigateWithFilters({[filterName]: this.value});
        });
    });

    document.querySelectorAll('.js-search-name').forEach(input => {
        const table = document.getElementById(input.dataset.tableId);
        const columnIndex = Number(input.dataset.colIndex);
        if (!table || Number.isNaN(columnIndex)) {
            return;
        }

        input.addEventListener('input', () => {
            const search = normalize(input.value);
            table.querySelectorAll('tbody tr').forEach(row => {
                const cell = row.cells[columnIndex];
                row.classList.toggle('d-none', search !== '' && !normalize(cell ? cell.textContent : '').includes(search));
            });
        });
    });

    document.querySelectorAll('.js-sort-name').forEach(button => {
        const table = document.getElementById(button.dataset.tableId);
        const columnIndex = Number(button.dataset.colIndex);
        if (!table || !table.tBodies.length || Number.isNaN(columnIndex)) {
            return;
        }

        button.addEventListener('click', () => {
            const tbody = table.tBodies[0];
            const direction = button.dataset.direction === 'asc' ? -1 : 1;
            button.dataset.direction = direction === 1 ? 'asc' : 'desc';
            const icon = button.querySelector('i');
            if (icon) {
                icon.className = direction === 1 ? 'fi fi-rr-sort-amount-down' : 'fi fi-rr-sort-amount-up';
            }

            Array.from(tbody.rows)
                .sort((a, b) => direction * normalize(a.cells[columnIndex]?.textContent || '').localeCompare(normalize(b.cells[columnIndex]?.textContent || ''), 'fr'))
                .forEach(row => tbody.appendChild(row));
        });
    });

    document.querySelectorAll('.js-stop-propagation').forEach(element => {
        element.addEventListener('click', event => event.stopPropagation());
    });

    showFiltersWhenReady();
})();
