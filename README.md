# shop-api — API bán hàng (Spring Boot 4, production-grade)

API bán hàng có **JWT RS256 + RBAC**, **PostgreSQL + Flyway**, **Redis** (cache/rate-limit/login-attempts
phân tán cho multi-instance), observability (Prometheus + tracing), idempotency, Docker + CI.

---

## 🚀 Live demo

Deploy trên **Render** (app) + **Neon** (PostgreSQL) + **Redis Cloud** — auto-deploy khi push `main`.

- **Swagger UI** (bấm *Authorize* để thử API có token): https://shop-api-ryfm.onrender.com/swagger-ui.html
- Sản phẩm (công khai): https://shop-api-ryfm.onrender.com/api/products
- Health: https://shop-api-ryfm.onrender.com/actuator/health

Tài khoản mẫu (qua `POST /api/auth/login`):

| username | password | vai trò |
|----------|----------|---------|
| `admin` | `admin123` | ADMIN (toàn quyền) |
| `staff` | `staff123` | STAFF (sản phẩm, đơn, báo cáo) |
| `customer` | `customer123` | CUSTOMER (đặt & xem đơn của mình) |

> ⚠️ Free tier **ngủ sau ~15 phút** không truy cập → lần gọi đầu cold-start ~40–60s. Chi tiết deploy: [`docs/DEPLOY.md`](docs/DEPLOY.md).

---

## 1. Chạy thử

Cần **Docker** (cho Postgres + Redis). Mở **PowerShell** tại thư mục này:

```powershell
$env:JAVA_HOME = (Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory | ? { $_.Name -like 'jdk-21*' } | Select -First 1).FullName
docker compose up -d db redis      # khoi dong Postgres + Redis
.\mvnw.cmd spring-boot:run          # app tai http://localhost:8080
```

> **Lần đầu clone:** khóa RS256 dev (`src/main/resources/keys/*.pem`) **không** được commit (xem `.gitignore`).
> Sinh khóa dev trước khi chạy (cần OpenSSL — Git for Windows đã kèm sẵn):
> ```powershell
> New-Item -ItemType Directory -Force src/main/resources/keys | Out-Null
> openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src/main/resources/keys/jwt-private.pem
> openssl rsa -pubout -in src/main/resources/keys/jwt-private.pem -out src/main/resources/keys/jwt-public.pem
> ```
> Prod **không** dùng file này — override qua env `APP_JWT_PRIVATE_KEY` / `APP_JWT_PUBLIC_KEY`.
Hoặc chạy trọn gói trong container: `docker compose up --build`.

Flyway tự tạo schema + seed (V1..V5). Mở:
- Sản phẩm (công khai): http://localhost:8080/api/products
- **Swagger UI** (có nút Authorize): http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

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
- Mật khẩu băm **BCrypt**; JWT ký **RS256** (private/public key) có kiểm `issuer`.
- **Phân quyền** RBAC qua `@PreAuthorize` + kiểm chủ sở hữu (CUSTOMER chỉ xem đơn của mình).
- **Khoá tài khoản có hiệu lực tức thì**: mỗi request nạp lại user từ DB (đổi vai trò/khoá user → áp dụng ngay, không đợi token hết hạn).
- **Refresh token**: httpOnly cookie, lưu **băm SHA-256** trong DB, **xoay vòng** mỗi lần refresh, **phát hiện tái sử dụng** (token cũ bị dùng lại → thu hồi toàn bộ).
- **Chống dò mật khẩu**: khoá đăng nhập sau 5 lần sai + **rate-limit theo IP qua Redis** (`429`, đa instance). **Chống dò tài khoản**: thông báo đăng ký chung chung.
- **Mật khẩu mạnh** (≥ 8 ký tự, có cả chữ và số). **CORS** cấu hình qua `app.cors.allowed-origins`.
- **Webhook thanh toán** xác thực bằng **HMAC-SHA256** (so sánh hằng-thời-gian) + idempotency theo `provider_ref`.

Vận hành cho production (profile `prod` — KHÔNG để mặc định):
- Khoá RS256 thật qua env file-mount: `APP_JWT_PRIVATE_KEY`/`APP_JWT_PUBLIC_KEY` (đừng commit private key).
- `APP_PAYMENTS_WEBHOOK_SECRET` **bắt buộc** (app fail-fast nếu thiếu/đang là secret dev).
- `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATA_REDIS_PASSWORD` (+ `SPRING_DATA_REDIS_SSL_ENABLED=true` nếu cần TLS).
- Chạy sau **HTTPS/TLS** (reverse proxy + `server.forward-headers-strategy=framework`); cookie `Secure` + `SameSite=Strict`.
- `/actuator/prometheus` **không** route ra public ingress (chỉ scrape nội bộ).

Còn có thể nâng cấp thêm (ngoài phạm vi hiện tại): xác minh email, MFA/OTP, nối cổng thanh toán thật (thay PaymentService mock).

### Rule/Guardrail tự động
Project có các "rule" như dự án thật (chi tiết: [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md)):
- **ArchUnit** ép phân tầng (chạy `mvnw test` — phá tầng là fail).
- **Checkstyle** cảnh báo style khi `mvnw verify` (không fail build).
- **Maven Enforcer** (Java ≥ 21, Maven ≥ 3.9). **Actuator**: `/actuator/health` công khai, còn lại cần ADMIN.
- File hygiene: `.editorconfig`, `.env.example` (mẫu biến môi trường), `.gitignore` (đã chặn `.env`).

