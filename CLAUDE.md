# CLAUDE.md — shop-api

## Mục đích
Project **học tập** của một người **mới bắt đầu** BE & Data. Người dùng giao tiếp tiếng Việt.
Ưu tiên **giải thích để học**, không chỉ làm hộ. Khi sửa code, nói rõ *vì sao*, liên hệ khái niệm.

## Stack
- Java 21, Spring Boot 4.1.0 (Maven, dùng `mvnw`)
- Spring Web (MVC), Spring Data JPA, Bean Validation
- DB: H2 in-memory (mặc định) — có sẵn driver PostgreSQL để chuyển sau
- KHÔNG dùng Lombok (cố ý — để code tường minh cho người học)

## Chạy / build (PowerShell, Windows)
```powershell
$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory | ? { $_.Name -like 'jdk-21*' } | Select -First 1).FullName
.\mvnw.cmd spring-boot:run     # chạy app tại http://localhost:8080
.\mvnw.cmd test                # chạy test
.\mvnw.cmd -q -B compile       # chỉ biên dịch
```
JDK 21 cài qua winget (EclipseAdoptium.Temurin.21.JDK), JAVA_HOME chưa set sẵn ở PATH nên luôn set thủ công như trên.

## Kiến trúc
Luồng: `controller → service → repository → entity (DB)`. DTO (`dto/`) tách entity khỏi API.
Lỗi xử lý tập trung ở `exception/GlobalExceptionHandler` (400/401/404). Dữ liệu mẫu nạp ở
`config/DataSeeder` mỗi lần khởi động (chỉ ở profile != postgres).

Bảng nghiệp vụ: `Category 1─* Product`, `Order *─1 Customer`, `Order 1─* OrderItem *─1 Product`.
Bảng Order map tên `orders` (ORDER là từ khoá SQL). Tiền dùng `BigDecimal`.

### Bảo mật & phân quyền (RBAC + JWT)
- `package security/`: `JwtService` (tạo/verify JWT), `JwtAuthenticationFilter` (đọc Bearer token mỗi
  request), `SecurityConfig` (filter chain stateless, `@EnableMethodSecurity`, BCrypt), `SecurityUtils`,
  handler trả JSON 401 (`RestAuthEntryPoint`) / 403 (`RestAccessDeniedHandler`).
- Mô hình: `User *─* Role *─* Permission`. 3 role: ADMIN/STAFF/CUSTOMER. Authority nạp lúc đăng nhập =
  `ROLE_<ten>` + tên permission. Chặn endpoint bằng `@PreAuthorize("hasAnyRole('STAFF','ADMIN')")` trên
  controller/method (KHÔNG cấu hình URL-based trừ vài endpoint public trong SecurityConfig).
- `Customer.user` (One-to-One) gắn tài khoản với hồ sơ khách. `OrderService` lấy Customer từ user đăng
  nhập (CUSTOMER không tự truyền `customerId`); CUSTOMER chỉ xem/đặt đơn của chính mình.

### Hardening đã áp dụng (lưu ý khi sửa)
- **Secret JWT** đọc từ env `APP_JWT_SECRET` (`application.properties` chỉ có fallback dev). JWT có
  `issuer=shop-api` và được `requireIssuer` khi verify.
- **JwtAuthenticationFilter nạp lại user từ DB mỗi request** (qua `UserDetailsService`) để đổi quyền/khoá
  user có hiệu lực ngay; vì vậy KHÔNG còn tin authorities trong token. `/api/auth/me` KHÔNG nằm trong
  permitAll (chỉ register/login/refresh/logout public) để user bị khoá nhận 401 rõ ràng.
- **RefreshToken** lưu **băm SHA-256** (không lưu token gốc), **xoay vòng** mỗi lần refresh; phát hiện tái
  sử dụng → thu hồi toàn bộ. Việc thu hồi đó dùng `REQUIRES_NEW` (`RefreshTokenService.revokeAllForUser`)
  để COMMIT độc lập trước khi ném 401 — đừng gộp lại vào transaction của `AuthService.refresh`.
- **LoginAttemptService**: khoá đăng nhập sau N lần sai (`429`). **Register** trả thông báo chung chung
  (chống user enumeration). **Mật khẩu** ≥ 8 ký tự + có chữ và số (`RegisterRequest`).
- **CORS** cấu hình qua `app.cors.allowed-origins`. **H2 console** chỉ permit khi
  `spring.h2.console.enabled=true`.
