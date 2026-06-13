package com.learn.shopapi.controller;

import com.learn.shopapi.dto.AuthResponse;
import com.learn.shopapi.dto.ChangePasswordRequest;
import com.learn.shopapi.dto.LoginRequest;
import com.learn.shopapi.dto.RegisterRequest;
import com.learn.shopapi.dto.UserResponse;
import com.learn.shopapi.security.RefreshTokenCookie;
import com.learn.shopapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * Cac endpoint xac thuc.
 *
 * REFRESH TOKEN duoc gui/nhan qua COOKIE httpOnly (RefreshTokenCookie), KHONG nam trong body
 * -> JavaScript khong doc duoc (chong danh cap token khi dinh XSS).
 * ACCESS TOKEN (ngan han) tra trong body de client giu trong bo nho.
 *
 * register/login/refresh/logout cong khai (SecurityConfig permitAll); /me + change-password can dang nhap.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Dang ky, dang nhap, refresh token (cookie httpOnly)")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshCookie;

    public AuthController(AuthService authService, RefreshTokenCookie refreshCookie) {
        this.authService = authService;
        this.refreshCookie = refreshCookie;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Dang ky tai khoan moi (role CUSTOMER) va dang nhap luon")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        refreshCookie.set(response, auth.refreshToken());
        return auth.withoutRefreshToken();
    }

    @PostMapping("/login")
    @Operation(summary = "Dang nhap: tra access token (body) + refresh token (cookie httpOnly)")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        refreshCookie.set(response, auth.refreshToken());
        return auth.withoutRefreshToken();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Xin access token moi - refresh token lay tu COOKIE (khong can body)")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = refreshCookie.read(request)
                .orElseThrow(() -> new BadCredentialsException("Thieu refresh token"));
        AuthResponse auth = authService.refresh(token);
        refreshCookie.set(response, auth.refreshToken());   // xoay vong -> dat cookie moi
        return auth.withoutRefreshToken();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dang xuat: thu hoi refresh token va xoa cookie")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        refreshCookie.read(request).ifPresent(authService::logout);
        refreshCookie.clear(response);
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Dang xuat khoi TAT CA thiet bi (thu hoi het refresh token)")
    public void logoutAll(HttpServletResponse response) {
        authService.logoutAll();      // can dang nhap (khong nam trong permitAll)
        refreshCookie.clear(response); // xoa cookie tren thiet bi hien tai
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
