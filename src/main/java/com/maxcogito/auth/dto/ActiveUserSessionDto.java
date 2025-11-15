// src/main/java/com/maxcogito/auth/dto/ActiveUserSessionDto.java
package com.maxcogito.auth.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ActiveUserSessionDto(
        UUID userId,
        String username,
        String email,
        List<String> roles,
        Instant lastLoginAt,
        Instant lastActivity,
        long activeTokens
) {}
