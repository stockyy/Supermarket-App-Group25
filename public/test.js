// pickingDetail.js

// --- SUBSTITUTION MODAL LOGIC ---

const modal = document.getElementById('substitution-modal');
const modalOriginalItem = document.getElementById('modal-original-item');
const confirmSubstituteBtn = document.getElementById('confirm-substitute-btn');
const cancelSubstituteBtn = document.getElementById('cancel-substitute-btn');
const selectedSubstituteDiv = document.getElementById('selected-substitute');
const selectedSubstituteName = document.getElementById('selected-substitute-name');
const substituteSearch = document.getElementById('substitute-search');
const substituteResults = document.getElementById('substitute-results');

let currentItemId = null;
let selectedProductId = null;

// Show modal when "Choose Substitute" is clicked
document.addEventListener('click', function (e) {
    if (e.target.classList.contains('substitute-btn')) {
        currentItemId = e.target.dataset.itemId;
        modalOriginalItem.textContent = e.target.dataset.itemName;
        selectedSubstituteDiv.style.display = 'none';
        confirmSubstituteBtn.disabled = true;
        substituteResults.innerHTML = '';
        substituteSearch.value = '';
        modal.style.display = 'flex';
    }
});

// Close modal on cancel
cancelSubstituteBtn.addEventListener('click', function () {
    modal.style.display = 'none';
    currentItemId = null;
    selectedProductId = null;
});

// Show substitute search results
// Backend will hook into this to return matching products
substituteSearch.addEventListener('input', function () {
    const query = substituteSearch.value.trim();
    if (query.length < 2) {
        substituteResults.innerHTML = '';
        return;
    }
    // TODO: backend to populate substituteResults ul
    // based on search query via fetch to /warehouse/products/search?q=query
});

// Select a substitute from results
document.addEventListener('click', function (e) {
    if (e.target.classList.contains('select-substitute-btn')) {
        selectedProductId = e.target.dataset.productId;
        selectedSubstituteName.textContent = e.target.textContent;
        selectedSubstituteDiv.style.display = 'block';
        confirmSubstituteBtn.disabled = false;
    }
});

// Confirm substitution
confirmSubstituteBtn.addEventListener('click', function () {
    if (!currentItemId || !selectedProductId) return;

    // TODO: backend to handle POST to /warehouse/picking/{orderId}/substitute
    // with body: { itemId: currentItemId, substituteProductId: selectedProductId }

    // Update the row status label for now as visual feedback
    const row = document.querySelector(`tr[data-item-id="${currentItemId}"]`);
    if (row) {
        const statusSelect = row.querySelector('.item-status-select');
        if (statusSelect) statusSelect.value = 'substituted';
    }

    modal.style.display = 'none';
    currentItemId = null;
    selectedProductId = null;
});


// --- SHOW/HIDE SUBSTITUTE BUTTON BASED ON STATUS SELECT ---

document.addEventListener('change', function (e) {
    if (e.target.classList.contains('item-status-select')) {
        const itemId = e.target.dataset.itemId;
        const substituteBtn = document.querySelector(
            `.substitute-btn[data-item-id="${itemId}"]`
        );
        if (substituteBtn) {
            substituteBtn.style.display =
                e.target.value === 'substituted' ? 'inline-block' : 'none';
        }
    }
});


// --- FINALISE ORDER ---

document.getElementById('finalise-order-btn').addEventListener('click', function () {
    // TODO: backend to handle POST to /warehouse/picking/{orderId}/finalise
    // Ktor will read all item statuses and substitutions, update stock,
    // recalculate price, and notify customer

    const confirmed = confirm(
        'Are you sure you want to finalise this order? ' +
        'This will update stock and notify the customer.'
    );

    if (confirmed) {
        // TODO: trigger backend finalise endpoint
    }
});