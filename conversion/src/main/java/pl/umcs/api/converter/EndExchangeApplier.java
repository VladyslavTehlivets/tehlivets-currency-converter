package pl.umcs.api.converter;

import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.Money;

import java.math.BigDecimal;

class EndExchangeApplier extends AbstractExchangeApplier {

	EndExchangeApplier(final ExchangeRate exchangeRate) {
		super(exchangeRate);
	}

	@Override
	Money performExchange(final Money money) {
		final BigDecimal valueAfterExchange = money.getValue().multiply(this.exchangeRate.getExchangeValue());
		final Currency currencyAfterExchange = exchangeRate.getTargetCurrency();

		return new Money(currencyAfterExchange, valueAfterExchange);
	}
}
