function loginin(login, password){
    var login = document.getElementById("login").value;
                    var password = document.getElementById("password").value;

                    var xhr = new XMLHttpRequest();
                    xhr.open('POST', 'http://localhost:8889/login', false);
                    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
                    var data = new FormData();
                    data.append('login', login);
                    data.append('password', password);
                    xhr.send(data);


                    showLoggedPage();
                    return true;
}

function showLoggedPage(){
        location.href = 'http://localhost:8889/html/logged.html';
}

function showRegisterPage(){
        location.href = 'http://localhost:8889/html/index.html';
}

function showLoginPage(){
        location.href = 'http://localhost:8889/html/login.html';
}

function showLogoutPage(){
         location.href = 'http://localhost:8889/html/notlogged.html';
 }
