package pl.umcs.worksheet.application;

import pl.umcs.api.converter.CurrencyConverter;
import pl.umcs.api.converter.ExchangeRates;
import pl.umcs.domain.Conversion;
import pl.umcs.domain.ConversionLeg;
import pl.umcs.domain.ExchangeRate;
import pl.umcs.domain.Money;
import pl.umcs.domain.exception.NotFoundExchangeRateException;
import pl.umcs.worksheet.domain.DatedRate;
import pl.umcs.worksheet.domain.Direction;
import pl.umcs.worksheet.domain.WorksheetId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

public final class ConvertOnWorksheet {

	private static final Currency BASE = Currency.getInstance("PLN");

	private final Worksheets worksheets;

	public ConvertOnWorksheet(Worksheets worksheets) {
		this.worksheets = worksheets;
	}

	public ConversionReceipt convert(WorksheetId worksheet, BigDecimal amount, Direction direction, Instant at) {
		ExchangeRates exchangeRates = new ExchangeRates();
		worksheets.ratesIn(worksheet, directionsThroughTheBase(direction))
				.forEach(rate -> exchangeRates.addExchangeRate(asExchangeRate(rate)));
		CurrencyConverter converter = new CurrencyConverter(inLibraryTerms(BASE), exchangeRates);
		try {
			Money sourceAmount = new Money(inLibraryTerms(direction.source()), amount);
			return receiptOf(converter.explain(sourceAmount, inLibraryTerms(direction.target()), at));
		} catch (NotFoundExchangeRateException missing) {
			throw new RateMissing(inWorksheetTerms(missing.getMissingSource()), inWorksheetTerms(missing.getMissingTarget()), missing.getRequestedMoment());
		}
	}

	private static List<Direction> directionsThroughTheBase(Direction asked) {
		if (asked.source().equals(BASE) || asked.target().equals(BASE)) {
			return List.of(asked);
		}
		return List.of(new Direction(asked.source(), BASE), new Direction(BASE, asked.target()));
	}

	private static ExchangeRate asExchangeRate(DatedRate rate) {
		return ExchangeRate.builder()
				.sourceCurrency(inLibraryTerms(rate.direction().source()))
				.targetCurrency(inLibraryTerms(rate.direction().target()))
				.exchangeValue(rate.value().value())
				.date(rate.ratedAt().value())
				.build();
	}

	private static ConversionReceipt receiptOf(Conversion conversion) {
		List<ConversionReceipt.Leg> legs = conversion.legs().stream().map(ConvertOnWorksheet::receiptLegOf).toList();
		return new ConversionReceipt(
				plain(conversion.sourceAmount().getValue()),
				plain(conversion.result().getValue()),
				BASE.getCurrencyCode(),
				conversion.requestedMoment().toString(),
				legs);
	}

	private static ConversionReceipt.Leg receiptLegOf(ConversionLeg leg) {
		ExchangeRate rate = leg.exchangeRate();
		return new ConversionReceipt.Leg(
				rate.getSourceCurrency().getName(),
				rate.getTargetCurrency().getName(),
				plain(rate.getExchangeValue()),
				rate.getDate().toString(),
				plain(leg.amountIn().getValue()),
				plain(leg.amountOut().getValue()));
	}

	private static String plain(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}

	private static pl.umcs.domain.Currency inLibraryTerms(Currency currency) {
		return new pl.umcs.domain.Currency(currency.getCurrencyCode());
	}

	private static Currency inWorksheetTerms(pl.umcs.domain.Currency currency) {
		return Currency.getInstance(currency.getName());
	}
}
