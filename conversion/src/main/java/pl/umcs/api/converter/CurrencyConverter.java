package pl.umcs.api.converter;

import lombok.RequiredArgsConstructor;
import pl.umcs.domain.Conversion;
import pl.umcs.domain.ConversionLeg;
import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class CurrencyConverter {

	private final Currency baseCurrency;
	private final ExchangeRates exchangeRates;

	public Money convert(final Money sourceAmount, final Currency targetCurrency, final Instant date) {
		return explain(sourceAmount, targetCurrency, date).result();
	}

	public Conversion explain(final Money sourceAmount, final Currency targetCurrency, final Instant date) {
		final List<ConversionLeg> legs = new ArrayList<>();
		Money amountIn = sourceAmount;
		for (final ExchangeRate exchangeRate : ratesAlongThePath(sourceAmount.getCurrency(), targetCurrency, date)) {
			final ConversionLeg leg = ConversionLeg.applying(exchangeRate, amountIn);
			legs.add(leg);
			amountIn = leg.amountOut();
		}
		return new Conversion(sourceAmount, targetCurrency, date, legs);
	}

	private List<ExchangeRate> ratesAlongThePath(final Currency sourceCurrency, final Currency targetCurrency, final Instant date) {
		if (anyCurrencyIsBase(sourceCurrency, targetCurrency)) {
			return List.of(exchangeRates.getExchangeRate(sourceCurrency, targetCurrency, date));
		}
		final ExchangeRate firstExchangeRate = exchangeRates.getExchangeRate(sourceCurrency, baseCurrency, date);
		final ExchangeRate secondExchangeRate = exchangeRates.getExchangeRate(baseCurrency, targetCurrency, date);
		return List.of(firstExchangeRate, secondExchangeRate);
	}

	private boolean anyCurrencyIsBase(final Currency sourceCurrency, final Currency targetCurrency) {
		return Arrays.asList(targetCurrency, sourceCurrency).contains(baseCurrency);
	}
}
