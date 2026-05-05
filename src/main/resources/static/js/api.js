(function() {
    function jsonHeaders() {
        return {
            'Content-Type': 'application/json'
        };
    }

    function withJsonBody(options, payload) {
        return Object.assign({}, options, {
            headers: Object.assign({}, jsonHeaders(), options && options.headers ? options.headers : {}),
            body: JSON.stringify(payload)
        });
    }

    function handleUnauthorized(redirectOnUnauthorized) {
        if (redirectOnUnauthorized) {
            window.location.href = '/customers/login';
        }
        return null;
    }

    function parseJsonResponse(response, redirectOnUnauthorized) {
        if (response.status === 401) {
            return handleUnauthorized(redirectOnUnauthorized);
        }

        if (response.status === 204) {
            return null;
        }

        if (!response.ok) {
            return response.text().then(function(message) {
                throw new Error(message || 'request_failed');
            });
        }

        return response.json();
    }

    function parseTextResponse(response, redirectOnUnauthorized) {
        if (response.status === 401) {
            return handleUnauthorized(redirectOnUnauthorized);
        }

        if (!response.ok) {
            return response.text().then(function(message) {
                throw new Error(message || 'request_failed');
            });
        }

        return response.text();
    }

    function getJson(url, options, redirectOnUnauthorized) {
        return fetch(url, options).then(function(response) {
            return parseJsonResponse(response, redirectOnUnauthorized !== false);
        });
    }

    function getText(url, options, redirectOnUnauthorized) {
        return fetch(url, options).then(function(response) {
            return parseTextResponse(response, redirectOnUnauthorized !== false);
        });
    }

    function getSession() {
        return fetch('/customers/session', { cache: 'no-store' })
            .then(function(response) {
                if (response.status === 401) {
                    return null;
                }

                if (!response.ok) {
                    throw new Error('Failed to check customer session, status: ' + response.status);
                }

                return response.json();
            });
    }

    function getAddress() {
        return fetch('/customers/me/address')
            .then(function(response) {
                if (response.status === 401) {
                    window.location.href = '/customers/login';
                    return null;
                }

                if (response.status === 404) {
                    return null;
                }

                return parseJsonResponse(response, true);
            });
    }

    window.CustomerApi = {
        getSession: getSession,
        logout: function() {
            return getText('/customers/logout', { method: 'POST' }, true);
        },
        getBasket: function(redirectOnUnauthorized) {
            return getJson('/orders/basket', undefined, redirectOnUnauthorized);
        },
        addToBasket: function(productId, quantity) {
            return getText('/orders/basket', withJsonBody({ method: 'POST' }, {
                productId: parseInt(productId),
                quantity: quantity || 1
            }), true);
        },
        updateBasketItem: function(cartItemId, quantity) {
            return getText('/orders/basket/' + cartItemId, withJsonBody({ method: 'PUT' }, {
                quantity: quantity
            }), true);
        },
        removeBasketItem: function(cartItemId) {
            return getText('/orders/basket/' + cartItemId, { method: 'DELETE' }, true);
        },
        getProfile: function() {
            return getJson('/customers/me', undefined, true);
        },
        updateProfile: function(payload) {
            return getJson('/customers/me', withJsonBody({ method: 'PUT' }, payload), true);
        },
        updatePassword: function(payload) {
            return getText('/customers/me/password', withJsonBody({ method: 'PUT' }, payload), true);
        },
        getAddress: getAddress,
        updateAddress: function(payload) {
            return getJson('/customers/me/address', withJsonBody({ method: 'PUT' }, payload), true);
        },
        getDeliveryWindows: function() {
            return getJson('/orders/delivery-windows', undefined, true);
        },
        placeOrder: function(payload) {
            return getJson('/orders', withJsonBody({ method: 'POST' }, payload), true);
        },
        getOrders: function() {
            return getJson('/orders', undefined, true);
        }
    };
})();
