package pl.umcs;

import pl.umcs.domain.Currency;

class CurrencySample {
	static Currency generateCurrency(final String currencyName) {
		return new Currency(currencyName);
	}
}
