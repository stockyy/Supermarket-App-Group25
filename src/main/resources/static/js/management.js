const state = {
    dashboard: null,
};

const currencyFormatter = new Intl.NumberFormat('en-GB', {
    style: 'currency',
    currency: 'GBP',
});

function qs(id) {
    return document.getElementById(id);
}

function money(value) {
    return currencyFormatter.format(Number(value || 0));
}

function number(value) {
    return Number(value || 0).toLocaleString('en-GB');
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function setStatus(message, isError = false) {
    const status = qs('status-message');
    status.textContent = message;
    status.classList.toggle('error', isError);
}

function setGenerationStatus(message, isError = false) {
    const status = qs('picklist-generation-status');
    status.textContent = message;
    status.classList.toggle('error', isError);
}

function setDatabaseStatus(message, isError = false) {
    const status = qs('database-maintenance-status');
    status.textContent = message;
    status.classList.toggle('error', isError);
}

function setAccountStatus(message, isError = false) {
    const status = qs('account-deletion-status');
    status.textContent = message;
    status.classList.toggle('error', isError);
}

function currentFilters() {
    return {
        dateFrom: qs('date-from').value,
        dateTo: qs('date-to').value,
        category: qs('category-filter').value,
        search: qs('dashboard-search').value.trim(),
    };
}

function buildQuery(filters) {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
        if (value && value !== 'all') params.set(key, value);
    });
    return params.toString();
}

function tomorrowDateString() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const year = tomorrow.getFullYear();
    const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
    const day = String(tomorrow.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

async function loadDashboard() {
    const filters = currentFilters();

    if (filters.dateFrom && filters.dateTo && filters.dateFrom > filters.dateTo) {
        setStatus('Date From cannot be after Date To.', true);
        return;
    }

    setStatus('Loading dashboard data');

    try {
        const query = buildQuery(filters);
        const response = await fetch(`/management/api/dashboard${query ? `?${query}` : ''}`);

        if (!response.ok) {
            throw new Error(`Dashboard request failed with status ${response.status}`);
        }

        state.dashboard = await response.json();
        renderDashboard(state.dashboard);
        setStatus('');
    } catch (error) {
        console.error(error);
        setStatus('Unable to load dashboard data. Please try again.', true);
    }
}

async function generatePicklists() {
    const date = qs('picklist-date').value;
    const button = qs('generate-picklists-btn');

    if (!date) {
        setGenerationStatus('Choose a due date first.', true);
        return;
    }

    button.disabled = true;
    setGenerationStatus('Generating picklists');

    try {
        const response = await fetch('/management/api/picklists/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ date }),
        });

        if (!response.ok) {
            throw new Error(await response.text());
        }

        const result = await response.json();
        setGenerationStatus(`${number(result.picklistsCreated)} picklists generated for ${result.date}.`);
        await loadDashboard();
    } catch (error) {
        console.error(error);
        setGenerationStatus('Unable to generate picklists for that date.', true);
    } finally {
        button.disabled = false;
    }
}

async function reseedDatabase() {
    const button = qs('reseed-db-btn');
    const confirmed = window.confirm('This will wipe and re-seed the database. Continue?');

    if (!confirmed) return;

    button.disabled = true;
    setDatabaseStatus('Resetting database');

    try {
        const response = await fetch('/seed-db', { method: 'PUT' });
        const message = await response.text();

        if (!response.ok) {
            throw new Error(message);
        }

        setDatabaseStatus(message);
        await loadDashboard();
    } catch (error) {
        console.error(error);
        setDatabaseStatus('Unable to reset the database. Please try again.', true);
    } finally {
        button.disabled = false;
    }
}

async function deleteUserAccount(userId, userName) {
    const confirmed = window.confirm(`Delete account for ${userName}? This cannot be undone.`);

    if (!confirmed) return;

    setAccountStatus('Deleting user account');

    try {
        const response = await fetch(`/management/api/users/${userId}`, { method: 'DELETE' });
        const message = response.ok ? (await response.json()).message : await response.text();

        if (!response.ok) {
            throw new Error(message);
        }

        setAccountStatus(message);
        await loadDashboard();
    } catch (error) {
        console.error(error);
        setAccountStatus(error.message || 'Unable to delete that user account.', true);
    }
}

