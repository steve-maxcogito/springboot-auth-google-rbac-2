
// src/main/java/com/maxcogito/auth/service/AdminSessionService.java
package com.maxcogito.auth.service;

import com.maxcogito.auth.dto.ActiveUserSessionDto;
import com.maxcogito.auth.repo.RefreshTokenRepository;
import com.maxcogito.auth.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminSessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public AdminSessionService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public long countActiveUsers() {
        return refreshTokenRepository.countActiveUsers(Instant.now());
    }

    public List<ActiveUserSessionDto> getActiveUserSessions() {
        var rows = refreshTokenRepository.findActiveUserSessionsRaw(Instant.now());
        List<ActiveUserSessionDto> result = new ArrayList<>();

        for (Object[] row : rows) {
            UUID userId = (UUID) row[0];
            Instant lastActivity = (Instant) row[1];
            long activeTokens = ((Number) row[2]).longValue();

            var userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) continue;

            var user = userOpt.get();
            String username = user.getUsername();
            String email = user.getEmail();
            var lastLoginAt = user.getLastLoginAt(); // may be null
            var roles = user.getRoles().stream()
                    .map(r -> r.getName())
                    .sorted()
                    .toList();

            result.add(new ActiveUserSessionDto(
                    userId,
                    username,
                    email,
                    roles,
                    lastLoginAt,
                    lastActivity,
                    activeTokens
            ));
        }

        return result;
    }
}
