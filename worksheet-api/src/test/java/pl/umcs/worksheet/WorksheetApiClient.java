package pl.umcs.worksheet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

final class WorksheetApiClient {

	record Reply(int status, JsonNode body, HttpResponse<String> raw) {

		String errorCode() {
			return body.path("error").path("code").asText();
		}

		String errorField() {
			return body.path("error").path("field").asText(null);
		}
	}

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Duration PATIENCE = Duration.ofSeconds(30);

	private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(PATIENCE).build();
	private final URI base;

	WorksheetApiClient(int port) {
		this.base = URI.create("http://127.0.0.1:" + port);
	}

	String startWorksheet() {
		return startWorksheetReply().body().path("worksheetKey").asText();
	}

	Reply startWorksheetReply() {
		return send(request("/v1/worksheets", Optional.empty()).POST(HttpRequest.BodyPublishers.noBody()));
	}

	Reply health() {
		return send(request("/v1/health", Optional.empty()).GET());
	}

	Reply listRates(Optional<String> worksheetKey) {
		return send(request("/v1/worksheet/rates", worksheetKey).GET());
	}

	Reply listRates(String worksheetKey) {
		return listRates(Optional.of(worksheetKey));
	}

	Reply keepRate(String worksheetKey, ObjectNode rate) {
		return keepRate(Optional.of(worksheetKey), rate.toString());
	}

	Reply keepRate(Optional<String> worksheetKey, String body) {
		return send(request("/v1/worksheet/rates", worksheetKey).POST(HttpRequest.BodyPublishers.ofString(body)));
	}

	Reply keepRateChunked(String worksheetKey, String body) {
		java.io.InputStream stream = new java.io.ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return send(request("/v1/worksheet/rates", Optional.of(worksheetKey)).POST(HttpRequest.BodyPublishers.ofInputStream(() -> stream)));
	}

	String keptRateId(String worksheetKey, ObjectNode rate) {
		Reply kept = keepRate(worksheetKey, rate);
		if (kept.status() != 201) {
			throw new IllegalStateException("rate was not kept: " + kept.status() + " " + kept.body());
		}
		return kept.body().path("id").asText();
	}

	Reply removeRate(String worksheetKey, String rateId) {
		return send(request("/v1/worksheet/rates/" + rateId, Optional.of(worksheetKey)).DELETE());
	}

	Reply convert(Optional<String> worksheetKey, ObjectNode conversion) {
		return send(request("/v1/worksheet/conversions", worksheetKey).POST(HttpRequest.BodyPublishers.ofString(conversion.toString())));
	}

	Reply convert(String worksheetKey, ObjectNode conversion) {
		return convert(Optional.of(worksheetKey), conversion);
	}

	private HttpRequest.Builder request(String path, Optional<String> worksheetKey) {
		HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path))
				.timeout(PATIENCE)
				.header("Content-Type", "application/json");
		worksheetKey.ifPresent(key -> request.header("Authorization", "Bearer " + key));
		return request;
	}

	private Reply send(HttpRequest.Builder request) {
		try {
			HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
			JsonNode body = response.body().isEmpty() ? JSON.missingNode() : JSON.readTree(response.body());
			return new Reply(response.statusCode(), body, response);
		} catch (IOException unreachable) {
			throw new UncheckedIOException(unreachable);
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(interrupted);
		}
	}
}
