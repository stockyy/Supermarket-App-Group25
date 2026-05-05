(function() {
    CustomerApi.getSession()
        .then(function(session) {
            if (session === null) {
                window.location.href = '/customers/login';
            }
        })
        .catch(function(error) {
            console.error('Error checking protected customer page session:', error);
            window.location.href = '/customers/login';
        });
})();
