package pl.umcs.worksheet.application;

import java.time.Instant;
import java.util.Currency;

public final class RateMissing extends RuntimeException {

	private final Currency from;
	private final Currency to;
	private final Instant at;

	RateMissing(Currency from, Currency to, Instant at) {
		super("rate missing from " + from + " to " + to + " at " + at, null, false, false);
		this.from = from;
		this.to = to;
		this.at = at;
	}

	public Currency from() {
		return from;
	}

	public Currency to() {
		return to;
	}

	public Instant at() {
		return at;
	}
}
