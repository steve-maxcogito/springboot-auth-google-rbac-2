// src/main/java/com/maxcogito/auth/repo/RefreshTokenRepository.java
package com.maxcogito.auth.repo;

import com.maxcogito.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("delete from RefreshToken r " +
            "where r.revoked = true or r.expiresAt < :now")
    int deleteRevokedOrExpired(@Param("now") Instant now);

    @Query("select r from RefreshToken r " +
            "join fetch r.user u " +
            "where r.revoked = false and r.expiresAt > :now")
    List<RefreshToken> findActive(@Param("now") Instant now);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true, r.revokedAt = :now " +
            "where r.user.id = :userId and r.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Query("""
        select count(distinct rt.user.id)
        from RefreshToken rt
        where rt.revoked = false
          and rt.expiresAt > :now
    """)
    long countActiveUsers(@Param("now") Instant now);

    @Query("""
        select rt.user.id,
               max(coalesce(rt.lastUsedAt, rt.createdAt)),
               count(rt)
        from RefreshToken rt
        where rt.revoked = false
          and rt.expiresAt > :now
        group by rt.user.id
        order by max(coalesce(rt.lastUsedAt, rt.createdAt)) desc
    """)
    List<Object[]> findActiveUserSessionsRaw(@Param("now") Instant now);
}

