package pl.umcs.auth.persistence;

import pl.umcs.auth.application.Members;
import pl.umcs.auth.domain.Member;
import pl.umcs.auth.domain.Provider;
import pl.umcs.auth.domain.ProviderIdentity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PostgresMembers implements Members {

	private final DataSource dataSource;

	public PostgresMembers(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Optional<Member> findByIdentity(Provider provider, String subject) {
		return findOne("select member_id from provider_identities where provider = ? and subject = ?", provider.slug(), subject);
	}

	@Override
	public Optional<Member> findByVerifiedEmail(String email) {
		return findOne("select id from members where verified_email = ? order by created_at limit 1", email);
	}

	@Override
	public void save(Member member) {
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			try {
				upsertMember(connection, member);
				for (ProviderIdentity identity : member.identities()) {
					insertIdentity(connection, member.id(), identity);
				}
				connection.commit();
			} catch (SQLException | RuntimeException failure) {
				connection.rollback();
				throw failure;
			}
		} catch (SQLException failure) {
			throw new IllegalStateException(failure);
		}
	}

	private static void upsertMember(Connection connection, Member member) throws SQLException {
		try (PreparedStatement insert = connection.prepareStatement("insert into members (id, verified_email, created_at) values (?, ?, ?) on conflict (id) do nothing")) {
			insert.setObject(1, member.id());
			insert.setString(2, member.verifiedEmail());
			insert.setTimestamp(3, Timestamp.from(member.createdAt()));
			insert.executeUpdate();
		}
	}

	private static void insertIdentity(Connection connection, UUID memberId, ProviderIdentity identity) throws SQLException {
		try (PreparedStatement insert = connection.prepareStatement("insert into provider_identities (provider, subject, member_id, verified_email, linked_at) values (?, ?, ?, ?, ?) on conflict (provider, subject) do nothing")) {
			insert.setString(1, identity.provider().slug());
			insert.setString(2, identity.subject());
			insert.setObject(3, memberId);
			insert.setString(4, identity.verifiedEmail());
			insert.setTimestamp(5, Timestamp.from(identity.linkedAt()));
			insert.executeUpdate();
		}
	}

	private Optional<Member> findOne(String idQuery, String... arguments) {
		try (Connection connection = dataSource.getConnection(); PreparedStatement select = connection.prepareStatement(idQuery)) {
			for (int index = 0; index < arguments.length; index++) {
				select.setString(index + 1, arguments[index]);
			}
			try (ResultSet found = select.executeQuery()) {
				return found.next() ? Optional.of(load(connection, found.getObject(1, UUID.class))) : Optional.empty();
			}
		} catch (SQLException failure) {
			throw new IllegalStateException(failure);
		}
	}

	private static Member load(Connection connection, UUID id) throws SQLException {
		List<ProviderIdentity> identities = new ArrayList<>();
		try (PreparedStatement select = connection.prepareStatement("select provider, subject, verified_email, linked_at from provider_identities where member_id = ?")) {
			select.setObject(1, id);
			try (ResultSet rows = select.executeQuery()) {
				while (rows.next()) {
					identities.add(new ProviderIdentity(Provider.valueOf(rows.getString(1).toUpperCase(java.util.Locale.ROOT)), rows.getString(2), rows.getString(3), rows.getTimestamp(4).toInstant()));
				}
			}
		}
		try (PreparedStatement select = connection.prepareStatement("select verified_email, created_at from members where id = ?")) {
			select.setObject(1, id);
			try (ResultSet row = select.executeQuery()) {
				row.next();
				return new Member(id, row.getString(1), row.getTimestamp(2).toInstant(), identities);
			}
		}
	}
}
