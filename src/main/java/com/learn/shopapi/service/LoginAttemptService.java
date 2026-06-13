package com.learn.shopapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Chong do mat khau (brute-force): dem so lan dang nhap sai theo tung username, LUU TREN REDIS
 * -> chia se giua cac replica (multi-instance) thay vi dem trong RAM tung node.
 *
 * Cua so = block-minutes: sau N lan sai trong cua so do thi tai khoan bi coi la "blocked"
 * cho den khi key het han (TTL).
 */
@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "shop:login-fail:";

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration window;

    public LoginAttemptService(StringRedisTemplate redis,
                               @Value("${app.security.login.max-attempts}") int maxAttempts,
                               @Value("${app.security.login.block-minutes}") long blockMinutes) {
        this.redis = redis;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(blockMinutes);
    }

    public boolean isBlocked(String username) {
        String v = redis.opsForValue().get(key(username));
        return v != null && Integer.parseInt(v) >= maxAttempts;
    }

    /** Tang dem khi sai; lan dau dat TTL = cua so khoa (atomic INCR cua Redis). */
    public void recordFailure(String username) {
        Long count = redis.opsForValue().increment(key(username));
        if (count != null && count == 1L) {
            redis.expire(key(username), window);
        }
    }

    /** Dang nhap dung -> xoa lich su sai. */
    public void recordSuccess(String username) {
        redis.delete(key(username));
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
