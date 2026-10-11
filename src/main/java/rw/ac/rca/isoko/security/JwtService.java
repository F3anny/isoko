package rw.ac.rca.isoko.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import rw.ac.rca.isoko.config.IsokoProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final IsokoProperties properties;
    private final SecretKey key;
    private final TokenBlacklist sessions;

    public JwtService(IsokoProperties properties, TokenBlacklist sessions) {
        this.properties = properties;
        this.sessions = sessions;
        byte[] bytes = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String issue(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.jwt().ttl());
        String jti = UUID.randomUUID().toString();
        sessions.startSession(jti);
        return Jwts.builder()
                .id(jti)
                .issuer(properties.jwt().issuer())
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.jwt().issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Instant expiry(Claims claims) {
        return claims.getExpiration().toInstant();
    }
}
