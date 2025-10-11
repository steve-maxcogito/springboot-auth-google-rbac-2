package com.maxcogito.auth.repo;

import com.maxcogito.auth.domain.MfaChallenge;
import com.maxcogito.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MfaChallengeRepository extends JpaRepository<MfaChallenge, UUID> {
    Optional<MfaChallenge> findFirstByUserAndPurposeOrderByCreatedAtDesc(User user, String purpose);
}
