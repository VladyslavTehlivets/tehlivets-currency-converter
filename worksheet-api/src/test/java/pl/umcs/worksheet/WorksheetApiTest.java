package pl.umcs.worksheet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.umcs.worksheet.application.OpenWorksheet;
import pl.umcs.worksheet.domain.DatedRate;
import pl.umcs.worksheet.domain.Direction;
import pl.umcs.worksheet.domain.RateValue;
import pl.umcs.worksheet.domain.RatedAt;
import pl.umcs.worksheet.domain.WorksheetId;
import pl.umcs.worksheet.domain.WorksheetKey;
import pl.umcs.worksheet.persistence.PostgresWorksheets;
import pl.umcs.worksheet.persistence.WorksheetDatabase;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pl.umcs.worksheet.ConversionRequestBuilder.aConversion;
import static pl.umcs.worksheet.RateRequestBuilder.aRate;

class WorksheetApiTest {

	private static final String LOCAL_DATABASE_URL = "postgresql://worksheets:worksheets@127.0.0.1:5446/worksheets";
	private static final String REVISION = "worksheet-api-test-revision";
	private static final int RACING_KEEPS = 8;

	private static WorksheetApi api;
	private static WorksheetApiClient client;
	private static HikariDataSource database;

	@BeforeAll
	static void startTheApiOnAFreePort() {
		String databaseUrl = Optional.ofNullable(System.getenv("DATABASE_URL")).filter(url -> !url.isBlank()).orElse(LOCAL_DATABASE_URL);
		api = WorksheetApi.start(new WorksheetApiSettings(databaseUrl, 0, REVISION, Path.of("target", "no-web-export")));
		client = new WorksheetApiClient(api.port());
		database = WorksheetDatabase.openMigrated(databaseUrl);
	}

	@AfterAll
	static void stopTheApi() {
		if (database != null) {
			database.close();
		}
		if (api != null) {
			api.close();
		}
	}

	@Test
	void starting_a_worksheet_returns_its_key_once() {
		WorksheetApiClient.Reply started = client.startWorksheetReply();
		String key = started.body().path("worksheetKey").asText();

		assertEquals(201, started.status());
		assertTrue(key.matches("[A-Za-z0-9_-]{43}"), "a 256-bit base64url key");
		assertEquals(Optional.of("no-store"), started.raw().headers().firstValue("Cache-Control"));
		assertNotEquals(key, client.startWorksheet());

		WorksheetApiClient.Reply listed = client.listRates(key);
		assertEquals(200, listed.status());
		assertFalse(listed.raw().body().contains(key), "no later reply repeats the key");
	}

	@Test
	void a_worksheet_route_without_a_key_is_refused() {
		WorksheetApiClient.Reply withoutKey = client.listRates(Optional.empty());
		WorksheetApiClient.Reply withEmptyKey = client.listRates(Optional.of(""));
		WorksheetApiClient.Reply withUnknownKey = client.listRates(WorksheetKey.generate().reveal());
		WorksheetApiClient.Reply keepWithoutKey = client.keepRate(Optional.empty(), aRate().json().toString());
		WorksheetApiClient.Reply convertWithoutKey = client.convert(Optional.empty(), aConversion().json());

		for (WorksheetApiClient.Reply refused : List.of(withoutKey, withEmptyKey, withUnknownKey, keepWithoutKey, convertWithoutKey)) {
			assertEquals(401, refused.status());
			assertEquals("unauthorized", refused.errorCode());
		}
	}

	@Test
	void a_worksheet_cannot_read_or_remove_the_rates_of_another() {
		String owner = client.startWorksheet();
		String stranger = client.startWorksheet();
		String ownersRate = client.keptRateId(owner, aRate().json());

		assertEquals(0, client.listRates(stranger).body().path("rates").size());

		WorksheetApiClient.Reply removal = client.removeRate(stranger, ownersRate);
		assertEquals(404, removal.status());
		assertEquals("rate_not_found", removal.errorCode());

		assertEquals(List.of(ownersRate), rateIdsOf(client.listRates(owner)));
	}

	@Test
	void a_kept_rate_is_listed_with_its_exact_value() {
		String key = client.startWorksheet();

		WorksheetApiClient.Reply kept = client.keepRate(key,
				aRate().from("USD").to("PLN").of("123456789012345.123456789010").ratedAt("2026-01-10T12:00:00+02:00").json());

		assertEquals(201, kept.status());
		JsonNode listed = client.listRates(key).body();
		assertEquals(500, listed.path("limit").asInt());
		assertEquals(1, listed.path("rates").size());
		JsonNode rate = listed.path("rates").get(0);
		assertEquals(kept.body(), rate);
		assertEquals("USD", rate.path("from").asText());
		assertEquals("PLN", rate.path("to").asText());
		assertEquals("123456789012345.12345678901", rate.path("rate").asText());
		assertEquals("2026-01-10T10:00:00Z", rate.path("ratedAt").asText());
	}

