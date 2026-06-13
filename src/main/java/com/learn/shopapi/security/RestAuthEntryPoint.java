package com.learn.shopapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xu ly khi nguoi dung CHUA dang nhap ma goi endpoint can dang nhap -> tra 401 Unauthorized.
 * Tra ve JSON cung dinh dang voi GlobalExceptionHandler de client xu ly nhat quan.
 */
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    // Tu tao ObjectMapper rieng: Spring Boot 4 dung Jackson 3 cho HTTP nen khong con
    // dang ky san bean ObjectMapper (Jackson 2) de inject.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Dinh dang RFC 7807 (problem+json) cho dong bo voi GlobalExceptionHandler.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", "Xac thuc that bai");
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("detail", "Ban can dang nhap (thieu hoac sai token)");
        body.put("timestamp", LocalDateTime.now().toString());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
