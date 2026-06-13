package com.learn.shopapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test ap COUPON luc tao don (luong tien): coupon hop le giam dung;
 * coupon khong ton tai -> 400.
 */
@SpringBootTest
class CouponIntegrationTest extends AbstractIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void coupon_percent_giamDung() throws Exception {
        // WELCOME10 (PERCENT 10) duoc seed o Flyway V7.
        var res = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}],\"couponCode\":\"WELCOME10\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String json = res.getResponse().getContentAsString();

        BigDecimal subtotal = new BigDecimal(JsonPath.read(json, "$.subtotal").toString());
        BigDecimal discount = new BigDecimal(JsonPath.read(json, "$.discountAmount").toString());
        BigDecimal total = new BigDecimal(JsonPath.read(json, "$.totalAmount").toString());
        String coupon = JsonPath.read(json, "$.couponCode");

        assertThat(coupon).isEqualTo("WELCOME10");
        assertThat(discount).isEqualByComparingTo(
                subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP));
        assertThat(total).isEqualByComparingTo(subtotal.subtract(discount));
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void coupon_khongTonTai_tra400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}],\"couponCode\":\"KHONG-CO\"}"))
                .andExpect(status().isBadRequest());
    }
}
