package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.RefreshToken;
import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.errors.UnauthorizedException;
import com.maxcogito.auth.repo.RefreshTokenRepository;
import com.maxcogito.auth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final long ttlDays;
    private final boolean rotateOnUse;
    private final SecureRandom sr = new SecureRandom();
    private final UserRepository userRepo;
    private final Duration refreshTtl = Duration.ofDays(30);
    public RefreshTokenService(RefreshTokenRepository repo,
                               UserRepository userRepo,
                               @Value("${app.refresh.tokenTtlDays:14}") long ttlDays,
                               @Value("${app.refresh.rotateOnUse:true}") boolean rotateOnUse) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.ttlDays = ttlDays;
        this.rotateOnUse = rotateOnUse;
    }


    @Transactional
    public String createToken(User user) {
        String raw = randomOpaque();                // return this to client
        String hash = hashToken(raw);               // store this in DB

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(hash);                          // DB stores HASH, not raw
        rt.setExpiresAt(Instant.now().plus(refreshTtl));
        rt.setRevoked(false);
        // createdAt set by @PrePersist in entity
        repo.save(rt);

        return raw;
    }


    /** Validate a presented refresh token: check DB row (by HASH), not revoked/expired; stamp lastUsedAt */
    @Transactional
    public RefreshToken validate(String presentedRaw) {
        String hash = hashToken(presentedRaw);
        RefreshToken rt = repo.findByToken(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Expired or revoked refresh token");
        }

        rt.setLastUsedAt(Instant.now());
        return repo.save(rt);
    }

    public void revoke(String token) {
        repo.findByToken(token).ifPresent(rt -> { rt.setRevoked(true); repo.save(rt); });
    }


    /** Revoke all active tokens for a user (logout everywhere) */
    @Transactional
    public int revokeAllForUser(UUID userId) {
        return repo.revokeAllForUser(userId, Instant.now());
    }

    private String generate() {
        byte[] b = new byte[48];
        sr.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }


    /** Rotate: revoke old and issue a new token; return NEW raw value */
    @Transactional
    public String rotate(RefreshToken old) {
        old.setRevoked(true);
        old.setRevokedAt(Instant.now());
        repo.save(old);

        return createToken(old.getUser()); // new row saved; returns new RAW token
    }

    // -------- helpers --------

    private static String randomOpaque() {
        // 32 bytes → Base64URL without padding (~43 chars). You can also use Hex.
        byte[] b = new byte[32];
        new java.security.SecureRandom().nextBytes(b);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String hashToken(String raw) {
        // Simple SHA-256; consider Argon2id if you treat refresh tokens like passwords.
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }
}
