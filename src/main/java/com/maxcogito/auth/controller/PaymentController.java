// com.maxcogito.auth.controller.PaymentController
package com.maxcogito.auth.controller;

import com.maxcogito.auth.dto.*;
import com.maxcogito.auth.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Authenticated user receives a quote for a set of subscriptions.
     */
    @PostMapping("/quote")
    @PreAuthorize("isAuthenticated()")
    public SubscriptionQuoteDto quote(Authentication auth,
                                      @RequestBody QuoteRequestDto req) {
        // (We could verify that the subscriptions belong to this user; your PaymentService
        // can also enforce that.)
        return paymentService.quote(req.getSubscriptionIds(), req.getCurrency());
    }

    /**
     * Authenticated user starts checkout for one or more subscriptions.
     * Redirect URL is returned to the frontend.
     */
    @PostMapping("/checkout/multi")
    @PreAuthorize("isAuthenticated()")
    public CheckoutResponseDto checkout(Authentication auth,
                                        @RequestBody CheckoutRequestDto req) {
        String username = auth.getName();
        return paymentService.startCheckout(username, req);
    }

    /**
     * Webhook endpoint called by mock-payment-gateway.
     * This is open (no JWT required) per SecurityConfig.
     */
    @PostMapping("/webhook/mock")
    public ResponseEntity<Void> mockWebhook(@RequestBody MockPaymentWebhookDto payload) {
        boolean success = "succeeded".equalsIgnoreCase(payload.getStatus());
        paymentService.handleMockWebhook(
                payload.getSessionId(),
                success,
                payload.getSubscriptionIds()
        );
        return ResponseEntity.ok().build();
    }
}

