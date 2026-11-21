package pl.umcs.auth.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.SignInRefused;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpJwksSource implements JwksSource {

	public static final URI GOOGLE_CERTS = URI.create("https://www.googleapis.com/oauth2/v3/certs");

	private static final Pattern MAX_AGE = Pattern.compile("max-age=(\\d+)");
	private static final Duration FALLBACK_FRESHNESS = Duration.ofMinutes(5);
	private static final ObjectMapper JSON = new ObjectMapper();

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final URI location;

	public HttpJwksSource(URI location) {
		this.location = location;
	}

	@Override
	public JwkSet fetch() {
		try {
			HttpRequest request = HttpRequest.newBuilder(location).timeout(Duration.ofSeconds(5)).GET().build();
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
			}
			return new JwkSet(keysIn(JSON.readTree(response.body())), freshnessOf(response.headers().firstValue("Cache-Control").orElse("")));
		} catch (java.io.IOException | GeneralSecurityFailure | IllegalArgumentException unusable) {
			throw new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
		}
	}

	static Duration freshnessOf(String cacheControl) {
		Matcher found = MAX_AGE.matcher(cacheControl);
		return found.find() ? Duration.ofSeconds(Long.parseLong(found.group(1))) : FALLBACK_FRESHNESS;
	}

	private static Map<String, PublicKey> keysIn(JsonNode document) {
		Map<String, PublicKey> keys = new HashMap<>();
		for (JsonNode jwk : document.path("keys")) {
			if ("RSA".equals(jwk.path("kty").asText())) {
				keys.put(jwk.path("kid").asText(), rsaKey(jwk.path("n").asText(), jwk.path("e").asText()));
			}
		}
		return keys;
	}

	private static PublicKey rsaKey(String modulus, String exponent) {
		try {
			Base64.Decoder decoder = Base64.getUrlDecoder();
			return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
					new BigInteger(1, decoder.decode(modulus)), new BigInteger(1, decoder.decode(exponent))));
		} catch (java.security.GeneralSecurityException unusable) {
			throw new GeneralSecurityFailure(unusable);
		}
	}

	private static final class GeneralSecurityFailure extends RuntimeException {
		GeneralSecurityFailure(Throwable cause) {
			super(cause);
		}
	}
}
