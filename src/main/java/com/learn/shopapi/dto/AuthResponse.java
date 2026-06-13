package com.learn.shopapi.dto;

import java.util.Set;

/**
 * Ket qua tra ve sau khi dang ky/dang nhap/refresh thanh cong.
 * Client luu accessToken va gui kem header "Authorization: Bearer <accessToken>" o cac request sau.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        String username,
        Set<String> roles
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs,
                                  String username, Set<String> roles) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs, username, roles);
    }

    /**
     * Ban sao KHONG kem refreshToken - dung khi tra ve cho client.
     * Refresh token duoc gui qua cookie httpOnly (xem RefreshTokenCookie), JS khong thay.
     */
    public AuthResponse withoutRefreshToken() {
        return new AuthResponse(accessToken, null, tokenType, expiresInMs, username, roles);
    }
}
