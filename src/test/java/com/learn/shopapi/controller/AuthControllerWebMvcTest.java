package com.learn.shopapi.controller;

import com.learn.shopapi.security.JwtAuthenticationFilter;
import com.learn.shopapi.security.JwtService;
import com.learn.shopapi.security.RefreshTokenCookie;
import com.learn.shopapi.security.RestAccessDeniedHandler;
import com.learn.shopapi.security.RestAuthEntryPoint;
import com.learn.shopapi.security.SecurityConfig;
import com.learn.shopapi.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice test (khong Spring context day du / khong Docker) cho AuthController:
 * validation -> 400 ProblemDetail co errors; endpoint can dang nhap -> 401 RFC7807.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthEntryPoint.class, RestAccessDeniedHandler.class, NoOpCacheTestConfig.class})
@EnableWebSecurity
class AuthControllerWebMvcTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @MockitoBean private AuthService authService;
    @MockitoBean private RefreshTokenCookie refreshTokenCookie;
    // Deps cua security chain / filter beans ma @WebMvcTest tu nap.
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private StringRedisTemplate redisTemplate;

    @Test
    void register_bodyKhongHopLe_tra400ProblemDetail() throws Exception {
        // username rong, password thieu so, email sai dinh dang.
        String body = "{\"username\":\"\",\"password\":\"short\",\"email\":\"bad\"}";
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Du lieu khong hop le"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void login_usernameRong_tra400() throws Exception {
        String body = "{\"username\":\"\",\"password\":\"\"}";
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Du lieu khong hop le"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void changePassword_chuaDangNhap_tra401() throws Exception {
        String body = "{\"oldPassword\":\"oldpass1\",\"newPassword\":\"newpass1\"}";
        mvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Xac thuc that bai"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
