# CLAUDE.md — shop-api

## Mục đích & cách làm việc
API bán hàng **production-grade**, deploy **multi-instance/k8s**. Người dùng là dev ~7 năm — trao đổi ở
mức senior: súc tích, tập trung tính đúng đắn/đánh đổi/bảo mật, KHÔNG giải thích nhập môn. Trả lời tiếng Việt.
KHÔNG dùng Lombok (cố ý — code tường minh). Comment trong code viết không dấu; tài liệu có dấu.

## Stack
- Java 21, Spring Boot 4.1.0 (Maven, `mvnw`)
- Spring Web MVC, Data JPA, Bean Validation, Spring Security 7 (JWT **RS256**)
- **PostgreSQL only** (Flyway sở hữu schema, `ddl-auto=validate`). **Redis** cho state phân tán.
- Observability: Actuator + Prometheus + Micrometer Tracing (OTLP). jjwt 0.12.x, springdoc 2.8.x.

## Chạy / build (PowerShell, Windows)
```powershell
$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory | ? { $_.Name -like 'jdk-21*' } | Select -First 1).FullName
docker compose up -d db redis     # Postgres + Redis cho dev
.\mvnw.cmd spring-boot:run         # app tai http://localhost:8080 (default = Postgres + Redis localhost)
.\mvnw.cmd verify                  # test (unit + ArchUnit) + Checkstyle + Enforcer + JaCoCo report
.\mvnw.cmd -Pci verify             # CI: them OWASP + SpotBugs + coverage gate (cham, can NVD key)
docker compose up --build          # chay full app+db+redis trong container
```
Profile `prod` (`--spring.profiles.active=prod`): Secure cookie, structured JSON log, ẩn lỗi, forward-headers.

## Kiến trúc
Luồng: `controller → service → repository → entity (DB)`. DTO tách entity khỏi API. Lỗi tập trung ở
`exception/GlobalExceptionHandler` theo **RFC 7807 ProblemDetail** (`application/problem+json`).
Schema + seed do **Flyway** (`db/migration/V1..V5`); KHÔNG còn DataSeeder.

Bảng nghiệp vụ: `Category 1─* Product`, `Order *─1 Customer`, `Order 1─* OrderItem *─1 Product`,
`Cart 1─* CartItem`, `Review`, RBAC (`User *─* Role *─* Permission`), `RefreshToken`, `LoginEvent`.
Bảng Order map `orders`. Tiền `BigDecimal`. `Product` + `Order` có `@Version` (optimistic lock).

### Bảo mật & phân quyền (RBAC + JWT RS256)
- `security/`: `JwtService` (RS256: ký bằng private key, verify public key — khoá dev ở `keys/*.pem`,
  prod override `APP_JWT_PRIVATE_KEY/PUBLIC_KEY`), `JwtAuthenticationFilter` (nạp lại user từ DB mỗi
  request → khoá/đổi quyền hiệu lực ngay), `SecurityConfig` (stateless, method security, headers HSTS/
  nosniff/frame-deny/referrer/permissions, CORS), handler 401/403.
- 3 role ADMIN/STAFF/CUSTOMER; chặn bằng `@PreAuthorize`. Refresh token httpOnly cookie
  (`RefreshTokenCookie`), lưu **băm SHA-256** ở DB, **xoay vòng** + phát hiện tái sử dụng.
- **Redis (multi-instance)**: rate-limit (`RateLimitFilter`, Lua INCR), login-attempts
  (`LoginAttemptService`), cache (categories `@Cacheable` + UserDetails). KHÔNG còn state in-memory.
- `Customer.user` (1-1) gắn tài khoản ↔ hồ sơ khách; CUSTOMER chỉ thao tác dữ liệu của mình.

### Nền tảng & tính năng
- **Audit** (`common/Auditable` + `JpaAuditingConfig`), **soft delete** (`Product` `@SQLDelete`/`@SQLRestriction`),
  **phân trang/lọc** (`PageResponse`, `ProductSpecifications`), **idempotency** (`IdempotencyService` +
  header `Idempotency-Key` cho tạo đơn/checkout), **access log** (`RequestLoggingFilter`).
- Nghiệp vụ: Category CRUD, Cart + checkout, Review, đổi mật khẩu, pay mock, logout-all.
- Data: report `revenue-by-category|-by-month|top-customers` (**Postgres `to_char`**), CSV, `@Scheduled`.

### Lưu ý kỹ thuật Spring Boot 4.1 (autoconfig tách module — dễ vấp)
- **Flyway** cần `spring-boot-flyway`; **Tracing** cần `spring-boot-micrometer-tracing` +
  `spring-boot-opentelemetry` (chỉ thư viện micrometer/otel là KHÔNG đủ — autoconfig không chạy).
- Boot 4 dùng **Jackson 3** cho HTTP → không có bean `ObjectMapper` (Jackson 2) để inject (handler tự new).
  Redis JSON serializer (Jackson 2) cần `jackson-datatype-jsr310` + `JavaTimeModule` (xem `RedisConfig`)
  để serialize `LocalDateTime`.
- Transaction: thao tác "phải commit dù caller ném lỗi" (thu hồi token khi reuse, ghi LoginEvent khi login
  fail) dùng `@Transactional(REQUIRES_NEW)` ở bean riêng — đừng gộp vào tx của caller.

### Guardrail tự động (xem `docs/CONVENTIONS.md`)
- **ArchUnit** ép phân tầng (mvnw test). **Checkstyle** cảnh báo (không fail). **Maven Enforcer** (Java≥21).
- **Testcontainers** (Postgres+Redis) cho integration test — chạy ở CI (Linux); local Windows có thể skip
  nếu Docker Desktop không kết nối được qua npipe (`disabledWithoutDocker`).
- **JaCoCo** report (build) + gate ở `-Pci`. **OWASP/SpotBugs** ở `-Pci` (report-only).

## Còn lại / caveat
- **API versioning `/api/v1`**: HOÃN — là breaking change cần đồng bộ với frontend `shop-client`; làm phối hợp.
- **Tracing traceId trên access-log**: span tạo ở scope DispatcherServlet nên dòng access-log (servlet filter,
  ngoài scope) có thể trống traceId; log controller/service vẫn có; span xuất OTLP khi set endpoint collector.
- Email-verify/MFA: ngoài phạm vi (cần SMTP/dịch vụ ngoài).
