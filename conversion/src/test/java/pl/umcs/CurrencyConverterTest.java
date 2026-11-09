package pl.umcs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.umcs.api.converter.CurrencyConverter;
import pl.umcs.api.converter.ExchangeRates;
import pl.umcs.domain.Currency;
import pl.umcs.domain.Money;
import pl.umcs.domain.exception.NotFoundExchangeRateException;

import java.math.BigDecimal;
import java.time.Instant;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.umcs.CurrencySample.generateCurrency;
import static pl.umcs.ExchangeRateSample.generateExchangeRate;
import static pl.umcs.MoneySample.generateMoney;

class CurrencyConverterTest {

	private CurrencyConverter currencyConverter;
	private ExchangeRates exchangeRates;

	@BeforeEach
	void setUp() {
		final Currency baseCurrency = new Currency("PLN");
		exchangeRates = new ExchangeRates();
		currencyConverter = new CurrencyConverter(baseCurrency, exchangeRates);
	}

	@Test
	void should_convert_from_usd_to_pln() {
		//given
		exchangeRates.addExchangeRate(generateExchangeRate("USD", "PLN", 0.123, now()));

		//when
		final Money converted = currencyConverter.convert(generateMoney("USD", 12), generateCurrency("PLN"), now());

		//then
		assertEquals(converted.getCurrency(), generateCurrency("PLN"));
		assertEquals(converted.getValue(), new BigDecimal(12).multiply(new BigDecimal(0.123)));
	}

	@Test
	void should_not_convert_from_usd_to_pln_if_there_is_no_exchange_rate() {
		//given
		exchangeRates.addExchangeRate(generateExchangeRate("PLN", "USD", 0.123, now()));

		//expect
		assertThrows(NotFoundExchangeRateException.class, () -> currencyConverter.convert(generateMoney("USD", 12), generateCurrency("PLN"), now()));
	}

	@Test
	void should_convert_from_usd_to_eur_with_double_conversion() {
		//given
		exchangeRates.addExchangeRate(generateExchangeRate("USD", "PLN", 0.123, now()));
		exchangeRates.addExchangeRate(generateExchangeRate("PLN", "EUR", 0.123, now()));
		exchangeRates.addExchangeRate(generateExchangeRate("USD", "EUR", 0.123, now()));

		//when
		final Money converted = currencyConverter.convert(generateMoney("USD", 12), generateCurrency("EUR"), now());

		//then
		assertEquals(converted.getCurrency(), generateCurrency("EUR"));
		assertEquals(converted.getValue(), new BigDecimal(12).multiply(new BigDecimal(0.123).multiply(new BigDecimal(0.123))));
	}

	@Test
	void should_convert_from_usd_to_pln_with_the_nearest_date_exchange_rate() {
		//given
		exchangeRates.addExchangeRate(generateExchangeRate("USD", "PLN", 2, Instant.ofEpochSecond(10)));
		exchangeRates.addExchangeRate(generateExchangeRate("USD", "PLN", 6, Instant.ofEpochSecond(10000)));
		exchangeRates.addExchangeRate(generateExchangeRate("USD", "PLN", 4, Instant.ofEpochSecond(1000)));

		//when
		final Money converted = currencyConverter.convert(generateMoney("USD", 12), generateCurrency("PLN"), now());

		//then
		assertEquals(converted.getCurrency(), generateCurrency("PLN"));
		assertEquals(converted.getValue(), new BigDecimal(12).multiply(new BigDecimal(6)));
	}
}