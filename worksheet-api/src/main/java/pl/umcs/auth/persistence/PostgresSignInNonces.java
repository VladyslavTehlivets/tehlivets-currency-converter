package pl.umcs.auth.persistence;

import pl.umcs.auth.application.SignInNonces;

import javax.sql.DataSource;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class PostgresSignInNonces implements SignInNonces {

	public static final Duration LIFETIME = Duration.ofMinutes(5);

	private final DataSource database;
	private final SecureRandom random = new SecureRandom();

	public PostgresSignInNonces(DataSource database) {
		this.database = database;
	}

	@Override
	public String issue(Instant now) {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		try (Connection connection = database.getConnection();
		     PreparedStatement statement = connection.prepareStatement("insert into sign_in_nonces (id, nonce, expires_at) values (?, ?, ?)")) {
			statement.setObject(1, UUID.randomUUID());
			statement.setString(2, nonce);
			statement.setTimestamp(3, Timestamp.from(now.plus(LIFETIME)));
			statement.executeUpdate();
			return nonce;
		} catch (SQLException failure) {
			throw new IllegalStateException("nonce unwritable", failure);
		}
	}

	@Override
	public boolean consume(String nonce, Instant now) {
		try (Connection connection = database.getConnection();
		     PreparedStatement statement = connection.prepareStatement("update sign_in_nonces set used_at = ? where nonce = ? and used_at is null and expires_at > ?")) {
			statement.setTimestamp(1, Timestamp.from(now));
			statement.setString(2, nonce);
			statement.setTimestamp(3, Timestamp.from(now));
			return statement.executeUpdate() == 1;
		} catch (SQLException failure) {
			throw new IllegalStateException("nonce unwritable", failure);
		}
	}
}
