package pl.umcs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.umcs.api.converter.CurrencyConverter;
import pl.umcs.api.converter.ExchangeRates;
import pl.umcs.domain.Conversion;
import pl.umcs.domain.ConversionLeg;
import pl.umcs.domain.Currency;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.Money;
import pl.umcs.domain.exception.NotFoundExchangeRateException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.umcs.ExchangeRateBuilder.aRate;

class NearestRateConversionTest {

	private static final Currency PLN = new Currency("PLN");
	private static final Currency EUR = new Currency("EUR");
	private static final Instant REQUESTED = Instant.parse("2026-01-10T12:00:00Z");

	private ExchangeRates exchangeRates;
	private CurrencyConverter converter;

	@BeforeEach
	void setUp() {
		exchangeRates = new ExchangeRates();
		converter = new CurrencyConverter(PLN, exchangeRates);
	}

	@Test
	void should_select_the_past_rate_when_it_is_nearer_than_a_future_rate() {
		final ExchangeRate past = keep(aRate().of("4").ratedAt("2026-01-10T10:00:00Z"));
		keep(aRate().of("5").ratedAt("2026-01-10T15:00:00Z"));

		assertSame(past, onlyLegOf(converter.explain(dollars("1"), PLN, REQUESTED)).exchangeRate());
	}

	@Test
	void should_select_the_future_rate_when_it_is_nearer_than_a_past_rate() {
		keep(aRate().of("4").ratedAt("2026-01-09T12:00:00Z"));
		final ExchangeRate future = keep(aRate().of("5").ratedAt("2026-01-10T13:00:00Z"));

		assertSame(future, onlyLegOf(converter.explain(dollars("1"), PLN, REQUESTED)).exchangeRate());
	}

	@Test
	void should_prefer_the_earlier_rate_when_two_rates_are_equally_near() {
		keep(aRate().of("5").ratedAt("2026-01-10T13:00:00Z"));
		final ExchangeRate earlier = keep(aRate().of("4").ratedAt("2026-01-10T11:00:00Z"));

		assertSame(earlier, onlyLegOf(converter.explain(dollars("1"), PLN, REQUESTED)).exchangeRate());
	}

	@Test
	void should_select_a_rate_centuries_away_without_overflow() {
		keep(aRate().of("3").ratedAt("1500-01-01T00:00:00Z"));
		final ExchangeRate nearer = keep(aRate().of("4").ratedAt("1700-01-01T00:00:00Z"));

		assertSame(nearer, onlyLegOf(converter.explain(dollars("1"), PLN, REQUESTED)).exchangeRate());
	}

	@Test
	void should_explain_a_direct_conversion_as_one_leg() {
		final ExchangeRate dollarToZloty = keep(aRate().from("USD").to("PLN").of("4.05"));

		final Conversion conversion = converter.explain(dollars("12"), PLN, REQUESTED);

		final ConversionLeg leg = onlyLegOf(conversion);
		assertSame(dollarToZloty, leg.exchangeRate());
		assertEquals(new BigDecimal("12"), leg.amountIn().getValue());
		assertEquals(new BigDecimal("48.60"), leg.amountOut().getValue());
		assertEquals(PLN, conversion.result().getCurrency());
		assertEquals(REQUESTED, conversion.requestedMoment());
	}

	@Test
	void should_explain_a_cross_conversion_as_two_legs_through_pln() {
		final ExchangeRate dollarToZloty = keep(aRate().from("USD").to("PLN").of("4"));
		final ExchangeRate zlotyToEuro = keep(aRate().from("PLN").to("EUR").of("0.25"));
		keep(aRate().from("USD").to("EUR").of("0.9"));

		final Conversion conversion = converter.explain(dollars("10"), EUR, REQUESTED);

		assertEquals(2, conversion.legs().size());
		final ConversionLeg intoZloty = conversion.legs().get(0);
		final ConversionLeg intoEuro = conversion.legs().get(1);
		assertSame(dollarToZloty, intoZloty.exchangeRate());
		assertSame(zlotyToEuro, intoEuro.exchangeRate());
		assertEquals(PLN, intoZloty.amountOut().getCurrency());
		assertSame(intoZloty.amountOut(), intoEuro.amountIn());
		assertEquals(EUR, conversion.result().getCurrency());
		assertEquals(new BigDecimal("10.00"), conversion.result().getValue());
	}

	@Test
	void should_keep_the_exact_decimal_product_across_two_legs() {
		keep(aRate().from("USD").to("PLN").of("4.123456789"));
		keep(aRate().from("PLN").to("EUR").of("0.2345678912"));

		final Money result = converter.convert(dollars("12.5"), EUR, REQUESTED);

		assertEquals(new BigDecimal("12.09038204312566696000"), result.getValue());
	}

	@Test
	void should_not_use_the_reverse_direction_of_a_rate() {
		keep(aRate().from("PLN").to("USD").of("0.25"));

		assertThrows(NotFoundExchangeRateException.class, () -> converter.explain(dollars("1"), PLN, REQUESTED));
	}

	@Test
	void should_name_the_missing_direction_and_moment() {
		keep(aRate().from("USD").to("PLN").of("4"));

		final NotFoundExchangeRateException missing = assertThrows(NotFoundExchangeRateException.class,
				() -> converter.explain(dollars("1"), EUR, REQUESTED));

		assertEquals(PLN, missing.getMissingSource());
		assertEquals(EUR, missing.getMissingTarget());
		assertEquals(REQUESTED, missing.getRequestedMoment());
	}

	private ExchangeRate keep(final ExchangeRateBuilder rate) {
		final ExchangeRate exchangeRate = rate.build();
		exchangeRates.addExchangeRate(exchangeRate);
		return exchangeRate;
	}

	private static Money dollars(final String amount) {
		return new Money(new Currency("USD"), new BigDecimal(amount));
	}

	private static ConversionLeg onlyLegOf(final Conversion conversion) {
		assertEquals(1, conversion.legs().size());
		return conversion.legs().getFirst();
	}
}
