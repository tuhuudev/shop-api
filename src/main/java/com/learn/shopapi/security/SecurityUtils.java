package com.learn.shopapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Tien ich nho: lay username cua nguoi dang dang nhap o bat ky dau (service/controller).
 * Vi du OrderService dung de biet "ai dang dat don".
 */
public final class SecurityUtils {

    private SecurityUtils() { }

    public static Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(auth.getName());
    }

    /** True neu nguoi dang dang nhap co it nhat 1 trong cac vai tro truyen vao (vd "STAFF","ADMIN"). */
    public static boolean hasAnyRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (String role : roles) {
            String authority = "ROLE_" + role;
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (authority.equals(ga.getAuthority())) return true;
            }
        }
        return false;
    }
}
