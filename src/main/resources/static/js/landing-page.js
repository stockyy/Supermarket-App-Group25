(function() {
    var categoryNames = {
        1: 'Fresh Produce',
        2: 'Bakery',
        3: 'Dairy, Eggs & Chilled',
        4: 'Meat, Poultry & Fish',
        5: 'Food Cupboard',
        6: 'Snacks & Confectionery',
        7: 'Beverages',
        8: 'Household & Cleaning',
        9: 'Toiletries & Health',
        10: 'Frozen Food'
    };

    document.addEventListener('DOMContentLoaded', loadLandingProducts);

    function loadLandingProducts() {
        fetch('/products/getAll')
            .then(function(response) {
                if (!response.ok) {
                    throw new Error('Could not load products');
                }
                return response.json();
            })
            .then(function(products) {
                var inStock = products.filter(function(product) {
                    return product.stockLevel > 0;
                });
                var recommendations = inStock
                    .filter(function(product) {
                        return product.categoryId === 2 || product.categoryId === 3;
                    })
                    .slice(0, 5);
                var trending = inStock
                    .slice()
                    .sort(function(a, b) {
                        return a.stockLevel - b.stockLevel;
                    })
                    .slice(0, 5);

                renderProducts('.recommendations-section .product-grid', recommendations.length ? recommendations : inStock.slice(0, 5), 'Recommended');
                renderProducts('.trending-section .product-grid', trending.length ? trending : inStock.slice(0, 5), 'Trending');
            })
            .catch(function(error) {
                console.error(error);
                setStatus('.recommendations-section .product-grid', 'Could not load recommendations.');
                setStatus('.trending-section .product-grid', 'Could not load trending products.');
            });
    }

    function renderProducts(selector, products, tagText) {
        var grid = document.querySelector(selector);
        if (!grid) {
            return;
        }

        if (products.length === 0) {
            setStatus(selector, 'No products available.');
            return;
        }

        grid.innerHTML = products.map(function(product) {
            return makeProductCard(product, tagText);
        }).join('');

        grid.querySelectorAll('.add-to-basket').forEach(function(button) {
            button.addEventListener('click', function() {
                addToBasket(button.dataset.productId, button);
            });
        });
    }

    function makeProductCard(product, tagText) {
        var stockInfo = getStockInfo(product.stockLevel);
        var imageUrl = product.imageUrl || '/static/images/placeholder.png';
        var disabledAttribute = product.stockLevel <= 0 ? ' disabled' : '';

        return '<div class="product-card">' +
            '<span class="product-tag">' + escapeHtml(tagText) + '</span>' +
            '<div class="product-image">' +
            '<img src="' + escapeHtml(imageUrl) + '" alt="' + escapeHtml(product.name) + '">' +
            '</div>' +
            '<div class="product-info">' +
            '<h3>' + escapeHtml(product.name) + '</h3>' +
            '<span class="product-category">' + escapeHtml(categoryNames[product.categoryId] || 'Category ' + product.categoryId) + '</span>' +
            '<span class="product-price">£' + Number(product.price || 0).toFixed(2) + '</span>' +
            '<span class="stock-indicator ' + stockInfo.className + '">' + stockInfo.text + '</span>' +
            '<button class="add-to-basket" data-product-id="' + product.id + '"' + disabledAttribute + '>Add to Basket</button>' +
            '</div>' +
            '</div>';
    }

    function getStockInfo(stockLevel) {
        if (stockLevel <= 0) {
            return { className: 'stock-out', text: 'Out of Stock' };
        }

        if (stockLevel < 10) {
            return { className: 'stock-low', text: 'Only ' + stockLevel + ' left' };
        }

        return { className: 'stock-in', text: 'In Stock' };
    }

    function addToBasket(productId, buttonElement) {
        buttonElement.disabled = true;
        var originalText = buttonElement.textContent;
        buttonElement.textContent = 'Adding...';

        CustomerApi.addToBasket(productId, 1)
            .then(function(message) {
                if (message === null) {
                    return;
                }

                buttonElement.textContent = 'Added!';
                refreshBasketCount();
                refreshBasketAside();

                setTimeout(function() {
                    buttonElement.textContent = originalText;
                    buttonElement.disabled = false;
                }, 1000);
            })
            .catch(function(error) {
                console.error('Error adding to basket:', error);
                buttonElement.textContent = originalText;
                buttonElement.disabled = false;
                alert('Could not add to basket. Please try again.');
            });
    }

    function setStatus(selector, message) {
        var grid = document.querySelector(selector);
        if (grid) {
            grid.innerHTML = '<p class="landing-product-status">' + escapeHtml(message) + '</p>';
        }
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
})();
