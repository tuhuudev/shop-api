package com.learn.shopapi.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * Quan ly cookie chua REFRESH TOKEN.
 *
 * Vi sao dung cookie httpOnly thay vi tra refresh token ve cho JavaScript?
 *  - httpOnly: JavaScript KHONG doc duoc cookie -> neu trang dinh XSS, ke tan cong
 *    cung khong lay duoc refresh token (token dai han, nguy hiem nhat).
 *  - SameSite: chong gui cookie kem trong request cross-site (giam CSRF).
 *  - Path=/api/auth: cookie CHI duoc gui toi cac endpoint auth (refresh/logout),
 *    khong di kem moi request API khac -> giam be mat lo.
 *
 * Access token (ngan han) van tra trong body de client giu TRONG BO NHO.
 */
@Component
public class RefreshTokenCookie {

    public static final String PATH = "/api/auth";

    private final String name;
    private final boolean secure;
    private final String sameSite;
    private final long maxAgeSeconds;

    public RefreshTokenCookie(
            @Value("${app.auth.cookie.name:refresh_token}") String name,
            @Value("${app.auth.cookie.secure:false}") boolean secure,
            @Value("${app.auth.cookie.same-site:Lax}") String sameSite,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshExpirationMs) {
        this.name = name;
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeSeconds = refreshExpirationMs / 1000;
    }

    /** Dat cookie refresh token (goi sau login/register/refresh). */
    public void set(HttpServletResponse response, String token) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(token, maxAgeSeconds).toString());
    }

    /** Xoa cookie (goi khi logout): maxAge=0 -> trinh duyet xoa ngay. */
    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build("", 0).toString());
    }

    /** Doc gia tri refresh token tu cookie cua request. */
    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private ResponseCookie build(String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
