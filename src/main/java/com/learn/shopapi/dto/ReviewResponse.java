package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Review;

import java.time.LocalDateTime;

/** Danh gia tra ve cho client. */
public record ReviewResponse(
        Long id,
        int rating,
        String comment,
        String username,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getRating(),
                r.getComment(),
                r.getUser().getUsername(),
                r.getCreatedAt());
    }
}
