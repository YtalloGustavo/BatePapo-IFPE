package br.edu.ifpe.batepapo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HTTP-level tests for the auth contract: register / login / me / rooms / logout.
 *
 * <p>The H2 database is shared across the whole test class (no rollback), so every
 * test registers a unique username instead of clearing the DB between tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static final AtomicInteger USER_COUNTER = new AtomicInteger(1000);

	private String uniqueUsername() {
		return "aluno" + USER_COUNTER.incrementAndGet();
	}

	private Map<String, Object> validRegisterPayload(String username) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("username", username);
		payload.put("password", "segredo123");
		payload.put("name", "Aluno Teste");
		payload.put("periodo", 3);
		return payload;
	}

	private Map<String, Object> loginPayload(String username, String password) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("username", username);
		payload.put("password", password);
		return payload;
	}

	private String asJson(Object payload) throws Exception {
		return objectMapper.writeValueAsString(payload);
	}

	private void register(String username) throws Exception {
		mockMvc.perform(post("/api/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJson(validRegisterPayload(username))))
				.andExpect(status().isCreated())
				.andReturn();
	}

	private MockHttpSession loginAndGetSession(String username, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJson(loginPayload(username, password))))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertNotNull(session, "login must create a session");
		return session;
	}

	@Test
	void registerReturns201WithCorrectJsonFields() throws Exception {
		String username = uniqueUsername();
		mockMvc.perform(post("/api/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJson(validRegisterPayload(username))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.name").value("Aluno Teste"))
				.andExpect(jsonPath("$.periodo").value(3))
				.andExpect(jsonPath("$.role").value("ROLE_USER"));
	}

	@Test
	void registerCreatesSession() throws Exception {
		String username = uniqueUsername();
		MvcResult result = mockMvc.perform(post("/api/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJson(validRegisterPayload(username))))
				.andExpect(status().isCreated())
				.andReturn();
		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
		assertNotNull(session, "register must create a session");
	}

	@Test
	void duplicateUsernameReturns409() throws Exception {
		String username = uniqueUsername();
		register(username);
		mockMvc.perform(post("/api/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJson(validRegisterPayload(username))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Username j\u00e1 cadastrado"));
	}

	@Test
	void invalidRegisterPayloadsReturn400() throws Exception {
		Map<String, Object> base = validRegisterPayload(uniqueUsername());

		Map<String, Object> blankUsername = new HashMap<>(base);
		blankUsername.put("username", "   ");

		Map<String, Object> shortUsername = new HashMap<>(base);
		shortUsername.put("username", "ab");

		Map<String, Object> shortPassword = new HashMap<>(base);
		shortPassword.put("password", "123");

		Map<String, Object> periodoZero = new HashMap<>(base);
		periodoZero.put("periodo", 0);

		Map<String, Object> periodoSete = new HashMap<>(base);
		periodoSete.put("periodo", 7);

		@SuppressWarnings("unchecked")
		Map<String, Object>[] invalidPayloads = new Map[] {
				blankUsername, shortUsername, shortPassword, periodoZero, periodoSete
		};

		for (Map<String, Object> payload : invalidPayloads) {
			mockMvc.perform(post("/api/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(asJson(payload)))
					.andExpect(status().isBadRequest());
		}
	}

	@Test
	void loginWithWrongPasswordReturns401() throws Exception {
		String username = uniqueUsername();
		register(username);
		mockMvc.perform(post("/api/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJson(loginPayload(username, "senha-errada"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Credenciais inv\u00e1lidas"));
	}

	@Test
	void loginReturns200WithSession() throws Exception {
		String username = uniqueUsername();
		register(username);
		loginAndGetSession(username, "segredo123");
	}

	@Test
	void meWithoutSessionReturns401() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void meWithSessionReturns200WithUsername() throws Exception {
		String username = uniqueUsername();
		register(username);
		MockHttpSession session = loginAndGetSession(username, "segredo123");
		mockMvc.perform(get("/api/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value(username))
				.andExpect(jsonPath("$.name").value("Aluno Teste"))
				.andExpect(jsonPath("$.role").value("ROLE_USER"));
	}

	@Test
	void roomsWithSessionReturnsSixRooms() throws Exception {
		String username = uniqueUsername();
		register(username);
		MockHttpSession session = loginAndGetSession(username, "segredo123");
		mockMvc.perform(get("/api/rooms").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(6))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Per\u00edodo 1"))
				.andExpect(jsonPath("$[5].id").value(6))
				.andExpect(jsonPath("$[5].name").value("Per\u00edodo 6"));
	}

	@Test
	void logoutInvalidatesSession() throws Exception {
		String username = uniqueUsername();
		register(username);
		MockHttpSession session = loginAndGetSession(username, "segredo123");

		mockMvc.perform(post("/api/logout").session(session))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/me").session(session))
				.andExpect(status().isUnauthorized());
	}
}