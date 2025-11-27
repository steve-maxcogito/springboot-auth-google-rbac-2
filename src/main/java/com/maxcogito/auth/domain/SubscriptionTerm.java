package com.maxcogito.auth.domain;

public enum SubscriptionTerm {

    ONE_MONTH("1m", 1),
    THREE_MONTHS("3m", 3),
    FOUR_MONTHS("4m", 4),
    SIX_MONTHS("6m", 6),
    TWELVE_MONTHS("12m", 12);

    /**
     * A short “external id” that you can safely expose to the UI.
     * Example: "1m", "3m", "12m"
     */
    private final String id;

    /**
     * Actual duration of the subscription in months.
     */
    private final int months;

    SubscriptionTerm(String id, int months) {
        this.id = id;
        this.months = months;
    }

    public String id() {
        return id;
    }

    public int getMonths() {
        return months;
    }

    /**
     * Convert UI-supplied identifiers to canonical SubscriptionTerm.
     * Supports ALL aliases you listed.
     */
    public static SubscriptionTerm fromId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Subscription term id cannot be null");
        }

        switch (id) {

            // ONE_MONTH
            case "ONE_MONTH":
            case "1m":
                return ONE_MONTH;

            // THREE_MONTHS (quarter)
            case "THREE_MONTHS":
            case "3m":
            case "QUARTER":
                return THREE_MONTHS;

            // FOUR_MONTHS
            case "FOUR_MONTHS":
            case "4m":
                return FOUR_MONTHS;

            // SIX_MONTHS (half-year)
            case "SIX_MONTHS":
            case "6m":
            case "HALF_YEAR":
                return SIX_MONTHS;

            // TWELVE_MONTHS (one year)
            case "TWELVE_MONTHS":
            case "12m":
            case "ONE_YEAR":
            case "1y":
                return TWELVE_MONTHS;

            default:
                throw new IllegalArgumentException("Unknown subscription term: " + id);
        }
    }
}

