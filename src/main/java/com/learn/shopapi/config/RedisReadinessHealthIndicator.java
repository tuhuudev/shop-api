package com.learn.shopapi.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Readiness health indicator cho Redis ngoai: KHONG chi PING ma lam round-trip THUC SU
 * (SET key co TTL ngan -> GET -> so khop). Phat hien Redis read-only / mat quyen ghi / sai
 * data path ma PING van bao OK. Dat trong nhom readiness -> pod bi danh NOT READY khi Redis
 * ghi/doc hong (Redis dang giu rate-limit, login-attempt, idempotency).
 *
 * Ten contributor = "redisReadiness" (Spring tu bo hau to "HealthIndicator"). Nhe, KHONG nem ra ngoai.
 */
@Component
public class RedisReadinessHealthIndicator implements HealthIndicator {

    // Key RIENG cho moi instance (sinh 1 lan luc khoi tao). Neu dung key chung, 2 pod chay
    // readiness probe cung cua so TTL se ghi de gia tri cua nhau -> GET doc ve gia tri pod khac
    // -> mismatch -> DOWN gia -> k8s rut pod khoi service du Redis van khoe. Key per-instance
    // cach ly hoan toan round-trip giua cac pod.
    private static final String KEY_PREFIX = "__health:readiness:";
    private static final Duration TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redis;
    private final String key = KEY_PREFIX + UUID.randomUUID();

    public RedisReadinessHealthIndicator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Health health() {
        String expected = UUID.randomUUID().toString();
        try {
            redis.opsForValue().set(key, expected, TTL);
            String actual = redis.opsForValue().get(key);
            if (expected.equals(actual)) {
                return Health.up().withDetail("redis", "write+read round-trip OK").build();
            }
            // Ket noi duoc nhung gia tri doc lai khong khop -> Redis khong dang tin -> DOWN.
            return Health.down()
                    .withDetail("redis", "round-trip mismatch")
                    .withDetail("expected", expected)
                    .withDetail("actual", String.valueOf(actual))
                    .build();
        } catch (RuntimeException ex) {
            // Bat moi loi runtime (mat ket noi, timeout, auth...) -> DOWN, KHONG nem ra ngoai.
            return Health.down(ex).build();
        }
    }
}
