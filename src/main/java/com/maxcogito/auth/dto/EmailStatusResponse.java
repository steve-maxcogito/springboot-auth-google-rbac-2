package com.maxcogito.auth.dto;

// src/main/java/com/maxcogito/auth/dto/EmailStatusResponse.java
public record EmailStatusResponse(
        String username,
        String email,
        boolean emailVerified
) {}

