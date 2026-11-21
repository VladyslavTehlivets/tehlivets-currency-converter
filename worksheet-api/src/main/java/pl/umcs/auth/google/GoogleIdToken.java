package pl.umcs.auth.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.Provider;
import pl.umcs.auth.domain.SignInRefusal;
import pl.umcs.auth.domain.SignInRefusal.Code;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;

public final class GoogleIdToken {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

	private final Set<String> clientIds;
	private final GoogleJwks jwks;
	private final Clock clock;

	public GoogleIdToken(Set<String> clientIds, GoogleJwks jwks, Clock clock) {
		this.clientIds = clientIds;
		this.jwks = jwks;
		this.clock = clock;
	}

	public Assertion.ProviderAssertion readFromTokenEndpoint(String token, String expectedNonce) {
		return assertionFrom(claimsOf(token), expectedNonce);
	}

	public Assertion.ProviderAssertion verifyFromClient(String token, String expectedNonce) {
		String[] parts = token.split("\\.", -1);
		if (parts.length != 3) {
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		}
		JsonNode header = decode(parts[0]);
		PublicKey key = jwks.keyFor(header.path("kid").asText()).orElseThrow(() -> new SignInRefusal(Code.PROVIDER_UNAVAILABLE));
		if (!"RS256".equals(header.path("alg").asText()) || !signatureHolds(key, parts)) {
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		}
		return assertionFrom(decode(parts[1]), expectedNonce);
	}

	private Assertion.ProviderAssertion assertionFrom(JsonNode claims, String expectedNonce) {
		if (!ISSUERS.contains(claims.path("iss").asText())) {
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		}
		if (!audienceIsThisEnvironment(claims.path("aud"))) {
			throw new SignInRefusal(Code.TOKEN_FOR_ANOTHER_CLIENT);
		}
		if (!clock.instant().isBefore(java.time.Instant.ofEpochSecond(claims.path("exp").asLong(0)))) {
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		}
		if (expectedNonce == null || !expectedNonce.equals(claims.path("nonce").asText())) {
			throw new SignInRefusal(Code.STATE_MISMATCH);
		}
		boolean verified = claims.path("email_verified").asBoolean(false) || "true".equals(claims.path("email_verified").asText());
		return new Assertion.ProviderAssertion(Provider.GOOGLE, claims.path("sub").asText(), claims.path("email").asText(), verified);
	}

	private boolean audienceIsThisEnvironment(JsonNode aud) {
		if (aud.isArray()) {
			for (JsonNode one : aud) {
				if (clientIds.contains(one.asText())) {
					return true;
				}
			}
			return false;
		}
		return clientIds.contains(aud.asText());
	}

	private static boolean signatureHolds(PublicKey key, String[] parts) {
		try {
			Signature verifier = Signature.getInstance("SHA256withRSA");
			verifier.initVerify(key);
			verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
			return verifier.verify(Base64.getUrlDecoder().decode(parts[2]));
		} catch (java.security.GeneralSecurityException | IllegalArgumentException unverifiable) {
			return false;
		}
	}

	private static JsonNode claimsOf(String token) {
		String[] parts = token.split("\\.", -1);
		if (parts.length != 3) {
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		}
		return decode(parts[1]);
	}

	private static JsonNode decode(String part) {
		try {
			return JSON.readTree(Base64.getUrlDecoder().decode(part));
		} catch (java.io.IOException | IllegalArgumentException malformed) {
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		}
	}
}
