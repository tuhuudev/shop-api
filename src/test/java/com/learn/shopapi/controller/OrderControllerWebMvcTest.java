package com.learn.shopapi.controller;

import com.learn.shopapi.security.JwtAuthenticationFilter;
import com.learn.shopapi.security.JwtService;
import com.learn.shopapi.security.RestAccessDeniedHandler;
import com.learn.shopapi.security.RestAuthEntryPoint;
import com.learn.shopapi.security.SecurityConfig;
import com.learn.shopapi.service.IdempotencyService;
import com.learn.shopapi.service.OrderService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice test cho OrderController: validation -> 400; chua dang nhap -> 401;
 * sai role (CUSTOMER goi PATCH status) -> 403. Tat ca body loi dang RFC7807.
 */
@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthEntryPoint.class, RestAccessDeniedHandler.class, NoOpCacheTestConfig.class})
@EnableWebSecurity
class OrderControllerWebMvcTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @MockitoBean private OrderService orderService;
    @MockitoBean private IdempotencyService idempotencyService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private StringRedisTemplate redisTemplate;

    @Test
    @WithMockUser
    void create_itemsRong_tra400ProblemDetail() throws Exception {
        String body = "{\"items\":[]}";
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Du lieu khong hop le"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    void create_chuaDangNhap_tra401() throws Exception {
        String body = "{\"items\":[{\"productId\":1,\"quantity\":1}]}";
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Xac thuc that bai"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateStatus_saiRole_tra403() throws Exception {
        String body = "{\"status\":\"PAID\"}";
        mvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Khong du quyen"))
                .andExpect(jsonPath("$.status").value(403));
    }
}