	@Test
	void converting_through_pln_returns_both_legs_and_their_selected_rates() {
		String key = client.startWorksheet();
		client.keptRateId(key, aRate().from("EUR").to("PLN").of("4.10").ratedAt("2025-12-01T00:00:00Z").json());
		client.keptRateId(key, aRate().from("EUR").to("PLN").of("4.30").ratedAt("2026-01-10T00:00:00Z").json());
		client.keptRateId(key, aRate().from("PLN").to("USD").of("0.25").ratedAt("2026-01-12T00:00:00Z").json());

		WorksheetApiClient.Reply converted = client.convert(key,
				aConversion().of("100").from("EUR").to("USD").at("2026-01-10T12:00:00+01:00").json());

		assertEquals(200, converted.status());
		JsonNode receipt = converted.body();
		assertEquals("100", receipt.path("amount").asText());
		assertEquals("107.5", receipt.path("result").asText());
		assertEquals("PLN", receipt.path("base").asText());
		assertEquals("2026-01-10T11:00:00Z", receipt.path("at").asText());
		assertEquals(2, receipt.path("legs").size());
		assertLeg(receipt.path("legs").get(0), "EUR", "PLN", "4.3", "2026-01-10T00:00:00Z", "100", "430");
		assertLeg(receipt.path("legs").get(1), "PLN", "USD", "0.25", "2026-01-12T00:00:00Z", "430", "107.5");
	}

	@Test
	void converting_with_a_missing_direction_names_that_direction() {
		String key = client.startWorksheet();
		client.keptRateId(key, aRate().from("EUR").to("PLN").json());
		client.keptRateId(key, aRate().from("USD").to("PLN").json());

		WorksheetApiClient.Reply refused = client.convert(key,
				aConversion().from("EUR").to("USD").at("2026-01-10T12:00:00+01:00").json());

		assertEquals(422, refused.status());
		JsonNode error = refused.body().path("error");
		assertEquals("rate_missing", error.path("code").asText());
		assertEquals("PLN", error.path("missing").path("from").asText());
		assertEquals("USD", error.path("missing").path("to").asText());
		assertEquals("2026-01-10T11:00:00Z", error.path("at").asText());
	}

	@Test
	void an_amount_with_a_comma_is_refused_beside_the_amount_field() {
		String key = client.startWorksheet();
		client.keptRateId(key, aRate().json());

		WorksheetApiClient.Reply refused = client.convert(key, aConversion().of("100,50").json());

		assertEquals(400, refused.status());
		assertEquals("invalid_amount", refused.errorCode());
		assertEquals("amount", refused.errorField());
	}

	@Test
	void an_amount_sent_as_a_json_number_is_refused() {
		String key = client.startWorksheet();
		client.keptRateId(key, aRate().json());
		ObjectNode amountAsNumber = aConversion().json().put("amount", new BigDecimal("100.50"));

		WorksheetApiClient.Reply refused = client.convert(key, amountAsNumber);

		assertEquals(400, refused.status());
		assertEquals("invalid_amount", refused.errorCode());
		assertEquals("amount", refused.errorField());
	}

	@Test
	void converting_a_currency_to_itself_is_refused() {
		String key = client.startWorksheet();

		WorksheetApiClient.Reply refused = client.convert(key, aConversion().from("PLN").to("PLN").json());

		assertEquals(400, refused.status());
		assertEquals("same_currency", refused.errorCode());
		assertEquals("to", refused.errorField());
	}

	@Test
	void a_rate_for_a_kept_direction_and_moment_is_refused() {
		String key = client.startWorksheet();
		client.keptRateId(key, aRate().from("EUR").to("PLN").of("4.30").ratedAt("2026-01-10T01:00:00+01:00").json());

		WorksheetApiClient.Reply refused = client.keepRate(key, aRate().from("EUR").to("PLN").of("4.40").ratedAt("2026-01-10T00:00:00Z").json());

		assertEquals(409, refused.status());
		assertEquals("rate_exists", refused.errorCode());
	}

