package pl.umcs.auth.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.umcs.auth.AuthSettings;
import pl.umcs.auth.provider.GoogleTokenBuilder;
import pl.umcs.worksheet.WorksheetApi;
import pl.umcs.worksheet.WorksheetApiSettings;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.umcs.auth.provider.GoogleTokenBuilder.ANOTHER_ENVIRONMENT_CLIENT;
import static pl.umcs.auth.provider.GoogleTokenBuilder.KEY_ID;
import static pl.umcs.auth.provider.GoogleTokenBuilder.THIS_ENVIRONMENT_CLIENT;
import static pl.umcs.auth.provider.GoogleTokenBuilder.aGoogleToken;

class AuthHttpTest {

	private static final String LOCAL_DATABASE_URL = "postgresql://worksheets:worksheets@127.0.0.1:5446/worksheets";
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final String TEST_PASSWORD = "a-long-random-test-password";
	private static final String TEST_EMAIL = "factory-e2e@currency.vtehsol.com";

	private static final KeyPair GOOGLE_KEY = GoogleTokenBuilder.newSigningKey();
	private static final AtomicReference<java.util.function.Function<String, String>> TOKEN_FOR_NONCE = new AtomicReference<>();
	private static final AtomicReference<String> LAST_VERIFIER = new AtomicReference<>();

	private static WorksheetApi api;
	private static WorksheetApi apiWithoutTestDoor;
	private static Path webExport;
	private static HttpClient http;
	private static String base;
	private static String baseWithoutTestDoor;

	@BeforeAll
	static void startTwoApisAgainstARealDatabase() throws IOException {
		String databaseUrl = Optional.ofNullable(System.getenv("DATABASE_URL")).filter(url -> !url.isBlank()).orElse(LOCAL_DATABASE_URL);
		webExport = Files.createTempDirectory("web-export");
		Files.writeString(webExport.resolve("sign-in.html"), "<html>sign in door</html>");
		WorksheetApi.GoogleEndpoints google = new WorksheetApi.GoogleEndpoints(
				() -> new pl.umcs.auth.provider.JwkSet(Map.of(KEY_ID, GOOGLE_KEY.getPublic()), Duration.ofHours(1)),
				(code, verifier, redirectUri) -> {
					LAST_VERIFIER.set(verifier);
					return TOKEN_FOR_NONCE.get().apply(code);
				});
		AuthSettings withTestDoor = new AuthSettings(Set.of(THIS_ENVIRONMENT_CLIENT), "secret", "https://dev.example",
				Optional.of(new AuthSettings.TestDoor(TEST_EMAIL, TEST_PASSWORD)));
		AuthSettings production = new AuthSettings(Set.of(THIS_ENVIRONMENT_CLIENT), "secret", "https://prod.example", Optional.empty());
		api = WorksheetApi.start(new WorksheetApiSettings(databaseUrl, 0, "auth-test", webExport, withTestDoor), google, Clock.systemUTC());
		apiWithoutTestDoor = WorksheetApi.start(new WorksheetApiSettings(databaseUrl, 0, "auth-test", webExport, production), google, Clock.systemUTC());
		http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
		base = "http://127.0.0.1:" + api.port();
		baseWithoutTestDoor = "http://127.0.0.1:" + apiWithoutTestDoor.port();
	}

	@AfterAll
	static void stopTheApis() {
		api.close();
		apiWithoutTestDoor.close();
	}

	private record Reply(int status, HttpResponse<String> raw) {
		String location() {
			return raw.headers().firstValue("Location").orElse("");
		}

		List<String> setCookies() {
			return raw.headers().allValues("Set-Cookie");
		}

		String setCookie(String name) {
			return setCookies().stream().filter(cookie -> cookie.startsWith(name + "=")).findFirst().orElseThrow();
		}

		JsonNode json() throws IOException {
			return JSON.readTree(raw.body());
		}
	}

