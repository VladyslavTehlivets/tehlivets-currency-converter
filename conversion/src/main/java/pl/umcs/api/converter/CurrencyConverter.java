package pl.umcs.api.converter;

import lombok.RequiredArgsConstructor;
import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.Money;

import java.time.Instant;
import java.util.Arrays;

@RequiredArgsConstructor
public class CurrencyConverter {

	private final Currency baseCurrency;
	private final ExchangeRates exchangeRates;

	public Money convert(final Money sourceAmount, final Currency targetCurrency, final Instant date) {
		final Currency sourceCurrency = sourceAmount.getCurrency();

		if (anyCurrencyIsBase(sourceCurrency, targetCurrency)) {

			final ExchangeRate exchangeRate = exchangeRates.getExchangeRate(sourceCurrency, targetCurrency, date);
			return new EndExchangeApplier(exchangeRate).performExchange(sourceAmount);
		} else {

			final ExchangeRate firstExchangeRate = exchangeRates.getExchangeRate(sourceCurrency, baseCurrency, date);
			final ExchangeRate secondExchangeRate = exchangeRates.getExchangeRate(baseCurrency, targetCurrency, date);
			return new ExchangeApplier(firstExchangeRate, new EndExchangeApplier(secondExchangeRate)).performExchange(sourceAmount);
		}
	}

	private boolean anyCurrencyIsBase(final Currency sourceCurrency, final Currency targetCurrency) {
		return Arrays.asList(targetCurrency, sourceCurrency).contains(baseCurrency);
	}
}
