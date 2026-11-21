package pl.umcs.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public record StateCookie(String state, String nonce, String verifier, String nextPath, Instant issuedAt) {

	public static final Duration LIFETIME = Duration.ofMinutes(10);
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Base64.Encoder URL = Base64.getUrlEncoder().withoutPadding();

	public static StateCookie begin(NextPath next, Instant now) {
		return new StateCookie(randomToken(), randomToken(), randomToken(), next.value(), now);
	}

	public static String randomToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return URL.encodeToString(bytes);
	}

	public String challenge() {
		try {
			return URL.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
		} catch (NoSuchAlgorithmException impossible) {
			throw new IllegalStateException(impossible);
		}
	}

	public boolean returnsWith(String returnedState, Instant now) {
		return state.equals(returnedState) && now.isBefore(issuedAt.plus(LIFETIME));
	}

	public String encode() {
		String[] parts = {state, nonce, verifier, nextPath, Long.toString(issuedAt.getEpochSecond())};
		return URL.encodeToString(String.join("\n", parts).getBytes(StandardCharsets.UTF_8));
	}

	public static Optional<StateCookie> decode(String encoded) {
		try {
			String[] parts = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8).split("\n", -1);
			if (parts.length != 5) {
				return Optional.empty();
			}
			return Optional.of(new StateCookie(parts[0], parts[1], parts[2], parts[3], Instant.ofEpochSecond(Long.parseLong(parts[4]))));
		} catch (IllegalArgumentException malformed) {
			return Optional.empty();
		}
	}
}
