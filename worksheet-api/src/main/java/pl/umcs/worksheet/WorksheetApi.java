package pl.umcs.worksheet;

import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import pl.umcs.auth.application.SignInService;
import pl.umcs.auth.google.GoogleDoor;
import pl.umcs.auth.google.GoogleIdToken;
import pl.umcs.auth.google.GoogleJwks;
import pl.umcs.auth.google.GoogleOidc;
import pl.umcs.auth.http.SignInRoutes;
import pl.umcs.auth.persistence.PostgresMembers;
import pl.umcs.auth.persistence.PostgresSessions;
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
			Javalin server = WorksheetHttpServer.create(useCases, settings.revision(), settings.webExportDir(), signInRoutes(settings, database)).start(settings.port());
			return new WorksheetApi(database, server);
		} catch (RuntimeException startFailure) {
			database.close();
			throw startFailure;
		}
	}

	private static SignInRoutes signInRoutes(WorksheetApiSettings settings, HikariDataSource database) {
		var signIn = settings.signIn();
		java.time.Clock clock = java.time.Clock.systemUTC();
		java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
		GoogleOidc oidc = new GoogleOidc(signIn.googleClientIds(), signIn.googleClientSecret(), java.net.URI.create(signIn.publicBaseUrl() + "/auth/google/callback"), GoogleOidc.AUTHORIZATION, signIn.googleTokenEndpoint(), signIn.googleJwksUri());
		GoogleDoor google = new GoogleDoor(oidc, http, new GoogleIdToken(oidc.clientIds(), new GoogleJwks(oidc.jwksUri(), http, clock), clock));
		PostgresSessions sessions = new PostgresSessions(database);
		return new SignInRoutes(new SignInService(new PostgresMembers(database), sessions, clock), sessions, google,
				new SignInRoutes.TestDoor(signIn.testDoorEmail(), signIn.testDoorPassword()), clock);
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
