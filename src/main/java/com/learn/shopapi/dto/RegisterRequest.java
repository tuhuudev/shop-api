package com.learn.shopapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Du lieu client gui len khi DANG KY tai khoan moi.
 * Cac rang buoc @NotBlank/@Size/@Pattern duoc @Valid kiem tra truoc khi vao service.
 */
public record RegisterRequest(
        @NotBlank(message = "Username khong duoc de trong")
        @Size(min = 3, max = 50, message = "Username tu 3 den 50 ky tu")
        String username,

        @NotBlank(message = "Mat khau khong duoc de trong")
        @Size(min = 8, message = "Mat khau toi thieu 8 ky tu")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Mat khau phai co ca chu va so")
        String password,

        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        String email,

        String fullName
) { }
