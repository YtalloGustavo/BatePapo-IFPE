// api.js — wrapper para as chamadas REST da API (mesma origem, sessão via cookie JSESSIONID)

async function apiFetch(path, options) {
    options = options || {};

    var res;
    try {
        res = await fetch(path, {
            method: options.method || "GET",
            headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: options.body
        });
    } catch (e) {
        throw new Error("Não foi possível conectar ao servidor. Tente novamente.");
    }

    var body = null;
    var text = await res.text();
    if (text) {
        try {
            body = JSON.parse(text);
        } catch (e) {
            body = null;
        }
    }

    if (res.status === 401 && options.redirectOn401) {
        // Só redireciona se ainda não estiver na página de login (evita loop de recarga)
        var jaNoLogin = window.location.pathname === "/" || window.location.pathname.endsWith("/index.html");
        if (!jaNoLogin) {
            window.location.href = "/index.html";
            return;
        }
    }

    if (res.status === 401) {
        throw new Error("Sessão expirada. Faça login novamente.");
    }

    if (!res.ok) {
        var mensagem = (body && body.message) ? body.message : "Ocorreu um erro inesperado. Tente novamente.";
        throw new Error(mensagem);
    }

    return body; // null para respostas 204
}

function login(data) {
    return apiFetch("/api/login", { method: "POST", body: JSON.stringify(data) });
}

function register(data) {
    return apiFetch("/api/register", { method: "POST", body: JSON.stringify(data) });
}

function me(options) {
    return apiFetch("/api/me", options);
}

function rooms(options) {
    return apiFetch("/api/rooms", options);
}

function logout() {
    return apiFetch("/api/logout", { method: "POST" });
}