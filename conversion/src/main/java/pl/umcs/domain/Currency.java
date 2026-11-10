package pl.umcs.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
@Getter
public class Currency {
	private final String name;

	@Override
	public String toString() {
		return name;
	}
}
