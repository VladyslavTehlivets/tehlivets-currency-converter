package pl.umcs.auth.provider;

import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.Provider;
import pl.umcs.auth.domain.ProviderClaims;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.SignInRefused;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class TestDoorVerifier {

	private final String email;
	private final String password;

	public TestDoorVerifier(String email, String password) {
		this.email = email;
		this.password = password;
	}

	public ProviderClaims verify(Assertion.TestPassword assertion) {
		boolean known = assertion.email() != null && assertion.password() != null
				&& same(assertion.email(), email) & same(assertion.password(), password);
		if (!known) {
			throw new SignInRefused(RefusalCode.NOT_INVITED);
		}
		return new ProviderClaims(Provider.TEST, email, email, true);
	}

	private static boolean same(String presented, String expected) {
		return MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
	}
}
