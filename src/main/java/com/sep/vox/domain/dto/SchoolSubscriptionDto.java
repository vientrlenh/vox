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
    String startDate,
    String endDate,
    String status,
    BigDecimal pricePaidSnapshot,
    String cancelledAt,
    String createdAt,
    String suspendedAt,
    String suspendedReason
) {

    public static SchoolSubscriptionDto toDto(SchoolSubscription domain) {
        return new SchoolSubscriptionDto(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionPlanId(),
            valueOf(domain.getStartDate()),
            valueOf(domain.getEndDate()),
            valueOf(domain.getStatus()),
            domain.getPricePaidSnapshot(),
            valueOf(domain.getCancelledAt()),
            valueOf(domain.getCreatedAt()),
            valueOf(domain.getSuspendedAt()),
            domain.getSuspendedReason()
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

    private static String valueOf(Instant value) {
        return value == null ? null : value.toString();
    }

    private static String valueOf(SchoolSubscriptionStatus status) {
        return status == null ? null : status.name();
    }
}
