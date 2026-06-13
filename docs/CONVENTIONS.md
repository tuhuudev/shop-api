# Quy ước & Rule của shop-api

Tài liệu này mô tả các quy ước code và **công cụ nào tự động enforce** chúng. Mục tiêu: giữ kiến trúc
rõ ràng và bảo mật như một project thực tế, đồng thời dễ học.

## 1. Kiến trúc phân tầng (bắt buộc)
Luồng phụ thuộc một chiều: `controller → service → repository → entity`.

| Quy tắc | Enforce bởi |
|---|---|
| Controller KHÔNG gọi thẳng Repository (phải qua Service) | **ArchUnit** (`ArchitectureTest`) |
| Repository không phụ thuộc Controller/Service | **ArchUnit** |
| Entity không phụ thuộc Controller/Service/Repository/DTO | **ArchUnit** |
| `@RestController` đặt tên `*Controller` và nằm trong `controller/` | **ArchUnit** |
| Mọi class trong `service/` là `@Service` | **ArchUnit** |

Chạy: `./mvnw test` → nếu phá tầng, `ArchitectureTest` sẽ fail.

## 2. Quy ước code
- **KHÔNG Lombok** — getter/setter/constructor viết tay (để người học đọc rõ).
- **Dependency Injection qua constructor**, không field injection (`@Autowired` trên field bị ArchUnit cấm).
- **Tiền luôn `BigDecimal`**, không dùng `double`.
- **Map Entity → DTO trong method `@Transactional`** (vì `open-in-view=false`).
- **Comment trong code viết không dấu** (tránh lỗi encoding); tài liệu Markdown thì có dấu.
- Style (import thừa, star import, đặt tên, độ dài dòng ≤160, ngoặc `{}`): **Checkstyle** cảnh báo
  (`config/checkstyle/checkstyle.xml`) — chạy `./mvnw verify`, chỉ cảnh báo, không fail build.

## 3. Bảo mật (ưu tiên)
- Mật khẩu băm BCrypt; JWT ký HS256 có `issuer`.
- Phân quyền bằng `@PreAuthorize` (RBAC) + kiểm chủ sở hữu ở service.
- Secret JWT đọc từ env `APP_JWT_SECRET` (xem `.env.example`) — KHÔNG hardcode/commit.
- Refresh token: lưu băm SHA-256, xoay vòng, phát hiện tái sử dụng.
- Rate limit đăng nhập, chống dò tài khoản, mật khẩu ≥ 8 ký tự (chữ + số).
- Endpoint mới: mặc định yêu cầu đăng nhập; chỉ mở `permitAll` khi thực sự công khai.

## 4. Build & môi trường
- **Maven Enforcer**: Java ≥ 21, Maven ≥ 3.9, cấm trùng dependency.
- **Actuator**: `/actuator/health` công khai; các endpoint actuator khác yêu cầu ADMIN.
- Dev = H2 (create-drop + DataSeeder). Prod = profile `postgres` (Flyway sở hữu schema).

## 5. Test
- Unit (Mockito) cho service; integration (`@SpringBootTest` + MockMvc) cho auth/security.
- `ArchitectureTest` (ArchUnit) cho kiến trúc.
- Lệnh: `./mvnw test` (unit+integration+arch), `./mvnw verify` (thêm Checkstyle, Enforcer).
