# Deploy shop-api — Render + Neon + Upstash (free)

Mục tiêu: chạy thật, **free**, auto-deploy khi push `main`. Stack: app Docker trên **Render**,
PostgreSQL trên **Neon**, Redis trên **Upstash**. Thời gian ~30 phút.

> Giới hạn free cần biết: Render free **ngủ sau ~15 phút** không request → request đầu cold-start
> ~40–60s; RAM 512MB. Hợp demo/portfolio, **không** hợp prod traffic thật.

---

## 1. PostgreSQL — Neon (free, vĩnh viễn)
1. Tạo project tại https://neon.tech → lấy **connection string**.
2. Chuyển sang dạng JDBC, **giữ `sslmode=require`**:
   ```
   jdbc:postgresql://<host>.neon.tech/<db>?sslmode=require
   ```
   Tách user/password riêng (Neon cho sẵn). Ví dụ:
   - `SPRING_DATASOURCE_URL = jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/shopdb?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME = <neon_user>`
   - `SPRING_DATASOURCE_PASSWORD = <neon_password>`

> Flyway sẽ tự chạy V1..V9 tạo schema + seed lần đầu app khởi động.

## 2. Redis — Upstash (free)
1. Tạo database tại https://upstash.com (Redis). Chọn region gần Singapore.
2. Lấy ở tab **"Connect" → endpoint** (KHÔNG dùng REST, dùng Redis protocol có TLS):
   - `SPRING_DATA_REDIS_HOST = <name>.upstash.io`
   - `SPRING_DATA_REDIS_PORT = 6379`
   - `SPRING_DATA_REDIS_PASSWORD = <token>`
   - `SPRING_DATA_REDIS_SSL_ENABLED = true`  (Upstash bắt buộc TLS — đã set sẵn trong `render.yaml`)

## 3. Sinh khóa RS256 (chạy local, KHÔNG commit)
```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl rsa -pubout -in jwt-private.pem -out jwt-public.pem
```
Giữ 2 file này để dán vào Render ở bước sau.

## 4. Render — tạo service từ Blueprint
1. https://render.com → **New → Blueprint** → kết nối repo `tuhuudev/shop-api` (Render đọc `render.yaml`).
2. Render hỏi các env `sync:false` → điền giá trị từ bước 1 & 2 (Neon + Upstash).
   - `APP_CORS_ALLOWED_ORIGINS` = origin frontend thật (vd `https://shop-client.example.com`); để trống nếu chỉ test API.
   - `APP_PAYMENTS_WEBHOOK_SECRET` Render **tự sinh** — xem giá trị trong Dashboard nếu cần test webhook.
3. **Secret Files** (Dashboard → service → *Environment* → *Secret Files*) — thêm 2 file đúng path:
   - `/etc/secrets/jwt-private.pem` ← nội dung `jwt-private.pem`
   - `/etc/secrets/jwt-public.pem` ← nội dung `jwt-public.pem`
   (env `APP_JWT_PRIVATE_KEY/PUBLIC_KEY` đã trỏ tới 2 path này trong `render.yaml`.)
4. **Create** → Render build Docker image + deploy. Lần đầu ~5–8 phút.

## 5. Kiểm tra
- Health: `https://<app>.onrender.com/actuator/health/liveness` → `{"status":"UP"}`
- Swagger: `https://<app>.onrender.com/swagger-ui.html`
- Login thử tài khoản seed (`admin/admin123`) qua `POST /api/auth/login`.

## CD
`autoDeploy: true` trong `render.yaml` → mỗi lần push `main` Render tự build + deploy lại. Không cần GitHub Actions cho deploy. (CI ở `.github/workflows/ci.yml` vẫn chạy test trước.)

## Nâng cấp khi cần prod thật (không còn free)
- Bỏ cold-start: Render plan trả phí (always-on) hoặc chuyển sang VPS/k8s.
- Khóa RS256 nên đến từ secret manager (đang dùng Secret File là đủ tốt).
- Redis Upstash free có giới hạn lệnh/ngày → traffic thật cần nâng tier.
- Nối cổng thanh toán thật (thay PaymentService mock) — đặt `APP_PAYMENTS_WEBHOOK_SECRET` theo provider.
