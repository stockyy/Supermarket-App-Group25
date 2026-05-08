function refreshBasketPage() {
    CustomerApi.getBasket(true)
        .then(function(basket) {
            if (basket === null) {
                return;
            }

            document.getElementById('basket-status').style.display = 'none';

            if (basket.items.length === 0) {
                showEmptyBasket();
            } else {
                showBasketWithItems(basket);
            }
        })
        .catch(function(error) {
            console.error('Error loading basket page :', error);
            document.getElementById('basket-status').textContent = 'Could not load your basket. Please refresh.';
        });
}

function showEmptyBasket() {
    document.getElementById('basket-page-content').style.display = 'none';
    document.getElementById('basket-empty').style.display = 'block';
}

function showBasketWithItems(basket) {
    document.getElementById('basket-empty').style.display = 'none';
    document.getElementById('basket-page-content').style.display = '';

    renderBasketPageItems(basket.items);

    updateBasketPageSummary(basket);
}

function renderBasketPageItems(items) {
    const container = document.getElementById('basket-items');
    container.innerHTML = '';

    for (let i = 0; i < items.length; i++) {
        const item = items[i];
        const cardHtml = makeBasketPageItemCard(item);
        container.innerHTML = container.innerHTML + cardHtml;
    }

    attachBasketPageItemListeners();
}

function makeBasketPageItemCard(item) {
    const unitPriceFormatted = item.unitPrice.toFixed(2);
    const lineTotalFormatted = item.lineTotal.toFixed(2);
    const amount = getBasketPageItemAmount(item);
    const unitLabel = item.soldByWeight ? 'per kg' : 'each';
    const amountUnit = item.soldByWeight ? 'kg' : '';

    let imageUrl = item.imageUrl;
    if (!imageUrl || imageUrl === '') {
        imageUrl = '/static/images/placeholder.png';
    }

    return '<div class="basket-item" data-cart-item-id="' + item.cartItemId + '">' +
        '<img src="' + imageUrl + '" alt="' + item.name + '">' +
        '<div class="basket-product-details">' +
        '<h3 class="basket-product-name">' + item.name + '</h3>' +
        '<p class="basket-product-price">£' + unitPriceFormatted + ' ' + unitLabel + '</p>' +
        '<p class="basket-product-line-total">Line total: £' + lineTotalFormatted + '</p>' +
        '</div>' +
        '<div class="basket-quantity">' +
        '<button class="basket-decrease" data-action="dec" data-cart-item-id="' + item.cartItemId + '">-</button>' +
        '<input type="number" class="item-quantity" value="' + formatBasketPageAmount(amount) + '" min="1" step="1" data-cart-item-id="' + item.cartItemId + '" aria-label="' + (item.soldByWeight ? 'Weight in kg' : 'Quantity') + '">' +
        '<button class="basket-increase" data-action="inc" data-cart-item-id="' + item.cartItemId + '">+</button>' +
        (amountUnit ? '<span class="basket-unit-label">' + amountUnit + '</span>' : '') +
        '</div>' +
        '<button class="remove-item" data-cart-item-id="' + item.cartItemId + '">Remove</button>' +
        '</div>';
}

function getBasketPageItemAmount(item) {
    if (item.soldByWeight) {
        return item.weight || 1;
    }

    return item.quantity || 1;
}

function formatBasketPageAmount(value) {
    return Number(value || 1).toFixed(2).replace(/\.00$/, '').replace(/0$/, '');
}

function updateBasketPageSummary(basket) {
    const summaryLabel = document.getElementById('basket-summary-label');
    const subtotal = document.getElementById('basket-subtotal');
    const delivery = document.getElementById('basket-delivery');
    const total = document.getElementById('basket-total');

    if (summaryLabel !== null) {
        summaryLabel.textContent = 'Subtotal (' + basket.itemCount + ' items)';
    }

    const subtotalFormatted = '£' + basket.totalCost.toFixed(2);

    if (subtotal !== null) {
        subtotal.textContent = subtotalFormatted;
    }

    if (delivery !== null) {
        delivery.textContent = '£0.00';
    }

    if (total !== null) {
        total.textContent = subtotalFormatted;
    }
}

function attachBasketPageItemListeners() {
    const stepperButtons = document.querySelectorAll('.basket-quantity button');

    for (let i = 0; i < stepperButtons.length; i++) {
        const button = stepperButtons[i];

        button.addEventListener('click', function() {
            const action = button.dataset.action;
            const cartItemId = button.dataset.cartItemId;

            const input = button.parentElement.querySelector('input');
            const currentQuantity = parseBasketPageAmount(input.value);

            let newQuantity;
            if (action === 'inc') {
                newQuantity = currentQuantity + 1;
            } else {
                newQuantity = currentQuantity - 1;
            }

            if (newQuantity < 1) {
                removeBasketItemAndRefreshPage(cartItemId);
                return;
            }

            updateBasketItemQuantityAndRefreshPage(cartItemId, newQuantity);
        });
    }

    const quantityInputs = document.querySelectorAll('.basket-quantity input');

    for (let i = 0; i < quantityInputs.length; i++) {
        const input = quantityInputs[i];

        input.addEventListener('change', function() {
            const cartItemId = input.dataset.cartItemId;
            const newQuantity = parseBasketPageAmount(input.value);

            if (isNaN(newQuantity) || newQuantity < 1) {
                input.value = 1;
                updateBasketItemQuantityAndRefreshPage(cartItemId, 1);
                return;
            }

            updateBasketItemQuantityAndRefreshPage(cartItemId, newQuantity);
        });
    }

    const removeButtons = document.querySelectorAll('.remove-item');

    for (let i = 0; i < removeButtons.length; i++) {
        const button = removeButtons[i];

        button.addEventListener('click', function() {
            const cartItemId = button.dataset.cartItemId;
            removeBasketItemAndRefreshPage(cartItemId);
        });
    }
}

function parseBasketPageAmount(value) {
    const amount = parseInt(value, 10);
    return isNaN(amount) ? 0 : amount;
}

function updateBasketItemQuantityAndRefreshPage(cartItemId, newQuantity) {
    CustomerApi.updateBasketItem(cartItemId, newQuantity)
        .then(function() {
            refreshBasketPage();
            refreshBasketAside();
            refreshBasketCount();
        })
        .catch(function(error) {
            console.error('Error updating quantity :', error);
            alert('Could not update quantity. Please try again.');
        });
}

function removeBasketItemAndRefreshPage(cartItemId) {
    CustomerApi.removeBasketItem(cartItemId)
        .then(function() {
            refreshBasketPage();
            refreshBasketAside();
            refreshBasketCount();
        })
        .catch(function(error) {
            console.error('Error removing item :', error);
            alert('Could not remove item. Please try again.');
        });
}
