package com.learn.shopapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * RefreshToken = ve "gia han" dang nhap.
 *
 * Access token (JWT) song rat ngan (15 phut) cho an toan. Khi het han, client dung
 * refresh token nay (song dai hon) de xin access token moi ma KHONG phai dang nhap lai.
 * Luu trong DB de co the THU HOI (revoke) khi logout hoac nghi ngo lo token.
 */
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_user", columnList = "user_id"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    private boolean revoked = false;

    protected RefreshToken() { }

    public RefreshToken(String token, User user, Instant expiryDate) {
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.revoked = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public Long getId() { return id; }
    public String getToken() { return token; }
    public User getUser() { return user; }
    public Instant getExpiryDate() { return expiryDate; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