	private static Reply get(String url, String cookie) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).GET();
		if (cookie != null) {
			request.header("Cookie", cookie);
		}
		HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
		return new Reply(response.statusCode(), response);
	}

	private static Reply post(String url, String json) throws Exception {
		HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(url))
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
		return new Reply(response.statusCode(), response);
	}

	private static String cookieValue(String setCookie) {
		return setCookie.substring(0, setCookie.indexOf(';'));
	}

	private static Map<String, String> queryOf(String location) {
		String query = URI.create(location).getRawQuery();
		return Arrays.stream(query.split("&")).map(pair -> pair.split("=", 2))
				.collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
	}

	private record Started(String binder, Map<String, String> authorization) {
	}

	private static Started startSignIn(String next) throws Exception {
		Reply started = get(base + "/auth/google/start" + (next == null ? "" : "?next=" + URLDecoder.decode(next, StandardCharsets.UTF_8).replace("?", "%3F")), null);
		assertEquals(302, started.status());
		return new Started(cookieValue(started.setCookie(AuthHttp.BINDER_COOKIE)), queryOf(started.location()));
	}

	private static String uniqueEmail() {
		return "member-" + UUID.randomUUID() + "@example.com";
	}

	private static void googleWillAnswer(String subject, String email, boolean verified, String clientId) {
		TOKEN_FOR_NONCE.set(code -> null);
		pendingToken = (nonce) -> aGoogleToken(GOOGLE_KEY).forSubject(subject).withEmail(email, verified).forClient(clientId).withNonce(nonce).build();
	}

	private static java.util.function.Function<String, String> pendingToken;

	private static Reply callback(Started started, String cookie, String stateReturned) throws Exception {
		String nonce = started.authorization().get("nonce");
		TOKEN_FOR_NONCE.set(code -> pendingToken.apply(nonce));
		return get(base + "/auth/google/callback?code=abc&state=" + stateReturned, cookie);
	}

	@Test
	void the_start_redirect_uses_pkce_s256_and_binds_the_browser_with_a_lax_host_cookie() throws Exception {
		Reply started = get(base + "/auth/google/start", null);
		Map<String, String> query = queryOf(started.location());
		String binder = started.setCookie(AuthHttp.BINDER_COOKIE);

		assertEquals(302, started.status());
		assertTrue(started.location().startsWith("https://accounts.google.com/o/oauth2/v2/auth?"));
		assertEquals("S256", query.get("code_challenge_method"));
		assertEquals(THIS_ENVIRONMENT_CLIENT, query.get("client_id"));
		assertEquals("https://dev.example/auth/google/callback", query.get("redirect_uri"));
		assertEquals("openid email", query.get("scope"));
		assertTrue(binder.contains("HttpOnly") && binder.contains("Secure") && binder.contains("SameSite=Lax"), binder);
		assertTrue(binder.contains("Max-Age=600") && binder.contains("Path=/") && !binder.contains("Domain"), binder);
		assertFalse(binder.contains("Strict"));
	}

	@Test
	void a_sign_in_creates_a_member_and_issues_a_fresh_host_cookie_session() throws Exception {
		String email = uniqueEmail();
		googleWillAnswer("sub-" + UUID.randomUUID(), email, true, THIS_ENVIRONMENT_CLIENT);
		Started first = startSignIn("/rates");

		Reply back = callback(first, first.binder(), first.authorization().get("state"));
		String session = back.setCookie(AuthHttp.SESSION_COOKIE);

		assertEquals(302, back.status());
		assertEquals("/rates", back.location());
		assertTrue(session.startsWith("__Host-session=") && session.contains("HttpOnly") && session.contains("Secure") && session.contains("Path=/"), session);
		assertFalse(session.contains("Domain"));
		assertTrue(back.setCookie(AuthHttp.BINDER_COOKIE).contains("Max-Age=0"), "the binder is read once");
		assertEquals(LAST_VERIFIER.get().length() >= 43, true);
	}

	@Test
	void every_sign_in_gets_a_session_id_nobody_held_before() throws Exception {
		String subject = "sub-" + UUID.randomUUID();
		googleWillAnswer(subject, uniqueEmail(), true, THIS_ENVIRONMENT_CLIENT);
		Started one = startSignIn(null);
		Started two = startSignIn(null);

		String sessionOne = cookieValue(callback(one, one.binder(), one.authorization().get("state")).setCookie(AuthHttp.SESSION_COOKIE));
		String sessionTwo = cookieValue(callback(two, two.binder(), two.authorization().get("state")).setCookie(AuthHttp.SESSION_COOKIE));

		assertNotEquals(sessionOne, sessionTwo);
	}

	@Test
	void state_mismatch_is_refused() throws Exception {
		googleWillAnswer("sub-" + UUID.randomUUID(), uniqueEmail(), true, THIS_ENVIRONMENT_CLIENT);
		Started started = startSignIn(null);

		Reply back = callback(started, started.binder(), "forged-state");

		assertEquals("/sign-in?refused=state-mismatch&door=google", back.location());
		assertTrue(back.setCookies().stream().noneMatch(cookie -> cookie.startsWith(AuthHttp.SESSION_COOKIE)));
	}

	@Test
	void a_sign_in_returned_to_a_different_browser_is_refused() throws Exception {
		googleWillAnswer("sub-" + UUID.randomUUID(), uniqueEmail(), true, THIS_ENVIRONMENT_CLIENT);
		Started started = startSignIn(null);

		Reply otherBrowser = callback(started, null, started.authorization().get("state"));
		Reply browserWithAnotherBinder = callback(started, startSignIn(null).binder(), started.authorization().get("state"));

		assertEquals("/sign-in?refused=state-mismatch&door=google", otherBrowser.location());
		assertEquals("/sign-in?refused=state-mismatch&door=google", browserWithAnotherBinder.location());
	}

	@Test
	void a_binder_older_than_ten_minutes_is_refused() throws Exception {
		googleWillAnswer("sub-" + UUID.randomUUID(), uniqueEmail(), true, THIS_ENVIRONMENT_CLIENT);
		Started started = startSignIn(null);
		String payload = started.binder().substring(started.binder().indexOf('=') + 1);
		String aged = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8).replaceFirst("\"issuedAt\":\\d+", "\"issuedAt\":1000");
		String agedCookie = AuthHttp.BINDER_COOKIE + "=" + Base64.getUrlEncoder().withoutPadding().encodeToString(aged.getBytes(StandardCharsets.UTF_8));

		Reply back = callback(started, agedCookie, started.authorization().get("state"));

		assertEquals("/sign-in?refused=state-mismatch&door=google", back.location());
	}

	@Test
	void the_next_path_must_start_with_one_slash() throws Exception {
		for (String next : List.of("//evil.example", "/%5Cevil.example", "https://evil.example", "rates")) {
			Reply started = get(base + "/auth/google/start?next=" + next, null);
			assertEquals(400, started.status(), next);
			assertEquals(0, started.setCookies().size(), next);
		}
	}

	@Test
	void a_tampered_next_path_in_the_binder_is_refused_at_the_callback() throws Exception {
		googleWillAnswer("sub-" + UUID.randomUUID(), uniqueEmail(), true, THIS_ENVIRONMENT_CLIENT);
		Started started = startSignIn(null);
		String payload = started.binder().substring(started.binder().indexOf('=') + 1);
		String tampered = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8).replace("\"next\":\"/\"", "\"next\":\"//evil.example\"");
		String tamperedCookie = AuthHttp.BINDER_COOKIE + "=" + Base64.getUrlEncoder().withoutPadding().encodeToString(tampered.getBytes(StandardCharsets.UTF_8));

		Reply back = callback(started, tamperedCookie, started.authorization().get("state"));

		assertEquals("/sign-in?refused=state-mismatch&door=google", back.location());
	}

	@Test
	void a_cancelled_consent_returns_a_closed_code() throws Exception {
		Started started = startSignIn(null);

		Reply back = get(base + "/auth/google/callback?error=access_denied&state=" + started.authorization().get("state"), started.binder());

		assertEquals("/sign-in?refused=cancelled&door=google", back.location());
	}

	@Test
	void an_unverified_google_address_returns_a_closed_code_without_the_address() throws Exception {
		String email = uniqueEmail();
		googleWillAnswer("sub-" + UUID.randomUUID(), email, false, THIS_ENVIRONMENT_CLIENT);
		Started started = startSignIn(null);

		Reply back = callback(started, started.binder(), started.authorization().get("state"));

		assertEquals("/sign-in?refused=email-unverified&door=google", back.location());
		assertFalse(back.location().contains("example.com"));
	}

	@Test
	void a_token_whose_aud_names_another_environment_s_client_is_refused_on_the_web_door() throws Exception {
		googleWillAnswer("sub-" + UUID.randomUUID(), uniqueEmail(), true, ANOTHER_ENVIRONMENT_CLIENT);
		Started started = startSignIn(null);

		Reply back = callback(started, started.binder(), started.authorization().get("state"));

		assertEquals("/sign-in?refused=token-for-another-client&door=google", back.location());
	}

	private static String freshNonce() throws Exception {
		Reply issued = post(base + "/v1/sessions/nonces", "{}");
		assertEquals(201, issued.status());
		return issued.json().path("nonce").asText();
	}

	private static String sessionBody(String nonce, String assertion) {
		return JSON.createObjectNode().put("nonce", nonce).put("assertion", assertion).toString();
	}

	@Test
	void post_v1_sessions_uses_a_server_issued_nonce_once() throws Exception {
		String nonce = freshNonce();
		String token = aGoogleToken(GOOGLE_KEY).forSubject("sub-" + UUID.randomUUID()).withEmail(uniqueEmail(), true).withNonce(nonce).build();

		Reply first = post(base + "/v1/sessions", sessionBody(nonce, token));
		Reply replay = post(base + "/v1/sessions", sessionBody(nonce, token));

		assertEquals(201, first.status());
		assertTrue(first.json().path("sessionId").asText().matches("[0-9a-f-]{36}"));
		assertTrue(first.setCookie(AuthHttp.SESSION_COOKIE).contains("Secure"));
		assertEquals(400, replay.status());
		assertEquals("state-mismatch", replay.json().path("error").path("code").asText());
	}

	@Test
	void post_v1_sessions_refuses_a_nonce_the_server_never_issued() throws Exception {
		String token = aGoogleToken(GOOGLE_KEY).withNonce("invented").build();

		Reply reply = post(base + "/v1/sessions", sessionBody("invented", token));

		assertEquals(400, reply.status());
	}

	@Test
	void post_v1_sessions_refuses_a_token_bound_to_another_nonce() throws Exception {
		String token = aGoogleToken(GOOGLE_KEY).withNonce("not-the-issued-one").build();

		Reply reply = post(base + "/v1/sessions", sessionBody(freshNonce(), token));

		assertEquals(400, reply.status());
	}

	@Test
	void post_v1_sessions_refuses_a_google_token_minted_for_another_client() throws Exception {
		String nonce = freshNonce();
		String token = aGoogleToken(GOOGLE_KEY).forClient(ANOTHER_ENVIRONMENT_CLIENT).withNonce(nonce).build();

		Reply reply = post(base + "/v1/sessions", sessionBody(nonce, token));

		assertEquals(403, reply.status());
		assertEquals("token-for-another-client", reply.json().path("error").path("code").asText());
	}

	@Test
	void post_v1_sessions_refuses_a_token_signed_by_a_key_google_does_not_publish() throws Exception {
		String nonce = freshNonce();
		String forged = aGoogleToken(GOOGLE_KEY).withNonce(nonce).buildSignedBy(GoogleTokenBuilder.newSigningKey());

		assertEquals(401, post(base + "/v1/sessions", sessionBody(nonce, forged)).status());
	}

	@Test
	void post_v1_sessions_refuses_an_incomplete_request() throws Exception {
		assertEquals(400, post(base + "/v1/sessions", "{\"nonce\":\"x\"}").status());
		assertEquals(400, post(base + "/v1/sessions", "not json").status());
	}

	@Test
	void the_dev_test_door_signs_in_only_where_the_dev_flag_is_set() throws Exception {
		String body = JSON.createObjectNode().put("email", TEST_EMAIL).put("password", TEST_PASSWORD).toString();

		Reply withFlag = post(base + "/auth/test-door", body);
		Reply production = post(baseWithoutTestDoor + "/auth/test-door", body);

		assertEquals(204, withFlag.status());
		assertTrue(withFlag.setCookie(AuthHttp.SESSION_COOKIE).contains("HttpOnly"));
		assertEquals(404, production.status());
		assertTrue(production.setCookies().isEmpty());
	}

	@Test
	void the_dev_test_door_refuses_a_wrong_password() throws Exception {
		Reply reply = post(base + "/auth/test-door", JSON.createObjectNode().put("email", TEST_EMAIL).put("password", "wrong").toString());

		assertEquals(403, reply.status());
		assertTrue(reply.setCookies().isEmpty());
	}

	@Test
	void a_signed_in_visitor_leaves_the_sign_in_page_for_the_next_path() throws Exception {
		String body = JSON.createObjectNode().put("email", TEST_EMAIL).put("password", TEST_PASSWORD).toString();
		String session = cookieValue(post(base + "/auth/test-door", body).setCookie(AuthHttp.SESSION_COOKIE));

		Reply signedIn = get(base + "/sign-in?next=/rates", session);
		Reply forgedSession = get(base + "/sign-in", AuthHttp.SESSION_COOKIE + "=" + UUID.randomUUID());
		Reply anonymous = get(base + "/sign-in", null);

		assertEquals(302, signedIn.status());
		assertEquals("/rates", signedIn.location());
		assertEquals(200, forgedSession.status());
		assertEquals(200, anonymous.status());
	}

	@Test
	void without_a_configured_client_the_door_answers_provider_unavailable() throws Exception {
		WorksheetApi unconfigured = WorksheetApi.start(new WorksheetApiSettings(
				Optional.ofNullable(System.getenv("DATABASE_URL")).filter(url -> !url.isBlank()).orElse(LOCAL_DATABASE_URL), 0, "auth-test", webExport));
		try {
			Reply started = get("http://127.0.0.1:" + unconfigured.port() + "/auth/google/start", null);

			assertEquals("/sign-in?refused=provider-unavailable&door=google", started.location());
		} finally {
			unconfigured.close();
		}
	}
}
