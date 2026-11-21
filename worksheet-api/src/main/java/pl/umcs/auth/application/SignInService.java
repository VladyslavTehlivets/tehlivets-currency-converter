package pl.umcs.auth.application;

import pl.umcs.auth.domain.Assertion;
import pl.umcs.auth.domain.MemberId;
import pl.umcs.auth.domain.ProviderClaims;
import pl.umcs.auth.domain.RefusalCode;
import pl.umcs.auth.domain.Session;
import pl.umcs.auth.domain.SignInRefused;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

public final class SignInService {

	private final AssertionVerifier verifier;
	private final Members members;
	private final Sessions sessions;
	private final Clock clock;

	public SignInService(AssertionVerifier verifier, Members members, Sessions sessions, Clock clock) {
		this.verifier = verifier;
		this.members = members;
		this.sessions = sessions;
		this.clock = clock;
	}

	public Session signIn(Assertion assertion) {
		ProviderClaims claims = verifier.verify(assertion);
		return sessions.issue(memberFor(claims), clock.instant());
	}

	private MemberId memberFor(ProviderClaims claims) {
		Optional<MemberId> known = members.findByIdentity(claims.provider(), claims.subject());
		if (known.isPresent()) {
			return known.get();
		}
		if (!claims.emailVerified() || claims.email() == null || claims.email().isBlank()) {
			throw new SignInRefused(RefusalCode.EMAIL_UNVERIFIED);
		}
		String email = claims.email().strip().toLowerCase(Locale.ROOT);
		try {
			Optional<MemberId> sameEmail = members.findByVerifiedEmail(email);
			if (sameEmail.isPresent()) {
				members.link(sameEmail.get(), claims.provider(), claims.subject());
				return sameEmail.get();
			}
			return members.createWithIdentity(email, claims.provider(), claims.subject());
		} catch (Members.IdentityConflict conflict) {
			return members.findByIdentity(claims.provider(), claims.subject())
					.orElseThrow(() -> new SignInRefused(RefusalCode.NOT_INVITED));
		}
	}
}
