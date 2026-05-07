let currentProduct = null;

// on load reads the id
function loadProductDetail() {
    // get product id from teh url
    const urlParams = new URLSearchParams(window.location.search);
    const productId = urlParams.get('id');

    // make sure it's a legit product id
    if (!productId) {
        showError('No product ID in URL');
        return;
    }

    // get the product info from teh backend
    fetch('/products/' + productId)
        .then(function(response) {
            if (response.status === 404) {
                showError('Product not found');
                return null;
            }

            if (!response.ok) {
                throw new Error('Failed to fetch product, status: ' + response.status);
            }

            return response.json();
        })
        .then(function(product) {
            if (product === null) {
                return;
            }

            // save this so we can add using the button later on without redoing the previous steps
            currentProduct = product;

            // fill in page with product info
            populateProductDetail(product);
        })
        .catch(function(error) {
            console.error('Error loading product :', error);
            showError('Could not load product. Please try again.');
        });
}

// takes product data and fills in the corresponding fields for the frontend
function populateProductDetail(product) {
    // image
    const img = document.querySelector('.main-img');
    let imageUrl = product.imageUrl;
    if (!imageUrl || imageUrl === '' || imageUrl === 'null') {
        imageUrl = '/static/images/placeholder.png';
    }
    img.src = imageUrl;
    img.alt = product.name;

    // we don't have the name so will identify based of this for now
    const catTag = document.querySelector('.cat-tag');
    catTag.textContent = 'Category ' + product.categoryId;

    // product title
    const title = document.querySelector('.item-title');
    title.textContent = product.name;

    // update product title in teh page now too
    document.title = product.name + ' - 2850Mart';

    // price
    const priceBig = document.querySelector('.price-big');
    priceBig.textContent = '£' + product.price.toFixed(2);

    // unit label like "p/kg"
    const unitLabel = document.querySelector('.unit-label');
    if (product.soldByWeight) {
        unitLabel.textContent = 'per kg';
    } else {
        unitLabel.textContent = 'per item';
    }

    // promo badge
    const promoBadge = document.querySelector('.promo-badge');
    if (product.onOffer) {
        promoBadge.style.display = '';
    } else {
        // hide if no offer
        promoBadge.style.display = 'none';
    }

    // stock indicator
    const stockIndicator = document.querySelector('.stock-indicator');
    // removes any existing ones to make sure that the right one is currently on it
    stockIndicator.classList.remove('stock-in', 'stock-low', 'stock-out');

    if (product.stockLevel === 0) {
        stockIndicator.classList.add('stock-out');
        stockIndicator.textContent = 'Out of Stock';
    } else if (product.stockLevel < 10) {
        stockIndicator.classList.add('stock-low');
        stockIndicator.textContent = 'Low stock - only ' + product.stockLevel + ' left';
    } else {
        stockIndicator.classList.add('stock-in');
        stockIndicator.textContent = 'In Stock';
    }

    // description
    const desc = document.querySelector('.item-desc');
    if (product.description && product.description !== 'null') {
        desc.textContent = product.description;
    } else {
        desc.textContent = 'No description available.';
    }

    // wiring up quant stepper and basket button
    setupQuantityStepper();
    setupAddToBasketButton(product);

    // if out of stock disable the button
    if (product.stockLevel === 0) {
        const addBtn = document.querySelector('.btn-add-main');
        addBtn.disabled = true;
        addBtn.textContent = 'Out of Stock';
    }
}

// wires +/-
function setupQuantityStepper() {
    const stepperButtons = document.querySelectorAll('.qty-stepper button');
    const input = document.querySelector('.qty-stepper input');

    for (let i = 0; i < stepperButtons.length; i++) {
        const button = stepperButtons[i];

        button.addEventListener('click', function() {
            const action = button.dataset.action;
            let currentValue = parseInt(input.value);

            if (isNaN(currentValue) || currentValue < 1) {
                currentValue = 1;
            }

            if (action === 'inc') {
                input.value = currentValue + 1;
            } else if (action === 'dec') {
                if (currentValue > 1) {
                    input.value = currentValue - 1;
                }
            }
        });
    }
}

// wires add to basket button
function setupAddToBasketButton(product) {
    const addBtn = document.querySelector('.btn-add-main');
    const input = document.querySelector('.qty-stepper input');

    addBtn.addEventListener('click', function() {
        const quantity = parseInt(input.value);

        if (isNaN(quantity) || quantity < 1) {
            alert('Please enter a valid quantity');
            return;
        }

        addProductToBasket(product.id, quantity, addBtn);
    });
}

// send off the add request through the shared customer API
function addProductToBasket(productId, quantity, buttonElement) {
    buttonElement.disabled = true;
    const originalText = buttonElement.textContent;
    buttonElement.textContent = 'Adding...';

    CustomerApi.addToBasket(productId, quantity)
        .then(function(message) {
            buttonElement.textContent = 'Added!';

            // Update the badge and aside in the nav
            if (typeof refreshBasketCount === 'function') {
                refreshBasketCount();
            }
            if (typeof refreshBasketAside === 'function') {
                refreshBasketAside();
            }

            // Reset button after a second
            setTimeout(function() {
                buttonElement.textContent = originalText;
                buttonElement.disabled = false;
            }, 1000);
        })
        .catch(function(error) {
            console.error('Error adding to basket :', error);
            buttonElement.textContent = originalText;
            buttonElement.disabled = false;
            alert('Could not add to basket. Please try again.');
        });
}

// error message popup
function showError(message) {
    const main = document.querySelector('.product-detail');
    if (main) {
        main.innerHTML = '<div style="text-align:center;padding:4rem;"><h2>' + message + '</h2><p><a href="/customers/products-listing">Back to products</a></p></div>';
    }
}
