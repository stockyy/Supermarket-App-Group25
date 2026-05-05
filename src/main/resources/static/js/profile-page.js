let currentProfile = null;
let currentAddress = null;

document.addEventListener('DOMContentLoaded', function() {
    loadProfile();
    loadAddress();
    bindProfileForms();
});

function bindProfileForms() {
    const profileForm = document.getElementById('profile-update-form');
    const addressForm = document.getElementById('address-update-form');
    const passwordForm = document.getElementById('password-update-form');
    const paymentForm = document.querySelector('.payment-update form');

    profileForm?.addEventListener('submit', submitProfileUpdate);
    addressForm?.addEventListener('submit', submitAddressUpdate);
    passwordForm?.addEventListener('submit', submitPasswordUpdate);
    paymentForm?.addEventListener('submit', function(event) {
        event.preventDefault();
        closeAllPopups();
    });
}

function loadProfile() {
    fetchJson('/customers/me')
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
    fetch('/customers/me/address')
        .then(function(response) {
            if (response.status === 401) {
                window.location.href = '/customers/login';
                return null;
            }

            if (response.status === 404) {
                renderNoAddress();
                return null;
            }

            if (!response.ok) {
                throw new Error('Request failed with status ' + response.status);
            }

            return response.json();
        })
        .then(function(address) {
            if (address === null) {
                return;
            }

            currentAddress = address;
            renderAddress(address);
            fillAddressForm(address);
        })
        .catch(function(error) {
            console.error(error);
            renderNoAddress();
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

    fetchJson('/customers/me', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
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
    const payload = {
        line1: getFormValue(form, 'line_1'),
        line2: getFormValue(form, 'line_2'),
        city: getFormValue(form, 'city'),
        postcode: getFormValue(form, 'postcode')
    };

    setFormLoading(form, true);
    clearFormMessage('address-form-message');

    fetchJson('/customers/me/address', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(function(address) {
            if (address === null) {
                return;
            }

            currentAddress = address;
            renderAddress(address);
            fillAddressForm(address);
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

    fetch('/customers/me/password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
        .then(function(response) {
            if (response.status === 401) {
                window.location.href = '/customers/login';
                return null;
            }

            if (!response.ok) {
                return response.text().then(function(message) {
                    throw new Error(message || 'password_update_failed');
                });
            }

            return response.text();
        })
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

function fetchJson(url, options) {
    return fetch(url, options)
        .then(function(response) {
            if (response.status === 401) {
                window.location.href = '/customers/login';
                return null;
            }

            if (!response.ok) {
                return response.text().then(function(message) {
                    throw new Error(message || 'request_failed');
                });
            }

            return response.json();
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

    setFormValue(form, 'line_1', address.line1 || '');
    setFormValue(form, 'line_2', address.line2 || '');
    setFormValue(form, 'city', address.city || '');
    setFormValue(form, 'postcode', address.postcode || '');
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
        invalid_current_password: 'The current password is incorrect.'
    };

    return messages[message] || 'Something went wrong. Please try again.';
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
