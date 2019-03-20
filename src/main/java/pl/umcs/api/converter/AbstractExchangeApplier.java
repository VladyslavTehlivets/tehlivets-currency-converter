package pl.umcs.api.converter;

import lombok.RequiredArgsConstructor;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.Money;

@RequiredArgsConstructor
abstract class AbstractExchangeApplier {

	protected final ExchangeRate exchangeRate;

	abstract Money performExchange(Money money);
}
