let checkoutBasket = null;
let deliveryWindows = [];
let selectedDeliveryWindow = null;

document.addEventListener('DOMContentLoaded', function() {
    bindCheckoutEvents();
    loadCheckoutData();
});

function bindCheckoutEvents() {
    const placeOrderButton = document.getElementById('place-order-btn');
    placeOrderButton?.addEventListener('click', placeOrder);

    bindPaymentFormatting();
}

function loadCheckoutData() {
    setCheckoutStatus('Loading checkout details...', false);

    Promise.all([
        loadBasket(),
        loadDeliveryAddress(),
        loadDeliveryWindows()
    ])
        .then(function() {
            if (checkoutBasket !== null && checkoutBasket.items.length > 0) {
                clearCheckoutStatus();
            }
            updateCheckoutSummary();
        })
        .catch(function(error) {
            console.error(error);
            setCheckoutStatus('Could not load checkout details. Please refresh.', true);
        });
}

function loadBasket() {
    return CustomerApi.getBasket(true)
        .then(function(basket) {
            if (basket === null) {
                return;
            }

            checkoutBasket = basket;

            if (basket.items.length === 0) {
                setCheckoutStatus('Your basket is empty. Add items before checkout.', true);
                setPlaceOrderDisabled(true);
            }
        });
}

function loadDeliveryAddress() {
    return CustomerApi.getAddress()
        .then(function(address) {
            if (address !== null) {
                fillAddressFields(address);
            }
        });
}

function loadDeliveryWindows() {
    return CustomerApi.getDeliveryWindows()
        .then(function(windows) {
            if (windows === null) {
                return;
            }

            deliveryWindows = windows;
            renderDeliveryWindows(windows);
        });
}

function renderDeliveryWindows(windows) {
    const container = document.getElementById('delivery-slots-grid');
    if (container === null) {
        return;
    }

    container.innerHTML = '';

    if (windows.length === 0) {
        container.innerHTML = '<p class="checkout-status">No delivery slots are available.</p>';
        setPlaceOrderDisabled(true);
        return;
    }

    for (let i = 0; i < windows.length; i++) {
        const slot = windows[i];
        container.insertAdjacentHTML('beforeend', makeDeliverySlot(slot, i));
    }

    const firstAvailable = windows.find(function(slot) {
        return slot.available;
    });

    if (firstAvailable !== undefined) {
        selectedDeliveryWindow = firstAvailable;
        const input = container.querySelector('input[value="' + firstAvailable.id + '"]');
        if (input !== null) {
            input.checked = true;
        }
    }

    container.querySelectorAll('input[name="delivery_slot"]').forEach(function(input) {
        input.addEventListener('change', function() {
            selectedDeliveryWindow = deliveryWindows.find(function(slot) {
                return slot.id === input.value;
            });
            updateCheckoutSummary();
        });
    });
}

function makeDeliverySlot(slot, index) {
    const labelParts = splitSlotLabel(slot.label);
    const disabledClass = slot.available ? '' : ' delivery-slot-disabled';
    const disabledAttribute = slot.available ? '' : ' disabled';
    const tag = index === 2 && slot.available ? '<span class="slot-tag">Popular</span>' : '';
    const feeText = slot.available ? formatMoney(slot.fee) : 'Full';

    return '<label class="delivery-slot' + disabledClass + '">' +
        '<input type="radio" name="delivery_slot" value="' + escapeHtml(slot.id) + '"' + disabledAttribute + '>' +
        tag +
        '<span class="slot-day">' + escapeHtml(labelParts.day) + '</span>' +
        '<span class="slot-time">' + escapeHtml(labelParts.time) + '</span>' +
        '<span class="slot-fee">' + feeText + '</span>' +
        '</label>';
}

