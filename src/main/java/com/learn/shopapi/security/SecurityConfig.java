package com.learn.shopapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Trung tam cau hinh bao mat.
 *
 * - @EnableMethodSecurity: bat @PreAuthorize tren cac method controller/service.
 * - SecurityFilterChain: dinh nghia "luat" cho moi request (cai gi public, cai gi can quyen).
 * - Stateless + tat CSRF: vi ta dung JWT, KHONG dung session/cookie nen khong can chong CSRF.
 * - Gan JwtAuthenticationFilter TRUOC bo loc dang nhap chuan de doc token o moi request.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    private final List<String> corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RestAuthEntryPoint authEntryPoint,
                          RestAccessDeniedHandler accessDeniedHandler,
                          @Value("${app.cors.allowed-origins:}") String corsAllowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.corsAllowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Bat CORS theo cau hinh (xem corsConfigurationSource ben duoi).
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Tat CSRF: an toan vi API stateless dung JWT (khong dua tren cookie phien).
                .csrf(AbstractHttpConfigurer::disable)
                // Khong tao session: moi request tu chung minh bang token.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // --- Cong khai (khong can dang nhap) ---
                    // CHI mo cac endpoint auth cu the, KHONG mo ca /api/auth/** -> /me van can dang nhap
                    // (nho vay user bi khoa cam token cu se nhan 401 ro rang o /me).
                    auth.requestMatchers("/api/auth/register", "/api/auth/login",
                            "/api/auth/refresh", "/api/auth/logout").permitAll();
                    // Webhook cong thanh toan: goi server-to-server (khong co JWT) -> xac thuc bang
                    // chu ky HMAC trong PaymentService, KHONG bao ve bang Spring Security.
                    auth.requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll();
                    // Swagger UI + tai lieu OpenAPI
                    auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                    // Actuator: health cong khai (cho health-check); cac endpoint khac chi ADMIN.
                    auth.requestMatchers("/actuator/health/**").permitAll();
                    auth.requestMatchers("/actuator/**").hasRole("ADMIN");
                    // --- Con lai: bat buoc dang nhap; quyen chi tiet do @PreAuthorize quyet dinh ---
                    auth.anyRequest().authenticated();
                })
                // Tra JSON 401/403 thay vi trang dang nhap mac dinh.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // Doc token TRUOC khi toi bo loc dang nhap username/password mac dinh.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Security headers: HSTS (ep HTTPS khi da chay sau TLS), nosniff, chong clickjacking.
        http.headers(headers -> {
            headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true).maxAgeInSeconds(31536000));
            // X-Content-Type-Options: nosniff la mac dinh; giu nguyen.
            // Referrer-Policy: khong gui URL hien tai sang trang khac (tranh lo thong tin qua Referer).
            headers.referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
            // Permissions-Policy: tat cac quyen trinh duyet khong dung den (giam be mat tan cong).
            headers.permissionsPolicyHeader(pp -> pp.policy("geolocation=(), camera=(), microphone=(), payment=()"));
            // Chong clickjacking: cam nhung trang khac nhung shop-api vao iframe.
            headers.frameOptions(frame -> frame.deny());
        });

        return http.build();
    }

    /**
     * Cau hinh CORS: chi cho cac origin liet ke trong app.cors.allowed-origins goi API tu trinh duyet.
     * De trong -> khong origin nao duoc phep (chi same-origin), an toan mac dinh.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // Cho phep gui cookie (refresh token httpOnly) kem request cross-origin.
        // Bat buoc allowedOrigins phai cu the (khong duoc "*") - da dam bao o tren.
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Ma hoa mat khau bang BCrypt (mau Strategy: co the doi thuat toan ma khong sua cho khac). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** AuthenticationManager dung o AuthService luc dang nhap (login). */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
