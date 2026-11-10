package pl.umcs.worksheet.application;

import pl.umcs.worksheet.domain.DatedRate;
import pl.umcs.worksheet.domain.Direction;
import pl.umcs.worksheet.domain.RateValue;
import pl.umcs.worksheet.domain.RatedAt;
import pl.umcs.worksheet.domain.WorksheetId;

public final class KeepRate {

	private final Worksheets worksheets;

	public KeepRate(Worksheets worksheets) {
		this.worksheets = worksheets;
	}

	public DatedRate keep(WorksheetId worksheet, Direction direction, RateValue value, RatedAt ratedAt) {
		DatedRate rate = DatedRate.newRate(direction, value, ratedAt);
		worksheets.change(worksheet, opened -> opened.keep(rate));
		return rate;
	}
}