function placeOrder() {
    clearCheckoutStatus();

    if (!checkoutBasket || checkoutBasket.items.length === 0) {
        setCheckoutStatus('Your basket is empty. Add items before checkout.', true);
        return;
    }

    if (!selectedDeliveryWindow) {
        setCheckoutStatus('Please choose a delivery slot.', true);
        return;
    }

    if (!validateCheckoutInputs()) {
        return;
    }

    const payload = {
        deliveryWindowStart: selectedDeliveryWindow.start,
        deliveryWindowEnd: selectedDeliveryWindow.end,
        address: {
            line1: getInputValue('address-line-1'),
            line2: getInputValue('address-line-2'),
            city: getInputValue('address-city'),
            postcode: getInputValue('address-postcode')
        },
        payment: {
            cardName: getInputValue('card-name'),
            cardNumber: getDigitsOnly('card-number'),
            cardExpiry: getInputValue('card-expiry'),
            cardCvv: getDigitsOnly('card-cvv')
        }
    };

    setPlaceOrderDisabled(true);
    setCheckoutStatus('Placing your order...', false);

    CustomerApi.placeOrder(payload)
        .then(function(order) {
            if (order === null) {
                return;
            }

            const estimatedTotal = (checkoutBasket.totalCost || 0) + (selectedDeliveryWindow.fee || 0);
            checkoutBasket = { items: [], totalCost: 0, itemCount: 0 };
            updateCheckoutSummary();
            refreshBasketAside();
            refreshBasketCount();
            clearCheckoutStatus();
            showOrderConfirmed({
                orderId: '#' + order.orderId,
                deliverySlot: order.deliveryWindow,
                deliveryAddress: order.deliveryAddress,
                estimatedTotal: formatMoney(estimatedTotal)
            });
        })
        .catch(function(error) {
            console.error(error);
            showCheckoutFailed(checkoutErrorMessage(error.message));
            setPlaceOrderDisabled(false);
            setCheckoutStatus('', false);
        });
}

function updateCheckoutSummary() {
    const basketTotal = checkoutBasket ? checkoutBasket.totalCost : 0;
    const itemCount = checkoutBasket ? checkoutBasket.itemCount : 0;
    const deliveryFee = selectedDeliveryWindow ? selectedDeliveryWindow.fee : 0;
    const total = basketTotal + deliveryFee;

    setText('summary-items-label', 'Items (' + itemCount + ')');
    setSummaryAmount(0, formatMoney(basketTotal));
    setSummaryAmount(1, formatMoney(deliveryFee));
    setText('summary-slot-value', selectedDeliveryWindow ? selectedDeliveryWindow.label : 'Not selected');
    setSummaryAmount(3, formatMoney(total));
}

function fillAddressFields(address) {
    setInputValue('address-line-1', address.line1);
    setInputValue('address-line-2', address.line2 || '');
    setInputValue('address-city', address.city);
    setInputValue('address-postcode', address.postcode);
}

function validateCheckoutInputs() {
    const inputs = document.querySelectorAll('.address-container input[required], .payment-container input[required]');

    for (let i = 0; i < inputs.length; i++) {
        if (!inputs[i].checkValidity()) {
            inputs[i].reportValidity();
            inputs[i].focus();
            return false;
        }
    }

    if (!isValidCardholderName(getInputValue('card-name'))) {
        setInputError('card-name', 'Please enter the name shown on the card.');
        return false;
    }

    const cardNumber = getDigitsOnly('card-number');
    if (!/^\d{16}$/.test(cardNumber) || !passesLuhnCheck(cardNumber)) {
        setInputError('card-number', 'Please enter a valid 16 digit card number.');
        return false;
    }

    if (!isValidExpiry(getInputValue('card-expiry'))) {
        setInputError('card-expiry', 'Please enter a valid future expiry date in MM / YY format.');
        return false;
    }

    if (!/^\d{3,4}$/.test(getDigitsOnly('card-cvv'))) {
        setInputError('card-cvv', 'Please enter a 3 or 4 digit CVV.');
        return false;
    }

    return true;
}

