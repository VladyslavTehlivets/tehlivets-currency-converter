package pl.umcs.auth.google;

import java.net.URI;
import java.util.Set;

public record GoogleOidc(Set<String> clientIds, String clientSecret, URI redirectUri, URI authorizationEndpoint, URI tokenEndpoint, URI jwksUri) {

	public static final URI AUTHORIZATION = URI.create("https://accounts.google.com/o/oauth2/v2/auth");
	public static final URI TOKEN = URI.create("https://oauth2.googleapis.com/token");
	public static final URI JWKS = URI.create("https://www.googleapis.com/oauth2/v3/certs");

	public static GoogleOidc production(Set<String> clientIds, String clientSecret, URI redirectUri) {
		return new GoogleOidc(clientIds, clientSecret, redirectUri, AUTHORIZATION, TOKEN, JWKS);
	}

	public boolean isConfigured() {
		return !clientIds.isEmpty() && clientSecret != null && !clientSecret.isBlank();
	}

	public String clientId() {
		return clientIds.iterator().next();
	}
}
