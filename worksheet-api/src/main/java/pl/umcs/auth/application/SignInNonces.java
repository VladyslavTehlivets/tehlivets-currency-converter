package pl.umcs.auth.application;

import java.time.Instant;

public interface SignInNonces {

	String issue(Instant now);

	boolean consume(String nonce, Instant now);
}
