package pl.umcs.worksheet.application;

import java.util.List;

public record ConversionReceipt(String amount, String result, String base, String at, List<Leg> legs) {

	public record Leg(String from, String to, String rate, String ratedAt, String amountIn, String amountOut) {
	}

	public ConversionReceipt {
		legs = List.copyOf(legs);
	}
}
