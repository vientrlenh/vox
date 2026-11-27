package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionJpaEntity;

public final class SchoolSubscriptionMapper {

    private SchoolSubscriptionMapper() {}

    public static SchoolSubscription toDomain(SchoolSubscriptionJpaEntity jpa) {
        return SchoolSubscription.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .subscriptionPlanId(jpa.getSubscriptionPlanId())
            .startDate(jpa.getStartDate())
            .endDate(jpa.getEndDate())
            .status(fromString(jpa.getStatus()))
            .pricePaidSnapshot(jpa.getPricePaidSnapshot())
            .cancelledAt(jpa.getCancelledAt())
            .createdAt(jpa.getCreatedAt())
            .version(jpa.getVersion())
            .suspendedAt(jpa.getSuspendedAt())
            .suspendedReason(jpa.getSuspendedReason())
            .suspendedBy(jpa.getSuspendedBy())
            .build();
    }

    public static SchoolSubscriptionJpaEntity toJpa(SchoolSubscription domain) {
        return SchoolSubscriptionJpaEntity.builder()
            .id(domain.getId())
            .schoolId(domain.getSchoolId())
            .subscriptionPlanId(domain.getSubscriptionPlanId())
            .startDate(domain.getStartDate())
            .endDate(domain.getEndDate())
            .status(valueOf(domain.getStatus()))
            .pricePaidSnapshot(domain.getPricePaidSnapshot())
            .cancelledAt(domain.getCancelledAt())
            .createdAt(domain.getCreatedAt())
            .version(domain.getVersion())
            .suspendedAt(domain.getSuspendedAt())
            .suspendedReason(domain.getSuspendedReason())
            .suspendedBy(domain.getSuspendedBy())
            .build();
    }

    private static SchoolSubscriptionStatus fromString(String status) {
        if (status == null)
            return null;
        try {
            return SchoolSubscriptionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid school subscription status: " + status);
        }
    }

    private static String valueOf(SchoolSubscriptionStatus status) {
        return status == null ? null : status.name();
    }
}
