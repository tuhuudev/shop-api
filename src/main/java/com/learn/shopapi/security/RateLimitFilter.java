package com.learn.shopapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Gioi han so request toi cac endpoint XAC THUC theo TUNG IP, dem TREN REDIS (chia se giua replica).
 *
 * Cua so co dinh 1 phut, atomic bang Lua (INCR + EXPIRE) -> khong race giua nhieu node/luong.
 * Bo sung cho LoginAttemptService (khoa theo username): chan credential-stuffing/spam tu 1 IP.
 *
 * IP: dung request.getRemoteAddr(). Sau reverse-proxy, bat server.forward-headers-strategy=framework
 * (xem application-prod) de getRemoteAddr() la IP client THAT -> KHONG tu parse X-Forwarded-For
 * (tranh bi gia mao header de vuot gioi han).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final RedisScript<Long> INCR_WITH_TTL = RedisScript.of(
            "local c = redis.call('INCR', KEYS[1]); "
                    + "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; return c",
            Long.class);

    private final boolean enabled;
    private final int maxPerMinute;
    private final StringRedisTemplate redis;

    public RateLimitFilter(@Value("${app.security.ratelimit.enabled:true}") boolean enabled,
                           @Value("${app.security.ratelimit.auth-per-minute:30}") int maxPerMinute,
                           StringRedisTemplate redis) {
        this.enabled = enabled;
        this.maxPerMinute = maxPerMinute;
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.endsWith("/api/auth/login")
                || uri.endsWith("/api/auth/register")
                || uri.endsWith("/api/auth/refresh")
                || uri.endsWith("/api/auth/change-password"));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        // Token khong duoc cache boi trinh duyet / proxy.
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");

        if (enabled && isOverLimit(request.getRemoteAddr())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Qua nhieu yeu cau tu IP nay, vui long thu lai sau.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isOverLimit(String ip) {
        long minute = Instant.now().getEpochSecond() / 60;
        String key = "shop:ratelimit:auth:" + ip + ":" + minute;
        Long count = redis.execute(INCR_WITH_TTL, List.of(key), "60");
        return count != null && count > maxPerMinute;
    }
}
