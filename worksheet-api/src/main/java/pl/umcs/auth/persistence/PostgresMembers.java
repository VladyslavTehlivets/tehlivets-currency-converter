package pl.umcs.auth.persistence;

import pl.umcs.auth.application.Members;
import pl.umcs.auth.domain.MemberId;
import pl.umcs.auth.domain.Provider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class PostgresMembers implements Members {

	private static final String UNIQUE_VIOLATION = "23505";

	private final DataSource database;

	public PostgresMembers(DataSource database) {
		this.database = database;
	}

	@Override
	public Optional<MemberId> findByIdentity(Provider provider, String subject) {
		return memberIdOf("select member_id from provider_identities where provider = ? and subject = ?", provider.name(), subject);
	}

	@Override
	public Optional<MemberId> findByVerifiedEmail(String email) {
		return memberIdOf("select id from members where verified_email = ?", email);
	}

	@Override
	public MemberId createWithIdentity(String verifiedEmail, Provider provider, String subject) {
		MemberId member = new MemberId(UUID.randomUUID());
		try (Connection connection = database.getConnection()) {
			connection.setAutoCommit(false);
			try {
				insertMember(connection, member, verifiedEmail);
				insertIdentity(connection, member, provider, subject);
				connection.commit();
				return member;
			} catch (SQLException | RuntimeException failure) {
				connection.rollback();
				throw failure;
			}
		} catch (SQLException failure) {
			throw conflictOrFailure(failure);
		}
	}

	@Override
	public void link(MemberId member, Provider provider, String subject) {
		try (Connection connection = database.getConnection()) {
			insertIdentity(connection, member, provider, subject);
		} catch (SQLException failure) {
			throw conflictOrFailure(failure);
		}
	}

	private Optional<MemberId> memberIdOf(String sql, String... parameters) {
		try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int position = 0; position < parameters.length; position++) {
				statement.setString(position + 1, parameters[position]);
			}
			try (ResultSet rows = statement.executeQuery()) {
				return rows.next() ? Optional.of(new MemberId(rows.getObject(1, UUID.class))) : Optional.empty();
			}
		} catch (SQLException failure) {
			throw new IllegalStateException("members unreadable", failure);
		}
	}

	private static void insertMember(Connection connection, MemberId member, String email) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("insert into members (id, verified_email) values (?, ?)")) {
			statement.setObject(1, member.value());
			statement.setString(2, email);
			statement.executeUpdate();
		}
	}

	private static void insertIdentity(Connection connection, MemberId member, Provider provider, String subject) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("insert into provider_identities (provider, subject, member_id) values (?, ?, ?)")) {
			statement.setString(1, provider.name());
			statement.setString(2, subject);
			statement.setObject(3, member.value());
			statement.executeUpdate();
		}
	}

	private static RuntimeException conflictOrFailure(SQLException failure) {
		return UNIQUE_VIOLATION.equals(failure.getSQLState()) ? new IdentityConflict() : new IllegalStateException("members unwritable", failure);
	}
}
