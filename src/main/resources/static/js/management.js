// APPLY FILTERS

document.getElementById('apply-filters-btn').addEventListener('click', function () {
    const date_from = document.getElementById('date-from').value;
    const date_to = document.getElementById('date-to').value;
    const category = document.getElementById('category-filter').value;
    const search = document.getElementById('dashboard-search').value.trim();

    if (dateFrom && dateTo && dateFrom > dateTo) {
        alert('Date From cannot be after Date To.');
        return;
    }

    const filters = { dateFrom, dateTo, category, search };

    // TODO:
    // Returns updated data for all sections all corresponding filters

    // command line check
    console.log('applying filters:', filters);
});


// RESET FILTERS 

document.getElementById('reset-filters-btn').addEventListener('click', function () {
    document.getElementById('date-from').value = '';
    document.getElementById('date-to').value = '';
    document.getElementById('category-filter').value = 'all';
    document.getElementById('dashboard-search').value = '';

    console.log('filters reset');
});

// EDPORT CSV

// Will do this part once I've actually got data fro the backend to work with 