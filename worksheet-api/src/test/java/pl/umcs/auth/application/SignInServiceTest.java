package pl.umcs.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.MemberId;
import pl.umcs.auth.domain.Provider;
import pl.umcs.auth.domain.ProviderClaims;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.Session;
import pl.umcs.auth.domain.SignInRefused;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignInServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-02T10:00:00Z");

	private static final class SessionLedger implements Sessions {
		private final List<Session> issued = new ArrayList<>();

		@Override
		public Session issue(MemberId member, Instant now) {
			Session session = new Session(UUID.randomUUID(), member, now, now.plusSeconds(3600));
			issued.add(session);
			return session;
		}

		@Override
		public Optional<MemberId> memberOf(UUID sessionId, Instant now) {
			return issued.stream().filter(session -> session.id().equals(sessionId)).map(Session::member).findFirst();
		}
	}

	private InMemoryMembers members;
	private SessionLedger sessions;
	private SignInService service;
	private ProviderClaims presented;

	@BeforeEach
	void aServiceWhoseProviderReportsWhateverWasPresented() {
		members = new InMemoryMembers();
		sessions = new SessionLedger();
		service = new SignInService(assertion -> presented, members, sessions, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private Session signInWith(ProviderClaims claims) {
		presented = claims;
		return service.signIn(new Assertion.GitHubCode("code", "https://app/callback"));
	}

	private static ProviderClaims google(String subject, String email, boolean verified) {
		return new ProviderClaims(Provider.GOOGLE, subject, email, verified);
	}

	private static ProviderClaims github(String numericId, String email, boolean verified) {
		return new ProviderClaims(Provider.GITHUB, numericId, email, verified);
	}

	@Test
	void a_google_assertion_creates_a_member_on_first_sign_in() {
		Session session = signInWith(google("g-1", "ada@example.com", true));

		assertEquals(1, members.memberCount());
		assertEquals(NOW, session.issuedAt());
		assertEquals(members.findByIdentity(Provider.GOOGLE, "g-1"), Optional.of(session.member()));
	}

	@Test
	void a_returning_google_assertion_returns_the_same_member() {
		Session first = signInWith(google("g-1", "ada@example.com", true));
		Session second = signInWith(google("g-1", "ada@example.com", true));

		assertEquals(first.member(), second.member());
		assertNotEquals(first.id(), second.id());
		assertEquals(1, members.memberCount());
	}

	@Test
	void a_member_stays_the_same_member_after_an_email_change_at_the_provider() {
		Session before = signInWith(google("g-1", "ada@example.com", true));
		Session after = signInWith(google("g-1", "ada.lovelace@example.org", true));

		assertEquals(before.member(), after.member());
		assertEquals(1, members.memberCount());
	}

	@Test
	void a_github_member_keeps_access_after_a_login_rename() {
		Session before = signInWith(github("583231", "octo@example.com", true));
		Session afterRename = signInWith(github("583231", "octo@example.com", true));

		assertEquals(before.member(), afterRename.member());
	}

	@Test
	void an_existing_member_who_first_signs_in_with_google_keeps_the_account() {
		Session viaGitHub = signInWith(github("583231", "ada@example.com", true));
		Session viaGoogle = signInWith(google("g-1", "Ada@Example.com ", true));

		assertEquals(viaGitHub.member(), viaGoogle.member());
		assertEquals(1, members.memberCount());
		assertEquals(Optional.of(viaGitHub.member()), members.findByIdentity(Provider.GOOGLE, "g-1"));
	}

	@Test
	void an_unverified_github_address_never_joins_an_existing_member() {
		Session owner = signInWith(google("g-1", "ada@example.com", true));

		SignInRefused refused = assertThrows(SignInRefused.class, () -> signInWith(github("999", "ada@example.com", false)));

		assertEquals(RefusalCode.EMAIL_UNVERIFIED, refused.code());
		assertEquals(Optional.empty(), members.findByIdentity(Provider.GITHUB, "999"));
		assertEquals(Optional.of(owner.member()), members.findByIdentity(Provider.GOOGLE, "g-1"));
	}

	@Test
	void an_unverified_first_sign_in_creates_no_member() {
		assertThrows(SignInRefused.class, () -> signInWith(google("g-2", "new@example.com", false)));

		assertEquals(0, members.memberCount());
	}

	@Test
	void a_provider_without_any_address_creates_no_member() {
		assertThrows(SignInRefused.class, () -> signInWith(google("g-3", null, true)));

		assertEquals(0, members.memberCount());
	}

	@Test
	void a_second_google_identity_never_joins_a_member_who_already_has_one() {
		Session owner = signInWith(google("g-1", "ada@example.com", true));

		SignInRefused refused = assertThrows(SignInRefused.class, () -> signInWith(google("g-other", "ada@example.com", true)));

		assertEquals(RefusalCode.NOT_INVITED, refused.code());
		assertEquals(Optional.empty(), members.findByIdentity(Provider.GOOGLE, "g-other"));
		assertEquals(Optional.of(owner.member()), members.findByIdentity(Provider.GOOGLE, "g-1"));
	}

	@Test
	void a_provider_refusal_reaches_the_caller_and_issues_no_session() {
		service = new SignInService(assertion -> {
			throw new SignInRefused(RefusalCode.TOKEN_FOR_ANOTHER_CLIENT);
		}, members, sessions, Clock.fixed(NOW, ZoneOffset.UTC));

		SignInRefused refused = assertThrows(SignInRefused.class, () -> service.signIn(new Assertion.GitHubCode("c", "r")));

		assertEquals(RefusalCode.TOKEN_FOR_ANOTHER_CLIENT, refused.code());
		assertEquals(0, sessions.issued.size());
	}
}
