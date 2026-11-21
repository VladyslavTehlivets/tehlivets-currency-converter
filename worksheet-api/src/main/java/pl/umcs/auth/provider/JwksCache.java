package pl.umcs.auth.provider;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class JwksCache {

	private static final Duration UNKNOWN_KEY_REFETCH_GAP = Duration.ofSeconds(30);

	private final JwksSource source;
	private final Clock clock;
	private JwkSet cached;
	private Instant fetchedAt;

	public JwksCache(JwksSource source, Clock clock) {
		this.source = source;
		this.clock = clock;
	}

	public synchronized Optional<PublicKey> keyFor(String keyId) {
		Instant now = clock.instant();
		if (cached == null || !now.isBefore(fetchedAt.plus(cached.freshFor()))) {
			refetch(now);
		}
		PublicKey known = cached.keysById().get(keyId);
		if (known == null && !now.isBefore(fetchedAt.plus(UNKNOWN_KEY_REFETCH_GAP))) {
			refetch(now);
			known = cached.keysById().get(keyId);
		}
		return Optional.ofNullable(known);
	}

	private void refetch(Instant now) {
		cached = source.fetch();
		fetchedAt = now;
	}
}
