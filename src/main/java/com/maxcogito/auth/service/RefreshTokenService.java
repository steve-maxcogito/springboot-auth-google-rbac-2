package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.RefreshToken;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.repo.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final long ttlDays;
    private final boolean rotateOnUse;
    private final SecureRandom sr = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${app.refresh.tokenTtlDays:14}") long ttlDays,
                               @Value("${app.refresh.rotateOnUse:true}") boolean rotateOnUse) {
        this.repo = repo;
        this.ttlDays = ttlDays;
        this.rotateOnUse = rotateOnUse;
    }

    public String createToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(generate());
        rt.setExpiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS));
        repo.save(rt);
        return rt.getToken();
    }

    @Transactional
    public RefreshToken validate(String token) {
        var rt = repo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }
        if (rotateOnUse) {
            rt.setRevoked(true);
            repo.save(rt);
        }
        return rt;
    }

    public void revoke(String token) {
        repo.findByToken(token).ifPresent(rt -> { rt.setRevoked(true); repo.save(rt); });
    }

    private String generate() {
        byte[] b = new byte[48];
        sr.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}
