package pl.umcs.auth.domain;

import java.time.Instant;

public record ProviderIdentity(Provider provider, String subject, String verifiedEmail, Instant linkedAt) {
}
