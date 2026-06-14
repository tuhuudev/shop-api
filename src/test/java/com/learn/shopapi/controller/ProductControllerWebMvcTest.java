package com.learn.shopapi.controller;

import com.learn.shopapi.security.JwtAuthenticationFilter;
import com.learn.shopapi.security.JwtService;
import com.learn.shopapi.security.RestAccessDeniedHandler;
import com.learn.shopapi.security.RestAuthEntryPoint;
import com.learn.shopapi.security.SecurityConfig;
import com.learn.shopapi.service.ProductService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice test cho ProductController: validation -> 400; chua dang nhap -> 401;
 * sai role (CUSTOMER tao san pham) -> 403. Body loi dang RFC7807.
 */
@WebMvcTest(controllers = ProductController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthEntryPoint.class, RestAccessDeniedHandler.class, NoOpCacheTestConfig.class})
@EnableWebSecurity
class ProductControllerWebMvcTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @MockitoBean private ProductService productService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private StringRedisTemplate redisTemplate;

    @Test
    @WithMockUser(roles = "STAFF")
    void create_bodyKhongHopLe_tra400ProblemDetail() throws Exception {
        // name rong + price am -> vi pham @NotBlank/@Positive (body van parse duoc).
        String body = "{\"name\":\"\",\"price\":-1,\"stockQuantity\":5}";
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Du lieu khong hop le"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void create_chuaDangNhap_tra401() throws Exception {
        String body = "{\"name\":\"Ban phim\",\"price\":100}";
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Xac thuc that bai"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void create_saiRole_tra403() throws Exception {
        String body = "{\"name\":\"Ban phim\",\"price\":100,\"stockQuantity\":5}";
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Khong du quyen"))
                .andExpect(jsonPath("$.status").value(403));
    }
}
