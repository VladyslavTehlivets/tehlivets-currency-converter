package pl.umcs.worksheet;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class RateRequestBuilder {

	private String from = "EUR";
	private String to = "PLN";
	private String rate = "4.30";
	private String ratedAt = "2026-01-10T00:00:00Z";

	private RateRequestBuilder() {
	}

	static RateRequestBuilder aRate() {
		return new RateRequestBuilder();
	}

	RateRequestBuilder from(String currencyCode) {
		this.from = currencyCode;
		return this;
	}

	RateRequestBuilder to(String currencyCode) {
		this.to = currencyCode;
		return this;
	}

	RateRequestBuilder of(String rate) {
		this.rate = rate;
		return this;
	}

	RateRequestBuilder ratedAt(String isoMomentWithOffset) {
		this.ratedAt = isoMomentWithOffset;
		return this;
	}

	ObjectNode json() {
		return JsonNodeFactory.instance.objectNode()
				.put("from", from)
				.put("to", to)
				.put("rate", rate)
				.put("ratedAt", ratedAt);
	}
}
