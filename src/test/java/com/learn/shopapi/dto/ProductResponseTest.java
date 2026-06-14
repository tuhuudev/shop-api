package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Category;
import com.learn.shopapi.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Kiem tra mapping Product -> ProductResponse: field moi (image/brand/rating) + tach gallery. */
class ProductResponseTest {

    private Product product() {
        Product p = new Product("iPhone 15", "Smartphone", new BigDecimal("24990000"), 10, new Category("Dien thoai"));
        p.setBrand("Apple");
        p.setRating(new BigDecimal("4.7"));
        return p;
    }

    @Test
    void mapsBrandRatingAndCategory() {
        ProductResponse r = ProductResponse.from(product());
        assertThat(r.brand()).isEqualTo("Apple");
        assertThat(r.rating()).isEqualByComparingTo("4.7");
        assertThat(r.categoryName()).isEqualTo("Dien thoai");
    }

    @Test
    void gallerySplitsImageUrlsByNewline() {
        Product p = product();
        p.setImageUrl("https://cdn/x/thumb.webp");
        p.setImageUrls("https://cdn/x/1.webp\nhttps://cdn/x/2.webp\n  \nhttps://cdn/x/3.webp");
        ProductResponse r = ProductResponse.from(p);
        assertThat(r.imageUrls()).containsExactly(
                "https://cdn/x/1.webp", "https://cdn/x/2.webp", "https://cdn/x/3.webp");
    }

    @Test
    void galleryFallsBackToImageUrlWhenNoList() {
        Product p = product();
        p.setImageUrl("https://cdn/x/thumb.webp");
        ProductResponse r = ProductResponse.from(p);
        assertThat(r.imageUrls()).containsExactly("https://cdn/x/thumb.webp");
    }

    @Test
    void galleryEmptyWhenNoImagesAtAll() {
        ProductResponse r = ProductResponse.from(product());
        assertThat(r.imageUrls()).isEmpty();
    }
}
