package pl.umcs.auth.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

public final class GoogleTokenBuilder {

	public static final String KEY_ID = "test-key-1";
	public static final String THIS_ENVIRONMENT_CLIENT = "dev-client.apps.googleusercontent.com";
	public static final String ANOTHER_ENVIRONMENT_CLIENT = "prod-client.apps.googleusercontent.com";

	private static final ObjectMapper JSON = new ObjectMapper();

	private final KeyPair signingKey;
	private final ObjectNode claims = JSON.createObjectNode();
	private String keyId = KEY_ID;
	private String algorithm = "RS256";

	private GoogleTokenBuilder(KeyPair signingKey) {
		this.signingKey = signingKey;
		claims.put("iss", "https://accounts.google.com");
		claims.put("aud", THIS_ENVIRONMENT_CLIENT);
		claims.put("sub", "google-subject-1");
		claims.put("email", "ada@example.com");
		claims.put("email_verified", true);
		claims.put("nonce", "nonce-1");
		claims.put("exp", Instant.now().plusSeconds(600).getEpochSecond());
	}

	public static KeyPair newSigningKey() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (java.security.GeneralSecurityException unavailable) {
			throw new IllegalStateException(unavailable);
		}
	}

	public static GoogleTokenBuilder aGoogleToken(KeyPair signingKey) {
		return new GoogleTokenBuilder(signingKey);
	}

	public static String jwksOf(KeyPair key) {
		RSAPublicKey pub = (RSAPublicKey) key.getPublic();
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		return "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" + KEY_ID + "\",\"alg\":\"RS256\",\"use\":\"sig\",\"n\":\""
				+ encoder.encodeToString(unsigned(pub.getModulus().toByteArray())) + "\",\"e\":\""
				+ encoder.encodeToString(unsigned(pub.getPublicExponent().toByteArray())) + "\"}]}";
	}

	private static byte[] unsigned(byte[] bytes) {
		return bytes.length > 1 && bytes[0] == 0 ? java.util.Arrays.copyOfRange(bytes, 1, bytes.length) : bytes;
	}

	public GoogleTokenBuilder issuedBy(String issuer) {
		claims.put("iss", issuer);
		return this;
	}

	public GoogleTokenBuilder forClient(String clientId) {
		claims.put("aud", clientId);
		return this;
	}

	public GoogleTokenBuilder forSubject(String subject) {
		claims.put("sub", subject);
		return this;
	}

	public GoogleTokenBuilder withEmail(String email, boolean verified) {
		claims.put("email", email);
		claims.put("email_verified", verified);
		return this;
	}

	public GoogleTokenBuilder withNonce(String nonce) {
		claims.put("nonce", nonce);
		return this;
	}

	public GoogleTokenBuilder expiringAt(Instant moment) {
		claims.put("exp", moment.getEpochSecond());
		return this;
	}

	public GoogleTokenBuilder signedWithKeyId(String id) {
		this.keyId = id;
		return this;
	}

	public GoogleTokenBuilder signedWith(String algorithmName) {
		this.algorithm = algorithmName;
		return this;
	}

	public String build() {
		return sign(signingKey.getPrivate());
	}

	public String buildSignedBy(KeyPair otherKey) {
		return sign(otherKey.getPrivate());
	}

	private String sign(PrivateKey key) {
		try {
			Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
			String header = encoder.encodeToString(("{\"alg\":\"" + algorithm + "\",\"kid\":\"" + keyId + "\",\"typ\":\"JWT\"}").getBytes(StandardCharsets.UTF_8));
			String payload = encoder.encodeToString(claims.toString().getBytes(StandardCharsets.UTF_8));
			Signature rs256 = Signature.getInstance("SHA256withRSA");
			rs256.initSign(key);
			rs256.update((header + "." + payload).getBytes(StandardCharsets.US_ASCII));
			return header + "." + payload + "." + encoder.encodeToString(rs256.sign());
		} catch (java.security.GeneralSecurityException unavailable) {
			throw new IllegalStateException(unavailable);
		}
	}
}
