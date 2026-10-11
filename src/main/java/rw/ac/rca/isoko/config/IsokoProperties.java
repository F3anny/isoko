package rw.ac.rca.isoko.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "cloudsphere")
public record IsokoProperties(
        Jwt jwt,
        Lockout lockout,
        Payment payment,
        Cors cors,
        Seed seed
) {
    public record Jwt(String secret, String issuer, Duration ttl) {}
    public record Lockout(int maxAttempts, Duration duration) {}
    public record Payment(StubMode stubMode) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Seed(boolean enabled) {}

    public enum StubMode {
        SUCCESS, DECLINE, TIMEOUT
    }
}
