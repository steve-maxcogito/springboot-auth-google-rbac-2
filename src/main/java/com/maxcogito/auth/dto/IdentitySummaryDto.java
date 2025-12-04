package com.maxcogito.auth.dto;

import java.time.Instant;
import java.util.List;

public class IdentitySummaryDto {
    private String serviceName;
    private String username;
    private List<String> roles;
    private String message;
    private Instant lastUpdated;

    public IdentitySummaryDto() {
    }

    public IdentitySummaryDto(String serviceName, String username, List<String> roles,
                              String message, Instant lastUpdated) {
        this.serviceName = serviceName;
        this.username = username;
        this.roles = roles;
        this.message = message;
        this.lastUpdated = lastUpdated;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
