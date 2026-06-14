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

## 3. Khóa RS256 — KHÔNG cần làm gì
Dockerfile tự sinh khóa dev lúc build (bake vào image) → dùng classpath mặc định, **không cần
Secret Files**. (Tradeoff demo: mỗi lần redeploy sinh khóa mới → token/refresh cũ hết hiệu lực,
user đăng nhập lại. Prod thật nên inject khóa qua secret/file mount thay vì bake.)

## 4. Render — tạo service từ Blueprint
1. https://render.com → **New → Blueprint** → kết nối repo `tuhuudev/shop-api` (Render đọc `render.yaml`).
2. Render hỏi các env `sync:false` → điền giá trị từ bước 1 & 2 (Neon + Redis).
   - Redis non-TLS (Redis Cloud free `redis://`): KHÔNG cần set SSL (app mặc định false).
     Nếu Redis có TLS (`rediss://`, vd Upstash) → thêm env `SPRING_DATA_REDIS_SSL_ENABLED=true`.
   - `APP_CORS_ALLOWED_ORIGINS` = origin frontend thật (vd `https://shop-client.example.com`); **để trống nếu chỉ test API** (Swagger same-origin vẫn chạy).
   - `APP_PAYMENTS_WEBHOOK_SECRET` Render **tự sinh** — xem giá trị trong Dashboard nếu cần test webhook.
3. **Create** → Render build Docker image (tự sinh khóa RS256) + deploy. Lần đầu ~5–8 phút.
   Không cần Secret Files.

## 5. Kiểm tra
- Health: `https://<app>.onrender.com/actuator/health/liveness` → `{"status":"UP"}`
- Swagger: `https://<app>.onrender.com/swagger-ui.html`
- Login thử tài khoản seed (`admin/admin123`) qua `POST /api/auth/login`.

## Cold-start & keepalive (Render free)
Render free **ngủ sau ~15' không có request** → request kế tiếp **cold-start ~45–100s**. Hệ quả thực tế:
demo bị chậm lần đầu, VÀ **Vercel build client có thể fail** (trang chủ ISR fetch BE lúc build — nếu BE
đang cold, fetch treo). Đã chống ở FE: `api-server.ts` race fetch với timer 6s → build luôn xong dù BE
ngủ (ISR nạp lại lúc runtime). Còn để giữ BE **luôn ấm**:

- **Khuyến nghị — external uptime monitor (free, ~2 phút):** [UptimeRobot](https://uptimerobot.com) hoặc
  [cron-job.org] → tạo HTTP monitor GET `https://<app>.onrender.com/actuator/health` mỗi **5 phút**.
  Đáng tin hơn hẳn GitHub cron. (Render free ~750h/tháng đủ cho 1 service chạy liên tục.)
- **Phụ — GitHub Actions** (`.github/workflows/keepalive.yml`): có sẵn, ping mỗi 10'. **Lưu ý**: lịch
  `schedule` của GitHub rất hay bị trễ/bỏ qua trên repo free → KHÔNG dựa làm chính. Chạy tay để test:
  `gh workflow run keepalive.yml` (hoặc tab Actions → Run workflow).
- **Triệt để** (khi cần prod thật): Render plan trả phí (always-on) — hết cold-start.

## CD
`autoDeploy: true` trong `render.yaml` → mỗi lần push `main` Render tự build + deploy lại. Không cần GitHub Actions cho deploy. (CI ở `.github/workflows/ci.yml` vẫn chạy test trước.)

## Nâng cấp khi cần prod thật (không còn free)
- Bỏ cold-start: Render plan trả phí (always-on) hoặc chuyển sang VPS/k8s.
- Khóa RS256 nên đến từ secret manager (đang dùng Secret File là đủ tốt).
- Redis Upstash free có giới hạn lệnh/ngày → traffic thật cần nâng tier.
- Nối cổng thanh toán thật (thay PaymentService mock) — đặt `APP_PAYMENTS_WEBHOOK_SECRET` theo provider.
