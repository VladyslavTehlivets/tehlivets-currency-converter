package pl.umcs.auth.domain;

import java.util.Optional;

public record NextPath(String value) {

	private static final NextPath HOME = new NextPath("/");

	public static NextPath home() {
		return HOME;
	}

	public static Optional<NextPath> parse(String written) {
		if (written == null || !written.startsWith("/") || written.chars().anyMatch(Character::isISOControl)) {
			return Optional.empty();
		}
		if (written.length() > 1 && (written.charAt(1) == '/' || written.charAt(1) == '\\')) {
			return Optional.empty();
		}
		return Optional.of(new NextPath(written));
	}
}
