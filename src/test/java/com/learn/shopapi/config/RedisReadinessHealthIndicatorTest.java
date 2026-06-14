package com.learn.shopapi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Unit test thuan (Mockito, KHONG can Docker/Testcontainers) cho RedisReadinessHealthIndicator:
 * round-trip ghi/doc OK -> UP; Redis nem RuntimeException -> DOWN (khong nem ra ngoai).
 */
@ExtendWith(MockitoExtension.class)
class RedisReadinessHealthIndicatorTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> ops;

    @Test
    void roundTripOk_statusUp() {
        String[] stored = new String[1];
        when(redis.opsForValue()).thenReturn(ops);
        // SET luu lai gia tri; GET tra ve dung gia tri da set -> round-trip khop.
        doAnswer(inv -> { stored[0] = inv.getArgument(1); return null; })
                .when(ops).set(anyString(), anyString(), any(Duration.class));
        when(ops.get(anyString())).thenAnswer(inv -> stored[0]);

        Health health = new RedisReadinessHealthIndicator(redis).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void redisThrows_statusDown() {
        when(redis.opsForValue()).thenReturn(ops);
        doThrow(new RuntimeException("connection refused"))
                .when(ops).set(anyString(), anyString(), any(Duration.class));

        Health health = new RedisReadinessHealthIndicator(redis).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void roundTripMismatch_statusDown() {
        when(redis.opsForValue()).thenReturn(ops);
        // GET tra ve gia tri KHAC voi gia tri da set -> Redis khong dang tin -> DOWN.
        when(ops.get(anyString())).thenReturn("some-other-value");

        Health health = new RedisReadinessHealthIndicator(redis).health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
