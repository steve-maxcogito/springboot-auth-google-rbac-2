package com.maxcogito.auth.repo;

import com.maxcogito.auth.domain.UserSubscription;
import com.maxcogito.auth.domain.SubscriptionServiceKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserId(UUID userId);

    Optional<UserSubscription> findByUserIdAndServiceKind(UUID userId, SubscriptionServiceKind kind);
}
