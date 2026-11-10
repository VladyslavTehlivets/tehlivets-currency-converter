package pl.umcs.domain;

public record ConversionLeg(ExchangeRate exchangeRate, Money amountIn, Money amountOut) {

	public static ConversionLeg applying(final ExchangeRate exchangeRate, final Money amountIn) {
		final Money amountOut = new Money(exchangeRate.getTargetCurrency(), amountIn.getValue().multiply(exchangeRate.getExchangeValue()));
		return new ConversionLeg(exchangeRate, amountIn, amountOut);
	}
}
