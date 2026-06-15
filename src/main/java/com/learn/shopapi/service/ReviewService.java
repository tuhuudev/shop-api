package com.learn.shopapi.service;

import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.dto.ReviewRequest;
import com.learn.shopapi.dto.ReviewResponse;
import com.learn.shopapi.entity.Product;
import com.learn.shopapi.entity.Review;
import com.learn.shopapi.entity.User;
import com.learn.shopapi.exception.ResourceNotFoundException;
import com.learn.shopapi.repository.ProductRepository;
import com.learn.shopapi.repository.ReviewRepository;
import com.learn.shopapi.repository.UserRepository;
import com.learn.shopapi.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Danh gia san pham: nguoi dung dang nhap tao; ai cung xem duoc danh sach + diem trung binh. */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse addReview(Long productId, ReviewRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham id=" + productId));
        String username = SecurityUtils.requireCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay user: " + username));
        Review saved = reviewRepository.save(new Review(product, user, req.rating(), req.comment()));
        return ReviewResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listByProduct(Long productId, Pageable pageable) {
        return PageResponse.from(reviewRepository.findByProductId(productId, pageable), ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public Summary summary(Long productId) {
        Double avg = reviewRepository.averageRating(productId);
        long count = reviewRepository.countByProductId(productId);
        double rounded = avg == null ? 0.0
                : BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
        return new Summary(rounded, count);
    }

    /** Tom tat danh gia cua 1 san pham. */
    public record Summary(double average, long count) { }
}
