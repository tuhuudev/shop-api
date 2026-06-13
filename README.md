# shop-api — Học Backend & Data qua quản lý đơn hàng

Project Spring Boot (Java 21) mô phỏng hệ thống bán hàng đơn giản, được thiết kế để **vừa code vừa học** Backend và Data từ con số 0.

---

## 1. Chạy thử trong 30 giây

Mở terminal **PowerShell** ngay tại thư mục này:

```powershell
$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory | ? { $_.Name -like 'jdk-21*' } | Select -First 1).FullName
.\mvnw.cmd spring-boot:run
```

Khi thấy dòng `DU LIEU MAU DA NAP XONG!` là app đã chạy ở `http://localhost:8080`.

Mở trình duyệt thử ngay:
- Danh sách sản phẩm (công khai): http://localhost:8080/api/products
- **Swagger UI** (thử mọi API, có nút Authorize): http://localhost:8080/swagger-ui.html
- Xem database trực tiếp: http://localhost:8080/h2-console
  (JDBC URL gõ đúng: `jdbc:h2:mem:shopdb`, user `sa`, password để trống → Connect)

**Tài khoản mẫu** (đăng nhập qua `POST /api/auth/login`):

| username | password | vai trò |
|----------|----------|---------|
| `admin` | `admin123` | ADMIN (toàn quyền) |
| `staff` | `staff123` | STAFF (quản lý sản phẩm, đơn, báo cáo) |
| `customer` | `customer123` | CUSTOMER (đặt & xem đơn của mình) |

Dừng app: bấm `Ctrl + C` trong terminal.

---

## 2. Bản đồ dự án — file nào làm gì

Một request đi qua các tầng theo thứ tự: **Controller → Service → Repository → Database**.

```
src/main/java/com/learn/shopapi/
├─ entity/        # CÁC BẢNG trong DB (+ User, Role, Permission, RefreshToken; Auditable nằm ở common)
├─ repository/    # Truy vấn DB (Spring tự sinh code + @Query + Specification để lọc động)
├─ dto/           # Dữ liệu vào/ra qua API (+ DTO auth, PageResponse cho phân trang)
├─ service/       # LOGIC NGHIỆP VỤ (trừ kho, tính tiền, transaction, auth)
├─ controller/    # Định nghĩa URL/endpoint REST (+ AuthController, UserController)
├─ security/      # JWT + Spring Security: filter, SecurityConfig, xử lý 401/403
├─ common/        # Auditable (base), AuditorAware, RequestLoggingFilter
├─ exception/     # Xử lý lỗi tập trung (trả 400/401/404 đẹp)
└─ config/        # DataSeeder (seed dữ liệu+tài khoản), OpenApiConfig (Swagger), JpaAuditingConfig
```

`src/main/resources/db/migration/` — script **Flyway** (V1, V2) chỉ dùng ở profile `postgres`.

Khi đọc code, hãy đi theo đúng luồng đó: mở `ProductController` → `ProductService` → `ProductRepository` → `Product`.

---

## 3. Các API có sẵn

### Xác thực (Auth) — công khai
| Method | URL | Việc |
|--------|-----|------|
| POST | `/api/auth/register` | Đăng ký (tạo tài khoản CUSTOMER) + trả token luôn |
| POST | `/api/auth/login` | Đăng nhập → `accessToken` + `refreshToken` |
| POST | `/api/auth/refresh` | Xin access token mới từ refresh token |
| POST | `/api/auth/logout` | Thu hồi refresh token |
| GET | `/api/auth/me` | Thông tin tài khoản đang đăng nhập |

**Cách gọi API cần đăng nhập:** lấy `accessToken` từ login rồi gắn header:
`Authorization: Bearer <accessToken>`. Trên Swagger UI bấm **Authorize** dán token một lần là xong.

### Sản phẩm (CRUD đầy đủ — học mẫu BE chuẩn)
| Method | URL | Việc | Quyền |
|--------|-----|------|-------|
| GET | `/api/products` | Danh sách (phân trang `?page=&size=&sort=`, lọc `?search=&minPrice=&maxPrice=&categoryId=`) | công khai |
| GET | `/api/products/{id}` | Chi tiết 1 sản phẩm | công khai |
| POST | `/api/products` | Tạo mới | STAFF/ADMIN |
| PUT | `/api/products/{id}` | Cập nhật | STAFF/ADMIN |
| DELETE | `/api/products/{id}` | Xoá **mềm** (soft delete) | STAFF/ADMIN |

### Đơn hàng
| Method | URL | Việc | Quyền |
|--------|-----|------|-------|
| GET | `/api/orders` | Danh sách đơn (phân trang) | đã đăng nhập (CUSTOMER chỉ thấy đơn của mình) |
| GET | `/api/orders/{id}` | Chi tiết đơn | đã đăng nhập (CUSTOMER chỉ xem đơn của mình) |
| POST | `/api/orders` | Tạo đơn (tự trừ kho, tự tính tiền) | đã đăng nhập (CUSTOMER đặt cho mình) |
| PATCH | `/api/orders/{id}/status` | Đổi trạng thái (theo luật, hủy đơn sẽ hoàn kho) | STAFF/ADMIN |

