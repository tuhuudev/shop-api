package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Product;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Du lieu API TRA VE cho client.
 * Tach rieng voi Entity de khong lo cau truc DB ra ngoai.
 */
public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        String imageUrl,
        List<String> imageUrls,
        String brand,
        BigDecimal rating,
        String categoryName
) {
    /** Chuyen tu Entity sang DTO. */
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStockQuantity(),
                p.getImageUrl(),
                gallery(p),
                p.getBrand(),
                p.getRating(),
                p.getCategory() != null ? p.getCategory().getName() : null
        );
    }

    /** image_urls (cach nhau bang xuong dong) -> List; rong thi fallback ve [imageUrl]. */
    private static List<String> gallery(Product p) {
        String raw = p.getImageUrls();
        if (raw != null && !raw.isBlank()) {
            return Arrays.stream(raw.split("\\R")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        return p.getImageUrl() != null ? List.of(p.getImageUrl()) : List.of();
    }
}
