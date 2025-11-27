// com.maxcogito.auth.dto.CheckoutRequestDto
package com.maxcogito.auth.dto;

import java.util.List;

public class CheckoutRequestDto {

    private List<Long> subscriptionIds;
    private String provider; // e.g. "MOCK_STRIPE"
    private String currency;

    // getters/setters


    public List<Long> getSubscriptionIds() {
        return subscriptionIds;
    }

    public void setSubscriptionIds(List<Long> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

