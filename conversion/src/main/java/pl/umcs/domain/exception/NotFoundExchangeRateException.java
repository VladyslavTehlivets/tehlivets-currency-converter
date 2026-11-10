package pl.umcs.domain.exception;

import lombok.Getter;
import pl.umcs.domain.Currency;

import java.time.Instant;

@Getter
public class NotFoundExchangeRateException extends RuntimeException {

	private final Currency missingSource;
	private final Currency missingTarget;
	private final Instant requestedMoment;

	public NotFoundExchangeRateException(final Currency missingSource, final Currency missingTarget, final Instant requestedMoment) {
		super("No exchange rate from " + missingSource + " to " + missingTarget + " for " + requestedMoment);
		this.missingSource = missingSource;
		this.missingTarget = missingTarget;
		this.requestedMoment = requestedMoment;
	}
}
