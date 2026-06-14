package com.learn.shopapi.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Du lieu CLIENT GUI LEN khi tao/sua san pham.
 * Cac annotation @NotBlank, @Positive... se tu dong kiem tra,
 * neu sai se tra loi 400 (xem GlobalExceptionHandler).
 *
 * record = kieu du lieu bat bien gon nhe cua Java (tu sinh constructor, getter).
 */
public record ProductRequest(
        @NotBlank(message = "Ten san pham khong duoc de trong")
        String name,

        String description,

        @NotNull(message = "Gia khong duoc null")
        @Positive(message = "Gia phai lon hon 0")
        BigDecimal price,

        @PositiveOrZero(message = "So luong ton khong duoc am")
        int stockQuantity,

        @Size(max = 512, message = "URL anh toi da 512 ky tu")
        String imageUrl,

        @Size(max = 128, message = "Brand toi da 128 ky tu")
        String brand,

        Long categoryId
) { }
