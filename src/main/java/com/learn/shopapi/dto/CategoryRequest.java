package com.learn.shopapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Du lieu tao/sua danh muc. */
public record CategoryRequest(
        @NotBlank(message = "Ten danh muc khong duoc de trong")
        @Size(max = 100, message = "Ten danh muc toi da 100 ky tu")
        String name
) { }
