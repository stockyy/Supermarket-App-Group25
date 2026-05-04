// get current basket count
function refreshBasketCount() {
    fetch('/orders/basket')
        .then(function(response) {
            // if user isn't logged in show nothing
            if (response.status === 401) {
                hideBasketBadge();
                return null;
            }

            if (!response.ok) {
                throw new Error('Failed to fetch basket, status: ' + response.status);
            }

            return response.json();
        })
        .then(function(basket) {
            if (basket === null) {
                // 401 case - already handled
                return;
            }

            // update item count
            updateBasketBadge(basket.itemCount);
        })
        .catch(function(error) {
            console.error('Error refreshing basket count :', error);
            hideBasketBadge();
        });
}

// show item badge with number if not put 9
function updateBasketBadge(count) {
    const badge = document.getElementById('basket-count');

    if (badge === null) {
        return;
    }

    if (count === 0) {
        badge.hidden = true;
        badge.textContent = '0';
    } else {
        badge.hidden = false;
        badge.textContent = count;
    }
}

function hideBasketBadge() {
    const badge = document.getElementById('basket-count');
    if (badge !== null) {
        badge.hidden = true;
    }
}