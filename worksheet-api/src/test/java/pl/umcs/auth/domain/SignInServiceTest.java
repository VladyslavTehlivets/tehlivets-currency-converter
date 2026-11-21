package pl.umcs.auth.domain;

import org.junit.jupiter.api.Test;
import pl.umcs.auth.application.SignInService;
import pl.umcs.auth.domain.Assertion.ProviderAssertion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignInServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-21T10:00:00Z");

	private final InMemoryMembers store = new InMemoryMembers();
	private final SignInService service = new SignInService(store, store, Clock.fixed(NOW, ZoneOffset.UTC));

	private static ProviderAssertion google(String subject, String email, boolean verified) {
		return new ProviderAssertion(Provider.GOOGLE, subject, email, verified);
	}

	private static ProviderAssertion github(String id, String email, boolean verified) {
		return new ProviderAssertion(Provider.GITHUB, id, email, verified);
	}

	@Test
	void a_google_assertion_creates_a_member_on_first_sign_in() {
		Session session = service.signIn(google("g-1", "ada@example.com", true));

		assertEquals(1, store.memberCount());
		assertEquals("ada@example.com", store.findByIdentity(Provider.GOOGLE, "g-1").orElseThrow().verifiedEmail());
		assertEquals(session.memberId(), store.findByIdentity(Provider.GOOGLE, "g-1").orElseThrow().id());
		assertTrue(session.isLiveAt(NOW.plusSeconds(60)));
	}

	@Test
	void a_returning_google_assertion_returns_the_same_member_with_a_fresh_session() {
		Session first = service.signIn(google("g-1", "ada@example.com", true));
		Session second = service.signIn(google("g-1", "ada@example.com", true));

		assertEquals(first.memberId(), second.memberId());
		assertNotEquals(first.id(), second.id());
		assertEquals(1, store.memberCount());
	}

	@Test
	void an_unverified_github_address_never_joins_an_existing_member() {
		Session existing = service.signIn(google("g-1", "ada@example.com", true));

		SignInRefusal refusal = assertThrows(SignInRefusal.class, () -> service.signIn(github("42", "ada@example.com", false)));

		assertEquals(SignInRefusal.Code.EMAIL_UNVERIFIED, refusal.code());
		assertEquals(1, store.memberCount());
		assertTrue(store.findByIdentity(Provider.GITHUB, "42").isEmpty());
		assertEquals(existing.memberId(), store.findByIdentity(Provider.GOOGLE, "g-1").orElseThrow().id());
	}

	@Test
	void a_verified_github_address_joins_the_member_with_that_verified_email() {
		Session existing = service.signIn(google("g-1", "ada@example.com", true));

		Session viaGithub = service.signIn(github("42", "ada@example.com", true));

		assertEquals(existing.memberId(), viaGithub.memberId());
		assertEquals(2, store.findByIdentity(Provider.GITHUB, "42").orElseThrow().identities().size());
	}

	@Test
	void a_member_stays_the_same_member_after_an_email_change_at_the_provider() {
		Session before = service.signIn(google("g-1", "ada@example.com", true));

		Session after = service.signIn(google("g-1", "ada@new-home.example", true));

		assertEquals(before.memberId(), after.memberId());
		assertEquals(1, store.memberCount());
	}

	@Test
	void a_github_member_keeps_access_after_a_login_rename() {
		Session before = service.signIn(github("42", "grace@example.com", true));

		Session after = service.signIn(new ProviderAssertion(Provider.GITHUB, "42", "grace@example.com", true));

		assertEquals(before.memberId(), after.memberId());
	}

	@Test
	void an_existing_member_who_first_signs_in_with_google_keeps_the_account_and_its_content() {
		Member existing = Member.firstSeenAtTheTestDoor("grace@example.com", NOW.minusSeconds(86400));
		store.save(existing);

		Session session = service.signIn(google("g-9", "grace@example.com", true));

		assertEquals(existing.id(), session.memberId());
		assertEquals(NOW.minusSeconds(86400), store.findByIdentity(Provider.GOOGLE, "g-9").orElseThrow().createdAt());
		assertEquals(1, store.memberCount());
	}

	@Test
	void an_unverified_first_sign_in_creates_no_member() {
		SignInRefusal refusal = assertThrows(SignInRefusal.class, () -> service.signIn(google("g-2", "eve@example.com", false)));

		assertEquals(SignInRefusal.Code.EMAIL_UNVERIFIED, refusal.code());
		assertEquals(0, store.memberCount());
	}

	@Test
	void a_second_subject_at_the_same_provider_never_joins_a_member_that_already_has_one() {
		service.signIn(google("g-1", "ada@example.com", true));

		SignInRefusal refusal = assertThrows(SignInRefusal.class, () -> service.signIn(google("g-other", "ada@example.com", true)));

		assertEquals(SignInRefusal.Code.NOT_INVITED, refusal.code());
		assertTrue(store.findByIdentity(Provider.GOOGLE, "g-other").isEmpty());
	}

	@Test
	void the_test_door_signs_the_same_member_in_again() {
		Session first = service.signIn(new Assertion.TestDoorAssertion("factory@example.com"));
		Session second = service.signIn(new Assertion.TestDoorAssertion("factory@example.com"));

		assertEquals(first.memberId(), second.memberId());
		assertEquals(1, store.memberCount());
	}
}
