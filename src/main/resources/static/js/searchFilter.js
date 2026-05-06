function initSearchFilter() {
    const filterBtn = document.getElementById('filter-btn');
    const filterPanel = document.getElementById('filter-panel');
    const searchInput = document.getElementById('search-bar');
    const searchBtn = document.querySelector('.search-submit');
    const activeFilters = document.getElementById('active-filters');

    if (!filterBtn || !filterPanel || !searchInput || !searchBtn || !activeFilters) {
        return;
    }

    let cat = null;
    let sort = null;

    function syncFromUrl() {
        const params = new URLSearchParams(window.location.search);
        cat = params.get('category');
        sort = params.get('sort');
        searchInput.value = params.get('search') || params.get('name') || '';

        updateSelectedOptions();
        renderChips();
    }

    function updateSelectedOptions() {
        filterPanel.querySelectorAll('.filter-option').forEach(function(option) {
            const type = option.dataset.filterType;
            const value = option.dataset.filterValue;
            const selected = (type === 'category' && value === cat) || (type === 'price' && value === sort);

            option.classList.toggle('selected', selected);
            option.setAttribute('aria-checked', selected ? 'true' : 'false');
        });
    }

    function renderChips() {
        activeFilters.innerHTML = '';

        if (cat) {
            addChip(cat, 'cat');
        }

        if (sort) {
            addChip(sort === 'price-asc' ? 'Low to High' : 'High to Low', 'sort');
        }
    }

    function addChip(text, type) {
        const chip = document.createElement('div');
        const removeButton = document.createElement('button');

        chip.className = 'filter-chip';
        chip.appendChild(document.createTextNode(text + ' '));

        removeButton.className = 'chip-remove';
        removeButton.type = 'button';
        removeButton.textContent = 'x';
        removeButton.setAttribute('aria-label', 'Remove ' + text + ' filter');
        removeButton.addEventListener('click', function() {
            if (type === 'cat') {
                cat = null;
            } else {
                sort = null;
            }
            runSearch();
        });

        chip.appendChild(removeButton);
        activeFilters.appendChild(chip);
    }

    function runSearch() {
        const name = searchInput.value.trim();
        const params = new URLSearchParams();

        if (name) params.append('search', name);
        if (cat) params.append('category', cat);
        if (sort) params.append('sort', sort);

        const queryStr = params.toString();
        const url = queryStr ? '/customers/products-listing?' + queryStr : '/customers/products-listing';

        updateSelectedOptions();
        renderChips();

        if (window.location.pathname === '/customers/products-listing' && typeof window.loadProductsFromSearchParams === 'function') {
            window.history.replaceState({}, '', url);
            window.loadProductsFromSearchParams();
            return;
        }

        window.location.href = url;
    }

    filterBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        filterPanel.classList.toggle('open');
        filterBtn.classList.toggle('active');
        filterBtn.setAttribute('aria-expanded', filterPanel.classList.contains('open') ? 'true' : 'false');
    });

    document.addEventListener('click', function(e) {
        if (!filterPanel.contains(e.target) && !filterBtn.contains(e.target)) {
            filterPanel.classList.remove('open');
            filterBtn.setAttribute('aria-expanded', 'false');
        }
    });

    filterPanel.addEventListener('click', function(e) {
        const opt = e.target.closest('.filter-option');
        if (!opt) {
            return;
        }

        const type = opt.dataset.filterType;
        const val = opt.dataset.filterValue;

        if (type === 'category') {
            cat = cat === val ? null : val;
        }

        if (type === 'price') {
            sort = sort === val ? null : val;
        }

        filterPanel.classList.remove('open');
        filterBtn.setAttribute('aria-expanded', 'false');
        runSearch();
    });

    searchBtn.addEventListener('click', runSearch);
    searchInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter') {
            runSearch();
        }
    });

    window.syncSearchFilterFromUrl = syncFromUrl;
    syncFromUrl();
}
