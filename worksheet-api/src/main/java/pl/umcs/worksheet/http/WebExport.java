package pl.umcs.worksheet.http;

import io.javalin.http.Context;
import org.eclipse.jetty.http.MimeTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

final class WebExport {

	private static final String IMMUTABLE = "public, max-age=31536000, immutable";
	private static final String REVALIDATE = "no-cache";

	private final Path root;

	WebExport(Path root) {
		this.root = root.toAbsolutePath().normalize();
	}

	Optional<Path> fileFor(String requestPath) {
		String relative = requestPath.replaceFirst("^/+", "");
		if (relative.isEmpty()) {
			relative = "index.html";
		}
		try {
			return Stream.of(relative, relative + ".html", relative + "/index.html")
					.map(candidate -> root.resolve(candidate).normalize())
					.filter(candidate -> candidate.startsWith(root))
					.filter(Files::isRegularFile)
					.findFirst();
		} catch (InvalidPathException unreadablePath) {
			return Optional.empty();
		}
	}

	String cacheControlFor(Path file) {
		return root.relativize(file).startsWith(Path.of("_next", "static")) ? IMMUTABLE : REVALIDATE;
	}

	void serve(Context ctx) throws IOException {
		Optional<Path> file = fileFor(ctx.path());
		if (file.isPresent()) {
			send(ctx, file.get(), 200);
			return;
		}
		Optional<Path> notFoundPage = fileFor("/404.html");
		if (notFoundPage.isPresent()) {
			send(ctx, notFoundPage.get(), 404);
			return;
		}
		ctx.status(404);
	}

	private void send(Context ctx, Path file, int status) throws IOException {
		String contentType = MimeTypes.getDefaultMimeByExtension(file.getFileName().toString());
		ctx.status(status)
				.header("Cache-Control", cacheControlFor(file))
				.contentType(contentType == null ? "application/octet-stream" : contentType)
				.result(Files.newInputStream(file));
	}
}
