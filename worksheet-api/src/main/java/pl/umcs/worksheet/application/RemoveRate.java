package pl.umcs.worksheet.application;

import pl.umcs.worksheet.domain.RateId;
import pl.umcs.worksheet.domain.WorksheetId;

public final class RemoveRate {

	private final Worksheets worksheets;

	public RemoveRate(Worksheets worksheets) {
		this.worksheets = worksheets;
	}

	public void remove(WorksheetId worksheet, RateId rate) {
		worksheets.change(worksheet, opened -> opened.remove(rate));
	}
}
