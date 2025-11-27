package com.maxcogito.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_subscription")
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -------------------------
    // User relationship
    // -------------------------
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // -------------------------
    // Subscription metadata
    // -------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "service_kind", nullable = false)
    private SubscriptionServiceKind serviceKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.REQUESTED;

    @Column(name = "trial", nullable = false)
    private boolean trial = false;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private String approvedBy; // may be "SYSTEM_PAYMENT" or an admin username

    @Column(name = "valid_until")
    private Instant validUntil;

    // -------------------------
    // SubscriptionTerm integration
    // -------------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "term", nullable = true)
    private SubscriptionTerm term;  // ONE_MONTH, FOUR_MONTHS, ONE_YEAR, etc.

    // Optional administrative notes
    @Column(name = "notes", length = 1000)
    private String notes;

    // -------------------------
    // Getters / Setters
    // -------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public SubscriptionTerm getTerm() {
        return term;
    }

    public void setTerm(SubscriptionTerm term) {
        this.term = term;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

