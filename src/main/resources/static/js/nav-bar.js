
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
    const closeBtn = document.getElementById('aside-close');
    const backdrop = document.getElementById('basket-backdrop');

    if (closeBtn !== null && closeBtn.dataset.bound !== 'true') {
        closeBtn.addEventListener('click', closeBasket);
        closeBtn.dataset.bound = 'true';
    }

    if (backdrop !== null && backdrop.dataset.bound !== 'true') {
        backdrop.addEventListener('click', closeBasket);
        backdrop.dataset.bound = 'true';
    }

    initCustomerNav();
}

function initCustomerNav() {
    const logoutButton = document.getElementById('nav-logout-btn');
    const loginItem = document.getElementById('nav-login-item');
    const profileItem = document.getElementById('nav-profile-item');

    if (logoutButton === null && loginItem === null && profileItem === null) {
        return;
    }

    if (logoutButton !== null && logoutButton.dataset.bound !== 'true') {
        logoutButton.addEventListener('click', logoutCustomer);
        logoutButton.dataset.bound = 'true';
    }

    fetch('/customers/session')
        .then(function(response) {
            if (response.status === 401) {
                renderLoggedOutNav();
                return null;
            }

            if (!response.ok) {
                throw new Error('Failed to check customer session, status: ' + response.status);
            }

            return response.json();
        })
        .then(function(session) {
            if (session === null) {
                return;
            }

            renderLoggedInNav(session);
        })
        .catch(function(error) {
            console.error('Error checking customer session:', error);
            renderLoggedOutNav();
        });
}

function renderLoggedInNav(session) {
    const loginItem = document.getElementById('nav-login-item');
    const profileItem = document.getElementById('nav-profile-item');
    const logoutItem = document.getElementById('nav-logout-item');
    const profileLink = document.getElementById('nav-profile-link');

    if (loginItem !== null) loginItem.hidden = true;
    if (profileItem !== null) profileItem.hidden = false;
    if (logoutItem !== null) logoutItem.hidden = false;
    if (profileLink !== null && session.name) profileLink.setAttribute('aria-label', session.name + "'s profile");

    updateNavBasketBadge(session.basketCount);
}

function renderLoggedOutNav() {
    const loginItem = document.getElementById('nav-login-item');
    const profileItem = document.getElementById('nav-profile-item');
    const logoutItem = document.getElementById('nav-logout-item');

    if (loginItem !== null) loginItem.hidden = false;
    if (profileItem !== null) profileItem.hidden = true;
    if (logoutItem !== null) logoutItem.hidden = true;

    updateNavBasketBadge(0);
}

function updateNavBasketBadge(count) {
    const badge = document.getElementById('basket-count');

    if (badge === null) {
        return;
    }

    if (!count || count < 1) {
        badge.hidden = true;
        badge.textContent = '0';
        return;
    }

    badge.hidden = false;
    badge.textContent = count;
}

function logoutCustomer() {
    fetch('/customers/logout', {
        method: 'POST'
    })
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Failed to logout, status: ' + response.status);
            }

            window.location.href = '/customers/login';
        })
        .catch(function(error) {
            console.error('Error logging out:', error);
            window.location.href = '/customers/login';
        });
}
