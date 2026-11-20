package com.sep.vox.domain.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;

public record SchoolSubscriptionDto(
    UUID id,
    UUID schoolId,
    UUID subscriptionPlanId,
    Instant startDate,
    Instant endDate,
    SchoolSubscriptionStatus status,
    BigDecimal pricePaidSnapshot,
    Instant cancelledAt,
    Instant createdAt,
    Instant suspendedAt,
    String suspendedReason,
    /**
     * The System Admin who ordered the suspension. It goes with the suspended_* trio so it must be
     * present here: the GraphQL schema still declares suspendedBy, and a missing component makes
     * PropertyDataFetcher return null forever -- the admin screen shows "unknown who suspended" for every
     * period, with no error surfacing.
     */
    UUID suspendedBy
) {

    public static SchoolSubscriptionDto toDto(SchoolSubscription domain) {
        return new SchoolSubscriptionDto(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionPlanId(),
            domain.getStartDate(),
            domain.getEndDate(),
            domain.getStatus(),
            domain.getPricePaidSnapshot(),
            domain.getCancelledAt(),
            domain.getCreatedAt(),
            domain.getSuspendedAt(),
            domain.getSuspendedReason(),
            domain.getSuspendedBy()
        );
    }

    public static PageResult<SchoolSubscriptionDto> toDtoPage(PageResult<SchoolSubscription> page) {
        return new PageResult<>(
            toDtoList(page.content()),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    public static List<SchoolSubscriptionDto> toDtoList(List<SchoolSubscription> domains) {
        return domains.stream()
            .map(SchoolSubscriptionDto::toDto)
            .toList();
    }
}
