package com.learn.shopapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Du lieu them/sua 1 dong trong gio hang. */
public record CartItemRequest(
        @NotNull(message = "productId khong duoc null")
        Long productId,

        @Positive(message = "So luong phai lon hon 0")
        int quantity
) { }
