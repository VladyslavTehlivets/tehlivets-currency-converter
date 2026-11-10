package pl.umcs.worksheet.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebExportTest {

	@TempDir
	Path workspace;

	private Path exportRoot;
	private WebExport webExport;

	@BeforeEach
	void exportWithASecretBesideIt() throws IOException {
		exportRoot = Files.createDirectories(workspace.resolve("out"));
		fileAt(exportRoot.resolve("index.html"));
		fileAt(exportRoot.resolve("rates.html"));
		fileAt(exportRoot.resolve("rates/new/index.html"));
		fileAt(exportRoot.resolve("_next/static/chunks/app.js"));
		fileAt(workspace.resolve("secret.txt"));
		fileAt(workspace.resolve("out-sibling/secret.html"));
		webExport = new WebExport(exportRoot);
	}

	@Test
	void serves_the_index_for_the_root() {
		assertEquals(Optional.of(exportRoot.resolve("index.html")), webExport.fileFor("/"));
	}

	@Test
	void resolves_a_route_to_its_exported_page() {
		assertEquals(Optional.of(exportRoot.resolve("rates.html")), webExport.fileFor("/rates"));
		assertEquals(Optional.of(exportRoot.resolve("rates/new/index.html")), webExport.fileFor("/rates/new"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"/../secret.txt", "/../secret", "/rates/../../secret.txt", "//../secret.txt",
			"/../out-sibling/secret", "/_next/../../secret.txt"})
	void refuses_a_path_that_escapes_the_export(String escapingPath) {
		assertEquals(Optional.empty(), webExport.fileFor(escapingPath));
	}

	@Test
	void finds_nothing_for_an_unknown_route() {
		assertEquals(Optional.empty(), webExport.fileFor("/unknown"));
	}

	@Test
	void marks_only_hashed_build_assets_as_immutable() {
		assertEquals("public, max-age=31536000, immutable", webExport.cacheControlFor(exportRoot.resolve("_next/static/chunks/app.js")));
		assertEquals("no-cache", webExport.cacheControlFor(exportRoot.resolve("index.html")));
	}

	private static void fileAt(Path file) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, file.getFileName().toString());
	}
}