- Production: chạy sau HTTPS/TLS (ngoài phạm vi code).

### Tính năng nền (common/)
- **Audit**: entity nghiệp vụ kế thừa `common/Auditable` (`@CreatedDate/@CreatedBy...`), bật bởi
  `config/JpaAuditingConfig` + `AuditorAwareImpl` (lấy username từ SecurityContext, mặc định "system").
- **Soft delete**: `Product` dùng `@SQLDelete` + `@SQLRestriction("deleted = false")` — DELETE chỉ set
  `deleted=true`, query mặc định tự ẩn.
- **Phân trang/lọc**: list trả `dto/PageResponse<T>`; lọc sản phẩm động bằng `ProductSpecifications`
  (JpaSpecificationExecutor).
- **Request logging**: `common/RequestLoggingFilter` log method/path/status/thời gian + correlationId (MDC).
- **Swagger**: `config/OpenApiConfig` khai báo security scheme Bearer; UI tại `/swagger-ui.html`.

### Tính năng mở rộng (Nhóm 2-5)
- Nghiệp vụ: `Category` CRUD; `Cart`/`CartItem` + checkout (tái dùng `OrderService.createOrder`);
  `Review` (rating+comment, điểm trung bình); `POST /api/auth/change-password`; `POST /api/orders/{id}/pay`
  (MOCK thanh toán → PAID).
- DevOps: `Dockerfile` + `docker-compose.yml` (app+Postgres); `.github/workflows/ci.yml` (mvnw verify);
  cache Caffeine cho danh mục (`@Cacheable`/`@CacheEvict`, bật bằng `@EnableCaching`);
  metrics `/actuator/prometheus` (ADMIN); lỗi theo **RFC 7807 ProblemDetail** (`application/problem+json`).
- Data: báo cáo `revenue-by-category`/`-by-month`/`top-customers` (native SQL, H2-specific),
  xuất CSV `best-sellers/csv`, job `@Scheduled` (`config/ReportScheduler`, bật bằng `@EnableScheduling`).
- Bảo mật: `LoginEvent` lưu DB (audit đăng nhập, `LoginEventService.record` dùng `REQUIRES_NEW` để
  commit độc lập); security headers (HSTS/nosniff/frame). Email-verify & MFA: HOÃN (cần dịch vụ ngoài).
- Lưu ý transaction: các thao tác "phải commit dù caller ném lỗi" (thu hồi token khi reuse, ghi LoginEvent
  khi login thất bại) đều dùng `@Transactional(REQUIRES_NEW)` ở bean riêng — đừng gộp vào tx của caller.

### Lưu ý kỹ thuật (Spring Boot 4.1)
- Boot 4 dùng Jackson 3 cho HTTP → KHÔNG có bean `com.fasterxml.jackson.databind.ObjectMapper` (Jackson 2)
  để inject. Các handler tự `new ObjectMapper()`.
- jjwt 0.12.x, springdoc 2.8.x, Spring Security 7. Mật khẩu seed lưu BCrypt.

## Quy ước
- Tiền: luôn `BigDecimal`, không dùng double.
- Báo cáo doanh thu loại đơn `CANCELLED`.
- Mapping Entity→DTO nằm trong method `@Transactional` (vì `open-in-view=false`).
- Comment trong code viết không dấu (tránh lỗi encoding), nhưng trả lời người dùng thì có dấu.

### Rule/Guardrail tự động (xem `docs/CONVENTIONS.md`)
- **ArchUnit** (`src/test/.../architecture/ArchitectureTest.java`): ép phân tầng (controller không gọi
  thẳng repository, entity không phụ thuộc tầng trên, không field injection, naming). Chạy với `mvnw test`.
- **Checkstyle** (`config/checkstyle/checkstyle.xml`): cảnh báo style khi `mvnw verify` (KHÔNG fail build).
- **Maven Enforcer**: Java ≥ 21, Maven ≥ 3.9, cấm trùng dependency.
- **Actuator**: `/actuator/health` công khai; actuator khác yêu cầu ADMIN.
- Secret qua env `APP_JWT_SECRET` (`.env.example`); `.env` đã được .gitignore.
- Khi thêm code mới: giữ đúng các rule trên (nếu ArchUnit/Checkstyle phàn nàn, sửa cho đúng quy ước).

## Khi người dùng nhờ "làm bài tập #N"
Xem danh sách ở `README.md` mục 5. Hướng dẫn từng bước, để họ tự gõ; chỉ viết hộ phần khó.
