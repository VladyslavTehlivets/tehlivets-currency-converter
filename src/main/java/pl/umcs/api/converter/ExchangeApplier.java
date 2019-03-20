package pl.umcs.api.converter;

import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.Money;

import java.math.BigDecimal;

class ExchangeApplier extends AbstractExchangeApplier {

	private final AbstractExchangeApplier nextExchangeApplier;

	ExchangeApplier(final ExchangeRate exchangeRate, final AbstractExchangeApplier nextExchangeApplier) {
		super(exchangeRate);
		this.nextExchangeApplier = nextExchangeApplier;
	}

	@Override
	Money performExchange(final Money money) {
		final BigDecimal valueAfterExchange = money.getValue().multiply(this.exchangeRate.getExchangeValue());
		final Currency currencyAfterExchange = exchangeRate.getTargetCurrency();

		final Money moneyAfterExchange = new Money(currencyAfterExchange, valueAfterExchange);
		return nextExchangeApplier.performExchange(moneyAfterExchange);
	}
}
