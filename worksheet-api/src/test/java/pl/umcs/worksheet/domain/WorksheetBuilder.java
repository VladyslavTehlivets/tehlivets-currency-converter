package pl.umcs.worksheet.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static pl.umcs.worksheet.domain.DatedRateBuilder.aDatedRate;

final class WorksheetBuilder {

	private static final Instant FIRST_MOMENT = Instant.parse("2026-01-01T00:00:00Z");

	private final List<DatedRate> rates = new ArrayList<>();

	private WorksheetBuilder() {
	}

	static WorksheetBuilder aWorksheet() {
		return new WorksheetBuilder();
	}

	WorksheetBuilder holdingRatesOnSuccessiveHours(int count) {
		for (int hour = 0; hour < count; hour++) {
			rates.add(aDatedRate().ratedAt(FIRST_MOMENT.plus(Duration.ofHours(hour))).build());
		}
		return this;
	}

	WorksheetBuilder holding(DatedRate rate) {
		rates.add(rate);
		return this;
	}

	Worksheet build() {
		return new Worksheet(WorksheetId.generate(), WorksheetKey.generate().hash(), rates);
	}
}
