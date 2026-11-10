package pl.umcs.worksheet.domain;

import java.util.Objects;
import java.util.UUID;

public record RateId(UUID value) {

	public RateId {
		Objects.requireNonNull(value);
	}

	public static RateId generate() {
		return new RateId(UUID.randomUUID());
	}
}
