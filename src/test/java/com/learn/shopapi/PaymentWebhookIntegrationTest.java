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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test luong WEBHOOK thanh toan (luong tien -> can chac chan):
 * chu ky dung -> don sang PAID; goi lai (idempotent); chu ky sai -> 401.
 */
@SpringBootTest
class PaymentWebhookIntegrationTest extends AbstractIntegrationTest {

    private static final String DEV_SECRET = "dev-webhook-secret";

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void webhook_chuKyDung_chuyenDonSangPaid_vaIdempotent() throws Exception {
        var order = createPendingOrder();
        long orderId = order.id();

        String ref = "pay-it-1";
        String sig = sign(ref + "|" + orderId + "|" + order.total() + "|SUCCEEDED");
        String body = webhookBody(ref, orderId, order.total());

        // Lan 1: 200, payment SUCCEEDED
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        // Don da PAID
        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // Idempotent: goi lai cung providerRef -> van 200, khong loi
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Signature", sig)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerRef").value(ref));
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void webhook_chuKySai_tra401() throws Exception {
        var order = createPendingOrder();
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Signature", "deadbeef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookBody("pay-bad", order.id(), order.total())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void webhook_thanhToanThieu_donVanPending() throws Exception {
        long orderId = createPendingOrder().id();

        String ref = "pay-it-underpaid";
        mockMvc.perform(post("/api/payments/webhook")
                        .header("X-Signature", sign(ref + "|" + orderId + "|1|SUCCEEDED"))
                        .contentType(MediaType.APPLICATION_JSON).content(webhookBody(ref, orderId, "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));   // van luu ban ghi de doi soat

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ---- helpers ----

    private record CreatedOrder(long id, String total) {}

    private CreatedOrder createPendingOrder() throws Exception {
        var res = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        // toPlainString giong PaymentService (chuoi ky): 1290000 chu khong phai 1.29E+6
        String total = new BigDecimal(JsonPath.read(json, "$.totalAmount").toString()).toPlainString();
        return new CreatedOrder(id, total);
    }

    private String webhookBody(String ref, long orderId, String amount) {
        return "{\"providerRef\":\"" + ref + "\",\"orderId\":" + orderId
                + ",\"amount\":" + amount + ",\"status\":\"SUCCEEDED\"}";
    }

    private static String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(DEV_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
