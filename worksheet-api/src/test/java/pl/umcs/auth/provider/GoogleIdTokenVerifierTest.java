package pl.umcs.auth.provider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.Provider;
import pl.umcs.auth.domain.ProviderClaims;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.SignInRefused;

import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.umcs.auth.provider.GoogleTokenBuilder.ANOTHER_ENVIRONMENT_CLIENT;
import static pl.umcs.auth.provider.GoogleTokenBuilder.KEY_ID;
import static pl.umcs.auth.provider.GoogleTokenBuilder.THIS_ENVIRONMENT_CLIENT;
import static pl.umcs.auth.provider.GoogleTokenBuilder.aGoogleToken;

class GoogleIdTokenVerifierTest {

	private static final String NONCE = "nonce-1";

	private static KeyPair googleKey;
	private static GoogleIdTokenVerifier verifier;

	@BeforeAll
	static void googlePublishesItsKey() {
		googleKey = GoogleTokenBuilder.newSigningKey();
		JwksSource published = () -> new JwkSet(Map.of(KEY_ID, googleKey.getPublic()), Duration.ofHours(1));
		verifier = new GoogleIdTokenVerifier(new JwksCache(published, Clock.systemUTC()), Set.of(THIS_ENVIRONMENT_CLIENT), Clock.systemUTC());
	}

	private static Assertion.GoogleIdToken fromClient(String token) {
		return new Assertion.GoogleIdToken(token, NONCE, Assertion.Origin.CLIENT);
	}

	private static Assertion.GoogleIdToken fromTokenEndpoint(String token) {
		return new Assertion.GoogleIdToken(token, NONCE, Assertion.Origin.TOKEN_ENDPOINT);
	}

	private static RefusalCode refusalOf(Assertion.GoogleIdToken assertion) {
		return assertThrows(SignInRefused.class, () -> verifier.verify(assertion)).code();
	}

	@Test
	void a_client_sent_google_token_is_verified_against_the_jwks() {
		ProviderClaims claims = verifier.verify(fromClient(aGoogleToken(googleKey).build()));

		assertEquals(new ProviderClaims(Provider.GOOGLE, "google-subject-1", "ada@example.com", true), claims);
	}

	@Test
	void a_client_sent_token_signed_by_another_key_is_refused() {
		String forged = aGoogleToken(googleKey).buildSignedBy(GoogleTokenBuilder.newSigningKey());

		assertEquals(RefusalCode.PROVIDER_UNAVAILABLE, refusalOf(fromClient(forged)));
	}

	@Test
	void a_client_sent_token_naming_an_unknown_key_is_refused() {
		assertEquals(RefusalCode.PROVIDER_UNAVAILABLE, refusalOf(fromClient(aGoogleToken(googleKey).signedWithKeyId("other").build())));
	}

	@Test
	void a_client_sent_token_with_an_unsigned_algorithm_is_refused() {
		assertEquals(RefusalCode.PROVIDER_UNAVAILABLE, refusalOf(fromClient(aGoogleToken(googleKey).signedWith("none").build())));
	}

	@Test
	void a_google_token_taken_from_the_token_endpoint_can_be_read_without_its_signature() {
		String unverifiableSignature = aGoogleToken(googleKey).buildSignedBy(GoogleTokenBuilder.newSigningKey());

		assertEquals("google-subject-1", verifier.verify(fromTokenEndpoint(unverifiableSignature)).subject());
	}

	@Test
	void a_google_token_minted_for_another_client_is_refused() {
		String other = aGoogleToken(googleKey).forClient(ANOTHER_ENVIRONMENT_CLIENT).build();

		assertEquals(RefusalCode.TOKEN_FOR_ANOTHER_CLIENT, refusalOf(fromClient(other)));
		assertEquals(RefusalCode.TOKEN_FOR_ANOTHER_CLIENT, refusalOf(fromTokenEndpoint(other)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"https://accounts.google.com", "accounts.google.com"})
	void the_google_iss_is_checked_exactly_and_both_spellings_pass(String issuer) {
		assertEquals("google-subject-1", verifier.verify(fromClient(aGoogleToken(googleKey).issuedBy(issuer).build())).subject());
	}

	@ParameterizedTest
	@ValueSource(strings = {"https://accounts.google.com/", "http://accounts.google.com", "https://evil.example", "", "accounts.google.com.evil.example"})
	void an_issuer_that_is_not_exactly_google_is_refused(String issuer) {
		assertEquals(RefusalCode.PROVIDER_UNAVAILABLE, refusalOf(fromClient(aGoogleToken(googleKey).issuedBy(issuer).build())));
	}

	@Test
	void an_expired_token_is_refused() {
		String expired = aGoogleToken(googleKey).expiringAt(Instant.now().minusSeconds(5)).build();

		assertEquals(RefusalCode.PROVIDER_UNAVAILABLE, refusalOf(fromClient(expired)));
	}

	@Test
	void a_token_carrying_another_sign_ins_nonce_is_refused() {
		String replayed = aGoogleToken(googleKey).withNonce("someone-else").build();

		assertEquals(RefusalCode.STATE_MISMATCH, refusalOf(fromClient(replayed)));
	}

	@Test
	void a_token_without_a_nonce_is_refused_even_when_none_is_expected() {
		String token = aGoogleToken(googleKey).withNonce("").build();

		assertEquals(RefusalCode.STATE_MISMATCH, refusalOf(new Assertion.GoogleIdToken(token, "", Assertion.Origin.CLIENT)));
	}

	@Test
	void an_unverified_email_is_reported_as_unverified() {
		ProviderClaims claims = verifier.verify(fromClient(aGoogleToken(googleKey).withEmail("ada@example.com", false).build()));

		assertFalse(claims.emailVerified());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "not-a-token", "a.b", "a.b.c.d", "!!!.???.***"})
	void a_malformed_token_is_refused(String malformed) {
		assertEquals(RefusalCode.PROVIDER_UNAVAILABLE, refusalOf(fromClient(malformed)));
	}
}
