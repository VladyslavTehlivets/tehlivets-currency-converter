package pl.umcs.auth.application;

import pl.umcs.auth.domain.Session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Sessions {

	void add(Session session);

	Optional<Session> find(UUID id);

	void issueNonce(String nonce, Instant expiresAt);

	boolean consumeNonce(String nonce, Instant now);
}
