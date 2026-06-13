package com.learn.shopapi.repository;

import com.learn.shopapi.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    /**
     * Tang used_count ATOMIC, chi khi chua cham tran maxUses (maxUses=null -> khong gioi han).
     * Tra ve 1 neu dung duoc, 0 neu da het luot -> chong race khi nhieu don dung cung coupon.
     */
    @Modifying
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 "
            + "WHERE c.id = :id AND (c.maxUses IS NULL OR c.usedCount < c.maxUses)")
    int tryConsume(@Param("id") Long id);
}
