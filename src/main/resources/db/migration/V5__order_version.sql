-- ============================================================
-- Flyway V5: them cot version cho orders (optimistic locking @Version tren Order).
-- Khong sua migration da chay (V1) - them migration moi theo dung quy tac Flyway.
-- ============================================================
ALTER TABLE orders ADD COLUMN version BIGINT;
UPDATE orders SET version = 0 WHERE version IS NULL;
