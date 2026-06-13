package com.learn.shopapi.controller;

import com.learn.shopapi.dto.PaymentResponse;
import com.learn.shopapi.dto.PaymentWebhookRequest;
import com.learn.shopapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook cong thanh toan (MOCK). KHONG yeu cau JWT - xac thuc bang chu ky HMAC (header X-Signature).
 * Da permitAll trong SecurityConfig cho POST /api/payments/webhook.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Webhook cong thanh toan (mock)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/webhook")
    @Operation(summary = "Nhan callback cong thanh toan (idempotent theo providerRef, xac thuc HMAC)")
    public PaymentResponse webhook(@Valid @RequestBody PaymentWebhookRequest request,
                                   @RequestHeader(value = "X-Signature", required = false) String signature) {
        return paymentService.handleWebhook(request, signature);
    }
}
