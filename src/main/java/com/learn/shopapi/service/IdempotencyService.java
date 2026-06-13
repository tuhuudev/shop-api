package com.learn.shopapi.service;

import com.learn.shopapi.security.SecurityUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Idempotency: cho phep client gui header "Idempotency-Key" khi tao tai nguyen (vd dat don)
 * de RETRY an toan - goi lai cung key tra ve cung ket qua, KHONG tao don trung.
 *
 * Luu ket qua tren Redis (chia se giua replica). Mot khoa "in-progress" (SETNX) chong 2 request
 * cung key chay dong thoi tao trung. Khong co key -> thuc thi binh thuong (khong idempotent).
 */
@Service
public class IdempotencyService {

    private static final String RESULT_PREFIX = "shop:idemp:result:";
    private static final String LOCK_PREFIX = "shop:idemp:lock:";
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, Object> redis;

    public IdempotencyService(RedisTemplate<String, Object> jsonRedisTemplate) {
        this.redis = jsonRedisTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T> T execute(String idempotencyKey, Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();   // khong dung idempotency
        }
        // Scope theo user dang dang nhap: 2 user gui cung key (vd "1") KHONG duoc dung chung
        // ket qua -> tranh tra ve don/ket qua cua nguoi khac.
        String scoped = SecurityUtils.getCurrentUsername().orElse("anon") + ":" + idempotencyKey;
        String resultKey = RESULT_PREFIX + scoped;
        String lockKey = LOCK_PREFIX + scoped;

        Object cached = redis.opsForValue().get(resultKey);
        if (cached != null) {
            return (T) cached;     // da xu ly truoc do -> tra ket qua cu
        }
        // Khoa "dang xu ly": neu khong gianh duoc -> co request cung key dang chay -> tu choi.
        Boolean acquired = redis.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            throw new IllegalStateException("Yeu cau trung dang duoc xu ly, vui long thu lai sau");
        }

        T result;
        try {
            result = action.get();
        } catch (RuntimeException e) {
            redis.delete(lockKey);   // tha lock de retry duoc neu that bai
            throw e;
        }
        redis.opsForValue().set(resultKey, result, RESULT_TTL);
        redis.delete(lockKey);
        return result;
    }
}
