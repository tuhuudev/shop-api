package com.learn.shopapi.service;

import com.learn.shopapi.dto.CouponRequest;
import com.learn.shopapi.dto.CouponResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.entity.Coupon;
import com.learn.shopapi.entity.CouponType;
import com.learn.shopapi.repository.CouponRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Ap dung coupon cho mot don: kiem tra hieu luc + tinh so tien giam + "tieu" 1 luot ATOMIC.
 * Goi TRONG tx tao don (cung commit/rollback) -> neu tao don loi thi luot coupon khong bi tru.
 */
@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    // ---- quan tri (ADMIN) ----

    @Transactional
    public CouponResponse create(CouponRequest req) {
        Coupon c = new Coupon(req.code().trim(), req.type(), req.value());
        c.setMinOrderAmount(req.minOrderAmount());
        c.setMaxUses(req.maxUses());
        c.setValidFrom(req.validFrom());
        c.setValidTo(req.validTo());
        c.setActive(req.active() == null || req.active());
        try {
            return CouponResponse.from(couponRepository.save(c));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Ma coupon da ton tai: " + req.code());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> list(Pageable pageable) {
        return PageResponse.from(couponRepository.findAll(pageable), CouponResponse::from);
    }

    /**
     * Tra ve so tien giam hop le cho subtotal, dong thoi tru 1 luot dung cua coupon.
     * Nem IllegalArgumentException neu coupon khong hop le / het luot / chua dat don toi thieu.
     */
    @Transactional
    public BigDecimal applyToSubtotal(String code, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Ma giam gia khong ton tai: " + code));

        LocalDateTime now = LocalDateTime.now();
        if (!coupon.isActive()
                || (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom()))
                || (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo()))) {
            throw new IllegalArgumentException("Ma giam gia khong con hieu luc: " + code);
        }
        if (coupon.getMinOrderAmount() != null && subtotal.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Don toi thieu " + coupon.getMinOrderAmount() + " moi dung duoc ma " + code);
        }

        BigDecimal discount = computeDiscount(coupon, subtotal);

        // Tru 1 luot ATOMIC: 0 -> da het luot (race-safe giua nhieu instance).
        if (couponRepository.tryConsume(coupon.getId()) == 0) {
            throw new IllegalArgumentException("Ma giam gia da het luot su dung: " + code);
        }
        return discount;
    }

    private BigDecimal computeDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal raw = (coupon.getType() == CouponType.PERCENT)
                ? subtotal.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : coupon.getValue();
        // Khong giam vuot qua gia tri don.
        return raw.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }
}
