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

    if (res.status === 401) {
        window.location.href = "/index.html";
        throw new Error("Sessão expirada. Faça login novamente.");
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

function me() {
    return apiFetch("/api/me");
}

function rooms() {
    return apiFetch("/api/rooms");
}

function logout() {
    return apiFetch("/api/logout", { method: "POST" });
}