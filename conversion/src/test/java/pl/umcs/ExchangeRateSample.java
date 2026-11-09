package pl.umcs;

import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;

import java.math.BigDecimal;
import java.time.Instant;

import static pl.umcs.domain.ExchangeRate.builder;

class ExchangeRateSample {
	static ExchangeRate generateExchangeRate(final String source, final String target, final double exchangeValue, final Instant date) {
		return builder()
				.sourceCurrency(new Currency(source))
				.targetCurrency(new Currency(target))
				.exchangeValue(new BigDecimal(exchangeValue))
				.date(date)
				.build();
	}
}
