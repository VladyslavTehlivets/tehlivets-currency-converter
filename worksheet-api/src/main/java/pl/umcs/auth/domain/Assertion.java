package pl.umcs.auth.domain;

public sealed interface Assertion {

	record ProviderAssertion(Provider provider, String subject, String email, boolean emailVerified) implements Assertion {
		public ProviderAssertion {
			if (subject == null || subject.isBlank()) {
				throw new SignInRefusal(SignInRefusal.Code.PROVIDER_UNAVAILABLE);
			}
		}
	}

	record TestDoorAssertion(String email) implements Assertion {
	}
}