function renderDashboard(data) {
    renderCategoryOptions(data.categories);
    renderKpis(data.kpis);
    renderSectionCounts(data);
    renderSalesBars(data.salesByCategory);
    renderTopProducts(data.topProducts);
    renderActiveOrders(data.activeOrders);
    renderAllOrders(data.allOrders);
    renderPicklistWorkload(data.picklistWorkload);
    renderRecentPicklists(data.recentPicklists);
    renderStaffPerformance(data.staffPerformance);
    renderUserAccounts(data.userAccounts);
    renderLogs('offsale-logs', data.recentOffsales, renderOffsaleLog);
    renderLogs('wastage-logs', data.recentWastage, renderWastageLog);
    renderLowStock(data.lowStockProducts);
    qs('generated-at').textContent = `Updated ${data.generatedAt}`;
}

function renderSectionCounts(data) {
    qs('active-orders-count').textContent = `${number(data.activeOrders.length)} current orders`;
    qs('all-orders-count').textContent = `${number(data.allOrders.length)} total orders`;
    qs('picklists-count').textContent = `${number(data.recentPicklists.length)} picklists`;
    qs('staff-count').textContent = `${number(data.staffPerformance.length)} staff members`;
    qs('user-accounts-count').textContent = `${number(data.userAccounts.length)} user accounts`;
    qs('offsale-count').textContent = `${number(data.recentOffsales.length)} offsale log entries`;
    qs('wastage-count').textContent = `${number(data.recentWastage.length)} wastage log entries`;
}

function renderCategoryOptions(categories) {
    const select = qs('category-filter');
    const currentValue = select.value || 'all';
    const options = ['<option value="all">All categories</option>']
        .concat(categories.map(category => `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`));

    select.innerHTML = options.join('');
    select.value = categories.includes(currentValue) ? currentValue : 'all';
}

function renderKpis(kpis) {
    qs('kpi-products').textContent = number(kpis.totalProducts);
    qs('kpi-low-stock').textContent = `${number(kpis.lowStockProducts)} low stock`;
    qs('kpi-active-orders').textContent = number(kpis.activeOrders);
    qs('kpi-active-value').textContent = `${money(kpis.activeOrderValue)} active value`;
    qs('kpi-open-picklists').textContent = number(kpis.openPicklists);
    qs('kpi-pick-rate').textContent = `${number(kpis.averagePickRate)} items/hour avg`;
    qs('kpi-staff').textContent = number(kpis.staffMembers);
    qs('kpi-offsale-logs').textContent = `${number(kpis.offsaleLogs)} offsale logs`;
    qs('kpi-carts').textContent = number(kpis.activeCarts);
    qs('kpi-cart-value').textContent = `${money(kpis.activeCartValue)} open cart value`;
}

function renderSalesBars(rows) {
    const container = qs('sales-bars');

    if (!rows.length) {
        container.innerHTML = '<p class="empty-state">No sales found for these filters.</p>';
        return;
    }

    const maxRevenue = Math.max(...rows.map(row => row.revenue), 1);
    container.innerHTML = rows.map(row => {
        const width = Math.max(3, (row.revenue / maxRevenue) * 100);
        return `
            <div class="bar-row">
                <div class="bar-meta">
                    <strong>${escapeHtml(row.category)}</strong>
                    <span>${number(row.itemsSold)} items - ${money(row.revenue)}</span>
                </div>
                <div class="bar-track" aria-hidden="true">
                    <div class="bar-fill" style="width: ${width}%"></div>
                </div>
            </div>
        `;
    }).join('');
}

function renderTopProducts(rows) {
    const tbody = qs('top-products-body');
    tbody.innerHTML = rows.length ? rows.map(row => `
        <tr>
            <td>
                <strong>${escapeHtml(row.productName)}</strong><br>
                <small>${escapeHtml(row.category)}</small>
            </td>
            <td class="number-cell">${number(row.itemsSold)}</td>
            <td class="number-cell">${money(row.revenue)}</td>
        </tr>
    `).join('') : emptyRow(3, 'No product sales found.');
}

function renderActiveOrders(rows) {
    const tbody = qs('active-orders-body');
    tbody.innerHTML = rows.length ? rows.map(row => `
        <tr>
            <td>#${row.orderId}</td>
            <td>${escapeHtml(row.customerName)}</td>
            <td>${statusPill(row.status)}</td>
            <td class="number-cell">${number(row.itemCount)}</td>
            <td class="number-cell">${money(row.totalCost)}</td>
            <td>${escapeHtml(row.deliveryWindow)}</td>
        </tr>
    `).join('') : emptyRow(6, 'No active orders match these filters.');
}

