package com.learn.shopapi.dto;

import com.learn.shopapi.entity.Role;
import com.learn.shopapi.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Du lieu user TRA VE cho client. KHONG bao gio chua password (kep ca dang bam).
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        boolean enabled,
        Set<String> roles
) {
    public static UserResponse from(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.isEnabled(),
                roleNames);
    }
}
