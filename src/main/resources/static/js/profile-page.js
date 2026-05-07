let currentProfile = null;
let currentAddresses = [];
let currentPayment = null;

document.addEventListener('DOMContentLoaded', function() {
    loadProfile();
    loadAddress();
    loadPayment();
    loadCurrentOrder();
    bindProfileForms();
});

function bindProfileForms() {
    const profileForm = document.getElementById('profile-update-form');
    const addressForm = document.getElementById('address-update-form');
    const passwordForm = document.getElementById('password-update-form');
    const paymentForm = document.querySelector('.payment-update form');
    const addAddressButton = document.querySelector('[data-popup="address-update"]');

    profileForm?.addEventListener('submit', submitProfileUpdate);
    addressForm?.addEventListener('submit', submitAddressUpdate);
    passwordForm?.addEventListener('submit', submitPasswordUpdate);
    paymentForm?.addEventListener('submit', submitPaymentUpdate);
    addAddressButton?.addEventListener('click', prepareAddressCreateForm);
    document.addEventListener('click', handleAddressListClick);
    bindPaymentFormatting();
}

function submitPaymentUpdate(event) {
    event.preventDefault();

    if (!validatePaymentForm()) {
        return;
    }

    clearFormMessage('payment-form-message');

    const form = event.currentTarget;
    const payload = {
        cardName: getPaymentInputValue('payment-card-name'),
        cardNumber: getPaymentDigitsOnly('payment-card-number'),
        cardExpiry: getPaymentInputValue('payment-card-expiry'),
        cardCvv: getPaymentDigitsOnly('payment-card-cvv')
    };

    setFormLoading(form, true);

    CustomerApi.updatePayment(payload)
        .then(function(payment) {
            if (payment === null) {
                return;
            }

            currentPayment = payment;
            renderPayment(payment);
            form.reset();
            closeAllPopups();
        })
        .catch(function(error) {
            showFormMessage('payment-form-message', profileErrorMessage(error.message), true);
        })
        .finally(function() {
            setFormLoading(form, false);
        });
}

function loadProfile() {
    CustomerApi.getProfile()
        .then(function(profile) {
            if (profile === null) {
                return;
            }

            currentProfile = profile;
            renderProfile(profile);
            fillProfileForm(profile);
        })
        .catch(function(error) {
            handleProfileError(error, 'Could not load your profile.');
        });
}

function loadAddress() {
    CustomerApi.getAddresses()
        .then(function(addresses) {
            currentAddresses = Array.isArray(addresses) ? addresses : [];

            if (currentAddresses.length === 0) {
                renderNoAddress();
                renderAddressList([]);
                return;
            }

            renderAddress(currentAddresses[0]);
            renderAddressList(currentAddresses);
        })
        .catch(function(error) {
            console.error(error);
            renderNoAddress();
            renderAddressList([]);
        });
}

function loadPayment() {
    CustomerApi.getPayment()
        .then(function(payment) {
            if (payment === null) {
                renderNoPayment();
                return;
            }

            currentPayment = payment;
            renderPayment(payment);
        })
        .catch(function(error) {
            console.error(error);
            renderNoPayment();
        });
}

function loadCurrentOrder() {
    CustomerApi.getOrders()
        .then(function(orders) {
            if (orders === null || orders.length === 0) {
                renderNoCurrentOrder();
                return;
            }

            const currentOrder = findCurrentOrder(orders);
            renderCurrentOrder(currentOrder);
        })
        .catch(function(error) {
            console.error(error);
            renderNoCurrentOrder();
        });
}

function submitProfileUpdate(event) {
    event.preventDefault();

    const form = event.currentTarget;
    const payload = {
        firstName: getFormValue(form, 'firstName'),
        lastName: getFormValue(form, 'lastName'),
        email: getFormValue(form, 'email'),
        phone: getFormValue(form, 'phone'),
        dateOfBirth: getFormValue(form, 'dateOfBirth')
    };

    setFormLoading(form, true);
    clearFormMessage('profile-form-message');

    CustomerApi.updateProfile(payload)
        .then(function(profile) {
            if (profile === null) {
                return;
            }

            currentProfile = profile;
            renderProfile(profile);
            fillProfileForm(profile);
            closeAllPopups();
        })
        .catch(function(error) {
            showFormMessage('profile-form-message', profileErrorMessage(error.message), true);
        })
        .finally(function() {
            setFormLoading(form, false);
        });
}

