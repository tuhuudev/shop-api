package com.learn.shopapi.controller;

import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.dto.UserResponse;
import com.learn.shopapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Quan tri user - CHI ADMIN. @PreAuthorize chan o tung method (method security).
 * hasRole('ADMIN') khop voi authority "ROLE_ADMIN" duoc nap luc dang nhap.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Users", description = "Quan ly tai khoan (chi ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Danh sach user (phan trang)")
    public PageResponse<UserResponse> list(Pageable pageable) {
        return userService.findAll(pageable);
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Gan lai bo vai tro cho user")
    public UserResponse setRoles(@PathVariable Long id, @RequestBody Set<String> roleNames) {
        return userService.setRoles(id, roleNames);
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Khoa (false) hoac mo (true) tai khoan")
    public UserResponse setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return userService.setEnabled(id, enabled);
    }
}
