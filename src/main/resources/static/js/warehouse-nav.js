
function initWarehouseNav(activePage = '') {
    fetch('/static/views/partials/managementNavBar.html')
        .then(res => res.text())
        .then(html => {
            document.getElementById('nav-placeholder').innerHTML = html;

            const navStaff = document.getElementById('nav-staff');
            const navSettings = document.getElementById('nav-settings');
            const navDashboard = document.querySelector('#nav-dashboard a');
            const navDashboardLi = document.getElementById('nav-dashboard');

            // Hide Staff, show Settings for warehouse workers
            if (navStaff) navStaff.style.display = 'none';
            if (navSettings) navSettings.style.display = 'list-item';

            // Correct the Dashboard link
            if (navDashboard) {
                navDashboard.href = '/warehouse/dashboard';
                if (activePage === 'dashboard') {
                    navDashboard.setAttribute('aria-current', 'page');
                } else {
                    navDashboard.removeAttribute('aria-current');
                }
            }

            // Correct the Settings link active state
            if (navSettings) {
                const settingsLink = navSettings.querySelector('a');
                if (activePage === 'settings') {
                    settingsLink.setAttribute('aria-current', 'page');
                } else {
                    settingsLink.removeAttribute('aria-current');
                }
            }

            const navBackBtn = document.getElementById('nav-back-btn');
            if (navBackBtn) {
                navBackBtn.style.display = 'none';
            }
            
            // Re-initialize any partials logic if needed (like basket, though warehouse doesn't have it)
            if (typeof initBasket === 'function') {
                initBasket();
            }
        })
        .catch(err => console.error('Warehouse nav failed to load:', err));
}
