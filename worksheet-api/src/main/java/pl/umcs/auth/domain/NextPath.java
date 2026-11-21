package pl.umcs.auth.domain;

public record NextPath(String value) {

	public static final NextPath HOME = new NextPath("/");

	public static NextPath of(String candidate) {
		return isSameOriginPath(candidate) ? new NextPath(candidate) : HOME;
	}

	private static boolean isSameOriginPath(String candidate) {
		if (candidate == null || candidate.isEmpty() || candidate.charAt(0) != '/') {
			return false;
		}
		if (candidate.length() > 1 && (candidate.charAt(1) == '/' || candidate.charAt(1) == '\\')) {
			return false;
		}
		return candidate.chars().noneMatch(Character::isISOControl);
	}
}
