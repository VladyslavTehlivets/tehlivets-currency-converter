package pl.umcs.worksheet.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pl.umcs.worksheet.domain.DatedRateBuilder.aDatedRate;
import static pl.umcs.worksheet.domain.WorksheetBuilder.aWorksheet;

class WorksheetTest {

	private static final Instant FAR_FROM_EVERY_HELD_RATE = Instant.parse("2090-06-01T00:00:00Z");

	@Test
	void refuses_a_rate_beyond_five_hundred() {
		Worksheet worksheet = aWorksheet().holdingRatesOnSuccessiveHours(499).build();
		worksheet.keep(aDatedRate().ratedAt(FAR_FROM_EVERY_HELD_RATE).build());

		WorksheetRefusal refusal = assertThrows(WorksheetRefusal.class,
				() -> worksheet.keep(aDatedRate().ratedAt(FAR_FROM_EVERY_HELD_RATE.plusSeconds(1)).build()));

		assertEquals(WorksheetRefusal.Reason.WORKSHEET_FULL, refusal.reason());
		assertEquals(500, worksheet.rates().size());
	}

	@Test
	void refuses_a_second_rate_for_the_same_direction_and_moment() {
		DatedRate first = aDatedRate().from("EUR").to("PLN").of("4.30").build();
		Worksheet worksheet = aWorksheet().holding(first).build();

		WorksheetRefusal refusal = assertThrows(WorksheetRefusal.class,
				() -> worksheet.keep(aDatedRate().from("EUR").to("PLN").of("4.10").ratedAt(first.ratedAt().value()).build()));

		assertEquals(WorksheetRefusal.Reason.RATE_EXISTS, refusal.reason());
		assertEquals(1, worksheet.rates().size());
	}

	@Test
	void keeps_the_reverse_direction_at_the_same_moment() {
		DatedRate eurToPln = aDatedRate().from("EUR").to("PLN").build();
		Worksheet worksheet = aWorksheet().holding(eurToPln).build();

		worksheet.keep(aDatedRate().from("PLN").to("EUR").ratedAt(eurToPln.ratedAt().value()).build());

		assertEquals(2, worksheet.rates().size());
	}

	@Test
	void refuses_a_rate_from_a_currency_to_itself() {
		WorksheetRefusal refusal = assertThrows(WorksheetRefusal.class, () -> aDatedRate().from("PLN").to("PLN").build());

		assertEquals(WorksheetRefusal.Reason.SAME_CURRENCY, refusal.reason());
	}

	@ParameterizedTest
	@ValueSource(strings = {"0", "0.000000000000", "-0.01", "-4.30"})
	void refuses_a_rate_that_is_not_greater_than_zero(String notPositive) {
		WorksheetRefusal refusal = assertThrows(WorksheetRefusal.class, () -> aDatedRate().of(notPositive).build());

		assertEquals(WorksheetRefusal.Reason.INVALID_RATE, refusal.reason());
	}

	@Test
	void removing_an_unknown_rate_is_refused() {
		Worksheet worksheet = aWorksheet().holding(aDatedRate().build()).build();

		WorksheetRefusal refusal = assertThrows(WorksheetRefusal.class, () -> worksheet.remove(RateId.generate()));

		assertEquals(WorksheetRefusal.Reason.RATE_NOT_FOUND, refusal.reason());
		assertEquals(1, worksheet.rates().size());
	}
}
