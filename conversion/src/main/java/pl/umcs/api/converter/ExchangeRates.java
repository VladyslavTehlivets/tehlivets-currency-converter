package pl.umcs.api.converter;

import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.exception.NotFoundExchangeRateException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExchangeRates {

	private final List<ExchangeRate> exchangeRates = new ArrayList<>();

	ExchangeRate getExchangeRate(Currency source, Currency target, Instant date) {
		return exchangeRates.stream()
				.filter(exchangeRate -> exchangeRate.getSourceCurrency().equals(source))
				.filter(exchangeRate -> exchangeRate.getTargetCurrency().equals(target))
				.min(Comparator.comparingLong(o -> Duration.between(o.getDate(), date).toNanos()))
				.orElseThrow(NotFoundExchangeRateException::new);
	}

	public void addExchangeRate(ExchangeRate exchangeRate) {
		exchangeRates.add(exchangeRate);
	}
}
