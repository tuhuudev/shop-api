-- ============================================================
-- Flyway V4: index cho cac cot tham gia JOIN / WHERE / GROUP BY / ORDER BY.
-- PostgreSQL KHONG tu tao index tren cot khoa ngoai -> phai tao thu cong.
-- (Cac cot UNIQUE/PK da co index san nen khong lap lai o day.)
-- ============================================================

-- San pham: loc/JOIN theo danh muc
CREATE INDEX idx_products_category ON products (category_id);

-- Don hang: "don cua toi" (customer_id), bao cao theo ngay (order_date)
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_order_date ON orders (order_date);

-- Dong don: JOIN order<->item va bao cao ban chay (product_id)
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

-- Danh gia: liet ke/diem trung binh theo san pham; theo user
CREATE INDEX idx_reviews_product ON reviews (product_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);

-- Refresh token: thu hoi/het han theo user (token da UNIQUE)
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- Gio hang
CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);
CREATE INDEX idx_cart_items_product ON cart_items (product_id);

-- Nhat ky dang nhap: sap xep moi nhat
CREATE INDEX idx_login_events_at ON login_events (at);
