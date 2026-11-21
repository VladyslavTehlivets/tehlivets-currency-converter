package pl.umcs.auth.persistence;

import pl.umcs.auth.application.Sessions;
import pl.umcs.auth.domain.Session;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class PostgresSessions implements Sessions {

	private final DataSource dataSource;

	public PostgresSessions(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void add(Session session) {
		run("insert into sessions (id, member_id, issued_at, expires_at) values (?, ?, ?, ?)", insert -> {
			insert.setObject(1, session.id());
			insert.setObject(2, session.memberId());
			insert.setTimestamp(3, Timestamp.from(session.issuedAt()));
			insert.setTimestamp(4, Timestamp.from(session.expiresAt()));
			return insert.executeUpdate();
		});
	}

	@Override
	public Optional<Session> find(UUID id) {
		try (Connection connection = dataSource.getConnection(); PreparedStatement select = connection.prepareStatement("select member_id, issued_at, expires_at from sessions where id = ?")) {
			select.setObject(1, id);
			try (ResultSet row = select.executeQuery()) {
				return row.next()
						? Optional.of(new Session(id, row.getObject(1, UUID.class), row.getTimestamp(2).toInstant(), row.getTimestamp(3).toInstant()))
						: Optional.empty();
			}
		} catch (SQLException failure) {
			throw new IllegalStateException(failure);
		}
	}

	@Override
	public void issueNonce(String nonce, Instant expiresAt) {
		run("insert into session_nonces (nonce, expires_at) values (?, ?)", insert -> {
			insert.setString(1, nonce);
			insert.setTimestamp(2, Timestamp.from(expiresAt));
			return insert.executeUpdate();
		});
	}

	@Override
	public boolean consumeNonce(String nonce, Instant now) {
		return run("delete from session_nonces where nonce = ? and expires_at > ?", delete -> {
			delete.setString(1, nonce);
			delete.setTimestamp(2, Timestamp.from(now));
			return delete.executeUpdate();
		}) == 1;
	}

	@FunctionalInterface
	private interface Statement {
		int run(PreparedStatement statement) throws SQLException;
	}

	private int run(String sql, Statement work) {
		try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			return work.run(statement);
		} catch (SQLException failure) {
			throw new IllegalStateException(failure);
		}
	}
}
