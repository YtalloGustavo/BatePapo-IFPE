package br.edu.ifpe.batepapo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.util.concurrent.ListenableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Real WebSocket integration test: registers + logs in over HTTP, then connects a
 * STOMP-over-SockJS client carrying the JSESSIONID cookie and asserts the chat
 * broadcast contract on /topic/room.{roomId}.
 *
 * <p>Connect approach that works: SockJsClient (WebSocketTransport over
 * StandardWebSocketClient) + WebSocketStompClient, connecting to the SockJS base
 * URL with the <b>http://</b> scheme ({@code http://localhost:{port}/ws}) — the
 * SockJS client normalizes it into /info + /websocket handshake requests. The
 * JSESSIONID cookie is carried in the WebSocketHttpHeaders handshake headers so
 * the server resolves the authenticated Principal for the STOMP session.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	private static final AtomicInteger USER_COUNTER = new AtomicInteger(2000);

	private WebSocketStompClient stompClient;
	private SockJsClient sockJsClient;
	private StompSession stompSession;

	@AfterEach
	void tearDown() {
		if (stompSession != null && stompSession.isConnected()) {
			stompSession.disconnect();
		}
		if (sockJsClient != null) {
			sockJsClient.stop();
		}
		if (stompClient != null) {
			stompClient.stop();
		}
	}

	/**
	 * Registers a fresh user and logs in, returning the JSESSIONID cookie value.
	 */
	private String registerAndLogin() {
		String username = "wsaluno" + USER_COUNTER.incrementAndGet();
		String password = "segredo123";

		Map<String, Object> register = new HashMap<>();
		register.put("username", username);
		register.put("password", password);
		register.put("name", "Aluno WebSocket");
		register.put("periodo", 3);

		ResponseEntity<String> registerResponse = restTemplate.postForEntity("/api/register", register, String.class);
		assertEquals(201, registerResponse.getStatusCodeValue(), "register must return 201");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> login = new HashMap<>();
		login.put("username", username);
		login.put("password", password);

		ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/login",
				new HttpEntity<>(login, headers), String.class);
		assertEquals(200, loginResponse.getStatusCodeValue(), "login must return 200");

		String cookie = extractJSessionId(loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE));
		assertNotNull(cookie, "login must set a JSESSIONID cookie");
		return cookie;
	}

	private String extractJSessionId(List<String> setCookieHeaders) {
		if (setCookieHeaders == null) {
			return null;
		}
		for (String header : setCookieHeaders) {
			for (String part : header.split(";")) {
				String trimmed = part.trim();
				if (trimmed.startsWith("JSESSIONID=")) {
					return trimmed.substring("JSESSIONID=".length());
				}
			}
		}
		return null;
	}

	/**
	 * Opens a STOMP-over-SockJS session authenticated with the given JSESSIONID.
	 */
	private StompSession connectStomp(String jsessionId) throws Exception {
		sockJsClient = new SockJsClient(
				Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient())));
		stompClient = new WebSocketStompClient(sockJsClient);
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());

		WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
		handshakeHeaders.add("Cookie", "JSESSIONID=" + jsessionId);

		ListenableFuture<StompSession> future = stompClient.connect(
				"http://localhost:" + port + "/ws",
				handshakeHeaders,
				new StompHeaders(),
				new StompSessionHandlerAdapter() {
					@Override
					public void handleTransportError(StompSession session, Throwable exception) {
						// surfaced through the future / poll timeouts
					}
				});

		stompSession = future.get(10, TimeUnit.SECONDS);
		assertNotNull(stompSession, "STOMP session must be established");
		assertTrue(stompSession.isConnected(), "STOMP session must be connected");
		return stompSession;
	}

	private BlockingQueue<JsonNode> subscribeToRoom(StompSession session, String roomTopic) {
		BlockingQueue<JsonNode> frames = new LinkedBlockingQueue<>();
		session.subscribe(roomTopic, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				return JsonNode.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				frames.add((JsonNode) payload);
			}
		});
		return frames;
	}

	private void sendChatMessage(StompSession session, int roomId, String text) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("roomId", roomId);
		payload.put("text", text);
		session.send("/app/chat.send", payload);
	}

	@Test
	void authenticatedUserSendsMessageAndReceivesBroadcast() throws Exception {
		String jsessionId = registerAndLogin();
		StompSession session = connectStomp(jsessionId);
		BlockingQueue<JsonNode> room1Frames = subscribeToRoom(session, "/topic/room.1");

		sendChatMessage(session, 1, "ola turma");

		JsonNode message = room1Frames.poll(5, TimeUnit.SECONDS);
		assertNotNull(message, "no broadcast received on /topic/room.1 within 5s");
		assertEquals("CHAT", message.get("type").asText());
		assertEquals("Aluno WebSocket", message.get("sender").asText());
		assertEquals(3, message.get("senderPeriodo").asInt());
		assertEquals(1, message.get("roomId").asInt());
		assertEquals("ola turma", message.get("text").asText());
		assertFalse(message.get("timestamp").asText().trim().isEmpty(), "timestamp must not be blank");
	}

	@Test
	void messagesAreIsolatedPerRoom() throws Exception {
		String jsessionId = registerAndLogin();
		StompSession session = connectStomp(jsessionId);
		BlockingQueue<JsonNode> room1Frames = subscribeToRoom(session, "/topic/room.1");

		sendChatMessage(session, 2, "mensagem para outra sala");

		JsonNode unexpected = room1Frames.poll(1500, TimeUnit.MILLISECONDS);
		assertNull(unexpected, "message sent to room 2 must not arrive on /topic/room.1");

		sendChatMessage(session, 1, "segunda mensagem");

		JsonNode expected = room1Frames.poll(5, TimeUnit.SECONDS);
		assertNotNull(expected, "no broadcast received on /topic/room.1 within 5s");
		assertEquals(1, expected.get("roomId").asInt());
		assertEquals("segunda mensagem", expected.get("text").asText());
	}
}