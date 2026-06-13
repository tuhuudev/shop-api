package com.learn.shopapi.dto;

import jakarta.validation.constraints.NotBlank;

/** Du lieu client gui len khi DANG NHAP. */
public record LoginRequest(
        @NotBlank(message = "Username khong duoc de trong")
        String username,

        @NotBlank(message = "Mat khau khong duoc de trong")
        String password
) { }
