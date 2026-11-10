package pl.umcs.worksheet;

import java.nio.file.Path;
import java.util.Map;

public record WorksheetApiSettings(String databaseUrl, int port, String revision, Path webExportDir) {

	public static WorksheetApiSettings fromEnvironment(Map<String, String> environment) {
		return new WorksheetApiSettings(
				environment.get("DATABASE_URL"),
				Integer.parseInt(environment.getOrDefault("PORT", "7070")),
				environment.getOrDefault("REVISION", "local"),
				Path.of(environment.getOrDefault("WEB_EXPORT_DIR", "web/out")));
	}

	@Override
	public String toString() {
		return "WorksheetApiSettings[port=" + port + ", revision=" + revision + ", webExportDir=" + webExportDir + "]";
	}
}
