package pl.umcs.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record Session(UUID id, MemberId member, Instant issuedAt, Instant expiresAt) {
}
