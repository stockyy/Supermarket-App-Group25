function initSearchFilter() {
    const filterBtn = document.getElementById('filter-btn');
    const filterPanel = document.getElementById('filter-panel');
    const searchInput = document.getElementById('search-bar');
    const searchBtn = document.querySelector('.search-submit');
    const activeFilters = document.getElementById('active-filters');

    // Make sure elements exist before attaching logic
    if (!filterBtn || !filterPanel) return;

    let cat = null;
    let sort = null;

    filterBtn.onclick = (e) => {
        e.stopPropagation();
        filterPanel.classList.toggle('open');
        filterBtn.classList.toggle('active');
    };

    document.onclick = (e) => {
        if (!filterPanel.contains(e.target) && !filterBtn.contains(e.target)) {
            filterPanel.classList.remove('open');
        }
    };

    filterPanel.onclick = (e) => {
        const opt = e.target.closest('.filter-option');
        if (!opt) return;

        const type = opt.dataset.filterType;
        const val = opt.dataset.filterValue;

        if (type === 'category') {
            if (cat === val) {
                cat = null;
                opt.classList.remove('selected');
            } else {
                filterPanel.querySelectorAll('[data-filter-type="category"]').forEach(el => el.classList.remove('selected'));
                cat = val;
                opt.classList.add('selected');
            }
        }

        if (type === 'price') {
            if (sort === val) {
                sort = null;
                opt.classList.remove('selected');
            } else {
                filterPanel.querySelectorAll('[data-filter-type="price"]').forEach(el => el.classList.remove('selected'));
                sort = val;
                opt.classList.add('selected');
            }
        }
        renderChips();
        filterPanel.classList.remove('open');
    };

    function renderChips() {
        activeFilters.innerHTML = '';
        if (cat) addChip(cat, 'cat');
        if (sort) {
            const label = sort === 'price-asc' ? 'Low to High' : 'High to Low';
            addChip(label, 'sort');
        }
    }

    function addChip(text, type) {
        const chip = document.createElement('div');
        chip.className = 'filter-chip';
        chip.innerHTML = `${text} <button class="chip-remove">✕</button>`;
        chip.querySelector('button').onclick = () => {
            if (type === 'cat') {
                cat = null;
                filterPanel.querySelectorAll('[data-filter-type="category"]').forEach(el => el.classList.remove('selected'));
            } else {
                sort = null;
                filterPanel.querySelectorAll('[data-filter-type="price"]').forEach(el => el.classList.remove('selected'));
            }
            renderChips();
        };
        activeFilters.appendChild(chip);
    }

    function runSearch() {
        const name = searchInput.value.trim();
        let url = cat ? `/products/category/${encodeURIComponent(cat)}` : `/products/search`;
        const params = new URLSearchParams();
        if (name) params.append('name', name);
        if (sort) params.append('sort', sort);
        const queryStr = params.toString();
        if (queryStr) url += `?${queryStr}`;
        window.location.href = url;
    }

    searchBtn.onclick = runSearch;
    searchInput.onkeydown = (e) => { if (e.key === 'Enter') runSearch(); };
}