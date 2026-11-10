package pl.umcs.worksheet.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

final class DatedRateBuilder {

	private String from = "EUR";
	private String to = "PLN";
	private String value = "4.30";
	private Instant ratedAt = Instant.parse("2026-01-10T12:00:00Z");

	private DatedRateBuilder() {
	}

	static DatedRateBuilder aDatedRate() {
		return new DatedRateBuilder();
	}

	DatedRateBuilder from(String currencyCode) {
		this.from = currencyCode;
		return this;
	}

	DatedRateBuilder to(String currencyCode) {
		this.to = currencyCode;
		return this;
	}

	DatedRateBuilder of(String value) {
		this.value = value;
		return this;
	}

	DatedRateBuilder ratedAt(Instant ratedAt) {
		this.ratedAt = ratedAt;
		return this;
	}

	DatedRate build() {
		return DatedRate.newRate(
				new Direction(Currency.getInstance(from), Currency.getInstance(to)),
				new RateValue(new BigDecimal(value)),
				new RatedAt(ratedAt));
	}
}
