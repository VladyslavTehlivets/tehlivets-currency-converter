package pl.umcs.auth;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record AuthSettings(Set<String> googleClientIds, String googleClientSecret, String publicBaseUrl, Optional<TestDoor> testDoor) {

	public record TestDoor(String email, String password) {
		@Override
		public String toString() {
			return "TestDoor[email=" + email + "]";
		}
	}

	private static final String TEST_IDENTITY_EMAIL = "factory-e2e@currency.vtehsol.com";

	public static AuthSettings none() {
		return new AuthSettings(Set.of(), "", "http://127.0.0.1", Optional.empty());
	}

	public static AuthSettings fromEnvironment(Map<String, String> environment, int port) {
		Set<String> clientIds = Arrays.stream(environment.getOrDefault("GOOGLE_CLIENT_ID", "").split(","))
				.map(String::strip).filter(id -> !id.isEmpty()).collect(Collectors.toUnmodifiableSet());
		boolean testDoorOn = "on".equals(environment.get("E2E_TEST_IDENTITY"));
		String password = environment.getOrDefault("E2E_TEST_PASSWORD", "");
		Optional<TestDoor> testDoor = testDoorOn && !password.isEmpty()
				? Optional.of(new TestDoor(environment.getOrDefault("E2E_TEST_EMAIL", TEST_IDENTITY_EMAIL), password))
				: Optional.empty();
		return new AuthSettings(
				clientIds,
				environment.getOrDefault("GOOGLE_CLIENT_SECRET", ""),
				environment.getOrDefault("PUBLIC_BASE_URL", "http://127.0.0.1:" + port),
				testDoor);
	}

	public String callbackUrl() {
		return publicBaseUrl + "/auth/google/callback";
	}

	@Override
	public String toString() {
		return "AuthSettings[googleClientIds=" + googleClientIds + ", publicBaseUrl=" + publicBaseUrl + ", testDoor=" + testDoor.isPresent() + "]";
	}
}
