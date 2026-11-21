package pl.umcs.auth.domain;

import pl.umcs.auth.application.Members;
import pl.umcs.auth.application.Sessions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class InMemoryMembers implements Members, Sessions {

	private final List<Member> members = new ArrayList<>();
	private final Map<UUID, Session> sessions = new HashMap<>();
	private final Set<String> nonces = new HashSet<>();

	@Override
	public Optional<Member> findByIdentity(Provider provider, String subject) {
		return members.stream().filter(member -> member.identities().stream().anyMatch(identity -> identity.provider() == provider && identity.subject().equals(subject))).findFirst();
	}

	@Override
	public Optional<Member> findByVerifiedEmail(String email) {
		return members.stream().filter(member -> member.verifiedEmail().equals(email)).findFirst();
	}

	@Override
	public void save(Member member) {
		if (!members.contains(member)) {
			members.add(member);
		}
	}

	int memberCount() {
		return members.size();
	}

	@Override
	public void add(Session session) {
		sessions.put(session.id(), session);
	}

	@Override
	public Optional<Session> find(UUID id) {
		return Optional.ofNullable(sessions.get(id));
	}

	@Override
	public void issueNonce(String nonce, Instant expiresAt) {
		nonces.add(nonce);
	}

	@Override
	public boolean consumeNonce(String nonce, Instant now) {
		return nonces.remove(nonce);
	}
}
