// com.maxcogito.auth.dto.QuoteRequestDto
package com.maxcogito.auth.dto;

import java.util.List;

public class QuoteRequestDto {

    private List<Long> subscriptionIds;
    private String currency;

    public List<Long> getSubscriptionIds() {
        return subscriptionIds;
    }

    public void setSubscriptionIds(List<Long> subscriptionIds) {
        this.subscriptionIds = subscriptionIds;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

