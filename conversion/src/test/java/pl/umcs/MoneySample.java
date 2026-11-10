package pl.umcs;

import pl.umcs.domain.Currency;
import pl.umcs.domain.Money;

import java.math.BigDecimal;

class MoneySample {
	static Money generateMoney(final String currency, final int i) {
		return new Money(new Currency(currency), new BigDecimal(i));
	}
}
