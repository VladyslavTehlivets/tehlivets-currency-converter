package pl.umcs.worksheet.persistence;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public record DatabaseUrl(String jdbcUrl, Optional<String> user, Optional<String> password) {

	public static DatabaseUrl parse(String databaseUrl) {
		if (databaseUrl == null || databaseUrl.isBlank()) {
			throw new IllegalArgumentException("DATABASE_URL is required");
		}
		if (databaseUrl.startsWith("jdbc:postgresql:")) {
			return new DatabaseUrl(databaseUrl, Optional.empty(), Optional.empty());
		}
		if (databaseUrl.startsWith("postgresql://") || databaseUrl.startsWith("postgres://")) {
			try {
				return fromLibpqForm(URI.create(databaseUrl));
			} catch (IllegalArgumentException malformed) {
				throw new IllegalArgumentException("DATABASE_URL is not a valid URL");
			}
		}
		throw new IllegalArgumentException("DATABASE_URL must start with jdbc:postgresql:, postgresql:// or postgres://");
	}

	private static DatabaseUrl fromLibpqForm(URI url) {
		String port = url.getPort() > 0 ? ":" + url.getPort() : "";
		String query = url.getRawQuery() == null ? "" : "?" + url.getRawQuery();
		String jdbcUrl = "jdbc:postgresql://" + url.getHost() + port + url.getRawPath() + query;
		String userInfo = url.getRawUserInfo();
		if (userInfo == null) {
			return new DatabaseUrl(jdbcUrl, Optional.empty(), Optional.empty());
		}
		int separator = userInfo.indexOf(':');
		if (separator < 0) {
			return new DatabaseUrl(jdbcUrl, Optional.of(decoded(userInfo)), Optional.empty());
		}
		return new DatabaseUrl(jdbcUrl, Optional.of(decoded(userInfo.substring(0, separator))), Optional.of(decoded(userInfo.substring(separator + 1))));
	}

	private static String decoded(String part) {
		return URLDecoder.decode(part, StandardCharsets.UTF_8);
	}

	@Override
	public String toString() {
		return "DatabaseUrl[credentials hidden]";
	}
}
