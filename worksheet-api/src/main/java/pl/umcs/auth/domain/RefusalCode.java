package pl.umcs.auth.domain;

import java.util.Arrays;
import java.util.Optional;

public enum RefusalCode {
	CANCELLED("cancelled"),
	STATE_MISMATCH("state-mismatch"),
	TOKEN_FOR_ANOTHER_CLIENT("token-for-another-client"),
	EMAIL_UNVERIFIED("email-unverified"),
	NOT_INVITED("not-invited"),
	PROVIDER_UNAVAILABLE("provider-unavailable");

	private final String wire;

	RefusalCode(String wire) {
		this.wire = wire;
	}

	public String wire() {
		return wire;
	}

	public static Optional<RefusalCode> fromWire(String written) {
		return Arrays.stream(values()).filter(code -> code.wire.equals(written)).findFirst();
	}
}
