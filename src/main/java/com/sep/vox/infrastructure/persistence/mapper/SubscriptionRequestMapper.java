package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.RequestStatus;
import com.sep.vox.domain.model.subscription.RequestType;
import com.sep.vox.domain.model.subscription.SubscriptionRequest;
import com.sep.vox.infrastructure.persistence.entity.SubscriptionRequestJpaEntity;

public final class SubscriptionRequestMapper {

    private SubscriptionRequestMapper() {}

    public static SubscriptionRequest toDomain(SubscriptionRequestJpaEntity jpa) {
        return new SubscriptionRequest(
            jpa.getId(),
            jpa.getSchoolId(),
            RequestType.valueOf(jpa.getRequestType()),
            jpa.getCurrentPlanId(),
            jpa.getRequestedPlanId(),
            jpa.getAmount(),
            RequestStatus.valueOf(jpa.getStatus()),
            jpa.getSubmittedAt(),
            jpa.getReviewedBy(),
            jpa.getReviewedAt()
        );
    }

    public static SubscriptionRequestJpaEntity toJpa(SubscriptionRequest domain) {
        return new SubscriptionRequestJpaEntity(
            domain.getId(),
            domain.getSchoolId(),
            domain.getRequestType().name(),
            domain.getCurrentPlanId(),
            domain.getRequestedPlanId(),
            domain.getAmount(),
            domain.getStatus().name(),
            domain.getSubmittedAt(),
            domain.getReviewedBy(),
            domain.getReviewedAt()
        );
    }
}
