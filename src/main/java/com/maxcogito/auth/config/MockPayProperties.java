package com.maxcogito.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mockpay")
public class MockPayProperties {

    private String baseUrl;
    private int timeoutMs;
    private boolean enabled;

    private String returnUrlSuccess;
    private String returnUrlFailure;

    private String returnUrlCancel;

    // getters and setters


    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getReturnUrlSuccess() {
        return returnUrlSuccess;
    }

    public void setReturnUrlSuccess(String returnUrlSuccess) {
        this.returnUrlSuccess = returnUrlSuccess;
    }

    public String getReturnUrlFailure() {
        return returnUrlFailure;
    }

    public void setReturnUrlFailure(String returnUrlFailure) {
        this.returnUrlFailure = returnUrlFailure;
    }

    public String getReturnUrlCancel() {
        return returnUrlCancel;
    }

    public void setReturnUrlCancel(String returnUrlCancel) {
        this.returnUrlCancel = returnUrlCancel;
    }
}


