package com.learn.shopapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Unit test LoginAttemptService: dem lan sai tren Redis, va FAIL-OPEN khi Redis loi. */
@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> ops;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService(redis, 5, 15);
    }

    @Test
    void duSoLanSai_thiBiKhoa() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("shop:login-fail:alice")).thenReturn("5");
        assertThat(service.isBlocked("alice")).isTrue();
    }

    @Test
    void chuaDuSoLanSai_khongKhoa() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get("shop:login-fail:alice")).thenReturn("4");
        assertThat(service.isBlocked("alice")).isFalse();
    }

    @Test
    void redisSap_khongChanDangNhap_vaKhongNemLoi() {
        RedisConnectionFailureException down = new RedisConnectionFailureException("Unable to connect to Redis");
        when(redis.opsForValue()).thenThrow(down);
        when(redis.execute(any(), anyList(), anyString())).thenThrow(down);
        when(redis.delete(anyString())).thenThrow(down);

        assertThat(service.isBlocked("alice")).isFalse();
        assertThatCode(() -> service.recordFailure("alice")).doesNotThrowAnyException();
        assertThatCode(() -> service.recordSuccess("alice")).doesNotThrowAnyException();
    }
}
