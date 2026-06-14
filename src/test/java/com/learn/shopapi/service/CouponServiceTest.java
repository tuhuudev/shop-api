package com.learn.shopapi.service;

import com.learn.shopapi.entity.Coupon;
import com.learn.shopapi.entity.CouponType;
import com.learn.shopapi.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test cho CouponService.applyToSubtotal (Mockito, khong dung DB). */
@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @InjectMocks private CouponService couponService;

    private void stubFind(Coupon c) {
        when(couponRepository.findByCode(c.getCode())).thenReturn(Optional.of(c));
    }

    @Test
    void percent_tinhDungVaTruLuot() {
        Coupon c = new Coupon("SALE10", CouponType.PERCENT, new BigDecimal("10"));
        stubFind(c);
        when(couponRepository.tryConsume(any())).thenReturn(1);

        BigDecimal discount = couponService.applyToSubtotal("SALE10", new BigDecimal("200.00"));

        assertThat(discount).isEqualByComparingTo("20.00");
        verify(couponRepository).tryConsume(any());
    }

    @Test
    void fixed_khongGiamVuotQuaSubtotal() {
        Coupon c = new Coupon("BIG", CouponType.FIXED, new BigDecimal("100.00"));
        stubFind(c);
        when(couponRepository.tryConsume(any())).thenReturn(1);

        BigDecimal discount = couponService.applyToSubtotal("BIG", new BigDecimal("50.00"));

        // raw=100 nhung don chi 50 -> giam toi da = 50 (raw.min(subtotal)).
        assertThat(discount).isEqualByComparingTo("50.00");
    }

    @Test
    void duoiDonToiThieu_nem_vaKhongTruLuot() {
        Coupon c = new Coupon("MIN100", CouponType.FIXED, new BigDecimal("10.00"));
        c.setMinOrderAmount(new BigDecimal("100.00"));
        stubFind(c);

        assertThatThrownBy(() -> couponService.applyToSubtotal("MIN100", new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Don toi thieu");
        verify(couponRepository, never()).tryConsume(any());
    }

    @Test
    void hetLuot_tryConsumeTraVe0_nem() {
        Coupon c = new Coupon("USEDUP", CouponType.FIXED, new BigDecimal("10.00"));
        stubFind(c);
        when(couponRepository.tryConsume(any())).thenReturn(0);

        assertThatThrownBy(() -> couponService.applyToSubtotal("USEDUP", new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("het luot");
    }

    @Test
    void inactive_nem_vaKhongTruLuot() {
        Coupon c = new Coupon("OFF", CouponType.FIXED, new BigDecimal("10.00"));
        c.setActive(false);
        stubFind(c);

        assertThatThrownBy(() -> couponService.applyToSubtotal("OFF", new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong con hieu luc");
        verify(couponRepository, never()).tryConsume(any());
    }

    @Test
    void hetHan_validToTrongQuaKhu_nem() {
        Coupon c = new Coupon("EXPIRED", CouponType.FIXED, new BigDecimal("10.00"));
        c.setValidTo(LocalDateTime.now().minusDays(1));
        stubFind(c);

        assertThatThrownBy(() -> couponService.applyToSubtotal("EXPIRED", new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong con hieu luc");
    }
}
