package pl.umcs;

import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;

import java.math.BigDecimal;
import java.time.Instant;

class ExchangeRateBuilder {

	private String source = "USD";
	private String target = "PLN";
	private String value = "4";
	private Instant ratedAt = Instant.parse("2026-01-10T12:00:00Z");

	static ExchangeRateBuilder aRate() {
		return new ExchangeRateBuilder();
	}

	ExchangeRateBuilder from(final String source) {
		this.source = source;
		return this;
	}

	ExchangeRateBuilder to(final String target) {
		this.target = target;
		return this;
	}

	ExchangeRateBuilder of(final String value) {
		this.value = value;
		return this;
	}

	ExchangeRateBuilder ratedAt(final String moment) {
		this.ratedAt = Instant.parse(moment);
		return this;
	}

	ExchangeRate build() {
		return ExchangeRate.builder()
				.sourceCurrency(new Currency(source))
				.targetCurrency(new Currency(target))
				.exchangeValue(new BigDecimal(value))
				.date(ratedAt)
				.build();
	}
}
