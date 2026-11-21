package pl.umcs.auth.domain;

public enum Provider {
	GOOGLE,
	GITHUB;

	public String slug() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
