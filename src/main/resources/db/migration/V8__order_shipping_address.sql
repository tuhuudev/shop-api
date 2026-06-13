-- ============================================================
-- Flyway V8: dia chi giao hang chup vao don (cac cot ship_* tren bang orders, deu nullable).
-- ============================================================
ALTER TABLE orders ADD COLUMN ship_recipient   VARCHAR(255);
ALTER TABLE orders ADD COLUMN ship_phone       VARCHAR(32);
ALTER TABLE orders ADD COLUMN ship_line1       VARCHAR(255);
ALTER TABLE orders ADD COLUMN ship_line2       VARCHAR(255);
ALTER TABLE orders ADD COLUMN ship_city        VARCHAR(128);
ALTER TABLE orders ADD COLUMN ship_province    VARCHAR(128);
ALTER TABLE orders ADD COLUMN ship_postal_code VARCHAR(16);
ALTER TABLE orders ADD COLUMN ship_country     VARCHAR(64);
