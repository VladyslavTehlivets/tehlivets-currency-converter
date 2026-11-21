package pl.umcs.auth.provider;

public interface GoogleTokenEndpoint {

	String idTokenFor(String authorizationCode, String codeVerifier, String redirectUri);
}