function submitAddressUpdate(event) {
    event.preventDefault();

    const form = event.currentTarget;
    const addressId = getFormValue(form, 'addressId');
    const payload = {
        line1: getFormValue(form, 'line_1'),
        line2: getFormValue(form, 'line_2'),
        city: getFormValue(form, 'city'),
        postcode: getFormValue(form, 'postcode')
    };

    setFormLoading(form, true);
    clearFormMessage('address-form-message');

    const request = addressId ? CustomerApi.updateAddressById(addressId, payload) : CustomerApi.addAddress(payload);

    request
        .then(function() {
            form.reset();
            loadAddress();
            closeAllPopups();
        })
        .catch(function(error) {
            showFormMessage('address-form-message', profileErrorMessage(error.message), true);
        })
        .finally(function() {
            setFormLoading(form, false);
        });
}

function submitPasswordUpdate(event) {
    event.preventDefault();

    const form = event.currentTarget;
    const payload = {
        currentPassword: getFormValue(form, 'currentPassword'),
        newPassword: getFormValue(form, 'newPassword')
    };

    setFormLoading(form, true);
    clearFormMessage('password-form-message');

    CustomerApi.updatePassword(payload)
        .then(function(result) {
            if (result === null) {
                return;
            }

            form.reset();
            closeAllPopups();
        })
        .catch(function(error) {
            showFormMessage('password-form-message', profileErrorMessage(error.message), true);
        })
        .finally(function() {
            setFormLoading(form, false);
        });
}

function renderProfile(profile) {
    setProfileField('name', profile.firstName + ' ' + profile.lastName);
    setProfileField('email', profile.email);
    setProfileField('phone', profile.phone || 'Not provided');
    setProfileField('dateOfBirth', formatDate(profile.dateOfBirth));
}

function renderAddress(address) {
    setAddressField('line1', address.line1);
    setAddressField('line2', address.line2 || 'Not provided');
    setAddressField('city', address.city);
    setAddressField('postcode', address.postcode);
}

function renderNoAddress() {
    setAddressField('line1', 'Not provided');
    setAddressField('line2', 'Not provided');
    setAddressField('city', 'Not provided');
    setAddressField('postcode', 'Not provided');
}

function renderAddressList(addresses) {
    const container = document.getElementById('saved-addresses-list');
    if (container === null) {
        return;
    }

    if (addresses.length === 0) {
        container.innerHTML = '<p class="saved-address-empty">No saved delivery addresses yet.</p>';
        return;
    }

    container.innerHTML = addresses.map(function(address, index) {
        const label = index === 0 ? 'Default address' : 'Saved address ' + (index + 1);
        return '<article class="saved-address-card">' +
            '<div>' +
            '<span class="saved-address-label">' + escapeHtml(label) + '</span>' +
            '<p>' + escapeHtml(formatAddressLine(address)) + '</p>' +
            '</div>' +
            '<div class="saved-address-actions">' +
            '<button type="button" class="profile-update-btn-secondary edit-address-btn" data-address-id="' + address.id + '">Edit</button>' +
            '<button type="button" class="saved-address-delete-btn delete-address-btn" data-address-id="' + address.id + '">Delete</button>' +
            '</div>' +
            '</article>';
    }).join('');
}

function renderPayment(payment) {
    setPaymentField('card', 'Card ending ' + payment.cardLastFour);
    setPaymentField('expiry', payment.expiry);
    setPaymentField('cardholderName', payment.cardholderName);
}

function renderNoPayment() {
    setPaymentField('card', 'Not saved');
    setPaymentField('expiry', 'Not saved');
    setPaymentField('cardholderName', 'Not saved');
}

function renderCurrentOrder(order) {
    setCurrentOrderField('orderId', '#' + order.orderId);
    setCurrentOrderField('status', formatStatus(order.status));
    setCurrentOrderField('deliverySlot', formatDeliveryWindow(order.deliveryWindowStart, order.deliveryWindowEnd));
    setCurrentOrderField('total', formatMoney(order.totalCost));
}

function renderNoCurrentOrder() {
    setCurrentOrderField('orderId', 'No current order');
    setCurrentOrderField('status', 'Not available');
    setCurrentOrderField('deliverySlot', 'Not available');
    setCurrentOrderField('total', 'Not available');
}

function findCurrentOrder(orders) {
    for (let i = 0; i < orders.length; i++) {
        if (orders[i].status !== 'DELIVERED') {
            return orders[i];
        }
    }

    return orders[0];
}

