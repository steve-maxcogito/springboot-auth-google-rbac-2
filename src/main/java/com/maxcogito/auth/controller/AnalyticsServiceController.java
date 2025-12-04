package com.maxcogito.auth.controller;

import com.maxcogito.auth.dto.AnalyticsSummaryDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsServiceController {

    /**
     * Simple summary endpoint for Analytics services.
     * Accessible if the user has any of:
     *   ROLE_DATA_ANALYTIC_SERVICE, ROLE_COMMODITY_ANALYTIC_SERVICE, ROLE_ADMIN
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('DATA_ANALYTIC_SERVICE','COMMODITY_ANALYTIC_SERVICE','ADMIN')")
    public AnalyticsSummaryDto summary(Authentication auth) {
        String username = auth.getName();
        List<String> roles = auth.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        // Placeholder modules: you can tweak this later
        List<String> enabled = Arrays.asList(
                "DATA_ANALYTICS_DASHBOARDS",
                "COMMODITY_ANALYTICS_DASHBOARDS"
        );

        String message =
                "Analytics services are available. Here you will upload/ingest data, " +
                        "run analytics jobs, and view dashboards.";

        return new AnalyticsSummaryDto(
                "Data & Commodity Analytics",
                username,
                roles,
                enabled,
                message,
                Instant.now()
        );
    }

    /**
     * Placeholder for future analytics job submission, configuration, etc.
     */
    @PostMapping("/settings")
    @PreAuthorize("hasAnyRole('DATA_ANALYTIC_SERVICE','COMMODITY_ANALYTIC_SERVICE','ADMIN')")
    public AnalyticsSummaryDto updateSettings(Authentication auth) {
        String username = auth.getName();
        List<String> roles = auth.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        List<String> enabled = Arrays.asList(
                "DATA_ANALYTICS_DASHBOARDS",
                "COMMODITY_ANALYTICS_DASHBOARDS"
        );

        String message = "Analytics settings updated successfully (placeholder).";

        return new AnalyticsSummaryDto(
                "Data & Commodity Analytics",
                username,
                roles,
                enabled,
                message,
                Instant.now()
        );
    }
}

