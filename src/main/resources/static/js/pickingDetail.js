const modal = document.getElementById('sub-modal');
const sub_mod_item = document.getElementById('sub-mod-item');
const confirm_sub_btn = document.getElementById('confirm-sub-btn');
const cancel_sub_btn = document.getElementById('cancel-sub-btn');
const selected_sub_div = document.getElementById('selected-sub');
const selected_sub_name = document.getElementById('selected-sub-name');
const sub_search = document.getElementById('sub_search');
const sub_results = document.getElementById('sub-results');
const modal_original_item = document.getElementById('sub-mod-item');

let current_item_id = null;
let selected_product_id = null;

document.addEventListener('click', function (e) {
    // This is essentially the code waiting for a button to be pressed
    if (e.target.classList.contains('sub-btn')) {
        current_item_id = e.target.dataset.itemId;
        modal_original_item.textContent = e.target.dataset.itemName;
        selected_sub_div.style.display = 'none';
        confirm_sub_btn.disabled = false;
        sub_results.innerHTML = '';
        sub_search.value = '';
        modal.style.display = 'flex';
    }
});


// if it's cancelled close the modal, by setting the modal into 'none'
cancel_sub_btn.addEventListener('click', function () {
    modal.style.display = 'none';
    current_item_id = null;
    selected_product_id = null;
});

// shows the subtitiute search results and populates it 
sub_search.addEventListener('input', function () {
    const query = sub_search.value.trim();
    if (query.length < 2) {
        sub_results.innerHTML = '';
        return;
    }
    // TODO: backend will need to popualate this side based of queries
});

// Select a substitute from results
document.addEventListener('click', function (e) {
    // Take the selected product, make it visible, and making the confirm sub btn pressable
    if (e.target.classList.contains('select-substitute-btn')) {
        selected_product_id = e.target.dataset.productId;
        selected_sub_name.textContent = e.target.textContent;
        selected_sub_div.style.display = 'block';
        confirm_substitute_btn.disabled = false;
    }
});

// Confirm substitutio
confirm_sub_btn.addEventListener('click', function () {
    if (!current_item_id || !selected_product_id) return;

    // TODO: backend needs to handle the posting
    // with body: { itemId: currentItemId, substituteProductId: selectedProductId }

    // Update row status label
    const row = document.querySelector(`tr[data-item-id="${current_item_id}"]`);
    if (row) {
        const status_select = row.querySelector('.item-status-select');
        if (status_select) status_select.value = 'substituted';
    }

    modal.style.display = 'none';
    current_item_id = null;
    selected_product_id = null;
});


// CONTROLS FOR SUB BUTTON DISPLAY  

document.addEventListener('change', function (e) {
    if (e.target.classList.contains('item-status-select')) {
        const itemId = e.target.dataset.itemId;
        const sub_btn = document.querySelector(
            `.substitute-btn[data-item-id="${itemId}"]`
        );
        if (sub_btn) {
            sub_btn.style.display =
                e.target.value === 'substituted' ? 'inline-block' : 'none';
        }
    }
});


// FINALISING RODER

document.getElementById('finalise-order-btn').addEventListener('click', function () {
    // Things like recalculating prices, updating stock, updating all the items will be done here

    const confirmed = confirm(
        'Are you sure you want to finalise this order?'
    );

    if (confirmed) {
        // Trigger backend finalise endpoint
    }
});