package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.SchoolSubscription;
import com.sep.vox.domain.model.subscription.SubscriptionStatus;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionJpaEntity;

public final class SchoolSubscriptionMapper {

    private SchoolSubscriptionMapper() {}

    public static SchoolSubscription toDomain(SchoolSubscriptionJpaEntity jpa) {
        return new SchoolSubscription(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getPlanId(),
            jpa.getStartDate(),
            jpa.getEndDate(),
            SubscriptionStatus.valueOf(jpa.getStatus()),
            jpa.getPricePaidSnapshot(),
            jpa.getCancelledAt(),
            jpa.getCreatedAt(),
            jpa.getVersion()
        );
    }

    public static SchoolSubscriptionJpaEntity toJpa(SchoolSubscription domain) {
        return new SchoolSubscriptionJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getPlanId(),
            domain.getStartDate(),
            domain.getEndDate(),
            domain.getStatus().name(),
            domain.getPricePaidSnapshot(),
            domain.getCancelledAt(),
            domain.getCreatedAt(),
            domain.getVersion()
        );
    }
}
