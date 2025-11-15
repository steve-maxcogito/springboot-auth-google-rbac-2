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

@RestController
@RequestMapping("/api/v1/admin/sessions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionController {

    private final RefreshTokenRepository refreshTokenRepository;

    private final AdminSessionService adminSessionService;

    public AdminSessionController(RefreshTokenRepository refreshTokenRepository,
                                  AdminSessionService adminSessionService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.adminSessionService = adminSessionService;
    }

    @GetMapping("/active/count")
    public ResponseEntity<Map<String, Long>> getActiveUserCount() {
        long count = refreshTokenRepository.countActiveUsers(Instant.now());
        return ResponseEntity.ok(Map.of("totalActiveUsers", count));
    }

    @GetMapping("/active/users")
    public ResponseEntity<List<ActiveUserSessionDto>> getActiveUsers() {
        List<ActiveUserSessionDto> sessions =
                adminSessionService.getActiveUserSessions();
        return ResponseEntity.ok(sessions);
    }
}
