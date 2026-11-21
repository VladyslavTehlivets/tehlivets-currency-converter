package pl.umcs.worksheet;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record WorksheetApiSettings(String databaseUrl, int port, String revision, Path webExportDir, SignInSettings signIn) {

	public record SignInSettings(Set<String> googleClientIds, String googleClientSecret, String publicBaseUrl, String testDoorEmail, String testDoorPassword, java.net.URI googleTokenEndpoint, java.net.URI googleJwksUri) {

		public static SignInSettings closed() {
			return new SignInSettings(Set.of(), null, "http://127.0.0.1", null, null, pl.umcs.auth.google.GoogleOidc.TOKEN, pl.umcs.auth.google.GoogleOidc.JWKS);
		}

		@Override
		public String toString() {
			return "SignInSettings[googleClientIds=" + googleClientIds + ", publicBaseUrl=" + publicBaseUrl + "]";
		}
	}

	public WorksheetApiSettings(String databaseUrl, int port, String revision, Path webExportDir) {
		this(databaseUrl, port, revision, webExportDir, SignInSettings.closed());
	}

	public static WorksheetApiSettings fromEnvironment(Map<String, String> environment) {
		Set<String> clientIds = Arrays.stream(environment.getOrDefault("GOOGLE_CLIENT_IDS", "").split(","))
				.map(String::trim).filter(id -> !id.isEmpty()).collect(Collectors.toUnmodifiableSet());
		return new WorksheetApiSettings(
				environment.get("DATABASE_URL"),
				Integer.parseInt(environment.getOrDefault("PORT", "7070")),
				environment.getOrDefault("REVISION", "local"),
				Path.of(environment.getOrDefault("WEB_EXPORT_DIR", "web/out")),
				new SignInSettings(clientIds, environment.get("GOOGLE_CLIENT_SECRET"), environment.getOrDefault("PUBLIC_BASE_URL", "http://127.0.0.1:7070"),
						environment.get("DEV_TEST_DOOR_EMAIL"), environment.get("DEV_TEST_DOOR_PASSWORD"),
						java.net.URI.create(environment.getOrDefault("GOOGLE_TOKEN_ENDPOINT", pl.umcs.auth.google.GoogleOidc.TOKEN.toString())),
						java.net.URI.create(environment.getOrDefault("GOOGLE_JWKS_URI", pl.umcs.auth.google.GoogleOidc.JWKS.toString()))));
	}

	@Override
	public String toString() {
		return "WorksheetApiSettings[port=" + port + ", revision=" + revision + ", webExportDir=" + webExportDir + "]";
	}
}
