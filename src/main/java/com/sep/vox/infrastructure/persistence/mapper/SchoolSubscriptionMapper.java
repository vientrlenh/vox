package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionStatus;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionJpaEntity;

public final class SchoolSubscriptionMapper {

    private SchoolSubscriptionMapper() {}

    public static SchoolSubscription toDomain(SchoolSubscriptionJpaEntity jpa) {
        return new SchoolSubscription(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getSubscriptionPlanId(),
            jpa.getStartDate(),
            jpa.getEndDate(),
            fromString(jpa.getStatus()),
            jpa.getPricePaidSnapshot(),
            jpa.getCancelledAt(),
            jpa.getCreatedAt(),
            jpa.getVersion(),
            jpa.getSuspendedAt(),
            jpa.getSuspendedReason(),
            jpa.getSuspendedBy()
        );
    }

    public static SchoolSubscriptionJpaEntity toJpa(SchoolSubscription domain) {
        return new SchoolSubscriptionJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getSubscriptionPlanId(),
            domain.getStartDate(),
            domain.getEndDate(),
            valueOf(domain.getStatus()),
            domain.getPricePaidSnapshot(),
            domain.getCancelledAt(),
            domain.getCreatedAt(),
            domain.getVersion(),
            domain.getSuspendedAt(),
            domain.getSuspendedReason(),
            domain.getSuspendedBy()
        );
    }

    private static SchoolSubscriptionStatus fromString(String status) {
        if (status == null)
            return null;
        try {
            return SchoolSubscriptionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trạng thái đơn đăng ký của trường không hợp lệ: " + status);
        }
    }

    private static String valueOf(SchoolSubscriptionStatus status) {
        return status == null ? null : status.name();
    }
}
