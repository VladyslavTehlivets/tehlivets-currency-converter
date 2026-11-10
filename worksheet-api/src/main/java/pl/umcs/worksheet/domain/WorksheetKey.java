package pl.umcs.worksheet.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class WorksheetKey {

	private static final int KEY_BYTES = 32;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final String value;

	private WorksheetKey(String value) {
		this.value = value;
	}

	public static WorksheetKey generate() {
		byte[] randomBytes = new byte[KEY_BYTES];
		RANDOM.nextBytes(randomBytes);
		return new WorksheetKey(Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes));
	}

	public static WorksheetKey presented(String value) {
		return new WorksheetKey(value);
	}

	public String reveal() {
		return value;
	}

	public byte[] hash() {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException everyJavaPlatformShipsSha256) {
			throw new IllegalStateException(everyJavaPlatformShipsSha256);
		}
	}

	@Override
	public String toString() {
		return "WorksheetKey[hidden]";
	}
}
