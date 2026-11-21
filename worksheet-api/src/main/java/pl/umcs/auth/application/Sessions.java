package pl.umcs.auth.application;

import pl.umcs.auth.domain.MemberId;
import pl.umcs.auth.domain.Session;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Sessions {

	Session issue(MemberId member, Instant now);

	Optional<MemberId> memberOf(UUID sessionId, Instant now);
}
