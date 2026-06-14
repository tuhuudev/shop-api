package com.learn.shopapi.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test (khong Spring context/DB) cho cau hinh CORS: bao dam preflight cho phep
 * header Idempotency-Key (can cho POST /orders, /cart/checkout cross-origin cua shop-client).
 */
class SecurityConfigCorsTest {

    @Test
    void corsAllowsIdempotencyKeyHeader() {
        SecurityConfig config = new SecurityConfig(null, null, null, "https://app.example.com");

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");

        assertThat(cors.getAllowedHeaders())
                .contains("Idempotency-Key", "Authorization", "Content-Type")
                .doesNotContain("*");   // van liet ke tuong minh vi allowCredentials=true
    }
}
