package pl.umcs.worksheet;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class ConversionRequestBuilder {

	private String amount = "100";
	private String from = "EUR";
	private String to = "PLN";
	private String at = "2026-01-10T12:00:00+01:00";

	private ConversionRequestBuilder() {
	}

	static ConversionRequestBuilder aConversion() {
		return new ConversionRequestBuilder();
	}

	ConversionRequestBuilder of(String amount) {
		this.amount = amount;
		return this;
	}

	ConversionRequestBuilder from(String currencyCode) {
		this.from = currencyCode;
		return this;
	}

	ConversionRequestBuilder to(String currencyCode) {
		this.to = currencyCode;
		return this;
	}

	ConversionRequestBuilder at(String isoMomentWithOffset) {
		this.at = isoMomentWithOffset;
		return this;
	}

	ObjectNode json() {
		return JsonNodeFactory.instance.objectNode()
				.put("amount", amount)
				.put("from", from)
				.put("to", to)
				.put("at", at);
	}
}
