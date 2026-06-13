package com.learn.shopapi.exception;

import com.learn.shopapi.entity.User;

/**
 * Nem ra khi mot refresh token DA bi thu hoi lai duoc dung tiep -> nghi bi danh cap.
 * Mang theo User de tang tren (AuthService) thu hoi TAT CA token cua user do
 * trong mot transaction RIENG (commit duoc, khong bi rollback boi viec nem loi).
 */
public class RefreshTokenReuseException extends RuntimeException {
    private final transient User user;

    public RefreshTokenReuseException(User user) {
        super("Refresh token da bi thu hoi (nghi bi lo)");
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
