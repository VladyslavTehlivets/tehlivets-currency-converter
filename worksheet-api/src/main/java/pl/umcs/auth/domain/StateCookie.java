package pl.umcs.auth.domain;

import java.time.Duration;
import java.time.Instant;

public record StateCookie(String state, String nonce, String verifier, NextPath next, Instant issuedAt) {

	public static final Duration LIFETIME = Duration.ofMinutes(10);

	public boolean isFresh(Instant now) {
		return now.isBefore(issuedAt.plus(LIFETIME));
	}

	public boolean answers(String returnedState) {
		return returnedState != null && java.security.MessageDigest.isEqual(
				state.getBytes(java.nio.charset.StandardCharsets.UTF_8),
				returnedState.getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}
}
