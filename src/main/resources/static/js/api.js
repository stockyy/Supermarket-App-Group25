(function() {
    const GUEST_BASKET_KEY = 'guestBasketItems';

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

    function getPayment() {
        return fetch('/customers/me/payment')
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

    function readGuestBasketItems() {
        try {
            const rawItems = localStorage.getItem(GUEST_BASKET_KEY);
            const parsedItems = rawItems ? JSON.parse(rawItems) : [];
            return Array.isArray(parsedItems) ? parsedItems : [];
        } catch (error) {
            console.error('Could not read guest basket:', error);
            return [];
        }
    }

    function writeGuestBasketItems(items) {
        localStorage.setItem(GUEST_BASKET_KEY, JSON.stringify(items));
    }

    function makeGuestCartItemId(productId) {
        return 'guest-' + productId;
    }

    function buildGuestBasketResponse() {
        const items = readGuestBasketItems().map(function(item) {
            const unitPrice = Number(item.unitPrice || 0);
            const quantity = item.quantity === null || item.quantity === undefined ? null : Number(item.quantity);
            const weight = item.weight === null || item.weight === undefined ? null : Number(item.weight);
            const amount = item.soldByWeight ? (weight || 1) : (quantity || 1);

            return Object.assign({}, item, {
                cartItemId: makeGuestCartItemId(item.productId),
                unitPrice: unitPrice,
                quantity: item.soldByWeight ? null : amount,
                weight: item.soldByWeight ? amount : null,
                lineTotal: unitPrice * amount
            });
        });

        return {
            items: items,
            totalCost: items.reduce(function(total, item) {
                return total + item.lineTotal;
            }, 0),
            itemCount: items.reduce(function(total, item) {
                if (item.soldByWeight) {
                    return total + Math.max(1, Math.floor(item.weight || 1));
                }
                return total + (item.quantity || 1);
            }, 0)
        };
    }

    function fetchProduct(productId) {
        return fetch('/products/' + parseInt(productId))
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('product_not_found');
                }

                return response.json();
            });
    }

    function addGuestBasketItem(productId, quantity) {
        const parsedProductId = parseInt(productId);
        const amount = Math.max(1, parseInt(quantity || 1));

        return fetchProduct(parsedProductId)
            .then(function(product) {
                const items = readGuestBasketItems();
                const existingItem = items.find(function(item) {
                    return item.productId === parsedProductId;
                });

                if (existingItem) {
                    if (product.soldByWeight) {
                        existingItem.weight = Number(existingItem.weight || 0) + amount;
                        existingItem.quantity = null;
                    } else {
                        existingItem.quantity = Number(existingItem.quantity || 0) + amount;
                        existingItem.weight = null;
                    }
                } else {
                    items.push({
                        productId: product.id,
                        name: product.name,
                        imageUrl: product.imageUrl || '',
                        unitPrice: Number(product.price || 0),
                        quantity: product.soldByWeight ? null : amount,
                        weight: product.soldByWeight ? amount : null,
                        soldByWeight: product.soldByWeight
                    });
                }

                writeGuestBasketItems(items);
                return 'Item added to basket';
            });
    }

    function isGuestCartItem(cartItemId) {
        return String(cartItemId).indexOf('guest-') === 0;
    }

    function parseGuestProductId(cartItemId) {
        return parseInt(String(cartItemId).replace('guest-', ''));
    }

    function updateGuestBasketItem(cartItemId, quantity) {
        const productId = parseGuestProductId(cartItemId);
        const amount = Math.max(1, parseInt(quantity || 1));
        const items = readGuestBasketItems();
        const item = items.find(function(candidate) {
            return candidate.productId === productId;
        });

        if (!item) {
            return Promise.reject(new Error('guest_item_not_found'));
        }

        if (item.soldByWeight) {
            item.weight = amount;
            item.quantity = null;
        } else {
            item.quantity = amount;
            item.weight = null;
        }

        writeGuestBasketItems(items);
        return Promise.resolve('Quantity updated');
    }

    function removeGuestBasketItem(cartItemId) {
        const productId = parseGuestProductId(cartItemId);
        const items = readGuestBasketItems().filter(function(item) {
            return item.productId !== productId;
        });

        writeGuestBasketItems(items);
        return Promise.resolve('Item removed from basket');
    }

    window.CustomerApi = {
        getSession: getSession,
        logout: function() {
            return getText('/customers/logout', { method: 'POST' }, true);
        },
        getBasket: function(redirectOnUnauthorized) {
            return fetch('/orders/basket')
                .then(function(response) {
                    if (response.status === 401) {
                        if (redirectOnUnauthorized) {
                            window.location.href = '/customers/login';
                            return null;
                        }

                        return buildGuestBasketResponse();
                    }

                    return parseJsonResponse(response, redirectOnUnauthorized !== false);
                });
        },
        addToBasket: function(productId, quantity) {
            return fetch('/orders/basket', withJsonBody({ method: 'POST' }, {
                productId: parseInt(productId),
                quantity: quantity || 1
            }))
                .then(function(response) {
                    if (response.status === 401) {
                        return addGuestBasketItem(productId, quantity || 1);
                    }

                    return parseTextResponse(response, true);
                });
        },
        updateBasketItem: function(cartItemId, quantity) {
            if (isGuestCartItem(cartItemId)) {
                return updateGuestBasketItem(cartItemId, quantity);
            }

            return getText('/orders/basket/' + cartItemId, withJsonBody({ method: 'PUT' }, {
                quantity: quantity
            }), true);
        },
        removeBasketItem: function(cartItemId) {
            if (isGuestCartItem(cartItemId)) {
                return removeGuestBasketItem(cartItemId);
            }

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
        getAddresses: function() {
            return getJson('/customers/me/addresses', undefined, true);
        },
        addAddress: function(payload) {
            return getJson('/customers/me/addresses', withJsonBody({ method: 'POST' }, payload), true);
        },
        updateAddressById: function(addressId, payload) {
            return getJson('/customers/me/addresses/' + parseInt(addressId), withJsonBody({ method: 'PUT' }, payload), true);
        },
        deleteAddress: function(addressId) {
            return getJson('/customers/me/addresses/' + parseInt(addressId), { method: 'DELETE' }, true);
        },
        getPayment: getPayment,
        updatePayment: function(payload) {
            return getJson('/customers/me/payment', withJsonBody({ method: 'PUT' }, payload), true);
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
