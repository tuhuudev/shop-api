package com.learn.shopapi.common;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Cho Spring Data biet "nguoi dung hien tai la ai" de dien vao cot createdBy/updatedBy.
 *
 * Lay username tu SecurityContext (noi Spring Security luu thong tin nguoi da dang nhap).
 * Neu chua dang nhap (vi du DataSeeder chay luc khoi dong) -> tra "system".
 */
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of("system");
        }
        return Optional.of(auth.getName());
    }
}
