package com.learn.shopapi.service;

import com.learn.shopapi.entity.RefreshToken;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.exception.RefreshTokenReuseException;
import com.learn.shopapi.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Quan ly vong doi cua refresh token: tao, kiem tra, xoay vong, thu hoi.
 *
 * Bao mat:
 * - Token tra cho client la chuoi ngau nhien (UUID). Trong DB ta CHI luu BAN BAM (SHA-256)
 *   cua no -> neu DB bi lo, ke tan cong khong dung lai duoc token.
 * - XOAY VONG (rotation): moi lan refresh, thu hoi token cu va cap token moi.
 * - PHAT HIEN TAI SU DUNG: neu mot token DA bi thu hoi lai duoc dung -> nghi bi danh cap,
 *   thu hoi TAT CA token cua user do.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /** Tao refresh token moi cho user, tra ve chuoi token GOC (chua bam) de gui cho client. */
    @Transactional
    public String create(User user) {
        String rawToken = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusMillis(refreshTokenExpirationMs);
        refreshTokenRepository.save(new RefreshToken(sha256(rawToken), user, expiry));
        return rawToken;
    }

    /**
     * Kiem tra token con dung duoc khong; tra entity neu hop le.
     * CHI DOC (readOnly) - khong tu thu hoi o day. Neu phat hien tai su dung, nem
     * RefreshTokenReuseException de tang tren thu hoi trong transaction rieng (commit duoc).
     */
    @Transactional(readOnly = true)
    public RefreshToken verifyUsable(String rawToken) {
        RefreshToken rt = refreshTokenRepository.findByToken(sha256(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Refresh token khong ton tai"));
        if (rt.isRevoked()) {
            throw new RefreshTokenReuseException(rt.getUser());   // token da thu hoi ma con dung -> nghi bi lo
        }
        if (rt.isExpired()) {
            throw new BadCredentialsException("Refresh token da het han, vui long dang nhap lai");
        }
        return rt;
    }

    /** Xoay vong: thu hoi token cu, cap token moi; tra chuoi goc moi cho client. */
    @Transactional
    public String rotate(RefreshToken current) {
        current.setRevoked(true);
        return create(current.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByToken(sha256(rawToken)).ifPresent(rt -> rt.setRevoked(true));
    }

    /**
     * Thu hoi TAT CA token cua user trong transaction RIENG (REQUIRES_NEW) de chac chan
     * COMMIT duoc, ngay ca khi nguoi goi sau do nem loi (vd phat hien tai su dung -> 401).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /** Bam SHA-256 -> chuoi hex. Dung de luu/tra cuu token an toan. */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Khong tim thay thuat toan SHA-256", e);
        }
    }
}
