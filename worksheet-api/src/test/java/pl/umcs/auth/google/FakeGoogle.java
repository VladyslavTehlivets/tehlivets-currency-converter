package pl.umcs.auth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FakeGoogle implements AutoCloseable {

	public static final String ISSUER = "https://accounts.google.com";
	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

	private final HttpServer server;
	private final KeyPair signingKey;
	private volatile String idTokenForTheNextExchange = "";
	private volatile int jwksFetches;

	public FakeGoogle() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			signingKey = generator.generateKeyPair();
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/jwks", exchange -> {
				jwksFetches++;
				RSAPublicKey key = (RSAPublicKey) signingKey.getPublic();
				Map<String, Object> jwk = Map.of("kty", "RSA", "kid", "test-key", "alg", "RS256", "use", "sig",
						"n", URL.encodeToString(unsigned(key.getModulus().toByteArray())), "e", URL.encodeToString(unsigned(key.getPublicExponent().toByteArray())));
				exchange.getResponseHeaders().add("Cache-Control", "public, max-age=3600");
				reply(exchange, 200, JSON.writeValueAsString(Map.of("keys", java.util.List.of(jwk))));
			});
			server.createContext("/token", exchange -> {
				exchange.getRequestBody().readAllBytes();
				reply(exchange, 200, JSON.writeValueAsString(Map.of("id_token", idTokenForTheNextExchange)));
			});
			server.start();
		} catch (java.security.GeneralSecurityException | IOException failure) {
			throw new IllegalStateException(failure);
		}
	}

	public URI jwksUri() {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/jwks");
	}

	public URI tokenEndpoint() {
		return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
	}

	public int jwksFetches() {
		return jwksFetches;
	}

	public void answerNextExchangeWith(String idToken) {
		idTokenForTheNextExchange = idToken;
	}

	public String mint(Map<String, Object> claims) {
		try {
			String header = URL.encodeToString(JSON.writeValueAsBytes(Map.of("alg", "RS256", "kid", "test-key", "typ", "JWT")));
			String payload = URL.encodeToString(JSON.writeValueAsBytes(claims));
			Signature signer = Signature.getInstance("SHA256withRSA");
			signer.initSign(signingKey.getPrivate());
			signer.update((header + "." + payload).getBytes(StandardCharsets.US_ASCII));
			return header + "." + payload + "." + URL.encodeToString(signer.sign());
		} catch (java.security.GeneralSecurityException | IOException failure) {
			throw new IllegalStateException(failure);
		}
	}

	public static Map<String, Object> claims(String clientId, String subject, String email, boolean emailVerified, String nonce, long expiresAtEpoch) {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("iss", ISSUER);
		claims.put("aud", clientId);
		claims.put("sub", subject);
		claims.put("email", email);
		claims.put("email_verified", emailVerified);
		claims.put("nonce", nonce);
		claims.put("exp", expiresAtEpoch);
		return claims;
	}

	private static byte[] unsigned(byte[] twosComplement) {
		return twosComplement.length > 1 && twosComplement[0] == 0 ? java.util.Arrays.copyOfRange(twosComplement, 1, twosComplement.length) : twosComplement;
	}

	private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

	@Override
	public void close() {
		server.stop(0);
	}
}
