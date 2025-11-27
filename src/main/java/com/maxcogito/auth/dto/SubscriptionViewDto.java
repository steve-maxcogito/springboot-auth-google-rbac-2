package com.maxcogito.auth.dto;

import com.maxcogito.auth.domain.SubscriptionServiceKind;
import com.maxcogito.auth.domain.SubscriptionStatus;
import com.maxcogito.auth.domain.SubscriptionTerm;

import java.time.Instant;

public class SubscriptionViewDto {

    private Long id;
    private String username;
    private SubscriptionServiceKind serviceKind;
    private SubscriptionStatus status;
    private boolean trial;
    private Instant requestedAt;
    private Instant approvedAt;

    private SubscriptionTerm term;
    private Instant validUntil;

    public SubscriptionTerm getTerm() {
        return term;
    }

    public void setTerm(SubscriptionTerm term) {
        this.term = term;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public SubscriptionServiceKind getServiceKind() {
        return serviceKind;
    }

    public void setServiceKind(SubscriptionServiceKind serviceKind) {
        this.serviceKind = serviceKind;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public boolean isTrial() {
        return trial;
    }

    public void setTrial(boolean trial) {
        this.trial = trial;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }
}
