-- ============================================================
-- Flyway V11: gallery nhieu anh moi san pham (cot image_urls, cac URL cach nhau bang xuong dong).
-- Nguon DummyJSON images[]. Map id 5..52 theo dung thu tu V10. Sinh tu scripts/gen-seed-v11.mjs.
-- ============================================================

ALTER TABLE products ADD COLUMN image_urls VARCHAR(2000);

-- San pham seed cu (1-3): gallery = dung anh dai dien
UPDATE products SET image_urls = image_url WHERE id IN (1, 2, 3) AND image_url IS NOT NULL;

-- Catalog dien tu
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/1.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/2.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-5s/3.webp' WHERE id = 5;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-6/1.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-6/2.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-6/3.webp' WHERE id = 6;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/1.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/2.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/3.webp' WHERE id = 7;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/iphone-x/1.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-x/2.webp
https://cdn.dummyjson.com/product-images/smartphones/iphone-x/3.webp' WHERE id = 8;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/1.webp
https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/2.webp
https://cdn.dummyjson.com/product-images/smartphones/oppo-a57/3.webp' WHERE id = 9;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/1.webp
https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/2.webp
https://cdn.dummyjson.com/product-images/smartphones/oppo-f19-pro-plus/3.webp' WHERE id = 10;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/1.webp
https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/2.webp
https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/3.webp
https://cdn.dummyjson.com/product-images/smartphones/oppo-k1/4.webp' WHERE id = 11;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/realme-c35/1.webp
https://cdn.dummyjson.com/product-images/smartphones/realme-c35/2.webp
https://cdn.dummyjson.com/product-images/smartphones/realme-c35/3.webp' WHERE id = 12;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/realme-x/1.webp
https://cdn.dummyjson.com/product-images/smartphones/realme-x/2.webp
https://cdn.dummyjson.com/product-images/smartphones/realme-x/3.webp' WHERE id = 13;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/realme-xt/1.webp
https://cdn.dummyjson.com/product-images/smartphones/realme-xt/2.webp
https://cdn.dummyjson.com/product-images/smartphones/realme-xt/3.webp' WHERE id = 14;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/1.webp
https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/2.webp
https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s7/3.webp' WHERE id = 15;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/1.webp
https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/2.webp
https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s8/3.webp' WHERE id = 16;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/1.webp
https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/2.webp
https://cdn.dummyjson.com/product-images/smartphones/samsung-galaxy-s10/3.webp' WHERE id = 17;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/1.webp
https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/2.webp
https://cdn.dummyjson.com/product-images/smartphones/vivo-s1/3.webp' WHERE id = 18;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/1.webp
https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/2.webp
https://cdn.dummyjson.com/product-images/smartphones/vivo-v9/3.webp' WHERE id = 19;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/1.webp
https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/2.webp
https://cdn.dummyjson.com/product-images/smartphones/vivo-x21/3.webp' WHERE id = 20;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/1.webp
https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/2.webp
https://cdn.dummyjson.com/product-images/laptops/apple-macbook-pro-14-inch-space-grey/3.webp' WHERE id = 21;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/1.webp
https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/2.webp
https://cdn.dummyjson.com/product-images/laptops/asus-zenbook-pro-dual-screen-laptop/3.webp' WHERE id = 22;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/1.webp
https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/2.webp
https://cdn.dummyjson.com/product-images/laptops/huawei-matebook-x-pro/3.webp' WHERE id = 23;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/1.webp
https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/2.webp
https://cdn.dummyjson.com/product-images/laptops/lenovo-yoga-920/3.webp' WHERE id = 24;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/1.webp
https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/2.webp
https://cdn.dummyjson.com/product-images/laptops/new-dell-xps-13-9300-laptop/3.webp' WHERE id = 25;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/1.webp
https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/2.webp
https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/3.webp
https://cdn.dummyjson.com/product-images/tablets/ipad-mini-2021-starlight/4.webp' WHERE id = 26;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-s8-plus-grey/1.webp
https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-s8-plus-grey/2.webp
https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-s8-plus-grey/3.webp
https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-s8-plus-grey/4.webp' WHERE id = 27;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-white/1.webp
https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-white/2.webp
https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-white/3.webp
https://cdn.dummyjson.com/product-images/tablets/samsung-galaxy-tab-white/4.webp' WHERE id = 28;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/amazon-echo-plus/1.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/amazon-echo-plus/2.webp' WHERE id = 29;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods/1.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods/2.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods/3.webp' WHERE id = 30;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpods-max-silver/1.webp' WHERE id = 31;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-airpower-wireless-charger/1.webp' WHERE id = 32;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-homepod-mini-cosmic-grey/1.webp' WHERE id = 33;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-iphone-charger/1.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/apple-iphone-charger/2.webp' WHERE id = 34;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-magsafe-battery-pack/1.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/apple-magsafe-battery-pack/2.webp' WHERE id = 35;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/apple-watch-series-4-gold/1.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/apple-watch-series-4-gold/2.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/apple-watch-series-4-gold/3.webp' WHERE id = 36;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/beats-flex-wireless-earphones/1.webp' WHERE id = 37;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/iphone-12-silicone-case-with-magsafe-plum/1.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/iphone-12-silicone-case-with-magsafe-plum/2.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/iphone-12-silicone-case-with-magsafe-plum/3.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/iphone-12-silicone-case-with-magsafe-plum/4.webp' WHERE id = 38;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/monopod/1.webp
https://cdn.dummyjson.com/product-images/mobile-accessories/monopod/2.webp' WHERE id = 39;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/selfie-lamp-with-iphone/1.webp' WHERE id = 40;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/selfie-stick-monopod/1.webp' WHERE id = 41;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mobile-accessories/tv-studio-camera-pedestal/1.webp' WHERE id = 42;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mens-watches/brown-leather-belt-watch/1.webp
https://cdn.dummyjson.com/product-images/mens-watches/brown-leather-belt-watch/2.webp
https://cdn.dummyjson.com/product-images/mens-watches/brown-leather-belt-watch/3.webp' WHERE id = 43;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mens-watches/longines-master-collection/1.webp
https://cdn.dummyjson.com/product-images/mens-watches/longines-master-collection/2.webp
https://cdn.dummyjson.com/product-images/mens-watches/longines-master-collection/3.webp' WHERE id = 44;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-date-black-dial/1.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-date-black-dial/2.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-date-black-dial/3.webp' WHERE id = 45;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-moonphase/1.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-moonphase/2.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-cellini-moonphase/3.webp' WHERE id = 46;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-datejust/1.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-datejust/2.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-datejust/3.webp' WHERE id = 47;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/mens-watches/rolex-submariner-watch/1.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-submariner-watch/2.webp
https://cdn.dummyjson.com/product-images/mens-watches/rolex-submariner-watch/3.webp' WHERE id = 48;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/womens-watches/iwc-ingenieur-automatic-steel/1.webp
https://cdn.dummyjson.com/product-images/womens-watches/iwc-ingenieur-automatic-steel/2.webp
https://cdn.dummyjson.com/product-images/womens-watches/iwc-ingenieur-automatic-steel/3.webp' WHERE id = 49;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/womens-watches/rolex-datejust-women/1.webp
https://cdn.dummyjson.com/product-images/womens-watches/rolex-datejust-women/2.webp
https://cdn.dummyjson.com/product-images/womens-watches/rolex-datejust-women/3.webp' WHERE id = 50;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/womens-watches/watch-gold-for-women/1.webp
https://cdn.dummyjson.com/product-images/womens-watches/watch-gold-for-women/2.webp
https://cdn.dummyjson.com/product-images/womens-watches/watch-gold-for-women/3.webp' WHERE id = 51;
UPDATE products SET image_urls = 'https://cdn.dummyjson.com/product-images/womens-watches/women''s-wrist-watch/1.webp
https://cdn.dummyjson.com/product-images/womens-watches/women''s-wrist-watch/2.webp
https://cdn.dummyjson.com/product-images/womens-watches/women''s-wrist-watch/3.webp' WHERE id = 52;
