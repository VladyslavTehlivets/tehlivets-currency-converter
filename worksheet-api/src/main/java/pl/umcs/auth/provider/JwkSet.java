package pl.umcs.auth.provider;

import java.security.PublicKey;
import java.time.Duration;
import java.util.Map;

public record JwkSet(Map<String, PublicKey> keysById, Duration freshFor) {
}
