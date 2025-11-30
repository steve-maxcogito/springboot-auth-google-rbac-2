package com.maxcogito.auth.service;

import com.maxcogito.auth.domain.*;
import com.maxcogito.auth.dto.SubscriptionRequestDto;
import com.maxcogito.auth.dto.SubscriptionViewDto;
import com.maxcogito.auth.mapper.SubscriptionMapper;
import com.maxcogito.auth.repo.RoleRepository;
import com.maxcogito.auth.repo.UserRepository;
import com.maxcogito.auth.repo.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final RoleRepository roleRepository;

    public SubscriptionService(UserRepository userRepository,
                               UserSubscriptionRepository subscriptionRepository,
                               RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.roleRepository = roleRepository;
    }

    // ---------------------------------------------------------------------
    // Query methods used by controllers
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SubscriptionViewDto> findAll() {
        return SubscriptionMapper.toDtoList(subscriptionRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionViewDto> findByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return SubscriptionMapper.toDtoList(subscriptionRepository.findByUserId(user.getId()));
    }

    @Transactional(readOnly = true)
    public SubscriptionViewDto getOne(Long id) {
        UserSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));
        return SubscriptionMapper.toDto(sub);
    }

    // ---------------------------------------------------------------------
    // User-facing functionality
    // ---------------------------------------------------------------------

    /**
     * User requests one or more subscription services.
     */
    public List<SubscriptionViewDto> requestSubscriptions(String username,
                                                          SubscriptionRequestDto req) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        if (req.getServiceIds() == null || req.getServiceIds().isEmpty()) {
            throw new IllegalArgumentException("At least one serviceId is required.");
        }

        // Resolve subscription term
        SubscriptionTerm term = (req.getTermId() != null && !req.getTermId().isBlank())
                ? SubscriptionTerm.fromId(req.getTermId())
                : SubscriptionTerm.ONE_MONTH;

        // Convert serviceIds → enum types
        Set<SubscriptionServiceKind> kinds = req.getServiceIds().stream()
                .map(SubscriptionServiceKind::fromId)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SubscriptionServiceKind.class)));

        Instant now = Instant.now();
        List<UserSubscription> updated = new ArrayList<>();

        for (SubscriptionServiceKind kind : kinds) {

            UserSubscription sub = subscriptionRepository
                    .findByUserIdAndServiceKind(user.getId(), kind)
                    .orElseGet(() -> {
                        UserSubscription s = new UserSubscription();
                        s.setUser(user);
                        s.setServiceKind(kind);
                        s.setRequestedAt(now);
                        return s;
                    });

            // If REJECTED / CANCELLED / PAYMENT_FAILED → reset back to REQUESTED
            if (sub.getStatus() == null
                    || sub.getStatus() == SubscriptionStatus.REJECTED
                    || sub.getStatus() == SubscriptionStatus.CANCELLED
                    || sub.getStatus() == SubscriptionStatus.PAYMENT_FAILED
                    || sub.getStatus() == SubscriptionStatus.EXPIRED) {

                sub.setStatus(SubscriptionStatus.REQUESTED);
                sub.setRequestedAt(now);
                sub.setApprovedAt(null);
                sub.setApprovedBy(null);
                sub.setValidUntil(null);
            }

            sub.setTrial(req.isTrial());
            sub.setTerm(term);     // Store term for approval step

            updated.add(subscriptionRepository.save(sub));
        }

        return SubscriptionMapper.toDtoList(updated);
    }


    /**
     * User cancels their own subscription.
     */
    public SubscriptionViewDto cancel(Long id, String username) {
        UserSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));

        if (!sub.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Subscription does not belong to user: " + username);
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setValidUntil(Instant.now());

        return SubscriptionMapper.toDto(subscriptionRepository.save(sub));
    }

    // ---------------------------------------------------------------------
    // Admin & system approval flows
    // ---------------------------------------------------------------------

    public SubscriptionViewDto approve(Long id, String adminUsername) {
        UserSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));

        // Already approved/active? Nothing to do.
        if (sub.getStatus() == SubscriptionStatus.APPROVED
                || sub.getStatus() == SubscriptionStatus.ACTIVE) {
            return SubscriptionMapper.toDto(sub);
        }

        Instant now = Instant.now();
        sub.setStatus(SubscriptionStatus.APPROVED);
        sub.setApprovedAt(now);
        sub.setApprovedBy(adminUsername);

        // Ensure a term exists
        if (sub.getTerm() == null) {
            sub.setTerm(SubscriptionTerm.ONE_MONTH);
        }

        // Compute expiration date
        int months = sub.getTerm().getMonths();
        ZonedDateTime start = now.atZone(ZoneId.systemDefault());
        ZonedDateTime end = start.plusMonths(months);
        sub.setValidUntil(end.toInstant());

        // Grant ROLE_* to the user based on serviceKind
        grantRoleForService(sub.getUser(), sub.getServiceKind());

        return SubscriptionMapper.toDto(subscriptionRepository.save(sub));
    }


    public SubscriptionViewDto reject(Long id, String adminUsername, String notes) {
        UserSubscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + id));

        sub.setStatus(SubscriptionStatus.REJECTED);
        sub.setApprovedAt(Instant.now());
        sub.setApprovedBy(adminUsername);
        sub.setValidUntil(null);
        sub.setNotes(notes);

        return SubscriptionMapper.toDto(subscriptionRepository.save(sub));
    }

    // ---------------------------------------------------------------------
    // Internal helper: Assign roles based on service kind
    // ---------------------------------------------------------------------

    protected void grantRoleForService(User user, SubscriptionServiceKind kind) {

        String roleName;
        log.info("Granting role for user: " + user.getUsername());
        log.info("roleName for user is:"+kind.toString());
        switch (kind) {
            case IDENTITY_SERVICE:
                roleName = "ROLE_IDENTITY_SERVICE";
                break;

            case USER_DATA_SERVICE:
                roleName = "ROLE_DATA_SERVICE";
                break;

            case USER_SECURITY_SERVICE:
                roleName = "ROLE_SECURITY_SERVICE";
                break;

            case USER_COMMODITY_ANALYTIC_SERVICE:
                roleName = "ROLE_COMMODITY_ANALYTIC_SERVICE";
                break;

            case USER_DATA_ANALYTIC_SERVICE:
                roleName = "ROLE_DATA_ANALYTIC_SERVICE";
                break;

            default:
                throw new IllegalArgumentException("No role mapping for service: " + kind);
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));

        if (user.getRoles().stream().noneMatch(r -> r.getName().equals(roleName))) {
            user.getRoles().add(role);
            userRepository.save(user);
        }
    }
}


