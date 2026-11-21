package pl.umcs.auth.application;

import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.Member;
import pl.umcs.auth.domain.Session;
import pl.umcs.auth.domain.SignInRefusal;

import java.time.Clock;
import java.util.Optional;

public final class SignInService {

	private final Members members;
	private final Sessions sessions;
	private final Clock clock;

	public SignInService(Members members, Sessions sessions, Clock clock) {
		this.members = members;
		this.sessions = sessions;
		this.clock = clock;
	}

	public Session signIn(Assertion assertion) {
		Member member = switch (assertion) {
			case Assertion.ProviderAssertion provider -> memberFor(provider);
			case Assertion.TestDoorAssertion testDoor -> members.findByVerifiedEmail(testDoor.email())
					.orElseGet(() -> saved(Member.firstSeenAtTheTestDoor(testDoor.email(), clock.instant())));
		};
		Session session = Session.issuedTo(member, clock.instant());
		sessions.add(session);
		return session;
	}

	private Member memberFor(Assertion.ProviderAssertion assertion) {
		Optional<Member> known = members.findByIdentity(assertion.provider(), assertion.subject());
		if (known.isPresent()) {
			return known.get();
		}
		if (!assertion.emailVerified()) {
			throw new SignInRefusal(SignInRefusal.Code.EMAIL_UNVERIFIED);
		}
		Optional<Member> sameEmail = members.findByVerifiedEmail(assertion.email());
		if (sameEmail.isPresent()) {
			Member existing = sameEmail.get();
			existing.link(assertion, clock.instant());
			members.save(existing);
			return existing;
		}
		return saved(Member.firstSeenThrough(assertion, clock.instant()));
	}

	private Member saved(Member member) {
		members.save(member);
		return member;
	}
}
