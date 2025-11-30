package com.maxcogito.auth.service;

import com.maxcogito.auth.config.MockPayProperties;
import com.maxcogito.auth.domain.PaymentTransaction;
import com.maxcogito.auth.domain.SubscriptionServiceKind;
import com.maxcogito.auth.domain.SubscriptionStatus;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.domain.UserSubscription;
import com.maxcogito.auth.dto.CheckoutRequestDto;
import com.maxcogito.auth.dto.CheckoutResponseDto;
import com.maxcogito.auth.dto.SubscriptionQuoteDto;
import com.maxcogito.auth.dto.SubscriptionQuoteItemDto;
import com.maxcogito.auth.repo.PaymentTransactionRepository;
import com.maxcogito.auth.repo.UserRepository;
import com.maxcogito.auth.repo.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class PaymentService {

    private static final String PAYMENT_SYSTEM_USERNAME = "PAYMENT_SYSTEM";
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final MockPayProperties props;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final PaymentTransactionRepository paymentRepository;
    private final SubscriptionService subscriptionService;

    private final RestTemplate restTemplate = new RestTemplate();

    public PaymentService(MockPayProperties props,
                          UserRepository userRepository,
                          UserSubscriptionRepository subscriptionRepository,
                          PaymentTransactionRepository paymentRepository,
                          SubscriptionService subscriptionService) {
        this.props = props;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionService = subscriptionService;
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

        String gateway = props.getBaseUrl();
        if (gateway == null || gateway.isBlank()) {
            throw new IllegalStateException("mockpay.base-url is not configured");
        }

        // Normalize base URL (avoid double slashes)
        gateway = gateway.replaceAll("/+$", "");

        String returnUrl = props.getReturnUrlSuccess();
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = "http://localhost:5173/subscriptions/success";
        }

        String cancelUrl = props.getReturnUrlCancel();
        if (cancelUrl == null || cancelUrl.isBlank()) {
            cancelUrl = "http://localhost:5173/subscriptions/cancel";
        }

        // Compute quote & total
        SubscriptionQuoteDto quote = quote(req.getSubscriptionIds(), currency);
        BigDecimal totalAmount = BigDecimal.valueOf(quote.getTotalAmount());

        // Persist payment transaction
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUser(user);
        tx.setSubscriptionIds(new ArrayList<>(req.getSubscriptionIds()));
        tx.setProvider("MOCK_STRIPE");
        tx.setAmount(totalAmount);
        tx.setCurrency(currency);
        tx.setStatus("CREATED");
        tx.setCreatedAt(Instant.now());

        tx = paymentRepository.save(tx);

        // ---------- Call mock-payment-gateway: JSON body + Content-Type ----------
        Map<String, Object> body = new HashMap<>();
        body.put("amount", totalAmount);
        body.put("currency", currency);
        body.put("description", "MaxCogito subscriptions for " + username);
        body.put("subscriptionIds", req.getSubscriptionIds());
        body.put("returnUrl", returnUrl);
        body.put("cancelUrl", cancelUrl);

        log.info("Inside startCheckout with body {}", body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.info("Inside startCheckout with headers {}", headers);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response;
        try {
            log.info("Inside startCheckout with entity {}", entity);
            log.info("Inside startCheckout with url {}", gateway+"/mockpay/api/v1/sessions");
            response = restTemplate.postForObject(
                    gateway + "/mockpay/api/v1/sessions",
                    entity,
                    Map.class
            );
        } catch (HttpClientErrorException e) {
            // Helpful when debugging 415/400/etc
            String msg = "MockPay /sessions call failed: " + e.getStatusCode() +
                    " body=" + e.getResponseBodyAsString();
            throw new IllegalStateException(msg, e);
        }

        if (response == null) {
            throw new IllegalStateException("MockPay /sessions returned null response");
        }

        String sessionId = (String) response.get("sessionId");
        String checkoutPath = (String) response.get("checkoutUrl");
        if (sessionId == null || checkoutPath == null) {
            throw new IllegalStateException(
                    "MockPay /sessions response missing sessionId or checkoutUrl: " + response
            );
        }

        String checkoutUrl = gateway + checkoutPath;

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
     * /api/v1/payments/webhook/mock  → PaymentController.mockWebhook(...)
     */
    public void handleMockWebhook(String sessionId, boolean success, List<Long> subscriptionIds) {
        // Find transaction by providerId
        PaymentTransaction tx = paymentRepository.findByProviderId(sessionId)
                .orElse(null);

        if (tx == null) {
            return;
        }

        tx.setStatus(success ? "SUCCEEDED" : "FAILED");
        tx.setUpdatedAt(Instant.now());
        paymentRepository.save(tx);

        // Subscription IDs from payload (preferred), otherwise from the transaction
        List<Long> targetIds = (subscriptionIds != null && !subscriptionIds.isEmpty())
                ? subscriptionIds
                : tx.getSubscriptionIds();

        if (targetIds == null || targetIds.isEmpty()) {
            return;
        }

        if (success) {
            // Auto-approve via SubscriptionService (also grants roles + sets validUntil)
            for (Long id : targetIds) {
                try {
                    subscriptionService.approve(id, PAYMENT_SYSTEM_USERNAME);
                } catch (Exception ex) {
                    // For now, log/ignore; you could add more robust failure handling.
                    ex.printStackTrace();
                }
            }
        } else {
            // Mark subscriptions as payment failed
            subscriptionRepository.findAllById(targetIds).forEach(sub -> {
                sub.setStatus(SubscriptionStatus.PAYMENT_FAILED);
                sub.setValidUntil(Instant.now());
                subscriptionRepository.save(sub);
            });
        }
    }
}



