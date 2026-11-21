package pl.umcs.auth.domain;

public sealed interface Assertion {

	enum Origin {
		TOKEN_ENDPOINT,
		CLIENT
	}

	record GoogleIdToken(String idToken, String expectedNonce, Origin origin) implements Assertion {
	}

	record GitHubCode(String code, String redirectUri) implements Assertion {
	}

	record TestPassword(String email, String password) implements Assertion {
	}
}
