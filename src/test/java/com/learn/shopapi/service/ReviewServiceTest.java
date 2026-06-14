package com.learn.shopapi.service;

import com.learn.shopapi.repository.ProductRepository;
import com.learn.shopapi.repository.ReviewRepository;
import com.learn.shopapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

/**
 * Unit test thuan (Mockito, khong Spring/DB) cho ReviewService.summary:
 * tinh diem trung binh - nhanh chua co review (avg==null -> 0.0) va lam tron HALF_UP scale 1.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRepository productRepository;   // chi de @InjectMocks tao bean
    @Mock private UserRepository userRepository;          // summary khong dung -> khong stub

    @InjectMocks private ReviewService reviewService;

    @Test
    void summary_chuaCoReview_average0vaCountTuRepo() {
        when(reviewRepository.averageRating(1L)).thenReturn(null);
        when(reviewRepository.countByProductId(1L)).thenReturn(0L);

        ReviewService.Summary s = reviewService.summary(1L);

        assertThat(s.average()).isEqualTo(0.0);
        assertThat(s.count()).isEqualTo(0L);
    }

    @Test
    void summary_lamTronLen_HALF_UP() {
        when(reviewRepository.averageRating(1L)).thenReturn(4.25);
        when(reviewRepository.countByProductId(1L)).thenReturn(8L);

        ReviewService.Summary s = reviewService.summary(1L);

        assertThat(s.average()).isEqualTo(4.3, within(1e-9));
        assertThat(s.count()).isEqualTo(8L);
    }

    @Test
    void summary_lamTronXuong_HALF_UP() {
        when(reviewRepository.averageRating(1L)).thenReturn(4.24);
        when(reviewRepository.countByProductId(1L)).thenReturn(5L);

        ReviewService.Summary s = reviewService.summary(1L);

        assertThat(s.average()).isEqualTo(4.2, within(1e-9));
        assertThat(s.count()).isEqualTo(5L);
    }
}
