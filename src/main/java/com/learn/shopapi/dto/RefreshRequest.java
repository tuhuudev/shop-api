package com.learn.shopapi.dto;

import jakarta.validation.constraints.NotBlank;

/** Du lieu gui len de xin access token moi tu refresh token. */
public record RefreshRequest(
        @NotBlank(message = "Refresh token khong duoc de trong")
        String refreshToken
) { }
