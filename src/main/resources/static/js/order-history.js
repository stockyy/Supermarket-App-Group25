document.addEventListener('DOMContentLoaded', function() {
    loadOrderHistory();
});

function loadOrderHistory() {
    fetch('/orders')
        .then(function(response) {
            if (response.status === 401) {
                window.location.href = '/customers/login';
                return null;
            }

            if (!response.ok) {
                throw new Error('Failed to load orders, status: ' + response.status);
            }

            return response.json();
        })
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
        makeOrderField('Order ID', '#' + order.orderId) +
        makeOrderField('Status', formatStatus(order.status)) +
        makeOrderField('Delivery Window', formatDeliveryWindow(order.deliveryWindowStart, order.deliveryWindowEnd)) +
        makeOrderField('Items', String(order.itemCount)) +
        makeOrderField('Price', formatMoney(order.totalCost)) +
        '<div class="order-field order-address-field">' +
        '<label>Delivery Address</label>' +
        '<span title="' + escapeHtml(order.deliveryAddress) + '">' + escapeHtml(order.deliveryAddress) + '</span>' +
        '</div>' +
        '</article>';
}

function makeOrderField(label, value) {
    return '<div class="order-field">' +
        '<label>' + escapeHtml(label) + '</label>' +
        '<span>' + escapeHtml(value) + '</span>' +
        '</div>';
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
