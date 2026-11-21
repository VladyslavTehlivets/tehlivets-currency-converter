package pl.umcs.worksheet;

import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import pl.umcs.auth.application.SignInService;
import pl.umcs.auth.http.AuthHttp;
import pl.umcs.auth.persistence.PostgresMembers;
import pl.umcs.auth.persistence.PostgresSessions;
import pl.umcs.auth.persistence.PostgresSignInNonces;
import pl.umcs.auth.provider.ConfiguredAssertionVerifier;
import pl.umcs.auth.provider.GoogleIdTokenVerifier;
import pl.umcs.auth.provider.GoogleTokenEndpoint;
import pl.umcs.auth.provider.HttpGoogleTokenEndpoint;
import pl.umcs.auth.provider.HttpJwksSource;
import pl.umcs.auth.provider.JwksCache;
import pl.umcs.auth.provider.JwksSource;
import pl.umcs.auth.provider.TestDoorVerifier;
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

import java.time.Clock;
import java.util.Optional;

public final class WorksheetApi implements AutoCloseable {

	private final HikariDataSource database;
	private final Javalin server;

	private WorksheetApi(HikariDataSource database, Javalin server) {
		this.database = database;
		this.server = server;
	}

	public record GoogleEndpoints(JwksSource jwks, GoogleTokenEndpoint tokens) {

		static GoogleEndpoints live(WorksheetApiSettings settings) {
			String clientId = settings.auth().googleClientIds().stream().findFirst().orElse("");
			return new GoogleEndpoints(
					new HttpJwksSource(HttpJwksSource.GOOGLE_CERTS),
					new HttpGoogleTokenEndpoint(HttpGoogleTokenEndpoint.GOOGLE_TOKEN, clientId, settings.auth().googleClientSecret()));
		}
	}

	public static WorksheetApi start(WorksheetApiSettings settings) {
		return start(settings, GoogleEndpoints.live(settings), Clock.systemUTC());
	}

	public static WorksheetApi start(WorksheetApiSettings settings, GoogleEndpoints google, Clock clock) {
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
			AuthHttp auth = authHttp(settings, google, clock, database);
			Javalin server = WorksheetHttpServer.create(useCases, auth, settings.revision(), settings.webExportDir()).start(settings.port());
			return new WorksheetApi(database, server);
		} catch (RuntimeException startFailure) {
			database.close();
			throw startFailure;
		}
	}

	private static AuthHttp authHttp(WorksheetApiSettings settings, GoogleEndpoints google, Clock clock, HikariDataSource database) {
		Optional<GoogleIdTokenVerifier> googleVerifier = settings.auth().googleClientIds().isEmpty()
				? Optional.empty()
				: Optional.of(new GoogleIdTokenVerifier(new JwksCache(google.jwks(), clock), settings.auth().googleClientIds(), clock));
		Optional<TestDoorVerifier> testDoor = settings.auth().testDoor().map(door -> new TestDoorVerifier(door.email(), door.password()));
		PostgresSessions sessions = new PostgresSessions(database);
		SignInService signIn = new SignInService(new ConfiguredAssertionVerifier(googleVerifier, testDoor), new PostgresMembers(database), sessions, clock);
		return new AuthHttp(signIn, sessions, new PostgresSignInNonces(database), google.tokens(), settings.auth(), clock);
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
