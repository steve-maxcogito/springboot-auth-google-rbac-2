
package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.PaymentTransaction;
import com.maxcogito.auth.domain.SubscriptionServiceKind;
import com.maxcogito.auth.domain.SubscriptionStatus;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.dto.*;
        import com.maxcogito.auth.repo.PaymentTransactionRepository;
import com.maxcogito.auth.repo.UserRepository;
import com.maxcogito.auth.repo.UserSubscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
        import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${mockpay.base-url:http://localhost:9090}")
    private String mockPayBaseUrl;

    public PaymentService(UserRepository userRepository,
                          UserSubscriptionRepository subscriptionRepository,
                          PaymentTransactionRepository paymentRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Very simple pricing: you can replace with real logic.
     */
    private BigDecimal priceFor(SubscriptionServiceKind kind) {
        switch (kind) {
            case IDENTITY_SERVICE:
                return BigDecimal.valueOf(5.00);
            case USER_DATA_SERVICE:
                return BigDecimal.valueOf(10.00);
            case USER_SECURITY_SERVICE:
                return BigDecimal.valueOf(15.00);
            case USER_COMMODITY_ANALYTIC_SERVICE:
                return BigDecimal.valueOf(12.00);
            case USER_DATA_ANALYTIC_SERVICE:
                return BigDecimal.valueOf(20.00);
            default:
                return BigDecimal.valueOf(10.00);
        }
    }

    public SubscriptionQuoteDto quote(List<Long> subscriptionIds, String currency) {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }

        var subs = subscriptionRepository.findAllById(subscriptionIds);
        List<SubscriptionQuoteItemDto> items = new ArrayList<>();
        double total = 0.0;

        for (var sub : subs) {
            var item = new SubscriptionQuoteItemDto();
            item.setSubscriptionId(sub.getId());
            item.setServiceKind(sub.getServiceKind().name());
            item.setTerm(sub.getTerm() != null ? sub.getTerm().name() : null);
            item.setCurrency(currency);

            double amount = priceFor(sub.getServiceKind()).doubleValue();
            item.setAmount(amount);
            total += amount;

            String label;
            switch (sub.getServiceKind()) {
                case IDENTITY_SERVICE:
                    label = "User Identity Services";
                    break;
                case USER_DATA_SERVICE:
                    label = "User Data Services";
                    break;
                case USER_SECURITY_SERVICE:
                    label = "User Security Services";
                    break;
                case USER_COMMODITY_ANALYTIC_SERVICE:
                    label = "User Commodity Analytic Services";
                    break;
                case USER_DATA_ANALYTIC_SERVICE:
                    label = "User Data Analytic Services";
                    break;
                default:
                    label = sub.getServiceKind().name();
            }
            item.setLabel(label);

            items.add(item);
        }

        SubscriptionQuoteDto dto = new SubscriptionQuoteDto();
        dto.setItems(items);
        dto.setTotalAmount(total);
        dto.setCurrency(currency);
        return dto;
    }

    public CheckoutResponseDto startCheckout(String username, CheckoutRequestDto req) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        String currency = (req.getCurrency() == null || req.getCurrency().isBlank())
                ? "USD"
                : req.getCurrency();

        SubscriptionQuoteDto quote = quote(req.getSubscriptionIds(), currency);
        BigDecimal totalAmount = BigDecimal.valueOf(quote.getTotalAmount());

        // Create payment transaction
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUser(user);
        tx.setSubscriptionIds(new ArrayList<>(req.getSubscriptionIds()));
        tx.setProvider("MOCK_STRIPE");
        tx.setAmount(totalAmount);
        tx.setCurrency(currency);
        tx.setStatus("CREATED");
        tx.setCreatedAt(Instant.now());

        tx = paymentRepository.save(tx);

        // Call mock-payment-gateway to create session
        Map<String, Object> body = new HashMap<>();
        body.put("amount", totalAmount);
        body.put("currency", currency);
        body.put("description", "MaxCogito subscriptions for " + username);
        body.put("subscriptionIds", req.getSubscriptionIds());
        body.put("returnUrl", "http://localhost:5173/subscriptions/success");
        body.put("cancelUrl", "http://localhost:5173/subscriptions/cancel");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(
                mockPayBaseUrl + "/mockpay/api/v1/sessions",
                body,
                Map.class
        );

        String sessionId = (String) response.get("sessionId");
        String checkoutPath = (String) response.get("checkoutUrl");
        String checkoutUrl = mockPayBaseUrl + checkoutPath;

        tx.setProviderId(sessionId);
        tx.setUpdatedAt(Instant.now());
        paymentRepository.save(tx);

        CheckoutResponseDto dto = new CheckoutResponseDto();
        dto.setPaymentId(tx.getId());
        dto.setProvider("MOCK_STRIPE");
        dto.setCheckoutUrl(checkoutUrl);

        return dto;
    }

    /**
     * Called by webhook when mock gateway reports success/failure.
     * You already wired mock gateway to call /api/v1/payments/webhook/mock
     * with { sessionId, status, subscriptionIds, ... }.
     */
    public void handleMockWebhook(String sessionId, boolean success, List<Long> subscriptionIds) {
        // Find transaction by providerId
        PaymentTransaction tx = paymentRepository.findAll().stream()
                .filter(t -> sessionId.equals(t.getProviderId()))
                .findFirst()
                .orElse(null);

        if (tx == null) {
            return;
        }

        tx.setStatus(success ? "SUCCEEDED" : "FAILED");
        tx.setUpdatedAt(Instant.now());
        paymentRepository.save(tx);

        if (success) {
            // Mark subscriptions as approved via SubscriptionService
            // (if you inject SubscriptionService here, call approve() for each)
            // or have the webhook controller call subscriptionService.approve()
        }
    }
}

