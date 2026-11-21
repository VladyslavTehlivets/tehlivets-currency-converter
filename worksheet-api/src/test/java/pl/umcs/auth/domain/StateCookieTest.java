package pl.umcs.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateCookieTest {

	private static final Instant NOW = Instant.parse("2026-11-21T10:00:00Z");

	@Test
	void the_binder_survives_a_round_trip_through_its_cookie_value() {
		StateCookie binder = StateCookie.begin(NextPath.of("/rates"), NOW);

		assertEquals(binder, StateCookie.decode(binder.encode()).orElseThrow());
	}

	@Test
	void a_garbled_cookie_value_binds_nothing() {
		assertTrue(StateCookie.decode("!!not-base64!!").isEmpty());
		assertTrue(StateCookie.decode("YQ").isEmpty());
	}

	@Test
	void the_binder_accepts_its_own_state_for_ten_minutes_only() {
		StateCookie binder = StateCookie.begin(NextPath.HOME, NOW);

		assertTrue(binder.returnsWith(binder.state(), NOW.plusSeconds(599)));
		assertFalse(binder.returnsWith(binder.state(), NOW.plusSeconds(600)));
		assertFalse(binder.returnsWith("another-state", NOW));
	}

	@Test
	void the_pkce_challenge_is_the_s256_of_the_verifier() {
		StateCookie rfc7636 = new StateCookie("s", "n", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "/", NOW);

		assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", rfc7636.challenge());
	}
}
