// com.maxcogito.auth.dto.MockPaymentWebhookDto
package com.maxcogito.auth.dto;

import java.math.BigDecimal;
import java.util.List;

public class MockPaymentWebhookDto {

    private String provider;
    private String sessionId;
    private String status;          // "succeeded" or "failed"
    private BigDecimal amount;
    private String currency;
    private List<Long> subscriptionIds;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<Long> getSubscriptionIds() {
        return subscriptionIds;
    }

    public void setSubscriptionIds(List<Long> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }
}

