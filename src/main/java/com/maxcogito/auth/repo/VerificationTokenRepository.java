package com.maxcogito.auth.repo;

import com.maxcogito.auth.domain.User;
import com.maxcogito.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByToken(String token);
    void deleteByToken(String token);
    void deleteByUserId(UUID userId);

    Optional<VerificationToken> findByTokenAndUserId(String token, UUID userId);

    Optional<VerificationToken> findAllByUserId(UUID userId);

    @Modifying
    @Query("delete from VerificationToken v " +
            "where v.used = true or v.expiresAt < :now")
    int deleteUsedOrExpired(@Param("now") Instant now);
}
