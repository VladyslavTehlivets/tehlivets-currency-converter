package pl.umcs.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Getter
public class Money {
	private final Currency currency;
	private final BigDecimal value;
}
