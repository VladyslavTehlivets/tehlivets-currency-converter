package pl.umcs.auth.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Member {

	private final UUID id;
	private final String verifiedEmail;
	private final Instant createdAt;
	private final List<ProviderIdentity> identities;

	public Member(UUID id, String verifiedEmail, Instant createdAt, List<ProviderIdentity> identities) {
		this.id = id;
		this.verifiedEmail = verifiedEmail;
		this.createdAt = createdAt;
		this.identities = new ArrayList<>(identities);
	}

	public static Member firstSeenThrough(Assertion.ProviderAssertion assertion, Instant now) {
		Member member = new Member(UUID.randomUUID(), assertion.email(), now, List.of());
		member.link(assertion, now);
		return member;
	}

	public static Member firstSeenAtTheTestDoor(String email, Instant now) {
		return new Member(UUID.randomUUID(), email, now, List.of());
	}

	public boolean hasIdentityAt(Provider provider) {
		return identity(provider).isPresent();
	}

	public Optional<ProviderIdentity> identity(Provider provider) {
		return identities.stream().filter(identity -> identity.provider() == provider).findFirst();
	}

	public ProviderIdentity link(Assertion.ProviderAssertion assertion, Instant now) {
		if (!assertion.emailVerified()) {
			throw new SignInRefusal(SignInRefusal.Code.EMAIL_UNVERIFIED);
		}
		if (hasIdentityAt(assertion.provider())) {
			throw new SignInRefusal(SignInRefusal.Code.NOT_INVITED);
		}
		ProviderIdentity linked = new ProviderIdentity(assertion.provider(), assertion.subject(), assertion.email(), now);
		identities.add(linked);
		return linked;
	}

	public UUID id() {
		return id;
	}

	public String verifiedEmail() {
		return verifiedEmail;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public List<ProviderIdentity> identities() {
		return List.copyOf(identities);
	}
}
