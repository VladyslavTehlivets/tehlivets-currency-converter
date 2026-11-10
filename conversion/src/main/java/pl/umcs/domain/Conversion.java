package pl.umcs.domain;

import java.time.Instant;
import java.util.List;

public record Conversion(Money sourceAmount, Currency targetCurrency, Instant requestedMoment, List<ConversionLeg> legs) {

	public Conversion {
		legs = List.copyOf(legs);
	}

	public Money result() {
		return legs.getLast().amountOut();
	}
}
