package pl.umcs.worksheet.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.regex.Pattern;

final class RequestFields {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Pattern PLAIN_DECIMAL = Pattern.compile("^[0-9]{1,15}(\\.[0-9]{1,12})?$");

	private final JsonNode fields;

	private RequestFields(JsonNode fields) {
		this.fields = fields;
	}

	static RequestFields of(String body) {
		try {
			JsonNode fields = JSON.readTree(body);
			if (fields == null || !fields.isObject()) {
				throw new FieldRefusal("invalid_body", null);
			}
			return new RequestFields(fields);
		} catch (JsonProcessingException malformed) {
			throw new FieldRefusal("invalid_body", null);
		}
	}

	BigDecimal positiveDecimal(String field, String refusalCode) {
		String text = text(field, refusalCode);
		if (!PLAIN_DECIMAL.matcher(text).matches()) {
			throw new FieldRefusal(refusalCode, field);
		}
		BigDecimal value = new BigDecimal(text);
		if (value.signum() <= 0) {
			throw new FieldRefusal(refusalCode, field);
		}
		return value;
	}

	Currency currency(String field) {
		try {
			return Currency.getInstance(text(field, "invalid_currency"));
		} catch (IllegalArgumentException unknownCode) {
			throw new FieldRefusal("invalid_currency", field);
		}
	}

	Instant moment(String field) {
		try {
			return OffsetDateTime.parse(text(field, "invalid_moment")).toInstant();
		} catch (DateTimeParseException notIsoWithOffset) {
			throw new FieldRefusal("invalid_moment", field);
		}
	}

	private String text(String field, String refusalCode) {
		JsonNode value = fields.get(field);
		if (value == null || !value.isTextual()) {
			throw new FieldRefusal(refusalCode, field);
		}
		return value.textValue();
	}
}
