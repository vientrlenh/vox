package com.sep.vox.domain.mapper;

import java.time.Instant;
import java.util.List;

import com.sep.vox.domain.dto.SubscriptionRequestDto;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;

public final class SubscriptionRequestDtoMapper {

    private SubscriptionRequestDtoMapper() {
    }

    public static SubscriptionRequestDto toDto(SubscriptionRequest domain) {
        return new SubscriptionRequestDto(
            domain.getId(),
            domain.getSchoolId(),
            domain.getRequestType().name(),
            domain.getCurrentPlanId(),
            domain.getRequestedPlanId(),
            domain.getAmount(),
            domain.getStatus().name(),
            valueOf(domain.getSubmittedAt()),
            domain.getReviewedBy(),
            valueOf(domain.getReviewedAt())
        );
    }

    public static List<SubscriptionRequestDto> toDtoList(List<SubscriptionRequest> domains) {
        return domains.stream().map(SubscriptionRequestDtoMapper::toDto).toList();
    }

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }
}
