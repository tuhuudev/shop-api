package com.learn.shopapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xu ly khi nguoi dung DA dang nhap nhung KHONG DU QUYEN -> tra 403 Forbidden.
 * (Khac voi 401: 401 = chua dang nhap; 403 = da dang nhap nhung khong duoc phep.)
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    // Tu tao ObjectMapper rieng (xem ghi chu o RestAuthEntryPoint).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Dinh dang RFC 7807 (problem+json) cho dong bo voi GlobalExceptionHandler.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Khong du quyen");
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("detail", "Ban khong du quyen truy cap tai nguyen nay");
        body.put("timestamp", LocalDateTime.now().toString());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
