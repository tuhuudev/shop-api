package com.learn.shopapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chong do mat khau (brute-force): dem so lan dang nhap sai theo tung username.
 *
 * Luu trong bo nho (ConcurrentHashMap) cho don gian/de hoc. Du an that nhieu instance
 * nen dung Redis/bucket4j de chia se trang thai giua cac may.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final long blockMillis;

    // username -> trang thai (so lan sai + thoi diem het khoa)
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts}") int maxAttempts,
            @Value("${app.security.login.block-minutes}") long blockMinutes) {
        this.maxAttempts = maxAttempts;
        this.blockMillis = blockMinutes * 60_000;
    }

    /** True neu username dang bi khoa tam thoi. */
    public boolean isBlocked(String username) {
        Attempt a = attempts.get(username);
        if (a == null) return false;
        if (a.count < maxAttempts) return false;
        if (Instant.now().isAfter(a.blockedUntil)) {   // het han khoa -> reset
            attempts.remove(username);
            return false;
        }
        return true;
    }

    /** Goi khi dang nhap SAI: tang dem; den nguong thi dat moc het khoa. */
    public void recordFailure(String username) {
        attempts.compute(username, (k, a) -> {
            Attempt cur = (a == null) ? new Attempt() : a;
            cur.count++;
            if (cur.count >= maxAttempts) {
                cur.blockedUntil = Instant.now().plusMillis(blockMillis);
            }
            return cur;
        });
    }

    /** Goi khi dang nhap THANH CONG: xoa lich su sai. */
    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    private static class Attempt {
        int count = 0;
        Instant blockedUntil = Instant.EPOCH;
    }
}