function renderAllOrders(rows) {
    const tbody = qs('all-orders-body');
    tbody.innerHTML = rows.length ? rows.map(row => `
        <tr>
            <td>#${row.orderId}</td>
            <td>${escapeHtml(row.customerName)}</td>
            <td>${statusPill(row.status)}</td>
            <td class="number-cell">${number(row.itemCount)}</td>
            <td class="number-cell">${money(row.totalCost)}</td>
            <td>${escapeHtml(row.orderTime)}</td>
            <td>${escapeHtml(row.deliveryWindow)}</td>
        </tr>
    `).join('') : emptyRow(7, 'No orders match these filters.');
}

function renderPicklistWorkload(rows) {
    const container = qs('picklist-workload');
    container.innerHTML = rows.length ? rows.map(row => `
        <div class="compact-row">
            <div>
                <strong>${escapeHtml(row.section)}</strong>
                <span>${number(row.totalPicklists)} total lists</span>
            </div>
            <div>
                <span>${number(row.unassigned)} unassigned</span>
                <span>${number(row.inProgress)} in progress</span>
                <span>${number(row.completed)} completed</span>
            </div>
        </div>
    `).join('') : '<p class="empty-state">No picklists generated yet.</p>';
}

function renderRecentPicklists(rows) {
    const tbody = qs('recent-picklists-body');
    tbody.innerHTML = rows.length ? rows.map(row => {
        const progress = row.quantity > 0 ? Math.min(100, (row.pickedQuantity / row.quantity) * 100) : 0;
        return `
            <tr>
                <td>#${row.picklistId}</td>
                <td>${escapeHtml(row.section)}</td>
                <td>${escapeHtml(row.pickerName)}</td>
                <td>${statusPill(row.status)}</td>
                <td>
                    ${number(row.pickedQuantity)} / ${number(row.quantity)}
                    <div class="progress-track" aria-hidden="true">
                        <div class="progress-fill" style="width: ${progress}%"></div>
                    </div>
                </td>
                <td class="number-cell">${number(row.pickRate)}</td>
            </tr>
        `;
    }).join('') : emptyRow(6, 'No picklists found.');
}

function renderStaffPerformance(rows) {
    const container = qs('staff-performance');
    container.innerHTML = rows.length ? rows.map(row => `
        <div class="compact-row">
            <div>
                <strong>${escapeHtml(row.name)}</strong>
                <span>${escapeHtml(row.role)} - ${escapeHtml(row.staffId)}</span>
            </div>
            <div>
                <span>${number(row.completedPicklists)} lists</span>
                <span>${number(row.averagePickRate)} items/hour</span>
                <span>${escapeHtml(row.lastActive)}</span>
            </div>
        </div>
    `).join('') : '<p class="empty-state">No staff activity found.</p>';
}

function renderUserAccounts(rows) {
    const staffRows = rows.filter(row => row.role !== 'CUSTOMER');
    const customerRows = rows.filter(row => row.role === 'CUSTOMER');

    qs('staff-accounts-count').textContent = `${number(staffRows.length)} staff accounts`;
    qs('customer-accounts-count').textContent = `${number(customerRows.length)} customer accounts`;
    renderUserAccountRows('staff-accounts-body', staffRows, 'No staff accounts found.');
    renderUserAccountRows('customer-accounts-body', customerRows, 'No customer accounts found.');
}

function renderUserAccountRows(elementId, rows, emptyMessage) {
    const tbody = qs(elementId);
    tbody.innerHTML = rows.length ? rows.map(row => `
        <tr>
            <td>#${row.userId}</td>
            <td>${escapeHtml(row.name)}</td>
            <td>${statusPill(row.role)}</td>
            <td>${escapeHtml(row.email)}</td>
            <td>${escapeHtml(row.staffId)}</td>
            <td class="number-cell">${number(row.orderCount)}</td>
            <td class="number-cell">${number(row.activeCartItems)}</td>
            <td>
                <button
                    type="button"
                    class="danger-button delete-user-btn"
                    data-user-id="${row.userId}"
                    data-user-name="${escapeHtml(row.name)}"
                >
                    Delete
                </button>
            </td>
        </tr>
    `).join('') : emptyRow(8, emptyMessage);
}

function renderLogs(elementId, rows, renderer) {
    const container = qs(elementId);
    container.innerHTML = rows.length ? rows.map(renderer).join('') : '<p class="empty-state">No recent logs found.</p>';
}

function renderOffsaleLog(row) {
    return `
        <div class="event-row">
            <div>
                <strong>${escapeHtml(row.productName)}</strong>
                <span>${escapeHtml(row.staffName)} - ${escapeHtml(row.dateTime)}</span>
            </div>
            ${statusPill(row.status)}
        </div>
    `;
}

