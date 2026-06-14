package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Product;
import java.math.BigDecimal;

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
                p.getBrand(),
                p.getRating(),
                p.getCategory() != null ? p.getCategory().getName() : null
        );
    }
}
