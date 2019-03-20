package pl.umcs.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.umcs.domain.Currency;

import java.math.BigDecimal;
import java.time.Instant;

@RequiredArgsConstructor
@Getter
@Builder
public class ExchangeRate {
	private final Currency sourceCurrency;
	private final Currency targetCurrency;
	private final BigDecimal exchangeValue;
	private final Instant date;
}