function renderWastageLog(row) {
    return `
        <div class="event-row">
            <div>
                <strong>${escapeHtml(row.productName)}</strong>
                <span>${escapeHtml(row.reason)} - ${escapeHtml(row.amount)}</span>
            </div>
            <span>${escapeHtml(row.dateTime)}</span>
        </div>
    `;
}

function renderLowStock(rows) {
    const tbody = qs('low-stock-body');
    tbody.innerHTML = rows.length ? rows.map(row => `
        <tr>
            <td>${escapeHtml(row.productName)}</td>
            <td>${escapeHtml(row.category)}</td>
            <td class="number-cell">${number(row.stockLevel)}</td>
        </tr>
    `).join('') : emptyRow(3, 'No low-stock products for these filters.');
}

function statusPill(status) {
    const normalized = String(status).toLowerCase();
    const className = normalized.includes('pending') || normalized.includes('waiting')
        ? 'warning'
        : normalized.includes('off') || normalized.includes('failed')
            ? 'alert'
            : '';
    return `<span class="pill ${className}">${escapeHtml(status.replaceAll('_', ' '))}</span>`;
}

function emptyRow(colspan, message) {
    return `<tr><td colspan="${colspan}" class="empty-state">${message}</td></tr>`;
}

function exportCsv() {
    if (!state.dashboard) return;

    const rows = [
        ['Section', 'Name', 'Metric A', 'Metric B', 'Metric C'],
        ...state.dashboard.salesByCategory.map(row => [
            'Sales by category',
            row.category,
            row.orderCount,
            row.itemsSold,
            row.revenue,
        ]),
        ...state.dashboard.topProducts.map(row => [
            'Top products',
            row.productName,
            row.category,
            row.itemsSold,
            row.revenue,
        ]),
        ...state.dashboard.staffPerformance.map(row => [
            'Staff pick rates',
            row.name,
            row.role,
            row.completedPicklists,
            row.averagePickRate,
        ]),
        ...state.dashboard.activeOrders.map(row => [
            'Current orders',
            `#${row.orderId}`,
            row.customerName,
            row.status,
            row.totalCost,
        ]),
        ...state.dashboard.allOrders.map(row => [
            'All orders',
            `#${row.orderId}`,
            row.customerName,
            row.status,
            row.totalCost,
        ]),
        ...state.dashboard.recentPicklists.map(row => [
            'Picklists',
            `#${row.picklistId}`,
            row.section,
            row.status,
            row.pickRate,
        ]),
        ...state.dashboard.recentOffsales.map(row => [
            'Offsale logs',
            row.productName,
            row.staffName,
            row.status,
            row.dateTime,
        ]),
        ...state.dashboard.recentWastage.map(row => [
            'Wastage logs',
            row.productName,
            row.staffName,
            row.reason,
            row.amount,
        ]),
        ...state.dashboard.lowStockProducts.map(row => [
            'Low stock',
            row.productName,
            row.category,
            row.stockLevel,
            row.price,
        ]),
    ];

    const csv = rows.map(row => row.map(cell => `"${String(cell).replaceAll('"', '""')}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'manager-dashboard-report.csv';
    link.click();
    URL.revokeObjectURL(url);
}

qs('apply-filters-btn').addEventListener('click', loadDashboard);
qs('reset-filters-btn').addEventListener('click', () => {
    qs('date-from').value = '';
    qs('date-to').value = '';
    qs('category-filter').value = 'all';
    qs('dashboard-search').value = '';
    loadDashboard();
});
qs('export-csv-btn').addEventListener('click', exportCsv);
qs('generate-picklists-btn').addEventListener('click', generatePicklists);
qs('reseed-db-btn').addEventListener('click', reseedDatabase);
qs('user-accounts-panel').addEventListener('click', event => {
    const button = event.target.closest('.delete-user-btn');

    if (!button) return;

    deleteUserAccount(button.dataset.userId, button.dataset.userName);
});
qs('picklist-date').value = tomorrowDateString();
qs('dashboard-search').addEventListener('keydown', event => {
    if (event.key === 'Enter') {
        event.preventDefault();
        loadDashboard();
    }
});

document.querySelectorAll('.collapse-toggle').forEach(button => {
    button.addEventListener('click', () => {
        const panel = qs(button.dataset.target);
        const parent = button.closest('.collapsible-panel');
        const isExpanded = button.getAttribute('aria-expanded') === 'true';

        panel.hidden = isExpanded;
        button.setAttribute('aria-expanded', String(!isExpanded));
        button.textContent = isExpanded ? 'Expand' : 'Collapse';
        parent?.classList.toggle('is-collapsed', isExpanded);
    });
});

loadDashboard();
