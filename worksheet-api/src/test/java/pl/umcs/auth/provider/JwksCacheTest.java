package pl.umcs.auth.provider;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.SignInRefused;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwksCacheTest {

	private static final class TickingClock extends Clock {
		private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));

		void advance(Duration by) {
			now.updateAndGet(moment -> moment.plus(by));
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now.get();
		}
	}

	private final KeyPair key = GoogleTokenBuilder.newSigningKey();

	@Test
	void the_cache_serves_the_key_until_the_published_freshness_ends() {
		AtomicInteger fetches = new AtomicInteger();
		TickingClock clock = new TickingClock();
		JwksCache cache = new JwksCache(() -> {
			fetches.incrementAndGet();
			return new JwkSet(Map.of("k", key.getPublic()), Duration.ofMinutes(10));
		}, clock);

		cache.keyFor("k");
		clock.advance(Duration.ofMinutes(9));
		cache.keyFor("k");
		assertEquals(1, fetches.get());

		clock.advance(Duration.ofMinutes(2));
		cache.keyFor("k");
		assertEquals(2, fetches.get());
	}

	@Test
	void an_unknown_key_id_triggers_one_refetch_but_not_a_fetch_storm() {
		AtomicInteger fetches = new AtomicInteger();
		TickingClock clock = new TickingClock();
		JwksCache cache = new JwksCache(() -> {
			fetches.incrementAndGet();
			return new JwkSet(Map.of("k", key.getPublic()), Duration.ofHours(1));
		}, clock);

		assertTrue(cache.keyFor("rotated").isEmpty());
		assertTrue(cache.keyFor("rotated").isEmpty());
		assertEquals(1, fetches.get());

		clock.advance(Duration.ofMinutes(1));
		assertTrue(cache.keyFor("rotated").isEmpty());
		assertEquals(2, fetches.get());
	}

	@Test
	void the_jwks_cache_honours_cache_control() {
		assertEquals(Duration.ofSeconds(19000), HttpJwksSource.freshnessOf("public, max-age=19000, must-revalidate, no-transform"));
		assertEquals(Duration.ofMinutes(5), HttpJwksSource.freshnessOf("no-store"));
	}

	@Test
	void the_published_key_set_is_read_over_http_with_its_cache_control() throws Exception {
		HttpServer google = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		byte[] body = GoogleTokenBuilder.jwksOf(key).getBytes(java.nio.charset.StandardCharsets.UTF_8);
		google.createContext("/certs", exchange -> {
			exchange.getResponseHeaders().add("Cache-Control", "public, max-age=3600");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		google.createContext("/down", exchange -> {
			exchange.sendResponseHeaders(503, -1);
			exchange.close();
		});
		google.start();
		try {
			String base = "http://127.0.0.1:" + google.getAddress().getPort();
			JwkSet fetched = new HttpJwksSource(URI.create(base + "/certs")).fetch();

			assertEquals(key.getPublic(), fetched.keysById().get(GoogleTokenBuilder.KEY_ID));
			assertEquals(Duration.ofHours(1), fetched.freshFor());
			SignInRefused unreachable = assertThrows(SignInRefused.class, () -> new HttpJwksSource(URI.create(base + "/down")).fetch());
			assertEquals(RefusalCode.PROVIDER_UNAVAILABLE, unreachable.code());
		} finally {
			google.stop(0);
		}
	}
}
