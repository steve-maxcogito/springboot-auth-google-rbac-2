package com.maxcogito.auth.domain;

public enum SubscriptionServiceKind {
    IDENTITY_SERVICE,
    USER_DATA_SERVICE,
    USER_SECURITY_SERVICE,
    USER_COMMODITY_ANALYTIC_SERVICE,
    USER_DATA_ANALYTIC_SERVICE;

    // Helpful to map from UI strings
    public static SubscriptionServiceKind fromId(String id) {
        switch (id) {
            case "identity": return IDENTITY_SERVICE;
            case "data": return USER_DATA_SERVICE;
            case "security": return USER_SECURITY_SERVICE;
            case "commodity": return USER_COMMODITY_ANALYTIC_SERVICE;
            case "analytics": return USER_DATA_ANALYTIC_SERVICE;
            default:
                throw new IllegalArgumentException("Unknown subscription id: " + id);
        }
    }
}

