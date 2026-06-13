package com.learn.shopapi.controller;

import com.learn.shopapi.dto.LoginEventResponse;
import com.learn.shopapi.dto.PageResponse;
import com.learn.shopapi.service.LoginEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN xem nhat ky dang nhap (audit bao mat). */
@RestController
@RequestMapping("/api/admin/login-events")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Login events", description = "Nhat ky dang nhap (chi ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class LoginEventController {

    private final LoginEventService loginEventService;

    public LoginEventController(LoginEventService loginEventService) {
        this.loginEventService = loginEventService;
    }

    @GetMapping
    @Operation(summary = "Nhat ky dang nhap moi nhat (phan trang)")
    public PageResponse<LoginEventResponse> list(Pageable pageable) {
        return loginEventService.recent(pageable);
    }
}
