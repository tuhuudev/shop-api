// Sinh Flyway V10: them cot image_url/brand/rating + nap catalog dien tu that tu DummyJSON.
// Chay: node scripts/gen-seed.mjs  (Node 18+, dung global fetch). Khong commit script nay vao build.
import { writeFileSync } from "node:fs";

// slug DummyJSON -> { id category, ten hien thi }
const CAT = {
  smartphones: { id: 1, name: "Điện thoại" },
  laptops: { id: 2, name: "Laptop" },
  tablets: { id: 4, name: "Máy tính bảng" },
  "mobile-accessories": { id: 5, name: "Phụ kiện" },
  "mens-watches": { id: 6, name: "Đồng hồ" },
  "womens-watches": { id: 6, name: "Đồng hồ" },
};
const USD_TO_VND = 25000;
const esc = (s) => (s == null ? null : String(s).replace(/'/g, "''"));
const vnd = (usd) => Math.round((usd * USD_TO_VND) / 1000) * 1000;
const sqlStr = (s) => (s == null ? "NULL" : `'${esc(s)}'`);
const sqlNum = (n) => (n == null || Number.isNaN(n) ? "NULL" : n);

async function fetchCat(slug) {
  const r = await fetch(`https://dummyjson.com/products/category/${slug}?limit=0`);
  if (!r.ok) throw new Error(`fetch ${slug} -> ${r.status}`);
  return (await r.json()).products ?? [];
}

const seen = new Set();
const rows = [];
for (const slug of Object.keys(CAT)) {
  const products = await fetchCat(slug);
  for (const p of products) {
    if (seen.has(p.title)) continue; // tranh trung ten (vd watches nam/nu)
    seen.add(p.title);
    rows.push({
      name: p.title,
      description: p.description,
      price: vnd(p.price),
      stock: p.stock ?? 0,
      categoryId: CAT[slug].id,
      image: p.thumbnail ?? p.images?.[0] ?? null,
      brand: p.brand ?? null,
      rating: p.rating ?? null,
    });
  }
}

// Anh cho 4 san pham cu: lay tu chinh data DummyJSON neu khop ten
const pick = (kw) => rows.find((r) => r.name.toLowerCase().includes(kw))?.image ?? null;
const imgIphone = pick("iphone");
const imgSamsung = pick("samsung") ?? pick("galaxy");
const imgMac = pick("macbook");

let id = 5; // product id cu cao nhat = 4
const values = rows
  .map(
    (r) =>
      `    (${id++}, ${sqlStr(r.name)}, ${sqlStr(r.description)}, ${r.price}, ${r.stock}, ${r.categoryId}, FALSE, 0, ${sqlStr(r.image)}, ${sqlStr(r.brand)}, ${sqlNum(r.rating)}, now(), now(), 'system')`,
  )
  .join(",\n");

const sql = `-- ============================================================
-- Flyway V10: anh/brand/rating cho san pham + catalog dien tu that.
-- Nguon data: DummyJSON (https://dummyjson.com) - dataset demo cong khai, anh CDN.
-- Gia quy doi USD -> VND (x${USD_TO_VND}), lam tron nghin. Sinh tu scripts/gen-seed.mjs.
-- ============================================================

ALTER TABLE products ADD COLUMN image_url VARCHAR(512);
ALTER TABLE products ADD COLUMN brand     VARCHAR(128);
ALTER TABLE products ADD COLUMN rating    NUMERIC(3, 2);

-- Chuan hoa ten category (them dau) + bo sung nhom dien tu
UPDATE categories SET name = 'Điện thoại' WHERE id = 1;
UPDATE categories SET name = 'Sách'       WHERE id = 3;
INSERT INTO categories (id, name) VALUES
    (4, 'Máy tính bảng'),
    (5, 'Phụ kiện'),
    (6, 'Đồng hồ');

-- Backfill 4 san pham seed cu
UPDATE products SET brand = 'Apple',   rating = 4.7, image_url = ${sqlStr(imgIphone)}  WHERE id = 1;
UPDATE products SET brand = 'Samsung', rating = 4.5, image_url = ${sqlStr(imgSamsung)} WHERE id = 2;
UPDATE products SET brand = 'Apple',   rating = 4.8, image_url = ${sqlStr(imgMac)}     WHERE id = 3;

-- Catalog dien tu (${rows.length} san pham)
INSERT INTO products (id, name, description, price, stock_quantity, category_id, deleted, version, image_url, brand, rating, created_at, updated_at, created_by) VALUES
${values};

-- Day sequence len qua id chen tay
SELECT setval(pg_get_serial_sequence('categories', 'id'), (SELECT MAX(id) FROM categories));
SELECT setval(pg_get_serial_sequence('products', 'id'),   (SELECT MAX(id) FROM products));
`;

writeFileSync("src/main/resources/db/migration/V10__product_images_and_catalog.sql", sql, "utf8");
console.log(`OK: ${rows.length} san pham, file V10 da ghi.`);
