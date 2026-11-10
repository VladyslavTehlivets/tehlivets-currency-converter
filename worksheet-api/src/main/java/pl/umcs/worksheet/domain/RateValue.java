package pl.umcs.worksheet.domain;

import java.math.BigDecimal;

public record RateValue(BigDecimal value) {

	public RateValue {
		if (value == null || value.signum() <= 0) {
			throw new WorksheetRefusal(WorksheetRefusal.Reason.INVALID_RATE);
		}
	}

	public String toPlainString() {
		return value.stripTrailingZeros().toPlainString();
	}
}
