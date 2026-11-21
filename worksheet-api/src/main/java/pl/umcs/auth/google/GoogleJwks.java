package pl.umcs.auth.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GoogleJwks {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");
	private static final Duration FALLBACK_LIFETIME = Duration.ofMinutes(5);

	private final URI uri;
	private final HttpClient http;
	private final Clock clock;
	private Map<String, PublicKey> keys = Map.of();
	private Instant freshUntil = Instant.MIN;

	public GoogleJwks(URI uri, HttpClient http, Clock clock) {
		this.uri = uri;
		this.http = http;
		this.clock = clock;
	}

	public synchronized Optional<PublicKey> keyFor(String keyId) {
		if (!clock.instant().isBefore(freshUntil) || !keys.containsKey(keyId)) {
			refresh();
		}
		return Optional.ofNullable(keys.get(keyId));
	}

	private void refresh() {
		try {
			HttpResponse<String> response = http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return;
			}
			Map<String, PublicKey> fresh = new HashMap<>();
			for (JsonNode jwk : JSON.readTree(response.body()).path("keys")) {
				if ("RSA".equals(jwk.path("kty").asText())) {
					fresh.put(jwk.path("kid").asText(), rsaKey(jwk));
				}
			}
			keys = Map.copyOf(fresh);
			freshUntil = clock.instant().plus(lifetimeOf(response));
		} catch (java.io.IOException | java.security.GeneralSecurityException unreachable) {
			return;
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	private static Duration lifetimeOf(HttpResponse<String> response) {
		Matcher maxAge = MAX_AGE.matcher(response.headers().firstValue("Cache-Control").orElse(""));
		return maxAge.find() ? Duration.ofSeconds(Long.parseLong(maxAge.group(1))) : FALLBACK_LIFETIME;
	}

	private static PublicKey rsaKey(JsonNode jwk) throws java.security.GeneralSecurityException {
		Base64.Decoder url = Base64.getUrlDecoder();
		BigInteger modulus = new BigInteger(1, url.decode(jwk.path("n").asText()));
		BigInteger exponent = new BigInteger(1, url.decode(jwk.path("e").asText()));
		return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
	}
}