---

## 8. Biến môi trường production

Profile `prod` cấu hình hoàn toàn qua biến môi trường. Bảng dưới liệt kê **đủ** nhóm DB / Redis / JWT / webhook / CORS.
Cột *Default* là giá trị dev (trong `application.properties` / khoá trong repo) — **dùng ở prod là LỘ/không an toàn**.

| Biến | Bắt buộc (prod)? | Default (dev) | Ý nghĩa | Hành vi nếu thiếu |
|------|------------------|---------------|---------|-------------------|
| `SPRING_DATASOURCE_URL` | Nên đặt | `jdbc:postgresql://localhost:5432/shopdb` | JDBC URL Postgres | Trỏ `localhost` dev → không nối được DB prod |
| `SPRING_DATASOURCE_USERNAME` | Nên đặt | `postgres` | User DB | Dùng user dev |
| `SPRING_DATASOURCE_PASSWORD` | **Có** | `shop123` | Mật khẩu DB | Dùng mật khẩu dev `shop123` → **LỘ** |
| `SPRING_DATA_REDIS_HOST` | Nên đặt | `localhost` | Host Redis | Trỏ `localhost` dev |
| `SPRING_DATA_REDIS_PORT` | Không | `6379` | Port Redis | Dùng 6379 |
| `SPRING_DATA_REDIS_PASSWORD` | **Có** nếu Redis bật AUTH | *(rỗng)* | Mật khẩu Redis | Kết nối không auth → **LỘ** nếu Redis đi qua mạng (đang giữ rate-limit/login-attempt/idempotency) |
| `SPRING_DATA_REDIS_SSL_ENABLED` | Bật nếu qua mạng không tin cậy | `false` | TLS tới Redis | Kết nối plaintext |
| `APP_JWT_PRIVATE_KEY` | **Có** | `keys/*.pem` (classpath) | Private key RS256 ký token | Dùng khoá dev trong repo → ai cũng **ký được token** → LỘ |
| `APP_JWT_PUBLIC_KEY` | **Có** | `keys/*.pem` (classpath) | Public key verify token | Dùng khoá dev trong repo |
| `APP_PAYMENTS_WEBHOOK_SECRET` | **Có** | `dev-webhook-secret` | HMAC secret xác thực webhook thanh toán | **Fail-fast**: prod **không khởi động** nếu rỗng hoặc `=dev-webhook-secret` (`PaymentService:49-53`) — nếu không, kẻ ngoài giả mạo webhook → mark đơn PAID |
| `APP_CORS_ALLOWED_ORIGINS` | Nên đặt | *(rỗng)* | Origin frontend được phép (phân tách dấu phẩy) | Rỗng → không origin cross-site nào gọi API từ browser |
| `DB_POOL_MAX_SIZE` | Không | `10` | Hikari max pool size | Dùng 10 (điều chỉnh theo số pod) |
| `MANAGEMENT_OTLP_TRACING_ENDPOINT` | Không | *(none)* | Endpoint OTLP collector (tracing) | Không xuất span ra collector |
| `TRACING_SAMPLING` | Không | `1.0` dev / `0.1` prod | Tỉ lệ lấy mẫu trace | Dùng default theo profile |

> Mẫu khai báo: xem [`.env.example`](.env.example). Tuyệt đối **không commit** `.env` thật / private key.

### Kiểm chứng readiness

`/actuator/health` tách hai nhóm probe (k8s/LB):

- **Liveness** (`/actuator/health/liveness`): tiến trình còn sống? Chỉ fail khi JVM hỏng → orchestrator **restart** pod. Không phụ thuộc DB/Redis.
- **Readiness** (`/actuator/health/readiness`): sẵn sàng nhận traffic? Gồm `db`, `redis` (TCP/PING) và `redisReadiness` (round-trip write+read). Mất DB/Redis → **`503`** → LB ngừng đưa traffic (không restart).

```bash
# UP  -> HTTP 200, {"status":"UP",...}
curl -i http://localhost:8080/actuator/health/readiness
# Dung Postgres hoac Redis (docker compose stop db | redis) -> HTTP 503, {"status":"OUT_OF_SERVICE"/"DOWN",...}
```

`redisReadiness` không chỉ PING mà `SET` một key TTL ngắn rồi `GET` so khớp → bắt được Redis read-only / mất quyền ghi mà PING vẫn OK.

> `render.yaml` dùng `healthCheckPath: /actuator/health/liveness` (không phải readiness): Render chỉ chạy 1 instance, dùng health check để quyết định **restart** — nếu trỏ readiness thì khi DB/Redis chớp nháy Render sẽ restart cả app (vòng lặp khởi động lại) thay vì chỉ tạm ngừng traffic. Readiness vẫn dùng cho LB/k8s nhiều instance.

---

## 9. Khái niệm cần tra khi gặp trong code

`@Entity` `@Id` `@GeneratedValue` · `@OneToMany`/`@ManyToOne` · `@Repository` / Spring Data JPA ·
`@Service` `@RestController` · Dependency Injection (constructor) · `@Transactional` · DTO · Bean Validation (`@Valid`) ·
Spring Security (`SecurityFilterChain`, `@PreAuthorize`, `OncePerRequestFilter`) · JWT · `@SQLDelete`/`@SQLRestriction`.

Gặp cái nào chưa hiểu → hỏi Claude Code: *"giải thích `@Transactional` trong file OrderService"*.
