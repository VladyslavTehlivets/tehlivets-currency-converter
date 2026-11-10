package pl.umcs.worksheet.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

public final class WorksheetDatabase {

	private WorksheetDatabase() {
	}

	public static HikariDataSource openMigrated(String databaseUrl) {
		DatabaseUrl url = DatabaseUrl.parse(databaseUrl);
		HikariConfig config = new HikariConfig();
		config.setPoolName("worksheets");
		config.setJdbcUrl(url.jdbcUrl());
		url.user().ifPresent(config::setUsername);
		url.password().ifPresent(config::setPassword);
		HikariDataSource dataSource = new HikariDataSource(config);
		try {
			Flyway.configure().dataSource(dataSource).load().migrate();
			return dataSource;
		} catch (RuntimeException migrationFailure) {
			dataSource.close();
			throw migrationFailure;
		}
	}
}
