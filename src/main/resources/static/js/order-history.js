document.addEventListener('DOMContentLoaded', function() {
    loadOrderHistory();
});

function loadOrderHistory() {
    CustomerApi.getOrders()
        .then(function(orders) {
            if (orders === null) {
                return;
            }

            renderOrderHistory(orders);
        })
        .catch(function(error) {
            console.error('Error loading order history:', error);
            setOrderHistoryStatus('Could not load your orders. Please refresh.', true);
        });
}

function renderOrderHistory(orders) {
    const container = document.getElementById('order-cards-container');
    if (container === null) {
        return;
    }

    container.innerHTML = '';

    if (orders.length === 0) {
        setOrderHistoryStatus('You have not placed any orders yet.', false);
        return;
    }

    setOrderHistoryStatus('', false);

    for (let i = 0; i < orders.length; i++) {
        container.insertAdjacentHTML('beforeend', makeOrderCard(orders[i]));
    }
}

function makeOrderCard(order) {
    return '<article class="order-card">' +
        '<div class="order-card-summary">' +
        makeOrderField('Order ID', '#' + order.orderId) +
        makeOrderField('Status', formatStatus(order.status)) +
        makeOrderField('Delivery Window', formatDeliveryWindow(order.deliveryWindowStart, order.deliveryWindowEnd)) +
        makeOrderField('Items', String(order.itemCount)) +
        makeOrderField('Price', formatMoney(order.totalCost)) +
        '<div class="order-field order-address-field">' +
        '<label>Delivery Address</label>' +
        '<span title="' + escapeHtml(order.deliveryAddress) + '">' + escapeHtml(order.deliveryAddress) + '</span>' +
        '</div>' +
        '</div>' +
        makeOrderItems(order.items || []) +
        '</article>';
}

function makeOrderField(label, value) {
    return '<div class="order-field">' +
        '<label>' + escapeHtml(label) + '</label>' +
        '<span>' + escapeHtml(value) + '</span>' +
        '</div>';
}

function makeOrderItems(items) {
    if (items.length === 0) {
        return '<p class="order-items-empty">No item details saved for this order.</p>';
    }

    let rows = '';
    for (let i = 0; i < items.length; i++) {
        rows += makeOrderItemRow(items[i]);
    }

    return '<details class="order-items-details">' +
        '<summary>View items (' + items.length + ')</summary>' +
        '<div class="order-items-list">' + rows + '</div>' +
        '</details>';
}

function makeOrderItemRow(item) {
    const imageUrl = item.imageUrl || '/static/images/placeholder.png';
    return '<div class="order-item-row">' +
        '<img src="' + escapeHtml(imageUrl) + '" alt="' + escapeHtml(item.name) + '" class="order-item-thumb">' +
        '<div class="order-item-info">' +
        '<span class="order-item-name">' + escapeHtml(item.name) + '</span>' +
        '<span class="order-item-meta">' + escapeHtml(formatOrderItemAmount(item)) + '</span>' +
        '</div>' +
        '<span class="order-item-total">' + escapeHtml(formatMoney(item.lineTotal)) + '</span>' +
        '</div>';
}

function formatOrderItemAmount(item) {
    if (item.soldByWeight) {
        return formatWeight(item.weight) + ' kg';
    }

    const quantity = item.quantity || 1;
    return quantity + (quantity === 1 ? ' item' : ' items');
}

function formatWeight(value) {
    return Number(value || 1).toFixed(2).replace(/\.00$/, '').replace(/0$/, '');
}

function setOrderHistoryStatus(message, isError) {
    const status = document.getElementById('order-history-status');
    if (status === null) {
        return;
    }

    status.hidden = message === '';
    status.textContent = message;
    status.classList.toggle('order-history-status-error', isError);
}

function formatDeliveryWindow(startValue, endValue) {
    const start = parseDate(startValue);
    const end = parseDate(endValue);

    if (start === null || end === null) {
        return 'Not available';
    }

    return start.toLocaleDateString('en-GB', {
        weekday: 'short',
        day: 'numeric',
        month: 'short'
    }) + ', ' + start.toLocaleTimeString('en-GB', {
        hour: 'numeric',
        minute: '2-digit'
    }) + ' - ' + end.toLocaleTimeString('en-GB', {
        hour: 'numeric',
        minute: '2-digit'
    });
}

function parseDate(value) {
    if (!value) {
        return null;
    }

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
}

function formatStatus(status) {
    const labels = {
        WAITING: 'Waiting',
        PICKED: 'Picked',
        TRANSIT: 'In transit',
        DELIVERED: 'Delivered'
    };

    return labels[status] || status;
}

function formatMoney(value) {
    return '\u00A3' + Number(value || 0).toFixed(2);
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
