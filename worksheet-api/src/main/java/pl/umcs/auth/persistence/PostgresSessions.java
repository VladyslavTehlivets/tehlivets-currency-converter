package pl.umcs.auth.persistence;

import pl.umcs.auth.application.Sessions;
import pl.umcs.auth.domain.MemberId;
import pl.umcs.auth.domain.Session;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class PostgresSessions implements Sessions {

	public static final Duration LIFETIME = Duration.ofDays(30);

	private final DataSource database;

	public PostgresSessions(DataSource database) {
		this.database = database;
	}

	@Override
	public Session issue(MemberId member, Instant now) {
		Session session = new Session(UUID.randomUUID(), member, now, now.plus(LIFETIME));
		try (Connection connection = database.getConnection();
		     PreparedStatement statement = connection.prepareStatement("insert into sessions (id, member_id, issued_at, expires_at) values (?, ?, ?, ?)")) {
			statement.setObject(1, session.id());
			statement.setObject(2, member.value());
			statement.setTimestamp(3, Timestamp.from(session.issuedAt()));
			statement.setTimestamp(4, Timestamp.from(session.expiresAt()));
			statement.executeUpdate();
			return session;
		} catch (SQLException failure) {
			throw new IllegalStateException("session unwritable", failure);
		}
	}

	@Override
	public Optional<MemberId> memberOf(UUID sessionId, Instant now) {
		try (Connection connection = database.getConnection();
		     PreparedStatement statement = connection.prepareStatement("select member_id from sessions where id = ? and expires_at > ?")) {
			statement.setObject(1, sessionId);
			statement.setTimestamp(2, Timestamp.from(now));
			try (ResultSet rows = statement.executeQuery()) {
				return rows.next() ? Optional.of(new MemberId(rows.getObject(1, UUID.class))) : Optional.empty();
			}
		} catch (SQLException failure) {
			throw new IllegalStateException("session unreadable", failure);
		}
	}
}
