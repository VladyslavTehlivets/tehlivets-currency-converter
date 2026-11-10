package pl.umcs.worksheet.persistence;

import pl.umcs.worksheet.application.Worksheets;
import pl.umcs.worksheet.domain.DatedRate;
import pl.umcs.worksheet.domain.Direction;
import pl.umcs.worksheet.domain.RateId;
import pl.umcs.worksheet.domain.RateValue;
import pl.umcs.worksheet.domain.RatedAt;
import pl.umcs.worksheet.domain.Worksheet;
import pl.umcs.worksheet.domain.WorksheetId;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class PostgresWorksheets implements Worksheets {

	private static final String RATE_COLUMNS = "select id, source_currency, target_currency, rate, rated_at from dated_rates where worksheet_id = ?";
	private static final String RATE_ORDER = " order by source_currency, target_currency, rated_at";

	public static final class WorksheetStoreFailure extends RuntimeException {
		WorksheetStoreFailure(SQLException cause) {
			super(cause);
		}
	}

	@FunctionalInterface
	private interface SqlWork<T> {
		T run(Connection connection) throws SQLException;
	}

	private final DataSource dataSource;

	public PostgresWorksheets(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void add(Worksheet worksheet) {
		withConnection(connection -> {
			try (PreparedStatement insert = connection.prepareStatement("insert into worksheets (id, key_hash) values (?, ?)")) {
				insert.setObject(1, worksheet.id().value());
				insert.setBytes(2, worksheet.keyHash());
				insert.executeUpdate();
			}
			insertRates(connection, worksheet.id(), worksheet.rates());
			return null;
		});
	}

	@Override
	public Optional<WorksheetId> openedBy(byte[] keyHash) {
		return withConnection(connection -> {
			try (PreparedStatement select = connection.prepareStatement("select id from worksheets where key_hash = ?")) {
				select.setBytes(1, keyHash);
				try (ResultSet row = select.executeQuery()) {
					return row.next() ? Optional.of(new WorksheetId(row.getObject("id", UUID.class))) : Optional.empty();
				}
			}
		});
	}

	@Override
	public List<DatedRate> ratesOf(WorksheetId worksheet) {
		return withConnection(connection -> ratesOf(connection, worksheet));
	}

	@Override
	public List<DatedRate> ratesIn(WorksheetId worksheet, Collection<Direction> directions) {
		if (directions.isEmpty()) {
			return List.of();
		}
		String directionPairs = String.join(", ", Collections.nCopies(directions.size(), "(?, ?)"));
		String sql = RATE_COLUMNS + " and (source_currency, target_currency) in (" + directionPairs + ")" + RATE_ORDER;
		return withConnection(connection -> {
			try (PreparedStatement select = connection.prepareStatement(sql)) {
				int parameter = 1;
				select.setObject(parameter++, worksheet.value());
				for (Direction direction : directions) {
					select.setString(parameter++, direction.source().getCurrencyCode());
					select.setString(parameter++, direction.target().getCurrencyCode());
				}
				return ratesFrom(select);
			}
		});
	}

	@Override
	public void change(WorksheetId worksheetId, Consumer<Worksheet> change) {
		withConnection(connection -> {
			connection.setAutoCommit(false);
			try {
				Worksheet worksheet = lockedForChange(connection, worksheetId);
				Set<RateId> keptBefore = idsOf(worksheet.rates());
				change.accept(worksheet);
				List<DatedRate> keptAfter = worksheet.rates();
				Set<RateId> stillKept = idsOf(keptAfter);
				insertRates(connection, worksheetId, keptAfter.stream().filter(rate -> !keptBefore.contains(rate.id())).toList());
				deleteRates(connection, worksheetId, keptBefore.stream().filter(id -> !stillKept.contains(id)).toList());
				connection.commit();
				return null;
			} catch (SQLException | RuntimeException failure) {
				connection.rollback();
				throw failure;
			}
		});
	}

	private Worksheet lockedForChange(Connection connection, WorksheetId worksheetId) throws SQLException {
		try (PreparedStatement lock = connection.prepareStatement("select key_hash from worksheets where id = ? for update")) {
			lock.setObject(1, worksheetId.value());
			try (ResultSet row = lock.executeQuery()) {
				if (!row.next()) {
					throw new IllegalStateException("worksheet " + worksheetId.value() + " does not exist");
				}
				return new Worksheet(worksheetId, row.getBytes("key_hash"), ratesOf(connection, worksheetId));
			}
		}
	}

	private static List<DatedRate> ratesOf(Connection connection, WorksheetId worksheet) throws SQLException {
		try (PreparedStatement select = connection.prepareStatement(RATE_COLUMNS + RATE_ORDER)) {
			select.setObject(1, worksheet.value());
			return ratesFrom(select);
		}
	}

	private static List<DatedRate> ratesFrom(PreparedStatement select) throws SQLException {
		List<DatedRate> rates = new ArrayList<>();
		try (ResultSet row = select.executeQuery()) {
			while (row.next()) {
				rates.add(new DatedRate(
						new RateId(row.getObject("id", UUID.class)),
						new Direction(Currency.getInstance(row.getString("source_currency")), Currency.getInstance(row.getString("target_currency"))),
						new RateValue(row.getBigDecimal("rate")),
						new RatedAt(row.getObject("rated_at", OffsetDateTime.class).toInstant())));
			}
		}
		return rates;
	}

	private static void insertRates(Connection connection, WorksheetId worksheet, List<DatedRate> rates) throws SQLException {
		if (rates.isEmpty()) {
			return;
		}
		String sql = "insert into dated_rates (id, worksheet_id, source_currency, target_currency, rate, rated_at) values (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement insert = connection.prepareStatement(sql)) {
			for (DatedRate rate : rates) {
				insert.setObject(1, rate.id().value());
				insert.setObject(2, worksheet.value());
				insert.setString(3, rate.direction().source().getCurrencyCode());
				insert.setString(4, rate.direction().target().getCurrencyCode());
				insert.setBigDecimal(5, rate.value().value());
				insert.setObject(6, OffsetDateTime.ofInstant(rate.ratedAt().value(), ZoneOffset.UTC));
				insert.addBatch();
			}
			insert.executeBatch();
		}
	}

	private static void deleteRates(Connection connection, WorksheetId worksheet, List<RateId> rateIds) throws SQLException {
		if (rateIds.isEmpty()) {
			return;
		}
		try (PreparedStatement delete = connection.prepareStatement("delete from dated_rates where worksheet_id = ? and id = ?")) {
			for (RateId rateId : rateIds) {
				delete.setObject(1, worksheet.value());
				delete.setObject(2, rateId.value());
				delete.addBatch();
			}
			delete.executeBatch();
		}
	}

	private static Set<RateId> idsOf(List<DatedRate> rates) {
		return rates.stream().map(DatedRate::id).collect(Collectors.toSet());
	}

	private <T> T withConnection(SqlWork<T> work) {
		try (Connection connection = dataSource.getConnection()) {
			return work.run(connection);
		} catch (SQLException failure) {
			throw new WorksheetStoreFailure(failure);
		}
	}
}
