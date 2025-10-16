package com.maxcogito.auth.dto;

import java.util.UUID;

public record RegisterResponse(UUID userId,
                               boolean mfaRequired,
                               boolean emailVerificationRequired,
                               String onboardingToken,          // only present if next step needed
                               long onboardingExpiresInSeconds
) {

}
