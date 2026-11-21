package pl.umcs.worksheet;

import pl.umcs.auth.AuthSettings;

import java.nio.file.Path;
import java.util.Map;

public record WorksheetApiSettings(String databaseUrl, int port, String revision, Path webExportDir, AuthSettings auth) {

	public WorksheetApiSettings(String databaseUrl, int port, String revision, Path webExportDir) {
		this(databaseUrl, port, revision, webExportDir, AuthSettings.none());
	}

	public static WorksheetApiSettings fromEnvironment(Map<String, String> environment) {
		int port = Integer.parseInt(environment.getOrDefault("PORT", "7070"));
		return new WorksheetApiSettings(
				environment.get("DATABASE_URL"),
				port,
				environment.getOrDefault("REVISION", "local"),
				Path.of(environment.getOrDefault("WEB_EXPORT_DIR", "web/out")),
				AuthSettings.fromEnvironment(environment, port));
	}

	@Override
	public String toString() {
		return "WorksheetApiSettings[port=" + port + ", revision=" + revision + ", webExportDir=" + webExportDir + ", auth=" + auth + "]";
	}
}
