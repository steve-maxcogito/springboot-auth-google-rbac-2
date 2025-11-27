package com.maxcogito.auth.mapper;

import com.maxcogito.auth.domain.UserSubscription;
import com.maxcogito.auth.dto.SubscriptionViewDto;

import java.util.List;
import java.util.stream.Collectors;

public final class SubscriptionMapper {

    private SubscriptionMapper() {
        // Utility class; no instantiation
    }

    public static SubscriptionViewDto toDto(UserSubscription sub) {
        if (sub == null) {
            return null;
        }

        SubscriptionViewDto dto = new SubscriptionViewDto();

        dto.setId(sub.getId());
        dto.setUsername(sub.getUser() != null ? sub.getUser().getUsername() : null);
        dto.setServiceKind(sub.getServiceKind());
        dto.setStatus(sub.getStatus());
        dto.setTrial(sub.isTrial());

        dto.setRequestedAt(sub.getRequestedAt());
        dto.setApprovedAt(sub.getApprovedAt());

        dto.setTerm(sub.getTerm());
        dto.setValidUntil(sub.getValidUntil());

        return dto;
    }

    public static List<SubscriptionViewDto> toDtoList(List<UserSubscription> subs) {
        return subs.stream()
                .map(SubscriptionMapper::toDto)
                .collect(Collectors.toList());
    }
}
