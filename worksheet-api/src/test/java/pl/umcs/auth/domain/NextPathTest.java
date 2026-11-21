package pl.umcs.auth.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NextPathTest {

	@ParameterizedTest
	@ValueSource(strings = {"/", "/rates", "/result?amount=1&from=EUR", "/rates/new#top"})
	void the_next_path_must_start_with_one_slash(String written) {
		assertEquals(written, NextPath.parse(written).orElseThrow().value());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "rates", "//evil.example", "/\\evil.example", "https://evil.example", "/ok\r\nSet-Cookie: a=b", "\\\\evil"})
	void a_next_path_that_could_leave_the_site_is_refused(String written) {
		assertTrue(NextPath.parse(written).isEmpty());
	}

	@Test
	void a_state_cookie_expires_after_ten_minutes_and_answers_only_its_own_state() {
		var issuedAt = java.time.Instant.parse("2026-01-01T00:00:00Z");
		StateCookie cookie = new StateCookie("s", "n", "v", NextPath.home(), issuedAt);

		assertTrue(cookie.isFresh(issuedAt.plusSeconds(599)));
		assertTrue(!cookie.isFresh(issuedAt.plusSeconds(600)));
		assertTrue(cookie.answers("s"));
		assertTrue(!cookie.answers("t"));
		assertTrue(!cookie.answers(null));
	}
}