> **Luật chuyển trạng thái đơn**: `PENDING→PAID/CANCELLED`, `PAID→SHIPPED/CANCELLED`, còn `SHIPPED`/`CANCELLED` là cuối. Nhảy sai → `400`. Chuyển sang `CANCELLED` sẽ **hoàn lại số lượng vào kho**. Kho được bảo vệ chống đặt trùng bằng **optimistic locking** (`@Version`); xung đột → `409`.

### Báo cáo (phần Data) — STAFF/ADMIN
| Method | URL | Việc |
|--------|-----|------|
| GET | `/api/reports/best-sellers` | Sản phẩm bán chạy (JOIN + GROUP BY + SUM) |
| GET | `/api/reports/daily-revenue` | Doanh thu theo ngày |
| GET | `/api/reports/total-revenue` | Tổng doanh thu |

### Quản trị user — ADMIN
| Method | URL | Việc |
|--------|-----|------|
| GET | `/api/admin/users` | Danh sách user (phân trang) |
| PUT | `/api/admin/users/{id}/roles` | Gán lại vai trò (body: `["ADMIN","STAFF"]`) |
| PATCH | `/api/admin/users/{id}/enabled?enabled=false` | Khoá/mở tài khoản |
| GET | `/api/admin/login-events` | Nhật ký đăng nhập (audit bảo mật) |

### Tính năng mở rộng
| Nhóm | Endpoint tiêu biểu | Quyền |
|---|---|---|
| Danh mục | `GET /api/categories` (+ `/all` cache), `POST/PUT/DELETE /api/categories` | đọc public / ghi STAFF-ADMIN |
| Giỏ hàng | `GET /api/cart`, `POST /api/cart/items`, `PUT/DELETE /api/cart/items/{productId}`, `POST /api/cart/checkout` | đã đăng nhập (giỏ của mình) |
| Đánh giá | `GET /api/products/{id}/reviews` (+ `/summary`), `POST /api/products/{id}/reviews` | đọc public / gửi cần đăng nhập |
| Tài khoản | `POST /api/auth/change-password` | đã đăng nhập (biết mật khẩu cũ) |
| Đơn hàng | `POST /api/orders/{id}/pay` (mock thanh toán → PAID) | đã đăng nhập (đơn của mình) |
| Báo cáo | `GET /api/reports/revenue-by-category`, `/revenue-by-month`, `/top-customers`, `/best-sellers/csv` | STAFF/ADMIN |

> File `requests.http` chứa sẵn các request mẫu (đã kèm lấy token + Authorization) — mở bằng VS Code (cài extension "REST Client") rồi bấm "Send Request".

---

## 4. Lộ trình học (làm theo thứ tự)

### Tuần 1 — Hiểu Backend
1. **Đọc luồng 1 request**: mở `ProductController.list()`, lần theo tới `ProductService` rồi `ProductRepository`. Hiểu vì sao chia 3 tầng.
2. **REST & HTTP**: vì sao POST trả 201, DELETE trả 204, lỗi trả 400/404. Xem `GlobalExceptionHandler`.
3. **DTO vs Entity**: tại sao không trả thẳng `Product` mà phải qua `ProductResponse`.

### Tuần 2 — Hiểu Data (ORM + SQL)
4. **Quan hệ bảng**: đọc các `@OneToMany` / `@ManyToOne` trong `entity/`. Vẽ sơ đồ 5 bảng ra giấy.
5. **Xem SQL thật**: bật app, gọi vài API, đọc câu SQL Hibernate in ra ở console. So sánh code Java ↔ SQL sinh ra.
6. **Mở H2 console**, tự viết SQL tay: `SELECT * FROM orders;`, thử `JOIN`, `GROUP BY`.

