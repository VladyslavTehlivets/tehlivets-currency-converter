package pl.umcs.auth.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public record Session(UUID id, UUID memberId, Instant issuedAt, Instant expiresAt) {

	public static final Duration LIFETIME = Duration.ofDays(30);

	public static Session issuedTo(Member member, Instant now) {
		return new Session(UUID.randomUUID(), member.id(), now, now.plus(LIFETIME));
	}

	public boolean isLiveAt(Instant moment) {
		return moment.isBefore(expiresAt);
	}
}
