package pl.umcs.auth.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.SignInRefusal;
import pl.umcs.auth.domain.SignInRefusal.Code;
import pl.umcs.auth.domain.StateCookie;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GoogleDoor {

	private static final ObjectMapper JSON = new ObjectMapper();

	private final GoogleOidc oidc;
	private final HttpClient http;
	private final GoogleIdToken idTokens;

	public GoogleDoor(GoogleOidc oidc, HttpClient http, GoogleIdToken idTokens) {
		this.oidc = oidc;
		this.http = http;
		this.idTokens = idTokens;
	}

	public boolean isOpen() {
		return oidc.isConfigured();
	}

	public GoogleIdToken idTokens() {
		return idTokens;
	}

	public URI authorizationRequest(StateCookie binder) {
		Map<String, String> query = new LinkedHashMap<>();
		query.put("client_id", oidc.clientId());
		query.put("redirect_uri", oidc.redirectUri().toString());
		query.put("response_type", "code");
		query.put("scope", "openid email");
		query.put("state", binder.state());
		query.put("nonce", binder.nonce());
		query.put("code_challenge", binder.challenge());
		query.put("code_challenge_method", "S256");
		return URI.create(oidc.authorizationEndpoint() + "?" + formEncoded(query));
	}

	public Assertion.ProviderAssertion exchange(String code, StateCookie binder) {
		Map<String, String> form = new LinkedHashMap<>();
		form.put("grant_type", "authorization_code");
		form.put("code", code);
		form.put("client_id", oidc.clientId());
		form.put("client_secret", oidc.clientSecret());
		form.put("redirect_uri", oidc.redirectUri().toString());
		form.put("code_verifier", binder.verifier());
		HttpRequest request = HttpRequest.newBuilder(oidc.tokenEndpoint())
				.timeout(Duration.ofSeconds(10))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(formEncoded(form)))
				.build();
		try {
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode body = JSON.readTree(response.body());
			if (response.statusCode() != 200 || !body.hasNonNull("id_token")) {
				throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
			}
			return idTokens.readFromTokenEndpoint(body.get("id_token").asText(), binder.nonce());
		} catch (IOException unreachable) {
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new SignInRefusal(Code.PROVIDER_UNAVAILABLE);
		}
	}

	private static String formEncoded(Map<String, String> fields) {
		StringBuilder encoded = new StringBuilder();
		fields.forEach((name, value) -> {
			if (!encoded.isEmpty()) {
				encoded.append('&');
			}
			encoded.append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
		});
		return encoded.toString();
	}
}
