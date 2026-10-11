package rw.ac.rca.isoko.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklist {

    private static final String PREFIX = "cloudsphere:revoked:";
    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.opsForValue().set(PREFIX + jti, "1", ttl);
    }

    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }

    public void startSession(String jti) {
        redis.opsForValue().set("isoko:session:" + jti, "active", Duration.ofMinutes(30));
    }

    public boolean touchSession(String jti) {
        var script = new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                "if redis.call('EXISTS',KEYS[1]) == 1 then redis.call('EXPIRE',KEYS[1],1800); return 1 else return 0 end", Long.class);
        return Long.valueOf(1).equals(redis.execute(script, java.util.List.of("isoko:session:" + jti)));
    }
}
