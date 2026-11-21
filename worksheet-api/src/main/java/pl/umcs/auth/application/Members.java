package pl.umcs.auth.application;

import pl.umcs.auth.domain.MemberId;
import pl.umcs.auth.domain.Provider;

import java.util.Optional;

public interface Members {

	final class IdentityConflict extends RuntimeException {
		public IdentityConflict() {
			super("provider identity conflict", null, false, false);
		}
	}

	Optional<MemberId> findByIdentity(Provider provider, String subject);

	Optional<MemberId> findByVerifiedEmail(String email);

	MemberId createWithIdentity(String verifiedEmail, Provider provider, String subject);

	void link(MemberId member, Provider provider, String subject);
}