### Tuần 3 — Phân tích dữ liệu
7. Đọc các `@Query(nativeQuery = true)` trong `OrderRepository` — đây là SQL phân tích thật.
8. Tự thêm 1 báo cáo mới (xem Bài tập #3 bên dưới).

### Tuần 4 — Lên DB thật
9. Chuyển từ H2 sang **PostgreSQL** (mục 6) để dữ liệu không mất khi tắt app.

---

## 5. Bài tập tự làm (từ dễ đến khó)

1. **Dễ** — Thêm trường `sku` (mã sản phẩm) vào `Product`: sửa entity, DTO, để ý DB tự thêm cột.
2. **Dễ** — Thêm endpoint `GET /api/products/low-stock` trả sản phẩm tồn < 30 (repo đã có sẵn `findByStockQuantityLessThan`).
3. **Vừa** — Thêm báo cáo `GET /api/reports/revenue-by-category` (doanh thu theo danh mục): viết native SQL JOIN 4 bảng + GROUP BY.
4. **Vừa** — Thêm `CustomerController` với CRUD đầy đủ (bắt chước `ProductController`).
5. **Khó** — Thêm phân trang cho `GET /api/products` dùng `Pageable` của Spring Data.
6. **Khó** — Viết unit test cho `OrderService.createOrder` (trường hợp đủ kho và hết kho).

> Cách làm: gõ cho Claude Code "làm bài tập #N", nó sẽ hướng dẫn từng bước thay vì làm hộ.

---

## 6. Chuyển sang PostgreSQL + Flyway (khi muốn dữ liệu được lưu lại)

Đã có sẵn **profile `postgres`** (file `application-postgres.properties`) và **Flyway** quản lý schema
(các script `src/main/resources/db/migration/V1__*.sql`, `V2__*.sql`). Không cần sửa code.

1. Chạy 1 container Postgres (cần Docker):
   ```powershell
   docker run --name shop-pg -e POSTGRES_PASSWORD=shop123 -e POSTGRES_DB=shopdb -p 5432:5432 -d postgres:16
   ```
2. Chạy app với profile `postgres`:
   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
   ```

Khi đó: Flyway tự tạo bảng + seed dữ liệu (role/permission/tài khoản/sản phẩm) theo các script `V*.sql`,
Hibernate chỉ chạy ở chế độ `ddl-auto=none` (tin tưởng Flyway). Dữ liệu nằm trong Postgres thật, tắt app
không mất. Ở chế độ dev mặc định (H2 in-memory) thì Flyway tắt, dùng `create-drop` + `DataSeeder`.

> Khác biệt để học: **dev** = H2 + Hibernate tự tạo bảng (nhanh, mất khi tắt). **postgres** = Flyway sở
> hữu schema có version (giống dự án thật, schema thay đổi qua từng file migration).

---

## 7. Bảo mật đã áp dụng (và giới hạn)

Các lớp bảo vệ đã có trong code (đã kiểm thử end-to-end):
- Mật khẩu băm **BCrypt**; JWT ký **HS256** có kiểm `issuer`.
- **Phân quyền** RBAC qua `@PreAuthorize` + kiểm chủ sở hữu (CUSTOMER chỉ xem đơn của mình).
- **Khoá tài khoản có hiệu lực tức thì**: mỗi request nạp lại user từ DB (đổi vai trò/khoá user → áp dụng ngay, không đợi token hết hạn).
- **Refresh token**: lưu **băm SHA-256** trong DB, **xoay vòng** mỗi lần refresh, **phát hiện tái sử dụng** (token cũ bị dùng lại → thu hồi toàn bộ).
- **Chống dò mật khẩu**: khoá đăng nhập sau 5 lần sai (`429`). **Chống dò tài khoản**: thông báo đăng ký chung chung.
- **Mật khẩu mạnh** (≥ 8 ký tự, có cả chữ và số). **CORS** cấu hình qua `app.cors.allowed-origins`.
- **H2 console** chỉ mở khi `spring.h2.console.enabled=true` (dev), tự đóng ở môi trường thật.

Vận hành cho production (KHÔNG để mặc định):
- Đặt secret qua biến môi trường: `setx APP_JWT_SECRET "<base64 >=32 byte>"` (đừng commit secret).
- Chạy sau **HTTPS/TLS** (reverse proxy hoặc `server.ssl.*`) để token không bị nghe lén.
- Bật Postgres + tắt H2 console (profile `postgres` đã làm sẵn).

Còn có thể nâng cấp thêm (ngoài phạm vi hiện tại): xác minh email, MFA/OTP, lưu rate-limit ở Redis cho nhiều máy.

### Rule/Guardrail tự động
Project có các "rule" như dự án thật (chi tiết: [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md)):
- **ArchUnit** ép phân tầng (chạy `mvnw test` — phá tầng là fail).
- **Checkstyle** cảnh báo style khi `mvnw verify` (không fail build).
- **Maven Enforcer** (Java ≥ 21, Maven ≥ 3.9). **Actuator**: `/actuator/health` công khai, còn lại cần ADMIN.
- File hygiene: `.editorconfig`, `.env.example` (mẫu biến môi trường), `.gitignore` (đã chặn `.env`).

---

## 8. Khái niệm cần tra khi gặp trong code

`@Entity` `@Id` `@GeneratedValue` · `@OneToMany`/`@ManyToOne` · `@Repository` / Spring Data JPA ·
`@Service` `@RestController` · Dependency Injection (constructor) · `@Transactional` · DTO · Bean Validation (`@Valid`) ·
Spring Security (`SecurityFilterChain`, `@PreAuthorize`, `OncePerRequestFilter`) · JWT · `@SQLDelete`/`@SQLRestriction`.

Gặp cái nào chưa hiểu → hỏi Claude Code: *"giải thích `@Transactional` trong file OrderService"*.