function fillProfileForm(profile) {
    const form = document.getElementById('profile-update-form');
    if (form === null) {
        return;
    }

    setFormValue(form, 'firstName', profile.firstName || '');
    setFormValue(form, 'lastName', profile.lastName || '');
    setFormValue(form, 'email', profile.email || '');
    setFormValue(form, 'phone', profile.phone || '');
    setFormValue(form, 'dateOfBirth', profile.dateOfBirth || '');
}

function fillAddressForm(address) {
    const form = document.getElementById('address-update-form');
    if (form === null) {
        return;
    }

    setFormValue(form, 'addressId', address.id || '');
    setFormValue(form, 'line_1', address.line1 || '');
    setFormValue(form, 'line_2', address.line2 || '');
    setFormValue(form, 'city', address.city || '');
    setFormValue(form, 'postcode', address.postcode || '');
}

function prepareAddressCreateForm() {
    const form = document.getElementById('address-update-form');
    if (form === null) {
        return;
    }

    form.reset();
    setFormValue(form, 'addressId', '');
    clearFormMessage('address-form-message');
}

function prepareAddressEditForm(addressId) {
    const address = currentAddresses.find(function(candidate) {
        return String(candidate.id) === String(addressId);
    });

    if (address === undefined) {
        return;
    }

    clearFormMessage('address-form-message');
    fillAddressForm(address);

    if (typeof openPopup === 'function') {
        openPopup('address-update');
    }
}

function handleAddressListClick(event) {
    const editButton = event.target.closest('.edit-address-btn');
    if (editButton !== null) {
        prepareAddressEditForm(editButton.dataset.addressId);
        return;
    }

    const deleteButton = event.target.closest('.delete-address-btn');
    if (deleteButton !== null) {
        deleteAddress(deleteButton.dataset.addressId);
    }
}

function deleteAddress(addressId) {
    if (!window.confirm('Delete this saved address?')) {
        return;
    }

    CustomerApi.deleteAddress(addressId)
        .then(function(addresses) {
            currentAddresses = Array.isArray(addresses) ? addresses : [];

            if (currentAddresses.length === 0) {
                renderNoAddress();
            } else {
                renderAddress(currentAddresses[0]);
            }

            renderAddressList(currentAddresses);
        })
        .catch(function(error) {
            window.alert(profileErrorMessage(error.message));
        });
}

function getFormValue(form, name) {
    const field = form.elements[name];
    return field ? field.value : '';
}

function setFormValue(form, name, value) {
    const field = form.elements[name];
    if (field !== undefined) {
        field.value = value;
    }
}

function setProfileField(field, value) {
    const element = document.querySelector('[data-profile-field="' + field + '"]');
    if (element !== null) {
        element.textContent = value;
    }
}

function setAddressField(field, value) {
    const element = document.querySelector('[data-address-field="' + field + '"]');
    if (element !== null) {
        element.textContent = value;
    }
}

function setCurrentOrderField(field, value) {
    const element = document.querySelector('[data-current-order-field="' + field + '"]');
    if (element !== null) {
        element.textContent = value;
    }
}

function setPaymentField(field, value) {
    const element = document.querySelector('[data-payment-field="' + field + '"]');
    if (element !== null) {
        element.textContent = value;
    }
}

function setFormLoading(form, isLoading) {
    const submitButton = form.querySelector('button[type="submit"]');
    if (submitButton !== null) {
        submitButton.disabled = isLoading;
        submitButton.textContent = isLoading ? 'Saving...' : 'Update';
    }
}

function showFormMessage(id, message, isError) {
    const element = document.getElementById(id);
    if (element === null) {
        return;
    }

    element.hidden = false;
    element.textContent = message;
    element.classList.toggle('error', isError);
}

function clearFormMessage(id) {
    const element = document.getElementById(id);
    if (element !== null) {
        element.hidden = true;
        element.textContent = '';
        element.classList.remove('error');
    }
}

function handleProfileError(error, fallbackMessage) {
    console.error(error);
    setProfileField('name', fallbackMessage);
    setProfileField('email', '');
    setProfileField('phone', 'Not available');
    setProfileField('dateOfBirth', 'Not available');
}

function profileErrorMessage(message) {
    const messages = {
        missing_fields: 'Please complete all required fields.',
        invalid_email: 'Please enter a valid email address.',
        invalid_date: 'Please enter a valid date of birth.',
        underage: 'Customers must be at least 18 years old.',
        invalid_phone: 'Please enter a valid phone number.',
        email_exists: 'That email address is already in use.',
        weak_password: 'Use at least 8 characters with upper, lower, number, and symbol.',
        invalid_current_password: 'The current password is incorrect.',
        invalid_card_name: 'Please enter the name shown on the card.',
        invalid_card_number: 'Please enter a valid 16 digit card number.',
        invalid_card_expiry: 'Please enter a valid future expiry date in MM / YY format.',
        invalid_card_cvv: 'Please enter a 3 or 4 digit CVV.',
        address_in_use: 'This address is attached to an order and cannot be deleted.'
    };

    return messages[message] || 'Something went wrong. Please try again.';
}

