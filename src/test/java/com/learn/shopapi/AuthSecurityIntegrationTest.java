package com.learn.shopapi;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tich hop (chay full context + DataSeeder) cho luong xac thuc va phan quyen.
 * Dung MockMvc de goi controller nhu that ma khong can mo cong mang.
 */
@SpringBootTest
class AuthSecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // apply(springSecurity()) de chuoi filter bao mat (JWT, phan quyen) duoc kich hoat trong test.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void danhSachSanPham_congKhai_tra200() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void taoSanPham_khongToken_tra401() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":1,\"stockQuantity\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dangKyRoiDangNhap_traToken() throws Exception {
        String registerBody = """
                {"username":"newbie","password":"secret123","email":"newbie@example.com","fullName":"Newbie"}
                """;
        var registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();
        String token = JsonPath.read(registerResult.getResponse().getContentAsString(), "$.accessToken");
        assertThat(token).isNotBlank();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newbie\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_xoayVongVaChongTaiSuDung() throws Exception {
        // Dang ky -> refresh token nam o COOKIE httpOnly (khong con trong body).
        String body = """
                {"username":"rotuser","password":"secret123","email":"rotuser@example.com"}
                """;
        var reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        Cookie cookieA = reg.getResponse().getCookie("refresh_token");
        assertThat(cookieA).isNotNull();
        String tokenA = cookieA.getValue();
        assertThat(tokenA).isNotBlank();

        // refresh bang cookie A -> 200, dat cookie MOI (B) khac A
        var refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", tokenA)))
                .andExpect(status().isOk()).andReturn();
        Cookie cookieB = refreshed.getResponse().getCookie("refresh_token");
        assertThat(cookieB).isNotNull();
        String tokenB = cookieB.getValue();
        assertThat(tokenB).isNotBlank().isNotEqualTo(tokenA);

        // Dung lai A (da xoay vong) -> 401 (phat hien tai su dung)
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", tokenA)))
                .andExpect(status().isUnauthorized());

        // B cung bi thu hoi theo (vi nghi bi lo) -> 401
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", tokenB)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAll_canDangNhap_vaThuHoiHetPhien() throws Exception {
        var reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alluser","password":"secret123","email":"alluser@example.com"}
                                """))
                .andExpect(status().isCreated()).andReturn();
        String access = JsonPath.read(reg.getResponse().getContentAsString(), "$.accessToken");
        String cookie = reg.getResponse().getCookie("refresh_token").getValue();

        // Chua dang nhap -> 401
        mockMvc.perform(post("/api/auth/logout-all"))
                .andExpect(status().isUnauthorized());

        // Co access token -> 204
        mockMvc.perform(post("/api/auth/logout-all").header("Authorization", "Bearer " + access))
                .andExpect(status().isNoContent());

        // Sau logout-all: refresh token cu khong con dung -> 401
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", cookie)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saiMatKhau_tra401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"sai-mat-khau\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerTaoSanPham_tra403() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":1000,\"stockQuantity\":1,\"categoryId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminTaoSanPham_tra201() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chuot\",\"price\":150000,\"stockQuantity\":10,\"categoryId\":1}"))
                .andExpect(status().isCreated());
    }
}
