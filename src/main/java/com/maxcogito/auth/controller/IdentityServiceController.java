package com.maxcogito.auth.controller;

import com.maxcogito.auth.dto.IdentitySummaryDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityServiceController {

    /**
     * Simple "is the service alive / what can I do here" endpoint.
     * Accessible to users with ROLE_IDENTITY_SERVICE or ROLE_ADMIN.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('IDENTITY_SERVICE','ADMIN')")
    public IdentitySummaryDto summary(Authentication auth) {
        String username = auth.getName();
        List<String> roles = auth.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        String message =
                "Identity Services are available. Here you will manage identities in the " +
                        "system wallet, link identities to services, and configure assured communication.";

        return new IdentitySummaryDto(
                "Identity Services",
                username,
                roles,
                message,
                Instant.now()
        );
    }

    /**
     * Placeholder for future settings update (e.g. preferences, default identity, etc.)
     * Kept very simple for now so the UI has something to POST to later.
     */
    @PostMapping("/settings")
    @PreAuthorize("hasAnyRole('IDENTITY_SERVICE','ADMIN')")
    public IdentitySummaryDto updateSettings(Authentication auth) {
        // In the future: accept a DTO in @RequestBody and persist.
        String username = auth.getName();
        List<String> roles = auth.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        String message = "Identity settings updated successfully (placeholder).";

        return new IdentitySummaryDto(
                "Identity Services",
                username,
                roles,
                message,
                Instant.now()
        );
    }
}