function bindPaymentFormatting() {
    const cardNumberInput = document.getElementById('payment-card-number');
    const expiryInput = document.getElementById('payment-card-expiry');
    const cvvInput = document.getElementById('payment-card-cvv');
    const cardNameInput = document.getElementById('payment-card-name');

    cardNameInput?.addEventListener('input', function() {
        cardNameInput.setCustomValidity('');
        clearFormMessage('payment-form-message');
    });

    cardNumberInput?.addEventListener('input', function() {
        const digits = cardNumberInput.value.replace(/\D/g, '').slice(0, 16);
        cardNumberInput.value = digits.replace(/(.{4})/g, '$1 ').trim();
        cardNumberInput.setCustomValidity('');
        clearFormMessage('payment-form-message');
    });

    expiryInput?.addEventListener('input', function() {
        const digits = expiryInput.value.replace(/\D/g, '').slice(0, 4);
        expiryInput.value = digits.length > 2 ? digits.slice(0, 2) + ' / ' + digits.slice(2) : digits;
        expiryInput.setCustomValidity('');
        clearFormMessage('payment-form-message');
    });

    cvvInput?.addEventListener('input', function() {
        cvvInput.value = cvvInput.value.replace(/\D/g, '').slice(0, 4);
        cvvInput.setCustomValidity('');
        clearFormMessage('payment-form-message');
    });
}

function validatePaymentForm() {
    const paymentForm = document.querySelector('.payment-update form');
    if (paymentForm === null) {
        return true;
    }

    const requiredInputs = paymentForm.querySelectorAll('input[required]');
    for (let i = 0; i < requiredInputs.length; i++) {
        if (!requiredInputs[i].checkValidity()) {
            requiredInputs[i].reportValidity();
            requiredInputs[i].focus();
            return false;
        }
    }

    if (!isValidCardholderName(getPaymentInputValue('payment-card-name'))) {
        return setPaymentInputError('payment-card-name', 'Please enter the name shown on the card.');
    }

    const cardNumber = getPaymentDigitsOnly('payment-card-number');
    if (!/^\d{16}$/.test(cardNumber) || !passesLuhnCheck(cardNumber)) {
        return setPaymentInputError('payment-card-number', 'Please enter a valid 16 digit card number.');
    }

    if (!isValidExpiry(getPaymentInputValue('payment-card-expiry'))) {
        return setPaymentInputError('payment-card-expiry', 'Please enter a valid future expiry date in MM / YY format.');
    }

    if (!/^\d{3,4}$/.test(getPaymentDigitsOnly('payment-card-cvv'))) {
        return setPaymentInputError('payment-card-cvv', 'Please enter a 3 or 4 digit CVV.');
    }

    return true;
}

function setPaymentInputError(id, message) {
    const input = document.getElementById(id);
    showFormMessage('payment-form-message', message, true);

    if (input !== null) {
        input.setCustomValidity(message);
        input.reportValidity();
        input.focus();
    }

    return false;
}

function getPaymentInputValue(id) {
    const input = document.getElementById(id);
    return input ? input.value.trim() : '';
}

function getPaymentDigitsOnly(id) {
    return getPaymentInputValue(id).replace(/\D/g, '');
}

function isValidCardholderName(value) {
    return /^[A-Za-z][A-Za-z .'\-]{1,}$/.test(value.trim());
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

function formatDate(value) {
    if (!value) {
        return 'Not provided';
    }

    const date = new Date(value + 'T00:00:00');
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleDateString('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric'
    });
}

function formatDeliveryWindow(startValue, endValue) {
    const start = parseDateTime(startValue);
    const end = parseDateTime(endValue);

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

function parseDateTime(value) {
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

    return labels[status] || status || 'Not available';
}

function formatMoney(value) {
    return '\u00A3' + Number(value || 0).toFixed(2);
}

function formatAddressLine(address) {
    return [
        address.line1,
        address.line2,
        address.city,
        address.postcode
    ].filter(function(part) {
        return part !== null && part !== undefined && String(part).trim() !== '';
    }).join(', ');
}

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
