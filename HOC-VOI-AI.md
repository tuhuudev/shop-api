# Học code bằng AI (Claude Code) — sổ tay

> Học code bằng AI rất nhanh, nhưng có **một cái bẫy**: code chạy được ≠ bạn hiểu.
> Mục tiêu của bạn KHÔNG phải "có code chạy" — mà là **hiểu vì sao nó chạy**.
> Nguyên tắc vàng: **bắt AI làm gia sư trước, làm thợ code sau.**

---

## Vòng lặp học 5 bước (áp dụng cho mỗi tính năng / khái niệm)

1. **HỎI trước, code sau** — bảo AI giải thích khái niệm / đoạn code *trước*, chưa cho viết gì.
2. **Nói lại bằng lời của bạn** — tự diễn đạt lại. Không nói được = chưa hiểu.
3. **Tự làm phần dễ** — tự gõ, chỉ nhờ AI phần khó.
4. **Nhờ AI review bài của bạn** — "chỉ lỗi và giải thích, đừng sửa hộ".
5. **Để AI đố bạn** — kiểm tra hiểu thật hay hiểu giả.

---

## 3 lệnh tắt đã tạo sẵn trong project

| Gõ | Tác dụng |
|----|----------|
| `/giai-thich <thứ cần hiểu>` | Giải thích cho người mới, đọc code thật, KHÔNG sửa code |
| `/bai-tap <số bài>` | Hướng dẫn tự làm bài tập, KHÔNG làm hộ |
| `/quiz <chủ đề>` | Đố bạn từng câu để kiểm tra hiểu bài |

Ví dụ: `/giai-thich @Transactional trong OrderService`, `/bai-tap 2`, `/quiz JPA relationships`

---

## Mẫu câu hỏi tốt (copy & sửa)

- **Giải thích:** "Đọc `OrderService.java`, giải thích `@Transactional` làm gì và chuyện gì xảy ra nếu một sản phẩm hết kho giữa chừng."
- **Lần theo luồng:** "Khi tôi gọi `POST /api/orders`, code chạy qua những file nào, theo thứ tự nào?"
- **So sánh / vì sao:** "Tại sao trả về `ProductResponse` thay vì trả thẳng entity `Product`?"
- **Đọc SQL:** "Câu `@Query` trong `OrderRepository` sinh ra SQL gì, từng phần JOIN/GROUP BY nghĩa là gì?"
- **Review bài tôi tự viết:** "Tôi vừa viết hàm này [dán code]. Review giúp, chỉ lỗi và giải thích, đừng sửa hộ."
- **Debug để học:** "Lỗi này nghĩa là gì? Giải thích NGUYÊN NHÂN trước khi sửa."

## Câu hỏi DỞ (tránh)

- ❌ "Làm hộ tôi tính năng X" → rồi copy mà không đọc.
- ❌ Thấy code chạy được là bỏ qua, không hỏi "vì sao".
- ❌ Không bao giờ tự gõ dòng nào.

---

## Mẹo dùng Claude Code khi học

- **Chế độ Plan (lập kế hoạch):** bấm `Shift+Tab` để đổi sang plan mode, hoặc nói *"chỉ lập kế hoạch, đừng viết code"*. Bạn xem cách làm TRƯỚC khi có code → học tư duy thiết kế.
- **Trỏ vào file bằng `@`:** gõ `@` rồi tên file để AI đọc đúng file đó.
- **Bắt đọc diff:** sau mỗi lần AI sửa, bảo *"giải thích lại từng thay đổi vừa làm"* rồi tự đọc lại.
- **`/code-review`:** nhờ review code bạn tự viết.
- **Bật `show-sql` (đã bật sẵn):** mỗi lần gọi API, nhìn câu SQL Hibernate in ra ở terminal — đối chiếu code Java ↔ SQL.

---

## Thói quen mỗi ngày

1. Mỗi buổi học chọn **1 khái niệm**, dùng `/giai-thich` cho tới khi tự nói lại được.
2. Tự gõ lại **1 đoạn nhỏ** mà không nhìn AI (luyện phản xạ cú pháp).
3. Cuối buổi gõ `/quiz` để tự kiểm tra.
4. Ghi lại 1 câu "hôm nay mình mới hiểu ra...".
