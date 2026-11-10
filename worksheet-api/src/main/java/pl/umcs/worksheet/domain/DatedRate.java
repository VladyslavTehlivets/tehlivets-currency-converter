package pl.umcs.worksheet.domain;

import java.util.Objects;

public record DatedRate(RateId id, Direction direction, RateValue value, RatedAt ratedAt) {

	public DatedRate {
		Objects.requireNonNull(id);
		Objects.requireNonNull(direction);
		Objects.requireNonNull(value);
		Objects.requireNonNull(ratedAt);
	}

	public static DatedRate newRate(Direction direction, RateValue value, RatedAt ratedAt) {
		return new DatedRate(RateId.generate(), direction, value, ratedAt);
	}

	boolean sharesDirectionAndMomentWith(DatedRate other) {
		return direction.equals(other.direction) && ratedAt.equals(other.ratedAt);
	}
}
