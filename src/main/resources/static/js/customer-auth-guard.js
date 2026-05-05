(function() {
    fetch('/customers/session', { cache: 'no-store' })
        .then(function(response) {
            if (response.status === 401) {
                window.location.href = '/customers/login';
                return;
            }

            if (!response.ok) {
                throw new Error('Session check failed, status: ' + response.status);
            }
        })
        .catch(function(error) {
            console.error('Error checking protected customer page session:', error);
            window.location.href = '/customers/login';
        });
})();
