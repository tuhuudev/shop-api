package com.learn.shopapi.config;

import com.learn.shopapi.common.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Bat tinh nang JPA Auditing cho toan ung dung.
 *
 * - @EnableJpaAuditing: kich hoat viec tu dong dien @CreatedDate/@CreatedBy...
 * - auditorAware bean: nguon "nguoi dung hien tai" cho cot createdBy/updatedBy.
 * - dateTimeProvider: nguon "thoi gian hien tai" cho cot createdAt/updatedAt.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware", dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareImpl();
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now());
    }
}
