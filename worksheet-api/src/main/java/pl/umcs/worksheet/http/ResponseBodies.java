package pl.umcs.worksheet.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import pl.umcs.worksheet.domain.DatedRate;

import java.util.List;

final class ResponseBodies {

	private ResponseBodies() {
	}

	record StartedWorksheet(String worksheetKey) {
	}

	record Rate(String id, String from, String to, String rate, String ratedAt) {
		static Rate of(DatedRate rate) {
			return new Rate(
					rate.id().value().toString(),
					rate.direction().source().getCurrencyCode(),
					rate.direction().target().getCurrencyCode(),
					rate.value().toPlainString(),
					rate.ratedAt().value().toString());
		}
	}

	record Rates(List<Rate> rates, int limit) {
	}

	record Health(String status, String revision) {
	}

	record Refusal(Error error) {
		static Refusal of(String code, String field) {
			return new Refusal(new Error(code, field));
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Error(String code, String field) {
	}

	record MissingRateRefusal(MissingRate error) {
	}

	record MissingRate(String code, MissingDirection missing, String at) {
	}

	record MissingDirection(String from, String to) {
	}
}
