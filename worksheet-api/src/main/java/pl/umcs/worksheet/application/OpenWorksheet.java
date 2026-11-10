package pl.umcs.worksheet.application;

import pl.umcs.worksheet.domain.WorksheetId;
import pl.umcs.worksheet.domain.WorksheetKey;

public final class OpenWorksheet {

	public static final class UnknownWorksheetKey extends RuntimeException {
		public UnknownWorksheetKey() {
			super("unknown worksheet key", null, false, false);
		}
	}

	private final Worksheets worksheets;

	public OpenWorksheet(Worksheets worksheets) {
		this.worksheets = worksheets;
	}

	public WorksheetId open(WorksheetKey key) {
		return worksheets.openedBy(key.hash()).orElseThrow(UnknownWorksheetKey::new);
	}
}
