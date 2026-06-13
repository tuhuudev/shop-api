package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Category;

/** Du lieu danh muc tra ve cho client. */
public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName());
    }
}
