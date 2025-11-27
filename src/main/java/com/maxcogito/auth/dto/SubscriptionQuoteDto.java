// com.maxcogito.auth.dto.SubscriptionQuoteDto
package com.maxcogito.auth.dto;

import java.util.List;

public class SubscriptionQuoteDto {

    private List<SubscriptionQuoteItemDto> items;
    private double totalAmount;
    private String currency;

    // getters/setters


    public List<SubscriptionQuoteItemDto> getItems() {
        return items;
    }

    public void setItems(List<SubscriptionQuoteItemDto> items) {
        this.items = items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}

