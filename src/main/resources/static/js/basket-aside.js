// Fetches the basket and renders it inside the slide-out aside
function refreshBasketAside() {
    CustomerApi.getBasket(false)
        .then(function(basket) {
            if (basket === null) {
                renderEmptyAside();
                return;
            }

            // if basket empty show empty state
            if (basket.items.length === 0) {
                renderEmptyAside();
            } else {
                renderAsideItems(basket);
            }

            // update total sat bottom
            updateAsideTotals(basket);
        })
        .catch(function(error) {
            console.error('Error refreshing basket aside :', error);
        });
}

// renders list of item inside the body
function renderAsideItems(basket) {
    const asideBody = document.getElementById('aside-body');

    if (asideBody === null) {
        return;
    }

    // clear everything not there
    asideBody.innerHTML = '';

    // card for each item
    for (let i = 0; i < basket.items.length; i++) {
        const item = basket.items[i];
        const cardHtml = makeAsideItemCard(item);
        asideBody.innerHTML = asideBody.innerHTML + cardHtml;
    }

    // +- buttons
    attachAsideItemListeners();
}

// builds html for single item in the aside
function makeAsideItemCard(item) {
    // formatting for pennies
    const unitPriceFormatted = item.unitPrice.toFixed(2);
    const lineTotalFormatted = item.lineTotal.toFixed(2);
    const amount = getBasketItemAmount(item);
    const unitLabel = item.soldByWeight ? 'per kg' : 'each';
    const amountUnit = item.soldByWeight ? 'kg' : '';

    // placeholder image if it don't have one
    let imageUrl = item.imageUrl;
    if (!imageUrl || imageUrl === '') {
        imageUrl = '/static/images/placeholder.png';
    }

    return '<article class="basket-item" data-cart-item-id="' + item.cartItemId + '">' +
        '<img src="' + imageUrl + '" alt="' + item.name + '" class="item-img" style="width:50px;height:50px;object-fit:cover;border-radius:4px;">' +
        '<div class="item-details">' +
        '<p class="item-name">' + item.name + '</p>' +
        '<p class="item-unit-price">£' + unitPriceFormatted + ' ' + unitLabel + '</p>' +
        '<div class="item-controls">' +
        '<div class="item-stepper">' +
        '<button data-action="dec" data-cart-item-id="' + item.cartItemId + '">−</button>' +
        '<input type="number" value="' + formatBasketAmount(amount) + '" min="1" step="1" data-cart-item-id="' + item.cartItemId + '" aria-label="' + (item.soldByWeight ? 'Weight in kg' : 'Quantity') + '">' +
        '<button data-action="inc" data-cart-item-id="' + item.cartItemId + '">+</button>' +
        (amountUnit ? '<span class="item-unit-label">' + amountUnit + '</span>' : '') +
        '</div>' +
        '<button class="item-remove" data-cart-item-id="' + item.cartItemId + '" aria-label="Remove ' + item.name + '">' +
        '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 6h18M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M10 11v6M14 11v6M9 6V4h6v2"/></svg>' +
        '</button>' +
        '</div>' +
        '</div>' +
        '<span class="item-line-total">£' + lineTotalFormatted + '</span>' +
        '</article>';
}

function getBasketItemAmount(item) {
    if (item.soldByWeight) {
        return item.weight || 1;
    }

    return item.quantity || 1;
}

function formatBasketAmount(value) {
    return Number(value || 1).toFixed(2).replace(/\.00$/, '').replace(/0$/, '');
}

// renders empty basket
function renderEmptyAside() {
    const asideBody = document.getElementById('aside-body');

    if (asideBody === null) {
        return;
    }

    asideBody.innerHTML =
        '<div class="aside-empty" style="text-align:center;padding:2rem;">' +
        '<span style="font-size:48px;">🛒</span>' +
        '<h3>Your basket is empty</h3>' +
        '<p>Add some items to get started.</p>' +
        '</div>';
}

// update everything at the bottom
function updateAsideTotals(basket) {
    const subtotalLabel = document.getElementById('aside-subtotal-label');
    const subtotal = document.getElementById('aside-subtotal');
    const total = document.getElementById('aside-total');

    if (subtotalLabel !== null) {
        subtotalLabel.textContent = 'Subtotal (' + basket.itemCount + ' items)';
    }

    const totalFormatted = '£' + basket.totalCost.toFixed(2);

    if (subtotal !== null) {
        subtotal.textContent = totalFormatted;
    }

    if (total !== null) {
        total.textContent = totalFormatted;
    }
}

// attaches click listeners to +- and removes buttons in teh aside
function attachAsideItemListeners() {
    // wire up + and - buttons
    const stepperButtons = document.querySelectorAll('.item-stepper button');

    for (let i = 0; i < stepperButtons.length; i++) {
        const button = stepperButtons[i];

        button.addEventListener('click', function() {
            const action = button.dataset.action;
            const cartItemId = button.dataset.cartItemId;

            // find the input for the field items
            const input = button.parentElement.querySelector('input');
            const currentQuantity = parseBasketAmount(input.value);

            let newQuantity;
            if (action === 'inc') {
                newQuantity = currentQuantity + 1;
            } else {
                newQuantity = currentQuantity - 1;
            }

            // If they go below 1, treat as a remove
            if (newQuantity < 1) {
                removeBasketItem(cartItemId);
                return;
            }

            updateBasketItemQuantity(cartItemId, newQuantity);
        });
    }

    // wire up manually
    const quantityInputs = document.querySelectorAll('.item-stepper input');

    for (let i = 0; i < quantityInputs.length; i++) {
        const input = quantityInputs[i];

        input.addEventListener('change', function() {
            const cartItemId = input.dataset.cartItemId;
            const newQuantity = parseBasketAmount(input.value);

            // If user typed something invalid, treat as 1
            if (isNaN(newQuantity) || newQuantity < 1) {
                input.value = 1;
                updateBasketItemQuantity(cartItemId, 1);
                return;
            }

            updateBasketItemQuantity(cartItemId, newQuantity);
        });
    }

    // wire up to remove buttons
    const removeButtons = document.querySelectorAll('.item-remove');

    for (let i = 0; i < removeButtons.length; i++) {
        const button = removeButtons[i];

        button.addEventListener('click', function() {
            const cartItemId = button.dataset.cartItemId;
            removeBasketItem(cartItemId);
        });
    }
}

function parseBasketAmount(value) {
    const amount = parseInt(value, 10);
    return isNaN(amount) ? 0 : amount;
}

// sends PUT to update an item's quantity
function updateBasketItemQuantity(cartItemId, newQuantity) {
    CustomerApi.updateBasketItem(cartItemId, newQuantity)
        .then(function() {
            // Refresh the aside and the badge
            refreshBasketAside();
            refreshBasketCount();
        })
        .catch(function(error) {
            console.error('Error updating quantity :', error);
            alert('Could not update quantity. Please try again.');
        });
}

// sends DELETE to remove an item
function removeBasketItem(cartItemId) {
    CustomerApi.removeBasketItem(cartItemId)
        .then(function() {
            // refresh the aside and the badge
            refreshBasketAside();
            refreshBasketCount();
        })
        .catch(function(error) {
            console.error('Error removing item :', error);
            alert('Could not remove item. Please try again.');
        });
}
