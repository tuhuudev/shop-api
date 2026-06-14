// Sinh Flyway V11: them cot image_urls (TEXT, cac URL cach nhau bang \n) cho gallery nhieu anh.
// Map id theo DUNG thu tu V10 (cung category order + dedup) -> id 5..52 khop.
import { writeFileSync } from "node:fs";

const SLUGS = ["smartphones", "laptops", "tablets", "mobile-accessories", "mens-watches", "womens-watches"];
const esc = (s) => String(s).replace(/'/g, "''");

async function fetchCat(slug) {
  const r = await fetch(`https://dummyjson.com/products/category/${slug}?limit=0`);
  if (!r.ok) throw new Error(`fetch ${slug} -> ${r.status}`);
  return (await r.json()).products ?? [];
}

const seen = new Set();
const rows = [];
for (const slug of SLUGS) {
  for (const p of await fetchCat(slug)) {
    if (seen.has(p.title)) continue;
    seen.add(p.title);
    const imgs = (p.images?.length ? p.images : [p.thumbnail]).filter(Boolean).slice(0, 5);
    rows.push(imgs.join("\n"));
  }
}

let id = 5;
const updates = rows.map((urls) => `UPDATE products SET image_urls = '${esc(urls)}' WHERE id = ${id++};`).join("\n");

const sql = `-- ============================================================
-- Flyway V11: gallery nhieu anh moi san pham (cot image_urls, cac URL cach nhau bang xuong dong).
-- Nguon DummyJSON images[]. Map id 5..${id - 1} theo dung thu tu V10. Sinh tu scripts/gen-seed-v11.mjs.
-- ============================================================

ALTER TABLE products ADD COLUMN image_urls VARCHAR(2000);

-- San pham seed cu (1-3): gallery = dung anh dai dien
UPDATE products SET image_urls = image_url WHERE id IN (1, 2, 3) AND image_url IS NOT NULL;

-- Catalog dien tu
${updates}
`;

writeFileSync("src/main/resources/db/migration/V11__product_gallery.sql", sql, "utf8");
console.log(`OK: ${rows.length} san pham co gallery, V11 da ghi.`);
