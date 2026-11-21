package pl.umcs.auth.provider;

import pl.umcs.auth.application.AssertionVerifier;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.ProviderClaims;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.SignInRefused;

import java.util.Optional;

public final class ConfiguredAssertionVerifier implements AssertionVerifier {

	private final Optional<GoogleIdTokenVerifier> google;
	private final Optional<TestDoorVerifier> testDoor;

	public ConfiguredAssertionVerifier(Optional<GoogleIdTokenVerifier> google, Optional<TestDoorVerifier> testDoor) {
		this.google = google;
		this.testDoor = testDoor;
	}

	@Override
	public ProviderClaims verify(Assertion assertion) {
		return switch (assertion) {
			case Assertion.GoogleIdToken token -> google.orElseThrow(ConfiguredAssertionVerifier::noDoor).verify(token);
			case Assertion.TestPassword password -> testDoor.orElseThrow(ConfiguredAssertionVerifier::noDoor).verify(password);
			case Assertion.GitHubCode code -> throw noDoor();
		};
	}

	private static SignInRefused noDoor() {
		return new SignInRefused(RefusalCode.PROVIDER_UNAVAILABLE);
	}
}
