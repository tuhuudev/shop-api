package com.learn.shopapi.service;

import com.learn.shopapi.dto.PaymentResponse;
import com.learn.shopapi.dto.PaymentWebhookRequest;
import com.learn.shopapi.entity.Payment;
import com.learn.shopapi.entity.PaymentStatus;
import com.learn.shopapi.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Xu ly callback (webhook) tu cong thanh toan - MOCK skeleton (chua noi cong that).
 *
 * - Xac thuc: chu ky HMAC-SHA256 tren chuoi chuan providerRef|orderId|amount|status voi shared secret.
 *   (Cong that thuong ky tren RAW body; o day ky tren chuoi field cho don gian, de doi sang raw sau.)
 * - Idempotency: provider_ref DUY NHAT -> callback trung tra ve ket qua cu, khong xu ly 2 lan.
 * - status=SUCCEEDED -> xac nhan don sang PAID (qua OrderService.confirmPaid, idempotent; so tien phai khop tong don).
 */
@Service
public class PaymentService {

    private static final String PROVIDER = "mock";
    private static final String DEV_SECRET = "dev-webhook-secret";

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderService orderService,
                          @Value("${app.payments.webhook-secret:" + DEV_SECRET + "}") String webhookSecret,
                          Environment env) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.webhookSecret = webhookSecret;
        // FAIL-FAST: o prod KHONG duoc dung secret dev/rong -> nguoi ngoai se gia mao duoc webhook
        // (ky giao dich gia -> mark don PAID). Bat dat APP_PAYMENTS_WEBHOOK_SECRET that.
        if (Arrays.asList(env.getActiveProfiles()).contains("prod")
                && (webhookSecret == null || webhookSecret.isBlank() || DEV_SECRET.equals(webhookSecret))) {
            throw new IllegalStateException(
                    "Prod yeu cau APP_PAYMENTS_WEBHOOK_SECRET (khong duoc rong / khong dung secret dev)");
        }
    }

    @Transactional
    public PaymentResponse handleWebhook(PaymentWebhookRequest req, String signature) {
        verifySignature(req, signature);

        // Idempotent: da nhan providerRef nay -> tra ket qua cu.
        var existing = paymentRepository.findByProviderRef(req.providerRef());
        if (existing.isPresent()) {
            return PaymentResponse.from(existing.get());
        }

        Payment payment = paymentRepository.save(
                new Payment(req.orderId(), PROVIDER, req.providerRef(), req.amount(), req.status()));

        if (req.status() == PaymentStatus.SUCCEEDED) {
            orderService.confirmPaid(req.orderId(), req.amount());
        }
        return PaymentResponse.from(payment);
    }

    /** Tinh chu ky ky vong va so sanh hang-thoi-gian (chong timing attack). 401 neu sai. */
    private void verifySignature(PaymentWebhookRequest req, String signature) {
        String canonical = req.providerRef() + "|" + req.orderId() + "|"
                + req.amount().toPlainString() + "|" + req.status().name();
        String expected = hmacSha256Hex(canonical);
        if (signature == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                          signature.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chu ky webhook khong hop le");
        }
    }

    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Khong tinh duoc HMAC", e);
        }
    }
}
