package pl.umcs.auth.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NextPathTest {

	@ParameterizedTest
	@ValueSource(strings = {"/", "/rates", "/result?amount=5", "/rates#top"})
	void a_path_with_one_leading_slash_is_kept(String candidate) {
		assertEquals(candidate, NextPath.of(candidate).value());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"//evil.example", "/\\evil.example", "https://evil.example", "rates", "javascript:alert(1)", "/ok\r\nSet-Cookie: x=1"})
	void anything_else_falls_back_to_home(String candidate) {
		assertEquals("/", NextPath.of(candidate).value());
	}
}
