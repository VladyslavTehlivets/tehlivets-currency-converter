package pl.umcs.worksheet.domain;

import java.util.Currency;
import java.util.Objects;

public record Direction(Currency source, Currency target) {

	public Direction {
		Objects.requireNonNull(source);
		Objects.requireNonNull(target);
		if (source.equals(target)) {
			throw new WorksheetRefusal(WorksheetRefusal.Reason.SAME_CURRENCY);
		}
	}
}
