package pl.umcs.auth.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.SignInRefused;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpGoogleTokenEndpoint implements GoogleTokenEndpoint {

	public static final URI GOOGLE_TOKEN = URI.create("https://oauth2.googleapis.com/token");

	private static final ObjectMapper JSON = new ObjectMapper();

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	private final URI location;
	private final String clientId;
	private final String clientSecret;

	public HttpGoogleTokenEndpoint(URI location, String clientId, String clientSecret) {
		this.location = location;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	@Override
	public String idTokenFor(String authorizationCode, String codeVerifier, String redirectUri) {
		Map<String, String> form = new LinkedHashMap<>();
		form.put("grant_type", "authorization_code");
		form.put("code", authorizationCode);
		form.put("code_verifier", codeVerifier);
		form.put("redirect_uri", redirectUri);
		form.put("client_id", clientId);
		form.put("client_secret", clientSecret);
		String body = form.entrySet().stream()
				.map(field -> field.getKey() + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8))
				.collect(Collectors.joining("&"));
		try {
			HttpRequest request = HttpRequest.newBuilder(location)
					.timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			String idToken = response.statusCode() == 200 ? JSON.readTree(response.body()).path("id_token").asText("") : "";
			if (idToken.isEmpty()) {
				throw new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
			}
			return idToken;
		} catch (java.io.IOException unreachable) {
			throw new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
		}
	}
}