	@Test
	void two_concurrent_rates_cannot_pass_the_limit() throws Exception {
		String key = client.startWorksheet();
		fillWithRatesOnSuccessiveHours(key, 499);
		CountDownLatch startTogether = new CountDownLatch(1);
		List<Callable<WorksheetApiClient.Reply>> racingKeeps = new ArrayList<>();
		for (int racer = 0; racer < RACING_KEEPS; racer++) {
			String moment = Instant.parse("2090-01-01T00:00:00Z").plus(Duration.ofMinutes(racer)).toString();
			racingKeeps.add(() -> {
				startTogether.await();
				return client.keepRate(key, aRate().from("USD").to("PLN").ratedAt(moment).json());
			});
		}

		List<WorksheetApiClient.Reply> replies = new ArrayList<>();
		try (ExecutorService racers = Executors.newFixedThreadPool(RACING_KEEPS)) {
			List<Future<WorksheetApiClient.Reply>> pending = racingKeeps.stream().map(racers::submit).toList();
			startTogether.countDown();
			for (Future<WorksheetApiClient.Reply> reply : pending) {
				replies.add(reply.get());
			}
		}

		Map<Integer, Long> repliesByStatus = replies.stream().collect(Collectors.groupingBy(WorksheetApiClient.Reply::status, Collectors.counting()));
		assertEquals(Map.of(201, 1L, 422, (long) RACING_KEEPS - 1), repliesByStatus);
		assertTrue(replies.stream().filter(reply -> reply.status() == 422).allMatch(reply -> reply.errorCode().equals("worksheet_full")));
		assertEquals(500, client.listRates(key).body().path("rates").size());
	}

	@Test
	void a_removed_rate_no_longer_takes_part_in_a_conversion() {
		String key = client.startWorksheet();
		client.keptRateId(key, aRate().from("EUR").to("PLN").of("4.10").ratedAt("2025-12-01T00:00:00Z").json());
		String nearest = client.keptRateId(key, aRate().from("EUR").to("PLN").of("4.30").ratedAt("2026-01-10T00:00:00Z").json());
		ObjectNode conversion = aConversion().of("100").from("EUR").to("PLN").at("2026-01-10T12:00:00Z").json();
		assertEquals("430", client.convert(key, conversion).body().path("result").asText());

		WorksheetApiClient.Reply removal = client.removeRate(key, nearest);

		assertEquals(204, removal.status());
		JsonNode receipt = client.convert(key, conversion).body();
		assertEquals("410", receipt.path("result").asText());
		assertEquals("2025-12-01T00:00:00Z", receipt.path("legs").get(0).path("ratedAt").asText());
		assertEquals(404, client.removeRate(key, nearest).status());
	}

	@Test
	void a_body_beyond_four_kilobytes_is_refused() {
		String key = client.startWorksheet();
		String oversizedBody = aRate().json().put("padding", "x".repeat(4096)).toString();

		WorksheetApiClient.Reply refused = client.keepRate(Optional.of(key), oversizedBody);

		assertEquals(413, refused.status());
		assertEquals("body_too_large", refused.errorCode());
		assertEquals(0, client.listRates(key).body().path("rates").size());
	}

	@Test
	void a_chunked_body_beyond_four_kilobytes_is_refused() {
		String key = client.startWorksheet();
		String oversizedBody = aRate().json().put("padding", "x".repeat(1_000_000)).toString();

		WorksheetApiClient.Reply refused = client.keepRateChunked(key, oversizedBody);

		assertEquals(413, refused.status());
		assertEquals("body_too_large", refused.errorCode());
		assertEquals(0, client.listRates(key).body().path("rates").size());
	}

	@Test
	void health_reports_the_running_revision() {
		WorksheetApiClient.Reply health = client.health();

		assertEquals(200, health.status());
		assertEquals("ok", health.body().path("status").asText());
		assertEquals(REVISION, health.body().path("revision").asText());
	}

	private static void fillWithRatesOnSuccessiveHours(String worksheetKey, int count) {
		PostgresWorksheets worksheets = new PostgresWorksheets(database);
		WorksheetId worksheet = new OpenWorksheet(worksheets).open(WorksheetKey.presented(worksheetKey));
		Direction eurToPln = new Direction(Currency.getInstance("EUR"), Currency.getInstance("PLN"));
		Instant firstHour = Instant.parse("2026-01-01T00:00:00Z");
		worksheets.change(worksheet, opened -> {
			for (int hour = 0; hour < count; hour++) {
				opened.keep(DatedRate.newRate(eurToPln, new RateValue(new BigDecimal("4.30")), new RatedAt(firstHour.plus(Duration.ofHours(hour)))));
			}
		});
	}

	private static List<String> rateIdsOf(WorksheetApiClient.Reply listed) {
		List<String> ids = new ArrayList<>();
		listed.body().path("rates").forEach(rate -> ids.add(rate.path("id").asText()));
		return ids;
	}

	private static void assertLeg(JsonNode leg, String from, String to, String rate, String ratedAt, String amountIn, String amountOut) {
		Map<String, String> expected = Map.of("from", from, "to", to, "rate", rate, "ratedAt", ratedAt, "amountIn", amountIn, "amountOut", amountOut);
		Map<String, String> actual = expected.keySet().stream().collect(Collectors.toMap(Function.identity(), field -> leg.path(field).asText()));
		assertEquals(expected, actual);
	}
}
