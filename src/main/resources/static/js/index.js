// index.js — lógica da página de login/registro

(function () {
    "use strict";

    var formLogin = document.getElementById("form-login");
    var formRegistro = document.getElementById("form-registro");
    var btnAlternar = document.getElementById("btn-alternar");
    var textoAlternar = document.getElementById("texto-alternar");
    var divErro = document.getElementById("erro");

    function mostrarErro(mensagem) {
        divErro.textContent = mensagem;
        divErro.hidden = false;
    }

    function limparErro() {
        divErro.hidden = true;
        divErro.textContent = "";
    }

    function alternarFormulario() {
        limparErro();
        var mostrandoLogin = !formLogin.hidden;
        formLogin.hidden = mostrandoLogin;
        formRegistro.hidden = !mostrandoLogin;
        if (mostrandoLogin) {
            textoAlternar.textContent = "Já tem uma conta?";
            btnAlternar.textContent = "Entrar";
        } else {
            textoAlternar.textContent = "Não tem uma conta?";
            btnAlternar.textContent = "Criar conta";
        }
    }

    async function enviarLogin(evento) {
        evento.preventDefault();
        limparErro();

        var username = document.getElementById("login-username").value.trim();
        var password = document.getElementById("login-password").value;

        if (!username || !password) {
            mostrarErro("Preencha usuário e senha.");
            return;
        }

        try {
            await login({ username: username, password: password });
            window.location.href = "/chat.html";
        } catch (e) {
            mostrarErro(e.message);
        }
    }

    async function enviarRegistro(evento) {
        evento.preventDefault();
        limparErro();

        var username = document.getElementById("reg-username").value.trim();
        var password = document.getElementById("reg-password").value;
        var name = document.getElementById("reg-name").value.trim();
        var periodo = document.getElementById("reg-periodo").value;

        if (!username || !password || !name || !periodo) {
            mostrarErro("Preencha todos os campos.");
            return;
        }

        try {
            await register({ username: username, password: password, name: name, periodo: parseInt(periodo, 10) });
            window.location.href = "/chat.html";
        } catch (e) {
            mostrarErro(e.message);
        }
    }

    // Se já estiver logado, vai direto para o chat
    me().then(function () {
        window.location.href = "/chat.html";
    }).catch(function () {
        // Não logado: permanece na página (api.js redireciona em 401, mas o catch evita erros não tratados)
    });

    formLogin.addEventListener("submit", enviarLogin);
    formRegistro.addEventListener("submit", enviarRegistro);
    btnAlternar.addEventListener("click", alternarFormulario);
})();