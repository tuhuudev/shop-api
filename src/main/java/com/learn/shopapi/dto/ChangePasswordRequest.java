package com.learn.shopapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Du lieu doi mat khau cua tai khoan dang dang nhap. */
public record ChangePasswordRequest(
        @NotBlank(message = "Mat khau cu khong duoc de trong")
        String oldPassword,

        @NotBlank(message = "Mat khau moi khong duoc de trong")
        @Size(min = 8, message = "Mat khau moi toi thieu 8 ky tu")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mat khau moi phai co ca chu va so")
        String newPassword
) { }
