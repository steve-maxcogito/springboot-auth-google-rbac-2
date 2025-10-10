package com.maxcogito.auth.service;

import com.maxcogito.auth.dto.ClientCredentialsToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AadClientCredentialsService {
    @Value("${msgraph.tenantId}")
    private String tenantId;

    @Value("${msgraph.clientId}")
    private String clientId;

    @Value("${msgraph.clientSecret}")
    private String clientSecret;

    @Value("${msgraph.oauthScope:https://graph.microsoft.com/.default}")
    private String defaultScope;

    private final RestTemplate rest = new RestTemplate();

    // Simple in-memory cache of the last token
    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public ClientCredentialsToken getAccessToken(String scopeOverride) {
        String scope = (scopeOverride != null && !scopeOverride.isBlank())
                ? scopeOverride
                : defaultScope;

        var cached = cache.get();
        long nowMs = System.currentTimeMillis();

        if (cached != null
                && cached.scope.equals(scope)
                && nowMs < (cached.expiresAtMs - 60_000)) { // return cached until 60s before expiry
            return cached.token;
        }

        // Build request
        String url = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);
        form.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String,String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<ClientCredentialsToken> resp =
                    rest.postForEntity(url, req, ClientCredentialsToken.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                var token = resp.getBody();
                long expiresAt = Instant.now().toEpochMilli() + (token.getExpires_in() * 1000L);
                cache.set(new CachedToken(scope, token, expiresAt));
                return token;
            }
            throw new IllegalStateException("AAD token request failed: " + resp.getStatusCode());
        } catch (RestClientException ex) {
            throw new IllegalStateException("AAD token request error", ex);
        }
    }

    private record CachedToken(String scope, ClientCredentialsToken token, long expiresAtMs) {}

}
