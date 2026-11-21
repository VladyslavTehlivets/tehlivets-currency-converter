package pl.umcs.auth.application;

import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.ProviderClaims;

public interface AssertionVerifier {

	ProviderClaims verify(Assertion assertion);
}
