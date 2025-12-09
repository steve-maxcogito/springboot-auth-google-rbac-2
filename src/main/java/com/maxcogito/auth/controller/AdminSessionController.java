// src/main/java/com/maxcogito/auth/controller/AdminSessionController.java
package com.maxcogito.auth.controller;

import com.maxcogito.auth.dto.ActiveUserSessionDto;
import com.maxcogito.auth.repo.RefreshTokenRepository;
import com.maxcogito.auth.service.AdminSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionController {

    private final RefreshTokenRepository refreshTokenRepository;

    private final AdminSessionService adminSessionService;

    public AdminSessionController(RefreshTokenRepository refreshTokenRepository,
                                  AdminSessionService adminSessionService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.adminSessionService = adminSessionService;
    }

    @GetMapping("/sessions/active/count")
    public ResponseEntity<Map<String, Long>> getActiveUserCount() {
        long count = refreshTokenRepository.countActiveUsers(Instant.now());
        return ResponseEntity.ok(Map.of("totalActiveUsers", count));
    }

    @GetMapping("/sessions/active/users")
    public ResponseEntity<List<ActiveUserSessionDto>> getActiveUsers() {
        List<ActiveUserSessionDto> sessions =
                adminSessionService.getActiveUserSessions();
        return ResponseEntity.ok(sessions);
    }

    // 🔹 NEW: logout this user everywhere (revoke all active refresh tokens)
    @PostMapping("/sessions/{userId}/revoke-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> revokeAllTokensForUser(@PathVariable UUID userId) {
        int revoked = adminSessionService.revokeAllTokensForUser(userId);
        return ResponseEntity.ok(Map.of("revoked", revoked));
    }

    @DeleteMapping("/sessions/revoked")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> deleteRevokedUserTokens() {
        int deleted = adminSessionService.purgeRevokedOrExpiredTokens();
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }

    @DeleteMapping("/sessions/revoked/tokenById")
    public ResponseEntity<Void> revokeUserTokenById(@RequestParam("id") UUID id) {
        Instant now = Instant.now();
        refreshTokenRepository.revokeAllForUser(id, now);
        refreshTokenRepository.deleteRevokedOrExpired(now);
        return ResponseEntity.ok().build();
    }
}
