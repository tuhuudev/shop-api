package com.learn.shopapi.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test RateLimitFilter: 429 khi vuot nguong, cho qua khi duoi nguong, FAIL-OPEN khi Redis loi. */
class RateLimitFilterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RateLimitFilter filter = new RateLimitFilter(true, 30, redis);

    private MockHttpServletResponse run(FilterChain chain) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res;
    }

    @Test
    void duoiNguong_choQua() throws Exception {
        when(redis.execute(any(), anyList(), anyString())).thenReturn(30L);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse res = run(chain);

        verify(chain).doFilter(any(), any());
        assertThat(res.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void vuotNguong_tra429_khongGoiTiep() throws Exception {
        when(redis.execute(any(), anyList(), anyString())).thenReturn(31L);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse res = run(chain);

        assertThat(res.getStatus()).isEqualTo(429);
        assertThat(res.getHeader("Retry-After")).isEqualTo("60");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void redisSap_failOpen_vanChoDangNhap() throws Exception {
        when(redis.execute(any(), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletResponse res = run(chain);

        verify(chain).doFilter(any(), any());
        assertThat(res.getStatus()).isEqualTo(200);
    }
}
