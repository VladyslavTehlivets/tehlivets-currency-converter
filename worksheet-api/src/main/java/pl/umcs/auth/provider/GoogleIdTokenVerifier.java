package pl.umcs.auth.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.Provider;
import pl.umcs.auth.domain.ProviderClaims;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.SignInRefused;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.util.Base64;
import java.util.Set;

public final class GoogleIdTokenVerifier {

	private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");
	private static final ObjectMapper JSON = new ObjectMapper();

	private final JwksCache keys;
	private final Set<String> clientIdsOfThisEnvironment;
	private final Clock clock;

	public GoogleIdTokenVerifier(JwksCache keys, Set<String> clientIdsOfThisEnvironment, Clock clock) {
		this.keys = keys;
		this.clientIdsOfThisEnvironment = Set.copyOf(clientIdsOfThisEnvironment);
		this.clock = clock;
	}

	public ProviderClaims verify(Assertion.GoogleIdToken assertion) {
		String[] parts = assertion.idToken() == null ? new String[0] : assertion.idToken().split("\\.", -1);
		if (parts.length != 3) {
			throw unusable();
		}
		JsonNode header = decodeJson(parts[0]);
		JsonNode claims = decodeJson(parts[1]);
		if (assertion.origin() == Assertion.Origin.CLIENT) {
			checkSignature(header, parts);
		}
		if (!GOOGLE_ISSUERS.contains(claims.path("iss").asText())) {
			throw unusable();
		}
		if (!audienceIsThisEnvironment(claims.path("aud"))) {
			throw new SignInRefused(RefusalCode.TOKEN_FOR_ANOTHER_CLIENT);
		}
		if (!claims.path("exp").canConvertToLong() || claims.path("exp").asLong() <= clock.instant().getEpochSecond()) {
			throw unusable();
		}
		if (!nonceMatches(claims.path("nonce").asText(), assertion.expectedNonce())) {
			throw new SignInRefused(RefusalCode.STATE_MISMATCH);
		}
		String subject = claims.path("sub").asText();
		if (subject.isBlank()) {
			throw unusable();
		}
		return new ProviderClaims(Provider.GOOGLE, subject, claims.path("email").asText(null), emailVerified(claims.path("email_verified")));
	}

	private void checkSignature(JsonNode header, String[] parts) {
		if (!"RS256".equals(header.path("alg").asText())) {
			throw unusable();
		}
		PublicKey key = keys.keyFor(header.path("kid").asText()).orElseThrow(GoogleIdTokenVerifier::unusable);
		try {
			Signature rs256 = Signature.getInstance("SHA256withRSA");
			rs256.initVerify(key);
			rs256.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
			if (!rs256.verify(Base64.getUrlDecoder().decode(parts[2]))) {
				throw unusable();
			}
		} catch (java.security.GeneralSecurityException | IllegalArgumentException unreadable) {
			throw unusable();
		}
	}

	private boolean audienceIsThisEnvironment(JsonNode audience) {
		if (audience.isArray()) {
			for (JsonNode one : audience) {
				if (clientIdsOfThisEnvironment.contains(one.asText())) {
					return true;
				}
			}
			return false;
		}
		return clientIdsOfThisEnvironment.contains(audience.asText());
	}

	private static boolean nonceMatches(String inToken, String expected) {
		return expected != null && !expected.isEmpty() && MessageDigest.isEqual(
				inToken.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
	}

	private static boolean emailVerified(JsonNode flag) {
		return flag.isBoolean() ? flag.asBoolean() : "true".equals(flag.asText());
	}

	private static JsonNode decodeJson(String segment) {
		try {
			return JSON.readTree(Base64.getUrlDecoder().decode(segment));
		} catch (java.io.IOException | IllegalArgumentException unreadable) {
			throw unusable();
		}
	}

	private static SignInRefused unusable() {
		return new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
	}
}
