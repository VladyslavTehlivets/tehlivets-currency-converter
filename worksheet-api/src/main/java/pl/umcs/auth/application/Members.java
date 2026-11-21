package pl.umcs.auth.application;

import pl.umcs.auth.domain.Member;
import pl.umcs.auth.domain.Provider;

import java.util.Optional;

public interface Members {

	Optional<Member> findByIdentity(Provider provider, String subject);

	Optional<Member> findByVerifiedEmail(String email);

	void save(Member member);
}
