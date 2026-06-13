package com.learn.shopapi.controller;

import com.learn.shopapi.dto.AuthResponse;
import com.learn.shopapi.dto.ChangePasswordRequest;
import com.learn.shopapi.dto.LoginRequest;
import com.learn.shopapi.dto.RefreshRequest;
import com.learn.shopapi.dto.RegisterRequest;
import com.learn.shopapi.dto.UserResponse;
import com.learn.shopapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Cac endpoint xac thuc. Tat ca deu CONG KHAI (xem SecurityConfig: /api/auth/** permitAll)
 * tru /me yeu cau da dang nhap.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Dang ky, dang nhap, refresh token")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dang ky tai khoan moi (role CUSTOMER) va dang nhap luon")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Dang nhap, tra ve access token + refresh token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Xin access token moi tu refresh token")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Thu hoi refresh token (dang xuat)")
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Thong tin tai khoan dang dang nhap")
    public UserResponse me() {
        return authService.getCurrentUser();
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Doi mat khau (can dang nhap, biet mat khau cu)")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.oldPassword(), request.newPassword());
    }
}
