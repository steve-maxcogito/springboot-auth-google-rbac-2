package com.maxcogito.auth.ms;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class AadTokenProvider {

    @Value("${msgraph.tenantId}")     private String tenantId;
    @Value("${msgraph.clientId}")     private String clientId;
    @Value("${msgraph.clientSecret}") private String clientSecret;
    @Value("${msgraph.oauthScope:https://graph.microsoft.com/.default}")
    private String scope;

    private final RestTemplate rest = new RestTemplate();

    private final AtomicReference<Cached> cache = new AtomicReference<>();

    public String getBearer() {
        var c = cache.get();
        long now = System.currentTimeMillis();
        if (c != null && now < (c.expiresAtMs - 60_000)) {
            return c.accessToken;
        }
        return refresh();
    }

    public synchronized String refresh() {
        // double-check inside the lock
        var c = cache.get();
        long now = System.currentTimeMillis();
        if (c != null && now < (c.expiresAtMs - 60_000)) {
            return c.accessToken;
        }

        String url = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String,String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", scope);
        form.add("grant_type", "client_credentials");

        try {
            var resp = rest.postForEntity(url, new HttpEntity<>(form, h), TokenResponse.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("AAD token request failed: " + resp.getStatusCode());
            }
            var body = resp.getBody();
            long exp = Instant.now().toEpochMilli() + body.expiresIn * 1000L;
            var fresh = new Cached(body.accessToken, exp);
            cache.set(fresh);
            return fresh.accessToken;
        } catch (RestClientException e) {
            throw new IllegalStateException("AAD token request error", e);
        }
    }

    private record Cached(String accessToken, long expiresAtMs) {}

    static class TokenResponse {
        @JsonProperty("access_token") String accessToken;
        @JsonProperty("expires_in")   long   expiresIn;
        @JsonProperty("token_type")   String tokenType;
        @JsonProperty("ext_expires_in") long extExpiresIn;
    }

}
