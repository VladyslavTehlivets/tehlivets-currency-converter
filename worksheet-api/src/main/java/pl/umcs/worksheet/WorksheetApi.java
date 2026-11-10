package pl.umcs.worksheet;

import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import pl.umcs.worksheet.application.ConvertOnWorksheet;
import pl.umcs.worksheet.application.KeepRate;
import pl.umcs.worksheet.application.ListRates;
import pl.umcs.worksheet.application.OpenWorksheet;
import pl.umcs.worksheet.application.RemoveRate;
import pl.umcs.worksheet.application.StartWorksheet;
import pl.umcs.worksheet.application.Worksheets;
import pl.umcs.worksheet.http.WorksheetHttpServer;
import pl.umcs.worksheet.persistence.PostgresWorksheets;
import pl.umcs.worksheet.persistence.WorksheetDatabase;

public final class WorksheetApi implements AutoCloseable {

	private final HikariDataSource database;
	private final Javalin server;

	private WorksheetApi(HikariDataSource database, Javalin server) {
		this.database = database;
		this.server = server;
	}

	public static WorksheetApi start(WorksheetApiSettings settings) {
		HikariDataSource database = WorksheetDatabase.openMigrated(settings.databaseUrl());
		try {
			Worksheets worksheets = new PostgresWorksheets(database);
			WorksheetHttpServer.UseCases useCases = new WorksheetHttpServer.UseCases(
					new StartWorksheet(worksheets),
					new OpenWorksheet(worksheets),
					new ListRates(worksheets),
					new KeepRate(worksheets),
					new RemoveRate(worksheets),
					new ConvertOnWorksheet(worksheets));
			Javalin server = WorksheetHttpServer.create(useCases, settings.revision(), settings.webExportDir()).start(settings.port());
			return new WorksheetApi(database, server);
		} catch (RuntimeException startFailure) {
			database.close();
			throw startFailure;
		}
	}

	public int port() {
		return server.port();
	}

	@Override
	public void close() {
		server.stop();
		database.close();
	}

	public static void main(String[] args) {
		WorksheetApi api = start(WorksheetApiSettings.fromEnvironment(System.getenv()));
		Runtime.getRuntime().addShutdownHook(new Thread(api::close));
	}
}
