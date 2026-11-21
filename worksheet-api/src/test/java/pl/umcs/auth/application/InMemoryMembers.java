package pl.umcs.auth.application;

import pl.umcs.auth.domain.MemberId;
import pl.umcs.auth.domain.Provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class InMemoryMembers implements Members {

	private record Identity(Provider provider, String subject) {
	}

	private final Map<Identity, MemberId> owners = new HashMap<>();
	private final Map<String, MemberId> byEmail = new HashMap<>();

	@Override
	public Optional<MemberId> findByIdentity(Provider provider, String subject) {
		return Optional.ofNullable(owners.get(new Identity(provider, subject)));
	}

	@Override
	public Optional<MemberId> findByVerifiedEmail(String email) {
		return Optional.ofNullable(byEmail.get(email));
	}

	@Override
	public MemberId createWithIdentity(String verifiedEmail, Provider provider, String subject) {
		MemberId member = new MemberId(UUID.randomUUID());
		byEmail.put(verifiedEmail, member);
		owners.put(new Identity(provider, subject), member);
		return member;
	}

	@Override
	public void link(MemberId member, Provider provider, String subject) {
		boolean memberHasThisProvider = owners.entrySet().stream().anyMatch(owned -> owned.getValue().equals(member) && owned.getKey().provider() == provider);
		if (memberHasThisProvider || owners.containsKey(new Identity(provider, subject))) {
			throw new IdentityConflict();
		}
		owners.put(new Identity(provider, subject), member);
	}

	int memberCount() {
		return byEmail.size();
	}
}
