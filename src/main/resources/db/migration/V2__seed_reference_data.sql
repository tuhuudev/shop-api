-- ============================================================
-- Flyway migration V2: nap du lieu nen cho PostgreSQL.
-- Vi DataSeeder KHONG chay o profile postgres (@Profile("!postgres")),
-- nen toan bo role/permission/user/san pham mau duoc nap o day.
-- Mat khau luu dang BCrypt (admin123 / staff123 / customer123).
-- ============================================================

-- Permissions
INSERT INTO permissions (id, name) VALUES
    (1, 'PRODUCT_WRITE'),
    (2, 'ORDER_READ_ALL'),
    (3, 'ORDER_UPDATE'),
    (4, 'REPORT_VIEW'),
    (5, 'USER_MANAGE');

-- Roles
INSERT INTO roles (id, name) VALUES
    (1, 'ADMIN'),
    (2, 'STAFF'),
    (3, 'CUSTOMER');

-- Role -> Permission
INSERT INTO role_permissions (role_id, permission_id) VALUES
    (1, 1), (1, 2), (1, 3), (1, 4), (1, 5),   -- ADMIN: tat ca
    (2, 1), (2, 2), (2, 3), (2, 4);           -- STAFF: tru USER_MANAGE

-- Users (BCrypt hashes)
INSERT INTO users (id, username, password, email, full_name, enabled, created_at, updated_at, created_by) VALUES
    (1, 'admin',    '$2a$10$Pn0.kiCmOGjS68xvzdfRM.66BMMbjRmRV7YEJH0RvjIERDg4WPcly', 'admin@example.com', 'Quan tri vien', TRUE, now(), now(), 'system'),
    (2, 'staff',    '$2a$10$5W.gMG9lDbHvpTIdovJBuOQvzuevDllH0zLhPJGwElwYeFMIjnDle', 'staff@example.com', 'Nhan vien',     TRUE, now(), now(), 'system'),
    (3, 'customer', '$2a$10$TLyX0iQRMyIO92IL74xXhOohwmqZz1lbxqFB3zLSZ21a0AMufufV2', 'an@example.com',    'Nguyen Van An',  TRUE, now(), now(), 'system');

-- User -> Role
INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1),   -- admin    -> ADMIN
    (2, 2),   -- staff    -> STAFF
    (3, 3);   -- customer -> CUSTOMER

-- Categories
INSERT INTO categories (id, name) VALUES
    (1, 'Dien thoai'),
    (2, 'Laptop'),
    (3, 'Sach');

-- Products (version=0 cho optimistic locking)
INSERT INTO products (id, name, description, price, stock_quantity, category_id, deleted, version, created_at, updated_at, created_by) VALUES
    (1, 'iPhone 15',      'Smartphone Apple',   24990000, 50, 1, FALSE, 0, now(), now(), 'system'),
    (2, 'Samsung S24',    'Smartphone Samsung', 19990000, 40, 1, FALSE, 0, now(), now(), 'system'),
    (3, 'MacBook Air M3', 'Laptop Apple',       28990000, 20, 2, FALSE, 0, now(), now(), 'system'),
    (4, 'Clean Code',     'Sach lap trinh',       350000, 100, 3, FALSE, 0, now(), now(), 'system');

-- Customers ("an" gan voi tai khoan "customer")
INSERT INTO customers (id, name, email, user_id, created_at, updated_at, created_by) VALUES
    (1, 'Nguyen Van An', 'an@example.com',   3,    now(), now(), 'system'),
    (2, 'Tran Thi Binh', 'binh@example.com', NULL, now(), now(), 'system');

-- Orders (total_amount tinh san) - de bao cao co du lieu
INSERT INTO orders (id, customer_id, order_date, status, total_amount, created_at, updated_at, created_by) VALUES
    (1, 1, now(), 'PAID',      25690000, now(), now(), 'system'),
    (2, 2, now(), 'SHIPPED',   48980000, now(), now(), 'system'),
    (3, 1, now(), 'PAID',       1050000, now(), now(), 'system'),
    (4, 2, now(), 'CANCELLED', 24990000, now(), now(), 'system');

-- Order items
INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES
    (1, 1, 1, 1, 24990000),  -- iPhone x1
    (2, 1, 4, 2,   350000),  -- Clean Code x2
    (3, 2, 3, 1, 28990000),  -- MacBook x1
    (4, 2, 2, 1, 19990000),  -- Samsung x1
    (5, 3, 4, 3,   350000),  -- Clean Code x3
    (6, 4, 1, 1, 24990000);  -- iPhone x1 (don bi huy)

-- Day cac sequence identity len qua MAX(id) da chen tay, de app chen tiep khong bi trung khoa.
SELECT setval(pg_get_serial_sequence('permissions', 'id'), (SELECT MAX(id) FROM permissions));
SELECT setval(pg_get_serial_sequence('roles', 'id'),       (SELECT MAX(id) FROM roles));
SELECT setval(pg_get_serial_sequence('users', 'id'),       (SELECT MAX(id) FROM users));
SELECT setval(pg_get_serial_sequence('categories', 'id'),  (SELECT MAX(id) FROM categories));
SELECT setval(pg_get_serial_sequence('products', 'id'),    (SELECT MAX(id) FROM products));
SELECT setval(pg_get_serial_sequence('customers', 'id'),   (SELECT MAX(id) FROM customers));
SELECT setval(pg_get_serial_sequence('orders', 'id'),      (SELECT MAX(id) FROM orders));
SELECT setval(pg_get_serial_sequence('order_items', 'id'), (SELECT MAX(id) FROM order_items));
