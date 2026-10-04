package com.learn.shopapi.service;

import com.learn.shopapi.dto.PaymentResponse;
import com.learn.shopapi.dto.PaymentWebhookRequest;
import com.learn.shopapi.entity.Payment;
import com.learn.shopapi.entity.PaymentStatus;
import com.learn.shopapi.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test cho PaymentService.handleWebhook: chu ky HMAC, idempotency, fail-fast secret o prod. */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String SECRET = "test-webhook-secret";

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderService orderService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderService, SECRET, new MockEnvironment());
    }

    private static String sign(PaymentWebhookRequest r, String secret) throws Exception {
        String canonical = r.providerRef() + "|" + r.orderId() + "|" + r.amount().toPlainString() + "|" + r.status().name();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    }

    private static PaymentWebhookRequest req(PaymentStatus status) {
        return new PaymentWebhookRequest("ref-1", 42L, new BigDecimal("199.00"), status);
    }

    @Test
    void succeeded_chuKyDung_luuVaXacNhanDon() throws Exception {
        var r = req(PaymentStatus.SUCCEEDED);
        when(paymentRepository.findByProviderRef("ref-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse res = paymentService.handleWebhook(r, sign(r, SECRET));

        assertThat(res.status()).isEqualTo("SUCCEEDED");
        assertThat(res.amount()).isEqualByComparingTo("199.00");
        verify(orderService).confirmPaid(42L);
    }

    @Test
    void failed_khongXacNhanDon() throws Exception {
        var r = req(PaymentStatus.FAILED);
        when(paymentRepository.findByProviderRef("ref-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handleWebhook(r, sign(r, SECRET));

        verify(orderService, never()).confirmPaid(anyLong());
    }

    @Test
    void callbackTrung_traKetQuaCu_khongXuLyLai() throws Exception {
        var r = req(PaymentStatus.SUCCEEDED);
        var existing = new Payment(42L, "mock", "ref-1", new BigDecimal("199.00"), PaymentStatus.SUCCEEDED);
        when(paymentRepository.findByProviderRef("ref-1")).thenReturn(Optional.of(existing));

        PaymentResponse res = paymentService.handleWebhook(r, sign(r, SECRET));

        assertThat(res.providerRef()).isEqualTo("ref-1");
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).confirmPaid(anyLong());
    }

    @Test
    void chuKySai_401_khongDungDb() throws Exception {
        var r = req(PaymentStatus.SUCCEEDED);
        String forged = sign(r, "attacker-secret");

        assertThatThrownBy(() -> paymentService.handleWebhook(r, forged))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(paymentRepository, never()).save(any());
        verify(orderService, never()).confirmPaid(anyLong());
    }

    @Test
    void suaSoTienSauKhiKy_401() throws Exception {
        var signed = req(PaymentStatus.SUCCEEDED);
        var tampered = new PaymentWebhookRequest("ref-1", 42L, new BigDecimal("1.00"), PaymentStatus.SUCCEEDED);

        assertThatThrownBy(() -> paymentService.handleWebhook(tampered, sign(signed, SECRET)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void thieuChuKy_401() {
        assertThatThrownBy(() -> paymentService.handleWebhook(req(PaymentStatus.SUCCEEDED), null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void prod_dungSecretDevHoacRong_failFast() {
        var prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> new PaymentService(paymentRepository, orderService, "dev-webhook-secret", prod))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PaymentService(paymentRepository, orderService, " ", prod))
                .isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> new PaymentService(paymentRepository, orderService, "real-secret", prod))
                .doesNotThrowAnyException();
    }
}
