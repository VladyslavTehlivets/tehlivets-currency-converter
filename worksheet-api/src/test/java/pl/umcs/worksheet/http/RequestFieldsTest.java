package pl.umcs.worksheet.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestFieldsTest {

	@ParameterizedTest
	@ValueSource(strings = {"1", "0.000000000001", "999999999999999.999999999999", "4.30"})
	void accepts_a_plain_positive_decimal_with_up_to_fifteen_digits_and_twelve_decimals(String plainDecimal) {
		BigDecimal value = RequestFields.of(bodyWithAmount("\"" + plainDecimal + "\"")).positiveDecimal("amount", "invalid_amount");

		assertEquals(new BigDecimal(plainDecimal), value);
	}

	@ParameterizedTest
	@ValueSource(strings = {"100,50", "1e3", "-1", "+1", " 1", "1.", ".5", "0", "0.000",
			"1.0000000000001", "1000000000000000", "", "1 000"})
	void refuses_a_decimal_that_is_not_plain_and_positive(String refused) {
		FieldRefusal refusal = assertThrows(FieldRefusal.class,
				() -> RequestFields.of(bodyWithAmount("\"" + refused + "\"")).positiveDecimal("amount", "invalid_amount"));

		assertEquals("invalid_amount", refusal.code());
		assertEquals("amount", refusal.field());
	}

	@ParameterizedTest
	@ValueSource(strings = {"100", "100.5", "null", "true", "[\"100\"]"})
	void refuses_a_decimal_that_is_not_a_json_string(String notAString) {
		FieldRefusal refusal = assertThrows(FieldRefusal.class,
				() -> RequestFields.of(bodyWithAmount(notAString)).positiveDecimal("amount", "invalid_amount"));

		assertEquals("invalid_amount", refusal.code());
	}

	@Test
	void refuses_a_missing_field_beside_that_field() {
		FieldRefusal refusal = assertThrows(FieldRefusal.class, () -> RequestFields.of("{}").positiveDecimal("rate", "invalid_rate"));

		assertEquals("invalid_rate", refusal.code());
		assertEquals("rate", refusal.field());
	}

	@Test
	void reads_an_iso_currency_code() {
		assertEquals(Currency.getInstance("EUR"), RequestFields.of("{\"from\":\"EUR\"}").currency("from"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"eur", "EURO", "ABC", ""})
	void refuses_a_code_that_is_not_an_iso_currency(String notACurrency) {
		FieldRefusal refusal = assertThrows(FieldRefusal.class,
				() -> RequestFields.of("{\"to\":\"" + notACurrency + "\"}").currency("to"));

		assertEquals("invalid_currency", refusal.code());
		assertEquals("to", refusal.field());
	}

	@Test
	void reads_a_moment_with_its_offset_as_an_instant() {
		Instant moment = RequestFields.of("{\"at\":\"2026-01-10T12:00:00+01:00\"}").moment("at");

		assertEquals(Instant.parse("2026-01-10T11:00:00Z"), moment);
	}

	@ParameterizedTest
	@ValueSource(strings = {"2026-01-10T12:00:00", "2026-01-10", "10/01/2026 12:00", "yesterday"})
	void refuses_a_moment_without_an_iso_offset(String notIsoWithOffset) {
		FieldRefusal refusal = assertThrows(FieldRefusal.class,
				() -> RequestFields.of("{\"at\":\"" + notIsoWithOffset + "\"}").moment("at"));

		assertEquals("invalid_moment", refusal.code());
		assertEquals("at", refusal.field());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "not json", "[]", "\"amount\"", "null", "{\"amount\":"})
	void refuses_a_body_that_is_not_a_json_object(String notAnObject) {
		FieldRefusal refusal = assertThrows(FieldRefusal.class, () -> RequestFields.of(notAnObject));

		assertEquals("invalid_body", refusal.code());
		assertNull(refusal.field());
	}

	private static String bodyWithAmount(String jsonValue) {
		return "{\"amount\":" + jsonValue + "}";
	}
}
