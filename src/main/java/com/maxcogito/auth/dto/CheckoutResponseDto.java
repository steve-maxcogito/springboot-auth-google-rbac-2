// com.maxcogito.auth.dto.CheckoutResponseDto
package com.maxcogito.auth.dto;

public class CheckoutResponseDto {

    private Long paymentId;
    private String provider;
    private String checkoutUrl;

    // getters/setters


    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }
}

