function isTokenValid() {
    const token = localStorage.getItem('.JAVA.application');
    const expirationTime = localStorage.getItem('JAVAapplicationExpiration');

    if (!token || !expirationTime) {
        return false; 
    }

    const currentTime = new Date().getTime();
    return currentTime < expirationTime; 
}

function getToken() {
    if (isTokenValid()) {
        return localStorage.getItem('.JAVA.application');
    } else {
        logout();
        return null;
    }
}

function logout() {
    localStorage.removeItem('.JAVA.application');
    localStorage.removeItem('JAVAapplicationExpiration');
    Swal.fire({
        icon: 'error',
        title: 'Invalid Token',
        text: 'You are being redirected...',
        timer: 2000,
        showConfirmButton: false
    });
    // setTimeout(() => {
    //     window.location.href = '/';
    // }, 2000);
}