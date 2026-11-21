package pl.umcs.auth.domain;

public record ProviderClaims(Provider provider, String subject, String email, boolean emailVerified) {
}
