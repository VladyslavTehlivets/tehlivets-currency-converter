package pl.umcs.worksheet.application;

import pl.umcs.worksheet.domain.Worksheet;
import pl.umcs.worksheet.domain.WorksheetKey;

public final class StartWorksheet {

	private final Worksheets worksheets;

	public StartWorksheet(Worksheets worksheets) {
		this.worksheets = worksheets;
	}

	public WorksheetKey start() {
		WorksheetKey key = WorksheetKey.generate();
		worksheets.add(Worksheet.startedWith(key));
		return key;
	}
}
