package pl.umcs.worksheet.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Worksheet {

	public static final int RATE_LIMIT = 500;

	private final WorksheetId id;
	private final byte[] keyHash;
	private final List<DatedRate> rates;

	public Worksheet(WorksheetId id, byte[] keyHash, List<DatedRate> rates) {
		this.id = Objects.requireNonNull(id);
		this.keyHash = keyHash.clone();
		this.rates = new ArrayList<>(rates);
	}

	public static Worksheet startedWith(WorksheetKey key) {
		return new Worksheet(WorksheetId.generate(), key.hash(), List.of());
	}

	public void keep(DatedRate rate) {
		if (rates.size() >= RATE_LIMIT) {
			throw new WorksheetRefusal(WorksheetRefusal.Reason.WORKSHEET_FULL);
		}
		if (rates.stream().anyMatch(rate::sharesDirectionAndMomentWith)) {
			throw new WorksheetRefusal(WorksheetRefusal.Reason.RATE_EXISTS);
		}
		rates.add(rate);
	}

	public void remove(RateId rateId) {
		if (!rates.removeIf(rate -> rate.id().equals(rateId))) {
			throw new WorksheetRefusal(WorksheetRefusal.Reason.RATE_NOT_FOUND);
		}
	}

	public WorksheetId id() {
		return id;
	}

	public byte[] keyHash() {
		return keyHash.clone();
	}

	public List<DatedRate> rates() {
		return List.copyOf(rates);
	}
}
