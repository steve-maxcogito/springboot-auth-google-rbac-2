package com.maxcogito.auth.repo;

import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByToken(String token);
    void deleteByUserId(UUID userId);
}
