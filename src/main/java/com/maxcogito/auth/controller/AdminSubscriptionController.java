package com.maxcogito.auth.controller;

import com.maxcogito.auth.domain.SubscriptionStatus;
import com.maxcogito.auth.dto.SubscriptionViewDto;
import com.maxcogito.auth.service.SubscriptionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@PreAuthorize("hasRole('ADMIN') or hasAuthority('ROLE_SECURITY_SERVICE')")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    public AdminSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * List all subscriptions, optionally filtered by status:
     *   GET /api/v1/admin/subscriptions
     *   GET /api/v1/admin/subscriptions?status=REQUESTED
     */
    @GetMapping
    public List<SubscriptionViewDto> all(
            @RequestParam(name = "status", required = false) String statusStr) {

        List<SubscriptionViewDto> all = subscriptionService.findAll();

        if (statusStr == null || statusStr.isBlank()) {
            return all;
        }

        SubscriptionStatus status = SubscriptionStatus.valueOf(statusStr);
        return all.stream()
                .filter(dto -> dto.getStatus() == status)
                .toList();
    }

    /**
     * Approve a subscription (admin or security service).
     */
    @PostMapping("/{id}/approve")
    public SubscriptionViewDto approve(@PathVariable Long id, Authentication auth) {
        String adminUsername = auth.getName();
        return subscriptionService.approve(id, adminUsername);
    }

    /**
     * Reject a subscription with optional notes.
     * The client can send a plain-text body with a note.
     */
    @PostMapping("/{id}/reject")
    public SubscriptionViewDto reject(@PathVariable Long id,
                                      Authentication auth,
                                      @RequestBody(required = false) String notes) {
        String adminUsername = auth.getName();
        return subscriptionService.reject(id, adminUsername, notes);
    }
}

