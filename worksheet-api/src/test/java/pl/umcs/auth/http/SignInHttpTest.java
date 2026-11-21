package pl.umcs.auth.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.umcs.auth.google.FakeGoogle;
import pl.umcs.worksheet.WorksheetApi;
import pl.umcs.worksheet.WorksheetApiSettings;
import pl.umcs.worksheet.persistence.WorksheetDatabase;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignInHttpTest {

	private static final String LOCAL_DATABASE_URL = "postgresql://worksheets:worksheets@127.0.0.1:5446/worksheets";
	private static final String THIS_CLIENT = "dev-client.apps.googleusercontent.com";
	private static final String TEST_EMAIL = "factory-e2e@currency.vtehsol.com";
	private static final String TEST_PASSWORD = "dev-only-secret";
	private static final ObjectMapper JSON = new ObjectMapper();

	private static FakeGoogle google;
	private static WorksheetApi api;
	private static WorksheetApi apiWithoutTestDoor;
	private static WorksheetApi apiWithoutGoogle;
	private static HikariDataSource database;
	private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

	record Reply(int status, String body, HttpResponse<String> raw) {
		String location() {
			return raw.headers().firstValue("Location").orElse("");
		}

		List<String> setCookies() {
			return raw.headers().allValues("Set-Cookie");
		}

		Optional<String> setCookie(String name) {
			return setCookies().stream().filter(cookie -> cookie.startsWith(name + "=")).findFirst();
		}

		String cookieValue(String name) {
			String header = setCookie(name).orElseThrow();
			return header.substring(name.length() + 1, header.indexOf(';'));
		}

		JsonNode json() throws IOException {
			return JSON.readTree(body);
		}
	}

	@BeforeAll
	static void startTheApiAgainstFakeGoogle() {
		google = new FakeGoogle();
		String databaseUrl = Optional.ofNullable(System.getenv("DATABASE_URL")).filter(url -> !url.isBlank()).orElse(LOCAL_DATABASE_URL);
		api = WorksheetApi.start(settings(databaseUrl, TEST_EMAIL, TEST_PASSWORD));
		apiWithoutTestDoor = WorksheetApi.start(settings(databaseUrl, null, null));
		apiWithoutGoogle = WorksheetApi.start(new WorksheetApiSettings(databaseUrl, 0, "sign-in-test", Path.of("target", "no-web-export")));
		database = WorksheetDatabase.openMigrated(databaseUrl);
	}

	@AfterAll
	static void stopEverything() {
		api.close();
		apiWithoutTestDoor.close();
		apiWithoutGoogle.close();
		database.close();
		google.close();
	}

	private static WorksheetApiSettings settings(String databaseUrl, String testEmail, String testPassword) {
		var signIn = new WorksheetApiSettings.SignInSettings(Set.of(THIS_CLIENT), "client-secret", "https://currency.example", testEmail, testPassword,
				google.tokenEndpoint(), google.jwksUri());
		return new WorksheetApiSettings(databaseUrl, 0, "sign-in-test", Path.of("target", "no-web-export"), signIn);
	}

	private static Reply post(WorksheetApi target, String path, String contentType, String body, String cookie) {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + target.port() + path))
				.header("Content-Type", contentType).POST(HttpRequest.BodyPublishers.ofString(body));
		if (cookie != null) {
			request.header("Cookie", cookie);
		}
		return send(request);
	}

	private static Reply get(WorksheetApi target, String path, String cookie) {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + target.port() + path)).GET();
		if (cookie != null) {
			request.header("Cookie", cookie);
		}
		return send(request);
	}

	private static Reply send(HttpRequest.Builder request) {
		try {
			HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
			return new Reply(response.statusCode(), response.body(), response);
		} catch (IOException unreachable) {
			throw new java.io.UncheckedIOException(unreachable);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(interrupted);
		}
	}

	private static String form(Map<String, String> fields) {
		return fields.entrySet().stream()
				.map(field -> URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8))
				.collect(Collectors.joining("&"));
	}

	private static Map<String, String> queryOf(String location) {
		return Arrays.stream(URI.create(location).getRawQuery().split("&")).map(pair -> pair.split("=", 2))
				.collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
	}

	private static Reply startAt(String next) {
		return post(api, "/auth/google/start", "application/x-www-form-urlencoded", form(Map.of("next", next)), null);
	}

	private static String uniqueEmail() {
		return "member-" + UUID.randomUUID() + "@example.com";
	}

	private static String idTokenFor(Map<String, String> authorizationQuery, String subject, String email, boolean verified) {
		return google.mint(FakeGoogle.claims(THIS_CLIENT, subject, email, verified, authorizationQuery.get("nonce"), Instant.now().plusSeconds(3600).getEpochSecond()));
	}

	@Test
	void the_door_sends_the_browser_to_google_with_pkce_s256_and_a_host_only_binder_cookie() {
		Reply started = startAt("/rates");

		Map<String, String> query = queryOf(started.location());
		assertEquals(303, started.status());
		assertEquals(THIS_CLIENT, query.get("client_id"));
		assertEquals("S256", query.get("code_challenge_method"));
		assertEquals("https://currency.example/auth/google/callback", query.get("redirect_uri"));
		String binder = started.setCookie("__Host-sign-in").orElseThrow();
		assertTrue(binder.contains("HttpOnly") && binder.contains("Secure") && binder.contains("SameSite=Lax") && binder.contains("Max-Age=600") && binder.contains("Path=/"));
		assertTrue(!binder.contains("Domain"));
	}

	@Test
	void a_full_sign_in_issues_a_fresh_session_id_in_a_host_cookie_and_returns_to_next() {
		Reply started = startAt("/rates");
		Map<String, String> query = queryOf(started.location());
		google.answerNextExchangeWith(idTokenFor(query, "g-" + UUID.randomUUID(), uniqueEmail(), true));

		Reply returned = get(api, "/auth/google/callback?code=abc&state=" + query.get("state"), "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

		assertEquals(303, returned.status());
		assertEquals("/rates", returned.location());
		String session = returned.setCookie("__Host-session").orElseThrow();
		assertTrue(session.contains("HttpOnly") && session.contains("Secure") && session.contains("Path=/") && !session.contains("Domain"));
		assertTrue(returned.setCookie("__Host-sign-in").orElseThrow().contains("Max-Age=0"));
	}

	@Test
	void a_signed_in_visitor_is_sent_on_from_the_sign_in_page() {
		Reply started = startAt("/");
		Map<String, String> query = queryOf(started.location());
		google.answerNextExchangeWith(idTokenFor(query, "g-" + UUID.randomUUID(), uniqueEmail(), true));
		Reply returned = get(api, "/auth/google/callback?code=abc&state=" + query.get("state"), "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

		Reply page = get(api, "/sign-in?next=/rates", "__Host-session=" + returned.cookieValue("__Host-session"));

		assertEquals(303, page.status());
		assertEquals("/rates", page.location());
	}

	@Test
	void state_mismatch_is_refused() {
		Reply started = startAt("/");

		Reply returned = get(api, "/auth/google/callback?code=abc&state=forged", "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

		assertEquals("/sign-in?refused=state-mismatch&door=google", returned.location());
		assertTrue(returned.setCookie("__Host-session").isEmpty());
	}

	@Test
	void a_sign_in_returned_to_a_different_browser_is_refused() {
		Reply started = startAt("/");
		Map<String, String> query = queryOf(started.location());
		google.answerNextExchangeWith(idTokenFor(query, "g-" + UUID.randomUUID(), uniqueEmail(), true));

		Reply otherBrowser = get(api, "/auth/google/callback?code=abc&state=" + query.get("state"), null);

		assertEquals("/sign-in?refused=state-mismatch&door=google", otherBrowser.location());
		assertTrue(otherBrowser.setCookie("__Host-session").isEmpty());
	}

	@Test
	void the_binder_is_read_once() {
		Reply started = startAt("/");
		Map<String, String> query = queryOf(started.location());
		google.answerNextExchangeWith(idTokenFor(query, "g-" + UUID.randomUUID(), uniqueEmail(), true));
		String binderCookie = "__Host-sign-in=" + started.cookieValue("__Host-sign-in");
		Reply first = get(api, "/auth/google/callback?code=abc&state=" + query.get("state"), binderCookie);
		assertTrue(first.setCookie("__Host-session").isPresent());

		assertTrue(first.setCookie("__Host-sign-in").orElseThrow().contains("Max-Age=0"));
	}

	@Test
	void a_google_token_minted_for_another_client_comes_back_as_a_closed_refusal_code() {
		Reply started = startAt("/");
		Map<String, String> query = queryOf(started.location());
		google.answerNextExchangeWith(google.mint(FakeGoogle.claims("prod-client.apps.googleusercontent.com", "g-x", uniqueEmail(), true, query.get("nonce"), Instant.now().plusSeconds(3600).getEpochSecond())));

		Reply returned = get(api, "/auth/google/callback?code=abc&state=" + query.get("state"), "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

		assertEquals("/sign-in?refused=token-for-another-client&door=google", returned.location());
	}

	@Test
	void an_unverified_google_email_is_refused_without_naming_the_email() {
		String email = uniqueEmail();
		Reply started = startAt("/");
		Map<String, String> query = queryOf(started.location());
		google.answerNextExchangeWith(idTokenFor(query, "g-" + UUID.randomUUID(), email, false));

		Reply returned = get(api, "/auth/google/callback?code=abc&state=" + query.get("state"), "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

		assertEquals("/sign-in?refused=email-unverified&door=google", returned.location());
		assertTrue(!returned.location().contains("example.com"));
	}

	@Test
	void cancelling_at_google_returns_the_cancelled_code_and_keeps_next() {
		Reply started = startAt("/rates");

		Reply returned = get(api, "/auth/google/callback?error=access_denied&state=" + queryOf(started.location()).get("state"), "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

		assertEquals("/sign-in?refused=cancelled&door=google&next=%2Frates", returned.location());
	}

	@Test
	void a_google_failure_returns_the_provider_unavailable_code() {
		Reply started = startAt("/");

		Reply returned = get(api, "/auth/google/callback?error=server_error&state=" + queryOf(started.location()).get("state"), "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

		assertEquals("/sign-in?refused=provider-unavailable&door=google", returned.location());
	}

	@Test
	void the_next_path_must_start_with_one_slash() {
		for (String hostile : List.of("//evil.example", "/\\evil.example", "https://evil.example")) {
			Reply started = startAt(hostile);
			Map<String, String> query = queryOf(started.location());
			google.answerNextExchangeWith(idTokenFor(query, "g-" + UUID.randomUUID(), uniqueEmail(), true));

			Reply returned = get(api, "/auth/google/callback?code=abc&state=" + query.get("state"), "__Host-sign-in=" + started.cookieValue("__Host-sign-in"));

			assertEquals("/", returned.location(), hostile);
		}
	}

	@Test
	void post_v1_sessions_uses_a_server_issued_nonce_once() throws IOException {
		String nonce = post(api, "/v1/sessions/nonce", "application/json", "", null).json().path("nonce").asText();
		String token = google.mint(FakeGoogle.claims(THIS_CLIENT, "g-" + UUID.randomUUID(), uniqueEmail(), true, nonce, Instant.now().plusSeconds(3600).getEpochSecond()));
		String body = JSON.writeValueAsString(Map.of("provider", "google", "idToken", token, "nonce", nonce));

		Reply first = post(api, "/v1/sessions", "application/json", body, null);
		Reply replay = post(api, "/v1/sessions", "application/json", body, null);

		assertEquals(201, first.status());
		assertTrue(first.setCookie("__Host-session").isPresent());
		assertEquals(401, replay.status());
		assertEquals("state-mismatch", replay.json().path("error").path("code").asText());
	}

	@Test
	void post_v1_sessions_refuses_a_token_minted_for_another_client() throws IOException {
		String nonce = post(api, "/v1/sessions/nonce", "application/json", "", null).json().path("nonce").asText();
		String token = google.mint(FakeGoogle.claims("prod-client.apps.googleusercontent.com", "g-x", uniqueEmail(), true, nonce, Instant.now().plusSeconds(3600).getEpochSecond()));

		Reply reply = post(api, "/v1/sessions", "application/json", JSON.writeValueAsString(Map.of("provider", "google", "idToken", token, "nonce", nonce)), null);

		assertEquals(401, reply.status());
		assertEquals("token-for-another-client", reply.json().path("error").path("code").asText());
	}

	@Test
	void post_v1_sessions_refuses_a_nonce_the_server_never_issued() throws IOException {
		Reply reply = post(api, "/v1/sessions", "application/json", JSON.writeValueAsString(Map.of("provider", "google", "idToken", "x.y.z", "nonce", "made-up")), null);

		assertEquals(401, reply.status());
		assertEquals("state-mismatch", reply.json().path("error").path("code").asText());
	}

	@Test
	void a_provider_identity_belongs_to_at_most_one_member_in_the_database() throws SQLException {
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		String subject = "unique-" + UUID.randomUUID();
		insertMember(first);
		insertMember(second);
		insertIdentity(first, subject);

		assertThrows(SQLException.class, () -> insertIdentity(second, subject));
	}

	@Test
	void the_dev_test_door_signs_in_only_where_the_dev_flag_is_set() {
		String credentials = form(Map.of("email", TEST_EMAIL, "password", TEST_PASSWORD, "next", "/rates"));

		Reply open = post(api, "/auth/test/sign-in", "application/x-www-form-urlencoded", credentials, null);
		Reply closed = post(apiWithoutTestDoor, "/auth/test/sign-in", "application/x-www-form-urlencoded", credentials, null);

		assertEquals(303, open.status());
		assertEquals("/rates", open.location());
		assertTrue(open.setCookie("__Host-session").isPresent());
		assertEquals(200, get(api, "/auth/test/door", null).status());
		assertEquals(404, closed.status());
		assertTrue(closed.setCookie("__Host-session").isEmpty());
		assertEquals(404, get(apiWithoutTestDoor, "/auth/test/door", null).status());
	}

	@Test
	void a_wrong_test_door_password_is_refused() {
		Reply reply = post(api, "/auth/test/sign-in", "application/x-www-form-urlencoded", form(Map.of("email", TEST_EMAIL, "password", "wrong")), null);

		assertEquals("/sign-in?refused=not-invited&door=google", reply.location());
		assertTrue(reply.setCookie("__Host-session").isEmpty());
	}

	@Test
	void without_google_credentials_the_door_reports_the_provider_unavailable() {
		Reply reply = post(apiWithoutGoogle, "/auth/google/start", "application/x-www-form-urlencoded", "next=%2F", null);

		assertEquals(303, reply.status());
		assertEquals("/sign-in?refused=provider-unavailable&door=google", reply.location());
	}

	private static void insertMember(UUID id) throws SQLException {
		try (Connection connection = database.getConnection(); PreparedStatement insert = connection.prepareStatement("insert into members (id, verified_email, created_at) values (?, ?, now())")) {
			insert.setObject(1, id);
			insert.setString(2, uniqueEmail());
			insert.executeUpdate();
		}
	}

	private static void insertIdentity(UUID memberId, String subject) throws SQLException {
		try (Connection connection = database.getConnection(); PreparedStatement insert = connection.prepareStatement("insert into provider_identities (provider, subject, member_id, verified_email, linked_at) values ('google', ?, ?, 'x@example.com', now())")) {
			insert.setString(1, subject);
			insert.setObject(2, memberId);
			insert.executeUpdate();
		}
	}
}
