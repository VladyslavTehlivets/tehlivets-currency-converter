package pl.umcs.auth.google;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.Provider;
import pl.umcs.auth.domain.SignInRefusal;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleIdTokenTest {

	private static final String THIS_CLIENT = "dev-client.apps.googleusercontent.com";
	private static final String ANOTHER_CLIENT = "prod-client.apps.googleusercontent.com";
	private static final Instant NOW = Instant.parse("2026-11-21T10:00:00Z");
	private static final long LATER = NOW.plusSeconds(3600).getEpochSecond();

	private static FakeGoogle google;
	private static GoogleJwks jwks;
	private static GoogleIdToken idTokens;

	@BeforeAll
	static void startFakeGoogle() {
		google = new FakeGoogle();
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		jwks = new GoogleJwks(google.jwksUri(), HttpClient.newHttpClient(), clock);
		idTokens = new GoogleIdToken(Set.of(THIS_CLIENT), jwks, clock);
	}

	@AfterAll
	static void stopFakeGoogle() {
		google.close();
	}

	private static SignInRefusal.Code refusalOf(Runnable verification) {
		return assertThrows(SignInRefusal.class, verification::run).code();
	}

	@Test
	void a_token_minted_for_this_environment_is_read_into_an_assertion() {
		String token = google.mint(FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-1", LATER));

		Assertion.ProviderAssertion assertion = idTokens.verifyFromClient(token, "n-1");

		assertEquals(new Assertion.ProviderAssertion(Provider.GOOGLE, "g-1", "ada@example.com", true), assertion);
	}

	@Test
	void a_google_token_minted_for_another_client_is_refused() {
		String token = google.mint(FakeGoogle.claims(ANOTHER_CLIENT, "g-1", "ada@example.com", true, "n-1", LATER));

		assertEquals(SignInRefusal.Code.TOKEN_FOR_ANOTHER_CLIENT, refusalOf(() -> idTokens.verifyFromClient(token, "n-1")));
		assertEquals(SignInRefusal.Code.TOKEN_FOR_ANOTHER_CLIENT, refusalOf(() -> idTokens.readFromTokenEndpoint(token, "n-1")));
	}

	@Test
	void an_audience_list_that_names_this_client_is_accepted() {
		Map<String, Object> claims = FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-1", LATER);
		claims.put("aud", java.util.List.of(ANOTHER_CLIENT, THIS_CLIENT));

		assertEquals("g-1", idTokens.verifyFromClient(google.mint(claims), "n-1").subject());
	}

	@Test
	void a_token_for_another_nonce_is_refused_as_a_state_mismatch() {
		String token = google.mint(FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-other", LATER));

		assertEquals(SignInRefusal.Code.STATE_MISMATCH, refusalOf(() -> idTokens.verifyFromClient(token, "n-1")));
	}

	@Test
	void an_expired_token_is_refused() {
		String token = google.mint(FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-1", NOW.getEpochSecond()));

		assertEquals(SignInRefusal.Code.PROVIDER_UNAVAILABLE, refusalOf(() -> idTokens.verifyFromClient(token, "n-1")));
	}

	@Test
	void a_token_from_another_issuer_is_refused() {
		Map<String, Object> claims = FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-1", LATER);
		claims.put("iss", "https://evil.example");

		assertEquals(SignInRefusal.Code.PROVIDER_UNAVAILABLE, refusalOf(() -> idTokens.verifyFromClient(google.mint(claims), "n-1")));
	}

	@Test
	void the_bare_google_issuer_is_accepted() {
		Map<String, Object> claims = FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-1", LATER);
		claims.put("iss", "accounts.google.com");

		assertEquals("g-1", idTokens.verifyFromClient(google.mint(claims), "n-1").subject());
	}

	@Test
	void a_client_token_with_a_broken_signature_is_refused() {
		String token = google.mint(FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-1", LATER));
		String forged = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";

		assertEquals(SignInRefusal.Code.PROVIDER_UNAVAILABLE, refusalOf(() -> idTokens.verifyFromClient(forged, "n-1")));
	}

	@Test
	void a_client_token_with_an_unsigned_payload_is_refused() {
		assertEquals(SignInRefusal.Code.PROVIDER_UNAVAILABLE, refusalOf(() -> idTokens.verifyFromClient("e30.e30.", "n-1")));
		assertEquals(SignInRefusal.Code.PROVIDER_UNAVAILABLE, refusalOf(() -> idTokens.verifyFromClient("garbage", "n-1")));
	}

	@Test
	void an_email_that_google_did_not_verify_stays_unverified_in_the_assertion() {
		String token = google.mint(FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", false, "n-1", LATER));

		assertFalse(idTokens.verifyFromClient(token, "n-1").emailVerified());
	}

	@Test
	void the_signing_keys_are_fetched_once_while_the_cache_control_allows() {
		int before = google.jwksFetches();
		String token = google.mint(FakeGoogle.claims(THIS_CLIENT, "g-1", "ada@example.com", true, "n-1", LATER));

		idTokens.verifyFromClient(token, "n-1");
		idTokens.verifyFromClient(token, "n-1");

		assertTrue(google.jwksFetches() - before <= 1);
	}
}
