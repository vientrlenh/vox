package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEventType;
import com.sep.vox.infrastructure.persistence.entity.subscription.SchoolSubscriptionEventJpaEntity;

public final class SchoolSubscriptionEventMapper {

    private SchoolSubscriptionEventMapper() {
    }

    public static SchoolSubscriptionEvent toDomain(SchoolSubscriptionEventJpaEntity jpa) {
        return SchoolSubscriptionEvent.builder()
            .id(jpa.getId())
            .schoolId(jpa.getSchoolId())
            .subscriptionId(jpa.getSubscriptionId())
            .eventType(SchoolSubscriptionEventType.valueOf(jpa.getEventType()))
            .actorId(jpa.getActorId())
            .reason(jpa.getReason())
            .occurredAt(jpa.getOccurredAt())
            .build();
    }

    public static SchoolSubscriptionEventJpaEntity toJpa(SchoolSubscriptionEvent domain) {
        return SchoolSubscriptionEventJpaEntity.builder()
            .id(domain.getId())
            .schoolId(domain.getSchoolId())
            .subscriptionId(domain.getSubscriptionId())
            .eventType(domain.getEventType().name())
            .actorId(domain.getActorId())
            .reason(domain.getReason())
            .occurredAt(domain.getOccurredAt())
            .build();
    }
}
