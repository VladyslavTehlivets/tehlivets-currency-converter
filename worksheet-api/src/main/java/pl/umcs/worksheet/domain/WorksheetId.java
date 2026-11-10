package pl.umcs.worksheet.domain;

import java.util.Objects;
import java.util.UUID;

public record WorksheetId(UUID value) {

	public WorksheetId {
		Objects.requireNonNull(value);
	}

	public static WorksheetId generate() {
		return new WorksheetId(UUID.randomUUID());
	}
}
