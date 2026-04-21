
///////////
//NAV BAR//
///////////

function openBasket(e) {
    e.preventDefault();
    const backdrop  = document.getElementById('basket-backdrop');
    const aside     = document.getElementById('basket-aside');
    const basketBtn = document.getElementById('basket-btn');
    const closeBtn  = document.getElementById('aside-close');

    backdrop.classList.add('open');
    aside.classList.add('open');
    backdrop.removeAttribute('aria-hidden');
    basketBtn.setAttribute('aria-expanded', 'true');
    closeBtn.focus();
    document.addEventListener('keydown', trapFocus);
}

function closeBasket() {
    const backdrop  = document.getElementById('basket-backdrop');
    const aside     = document.getElementById('basket-aside');
    const basketBtn = document.getElementById('basket-btn');

    backdrop.classList.remove('open');
    aside.classList.remove('open');
    backdrop.setAttribute('aria-hidden', 'true');
    basketBtn.setAttribute('aria-expanded', 'false');
    basketBtn.focus();
    document.removeEventListener('keydown', trapFocus);
}

function trapFocus(e) {
    const aside = document.getElementById('basket-aside');
    if (e.key === 'Escape') { closeBasket(); return; }
    if (e.key !== 'Tab') return;
    const focusable = [...aside.querySelectorAll('a, button:not([disabled]), input')];
    const first = focusable[0];
    const last  = focusable[focusable.length - 1];
    if (e.shiftKey && document.activeElement === first) {
        e.preventDefault(); last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault(); first.focus();
    }
}

function initBasket() {
    const searchBtn   = document.querySelector('.search-submit');
    const searchInput = document.getElementById('search-bar');

    if (searchBtn && searchInput) {
        searchBtn.onclick = () => {
            const query = searchInput.value.trim();
            if (query) {
                window.location.href = `/products/search?name=${encodeURIComponent(query)}`;
            }
        };
        searchInput.onkeydown = (e) => {
            if (e.key === 'Enter') searchBtn.click();
        };
    }

    const closeBtn = document.getElementById('aside-close');
    const backdrop = document.getElementById('basket-backdrop');
    closeBtn?.addEventListener('click', closeBasket);
    backdrop?.addEventListener('click', closeBasket);
}

