// chat.js — lógica da página de chat (salas + WebSocket STOMP via SockJS)

(function () {
    "use strict";

    var MAX_TENTATIVAS_RECONEXAO = 5;
    var INTERVALO_RECONEXAO = 2000;

    var currentRoomId = null;
    var currentUserName = null;
    var sock = null;
    var stomp = null;
    var saindoDaSala = false;
    var reconectando = false;
    var tentativasReconexao = 0;

    var telaSalas = document.getElementById("tela-salas");
    var telaChat = document.getElementById("tela-chat");
    var divSalas = document.getElementById("salas");
    var tituloSala = document.getElementById("titulo-sala");
    var statusConexao = document.getElementById("status-conexao");
    var divMensagens = document.getElementById("mensagens");
    var inputMensagem = document.getElementById("mensagem-input");
    var btnEnviar = document.getElementById("btn-enviar");
    var btnSairSala = document.getElementById("btn-sair-sala");
    var btnSair = document.getElementById("btn-sair");

    function definirStatus(texto, classe) {
        statusConexao.textContent = texto;
        statusConexao.className = "status " + classe;
    }

    function formatarHora(timestamp) {
        var d = new Date(timestamp);
        var h = d.getHours();
        var m = d.getMinutes();
        return (h < 10 ? "0" + h : h) + ":" + (m < 10 ? "0" + m : m);
    }

    // Renderização segura: apenas createElement + textContent (conteúdo do chat não é confiável)
    function renderizarMensagem(msg) {
        var div = document.createElement("div");
        div.className = "mensagem " + (msg.sender === currentUserName ? "minha-mensagem" : "outra-mensagem");

        var cabecalho = document.createElement("div");
        cabecalho.className = "mensagem-cabecalho";

        var autor = document.createElement("span");
        autor.className = "mensagem-autor";
        autor.textContent = msg.sender;

        var badge = document.createElement("span");
        badge.className = "badge";
        badge.textContent = "Período " + msg.senderPeriodo;

        var hora = document.createElement("span");
        hora.className = "mensagem-hora";
        hora.textContent = formatarHora(msg.timestamp);

        cabecalho.appendChild(autor);
        cabecalho.appendChild(badge);
        cabecalho.appendChild(hora);

        var texto = document.createElement("p");
        texto.className = "mensagem-texto";
        texto.textContent = msg.text;

        div.appendChild(cabecalho);
        div.appendChild(texto);
        divMensagens.appendChild(div);
        divMensagens.scrollTop = divMensagens.scrollHeight;
    }

    function onMensagem(frame) {
        var msg;
        try {
            msg = JSON.parse(frame.body);
        } catch (e) {
            return;
        }
        renderizarMensagem(msg);
    }

    function conectarSala(roomId) {
        sock = new SockJS("/ws");
        stomp = Stomp.over(sock);

        sock.onclose = function () {
            if (currentRoomId !== null && !saindoDaSala) {
                iniciarReconexao();
            }
        };

        stomp.connect({}, function () {
            stomp.subscribe("/topic/room." + roomId, onMensagem);
            tentativasReconexao = 0;
            reconectando = false;
            definirStatus("conectado", "status-conectado");
        }, function () {
            definirStatus("falha de conexão", "status-erro");
        });
    }

    function iniciarReconexao() {
        if (reconectando) {
            return;
        }
        reconectando = true;
        tentativasReconexao = 0;
        tentarReconexao();
    }

    function tentarReconexao() {
        if (currentRoomId === null || saindoDaSala) {
            return;
        }
        if (tentativasReconexao >= MAX_TENTATIVAS_RECONEXAO) {
            reconectando = false;
            definirStatus("falha de conexão", "status-erro");
            return;
        }

        tentativasReconexao++;
        definirStatus("reconectando... (" + tentativasReconexao + "/" + MAX_TENTATIVAS_RECONEXAO + ")", "status-reconectando");

        var novoSock = new SockJS("/ws");
        var novoStomp = Stomp.over(novoSock);
        sock = novoSock;
        stomp = novoStomp;

        novoSock.onclose = function () {
            if (currentRoomId !== null && !saindoDaSala) {
                setTimeout(tentarReconexao, INTERVALO_RECONEXAO);
            }
        };

        novoStomp.connect({}, function () {
            novoStomp.subscribe("/topic/room." + currentRoomId, onMensagem);
            tentativasReconexao = 0;
            reconectando = false;
            definirStatus("conectado", "status-conectado");
        }, function () {
            definirStatus("reconectando... (" + tentativasReconexao + "/" + MAX_TENTATIVAS_RECONEXAO + ")", "status-reconectando");
        });
    }

    function entrarSala(roomId) {
        currentRoomId = roomId;
        saindoDaSala = false;
        reconectando = false;
        tentativasReconexao = 0;

        tituloSala.textContent = "Período " + roomId;
        definirStatus("conectando...", "status-reconectando");
        divMensagens.innerHTML = "";

        telaSalas.hidden = true;
        telaChat.hidden = false;

        conectarSala(roomId);
    }

    function sairDaSala() {
        saindoDaSala = true;
        reconectando = false;
        tentativasReconexao = 0;

        if (stomp) {
            try {
                stomp.disconnect(function () {});
            } catch (e) {
                // ignora erros ao desconectar
            }
        }
        if (sock) {
            try {
                sock.close();
            } catch (e) {
                // ignora erros ao fechar o socket
            }
        }

        stomp = null;
        sock = null;
        currentRoomId = null;

        divMensagens.innerHTML = "";
        inputMensagem.value = "";

        telaChat.hidden = true;
        telaSalas.hidden = false;
    }

    function sendMessage() {
        var texto = inputMensagem.value.trim();
        if (!texto || !stomp || !stomp.connected) {
            return;
        }
        stomp.send("/app/chat.send", {}, JSON.stringify({ roomId: currentRoomId, text: texto }));
        inputMensagem.value = "";
        inputMensagem.focus();
    }

    function renderizarSalas(salas) {
        divSalas.innerHTML = "";
        salas.forEach(function (sala) {
            var card = document.createElement("div");
            card.className = "sala-card";

            var titulo = document.createElement("h3");
            titulo.textContent = sala.name || ("Período " + sala.id);

            card.appendChild(titulo);
            card.addEventListener("click", function () {
                entrarSala(sala.id);
            });
            divSalas.appendChild(card);
        });
    }

    function init() {
        me().then(function (usuario) {
            currentUserName = usuario.username;
            document.getElementById("usuario-nome").textContent = usuario.name;
            var badge = document.getElementById("usuario-periodo");
            badge.textContent = "Período " + usuario.periodo;
            badge.hidden = false;
            return rooms();
        }).then(function (salas) {
            renderizarSalas(salas);
        }).catch(function (e) {
            // 401 já redireciona via api.js; demais erros são exibidos
            definirStatus(e.message, "status-erro");
        });
    }

    btnEnviar.addEventListener("click", sendMessage);
    btnSairSala.addEventListener("click", sairDaSala);
    btnSair.addEventListener("click", function () {
        logout().catch(function () {}).then(function () {
            window.location.href = "/index.html";
        });
    });

    window.addEventListener("beforeunload", function () {
        try {
            if (stomp) {
                stomp.disconnect(function () {});
            }
        } catch (e) {
            // ignora erros ao desconectar
        }
    });

    init();
})();