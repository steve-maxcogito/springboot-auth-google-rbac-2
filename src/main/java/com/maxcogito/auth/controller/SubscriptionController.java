package com.maxcogito.auth.controller;

import com.maxcogito.auth.dto.SubscriptionRequestDto;
import com.maxcogito.auth.dto.SubscriptionViewDto;
import com.maxcogito.auth.service.SubscriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * User posts from the "Subscribe to Services" card.
     */
    @PostMapping("/request")
    public List<SubscriptionViewDto> requestSubscriptions(
            Authentication auth,
            @RequestBody SubscriptionRequestDto req
    ) {
        String username = auth.getName();
        return subscriptionService.requestSubscriptions(username, req);
    }

    /**
     * Current user's subscriptions.
     */
    @GetMapping("/me")
    public List<SubscriptionViewDto> mySubscriptions(Authentication auth) {
        String username = auth.getName();
        return subscriptionService.findByUser(username);
    }

    /**
     * User cancels one of their own subscriptions.
     */
    @PostMapping("/{id}/cancel")
    public SubscriptionViewDto cancel(Authentication auth, @PathVariable Long id) {
        String username = auth.getName();
        return subscriptionService.cancel(id, username);
    }
}