function bindPaymentFormatting() {
    const cardNumberInput = document.getElementById('card-number');
    const expiryInput = document.getElementById('card-expiry');
    const cvvInput = document.getElementById('card-cvv');
    const cardNameInput = document.getElementById('card-name');

    if (cardNameInput !== null) {
        cardNameInput.addEventListener('input', function() {
            cardNameInput.setCustomValidity('');
        });
    }

    if (cardNumberInput !== null) {
        cardNumberInput.addEventListener('input', function() {
            const digits = cardNumberInput.value.replace(/\D/g, '').slice(0, 16);
            cardNumberInput.value = digits.replace(/(.{4})/g, '$1 ').trim();
            cardNumberInput.setCustomValidity('');
        });
    }

    if (expiryInput !== null) {
        expiryInput.addEventListener('input', function() {
            const digits = expiryInput.value.replace(/\D/g, '').slice(0, 4);
            expiryInput.value = digits.length > 2 ? digits.slice(0, 2) + ' / ' + digits.slice(2) : digits;
            expiryInput.setCustomValidity('');
        });
    }

    if (cvvInput !== null) {
        cvvInput.addEventListener('input', function() {
            cvvInput.value = cvvInput.value.replace(/\D/g, '').slice(0, 4);
            cvvInput.setCustomValidity('');
        });
    }
}

function isValidExpiry(value) {
    const match = value.match(/^(\d{2})\s*\/\s*(\d{2})$/);
    if (match === null) {
        return false;
    }

    const month = parseInt(match[1], 10);
    const year = 2000 + parseInt(match[2], 10);

    if (month < 1 || month > 12) {
        return false;
    }

    const expiryDate = new Date(year, month, 0, 23, 59, 59);
    return expiryDate >= new Date();
}

function isValidCardholderName(value) {
    return /^[A-Za-z][A-Za-z .'\-]{1,}$/.test(value.trim());
}

function passesLuhnCheck(cardNumber) {
    let sum = 0;
    let shouldDouble = false;

    for (let i = cardNumber.length - 1; i >= 0; i--) {
        let digit = parseInt(cardNumber.charAt(i), 10);

        if (shouldDouble) {
            digit *= 2;
            if (digit > 9) {
                digit -= 9;
            }
        }

        sum += digit;
        shouldDouble = !shouldDouble;
    }

    return sum > 0 && sum % 10 === 0;
}

function getDigitsOnly(id) {
    return getInputValue(id).replace(/\D/g, '');
}

function setInputError(id, message) {
    const input = document.getElementById(id);

    if (input === null) {
        setCheckoutStatus(message, true);
        return;
    }

    input.setCustomValidity(message);
    input.reportValidity();
    input.focus();
}

function setCheckoutStatus(message, isError) {
    const status = document.getElementById('checkout-status');
    if (status === null) {
        return;
    }

    status.hidden = message === '';
    status.textContent = message;
    status.classList.toggle('checkout-status-error', isError);
}

function clearCheckoutStatus() {
    setCheckoutStatus('', false);
}

function setPlaceOrderDisabled(disabled) {
    const button = document.getElementById('place-order-btn');
    if (button !== null) {
        button.disabled = disabled;
    }
}

function getInputValue(id) {
    const input = document.getElementById(id);
    return input ? input.value.trim() : '';
}

function setInputValue(id, value) {
    const input = document.getElementById(id);
    if (input !== null) {
        input.value = value || '';
    }
}

function setText(id, value) {
    const element = document.getElementById(id);
    if (element !== null) {
        element.textContent = value;
    }
}

function setSummaryAmount(rowIndex, value) {
    const rows = document.querySelectorAll('.checkout-summary-container .summary-row');
    const row = rows[rowIndex];
    if (row !== undefined && row.lastElementChild !== null) {
        row.lastElementChild.textContent = value;
    }
}

function splitSlotLabel(label) {
    const parts = label.split(', ');
    return {
        day: parts[0] || label,
        time: parts[1] || ''
    };
}

function formatMoney(value) {
    return '\u00A3' + Number(value || 0).toFixed(2);
}

function checkoutErrorMessage(message) {
    const messages = {
        empty_basket: 'Your basket is empty. Add items before checkout.',
        invalid_delivery_window: 'Please choose an available delivery slot.',
        invalid_address: 'Please enter a valid delivery address.',
        missing_fields: 'Please complete all required checkout fields.',
        invalid_card_name: 'Please enter the name shown on the card.',
        invalid_card_number: 'Please enter a valid 16 digit card number.',
        invalid_card_expiry: 'Please enter a valid future expiry date in MM / YY format.',
        invalid_card_cvv: 'Please enter a 3 or 4 digit CVV.'
    };

    return messages[message] || 'Please try again or contact support if the problem persists.';
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
