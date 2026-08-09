# BatePapo IFPE 💬

Sistema de bate-papo em tempo real para os 6 períodos do IFPE, desenvolvido como projeto da disciplina de Web 3.

**Tecnologias:** Spring Boot 2.7.18 (REST) · Spring Security 5.7 · WebSocket (STOMP + SockJS) · Spring Data JPA · H2 (embutido) · HTML5 + JavaScript puro

## Funcionalidades

- Cadastro e login de estudantes (senha criptografada com BCrypt, sessão HTTP)
- 6 salas de bate-papo: **Período 1** a **Período 6**
- Mensagens em tempo real: tudo que um estudante envia na sala é recebido por todos os conectados na mesma sala
- Isolamento entre salas (mensagens do Período 1 não chegam ao Período 3)
- **Sem histórico:** ao sair da sala, todo o conteúdo da conversa é descartado
- Front-end servido pelo próprio Spring Boot (dos arquivos estáticos) — um único deploy

## Como rodar

Pré-requisitos: **JDK 11**

```bash
# no diretório raiz do projeto
.\mvnw.cmd spring-boot:run
```

Abra <http://localhost:8080> no navegador.

> Dica: para testar a troca de mensagens, abra duas janelas (uma normal e uma anônima/InPrivate) e registre dois estudantes.

### Banco de dados

- **H2 em modo arquivo** em `./data/batepapo` (os cadastros sobrevivem a reinícios)
- Console web: <http://localhost:8080/h2-console>
  - JDBC URL: `jdbc:h2:file:./data/batepapo`
  - Usuário: `sa` · Senha: *(vazia)*

### Testes

```bash
.\mvnw.cmd test
```

12 testes: fluxo de autenticação (MockMvc) + integração real de WebSocket (conexão SockJS/STOMP autenticada, broadcast e isolamento de salas).

## API REST

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/register` | Cadastro `{username, password, name, periodo}` → 201 / 400 / 409 |
| POST | `/api/login` | Login `{username, password}` → 200 (sessão) / 401 |
| GET | `/api/me` | Usuário logado (requer sessão) |
| GET | `/api/rooms` | Lista as 6 salas (requer sessão) |
| POST | `/api/logout` | Encerra a sessão → 204 |

## WebSocket

- Endpoint SockJS: `/ws`
- Enviar mensagem: `/app/chat.send` → `{"roomId": 3, "text": "Olá, turma!"}`
- Receber (inscrição): `/topic/room.{id}` → `{"type":"CHAT","sender":"Maria Silva","senderPeriodo":3,"roomId":3,"text":"Olá, turma!","timestamp":"..."}`

A identidade do remetente é sempre derivada do usuário autenticado no servidor (nunca do payload do cliente).

## Estrutura

```
src/main/java/br/edu/ifpe/batepapo/
├── config/        SecurityConfig · WebSocketConfig
├── controller/    AuthController · RoomController · ChatController
├── dto/           request/response DTOs
├── entity/        Student · Role
├── repository/    StudentRepository
├── security/      UserDetailsServiceImpl
└── service/       RoomService
src/main/resources/static/   index.html · chat.html · css · js (front-end)
```

## Script de demonstração (vídeo)

1. Abrir o app em duas janelas do navegador
2. Registrar o estudante A (Período 3) e o estudante B (Período 3)
3. Ambos entram na sala "Período 3": A envia "Olá, turma!" e a mensagem aparece para B (e vice-versa)
4. Mostrar que um usuário do Período 1 não recebe as mensagens do Período 3
5. Sair da sala e voltar: a conversa foi descartada (sem histórico)
6. Abrir o `/h2-console` e mostrar a tabela `student` com as senhas em hash BCrypt