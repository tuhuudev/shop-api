package com.learn.shopapi.controller;

import com.learn.shopapi.security.JwtAuthenticationFilter;
import com.learn.shopapi.security.JwtService;
import com.learn.shopapi.security.RestAccessDeniedHandler;
import com.learn.shopapi.security.RestAuthEntryPoint;
import com.learn.shopapi.security.SecurityConfig;
import com.learn.shopapi.service.CartService;
import com.learn.shopapi.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice test cho CartController: validation -> 400; chua dang nhap -> 401. Body loi dang RFC7807.
 */
@WebMvcTest(controllers = CartController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthEntryPoint.class, RestAccessDeniedHandler.class, NoOpCacheTestConfig.class})
@EnableWebSecurity
class CartControllerWebMvcTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @MockitoBean private CartService cartService;
    @MockitoBean private IdempotencyService idempotencyService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private StringRedisTemplate redisTemplate;

    @Test
    @WithMockUser
    void addItem_bodyKhongHopLe_tra400ProblemDetail() throws Exception {
        // productId null + quantity <= 0 -> vi pham @NotNull/@Positive.
        String body = "{\"productId\":null,\"quantity\":0}";
        mvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Du lieu khong hop le"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.productId").exists());
    }

    @Test
    void view_chuaDangNhap_tra401() throws Exception {
        mvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Xac thuc that bai"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
