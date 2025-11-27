package com.maxcogito.auth.dto;

import java.util.List;

/**
 * Incoming from user dashboard.
 *
 * Example:
 * {
 *   "serviceIds": ["identity", "data", "security"],
 *   "trial": true
 * }
 */

public class SubscriptionRequestDto {

    private List<String> serviceIds;
    private boolean trial;

    // NEW: optional term id (e.g. "ONE_MONTH", "FOUR_MONTHS", "ONE_YEAR")
    private String termId;

    public List<String> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public boolean isTrial() {
        return trial;
    }

    public void setTrial(boolean trial) {
        this.trial = trial;
    }

    public String getTermId() {
        return termId;
    }

    public void setTermId(String termId) {
        this.termId = termId;
    }
}


