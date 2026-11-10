package pl.umcs.worksheet.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record RatedAt(Instant value) {

	private static final Instant EARLIEST = Instant.parse("1900-01-01T00:00:00Z");
	private static final Instant FIRST_MOMENT_AFTER_LATEST_DAY = Instant.parse("2101-01-01T00:00:00Z");

	public RatedAt {
		if (value == null || value.isBefore(EARLIEST) || !value.isBefore(FIRST_MOMENT_AFTER_LATEST_DAY)) {
			throw new WorksheetRefusal(WorksheetRefusal.Reason.INVALID_MOMENT);
		}
		value = keptAtStoredPrecision(value);
	}

	private static Instant keptAtStoredPrecision(Instant moment) {
		return moment.truncatedTo(ChronoUnit.MICROS);
	}
}
