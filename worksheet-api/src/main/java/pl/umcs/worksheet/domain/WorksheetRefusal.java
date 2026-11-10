package pl.umcs.worksheet.domain;

public final class WorksheetRefusal extends RuntimeException {

	public enum Reason {
		WORKSHEET_FULL,
		RATE_EXISTS,
		RATE_NOT_FOUND,
		SAME_CURRENCY,
		INVALID_RATE,
		INVALID_MOMENT
	}

	private final Reason reason;

	public WorksheetRefusal(Reason reason) {
		super(reason.name(), null, false, false);
		this.reason = reason;
	}

	public Reason reason() {
		return reason;
	}
}
