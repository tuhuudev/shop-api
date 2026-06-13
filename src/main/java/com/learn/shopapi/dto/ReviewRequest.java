package com.learn.shopapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Du lieu gui len khi danh gia san pham. */
public record ReviewRequest(
        @Min(value = 1, message = "Diem tu 1 den 5")
        @Max(value = 5, message = "Diem tu 1 den 5")
        int rating,

        @Size(max = 1000, message = "Nhan xet toi da 1000 ky tu")
        String comment
) { }
