package com.maxcogito.auth.controller;

import com.maxcogito.auth.dto.ClientCredentialsToken;
import com.maxcogito.auth.service.AadClientCredentialsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AadTokenController {
    private final AadClientCredentialsService aad;

    public AadTokenController(AadClientCredentialsService aad) {
        this.aad = aad;
    }

    /**
     * Returns a fresh client-credentials access token from Azure AD.
     * Admin-only because this exposes a bearer token.
     *
     * Optional: override scope via query param (?scope=...).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/clientSecret")
    public ResponseEntity<ClientCredentialsToken> getClientSecretToken(
            @RequestParam(value = "scope", required = false) String scope) {
        var token = aad.getAccessToken(scope);
        return ResponseEntity.ok(token);
    }
}
