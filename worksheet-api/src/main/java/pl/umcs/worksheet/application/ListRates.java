package pl.umcs.worksheet.application;

import pl.umcs.worksheet.domain.DatedRate;
import pl.umcs.worksheet.domain.WorksheetId;

import java.util.List;

public final class ListRates {

	private final Worksheets worksheets;

	public ListRates(Worksheets worksheets) {
		this.worksheets = worksheets;
	}

	public List<DatedRate> list(WorksheetId worksheet) {
		return worksheets.ratesOf(worksheet);
	}
}
