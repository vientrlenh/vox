package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.subscription.SchoolSubscriptionEvent;
import com.sep.vox.domain.model.subscription.SchoolSubscriptionEventType;
import com.sep.vox.infrastructure.persistence.entity.SchoolSubscriptionEventJpaEntity;

public final class SchoolSubscriptionEventMapper {

    private SchoolSubscriptionEventMapper() {
    }

    public static SchoolSubscriptionEvent toDomain(SchoolSubscriptionEventJpaEntity jpa) {
        return new SchoolSubscriptionEvent(
            jpa.getId(),
            jpa.getSchoolId(),
            jpa.getSubscriptionId(),
            SchoolSubscriptionEventType.valueOf(jpa.getEventType()),
            jpa.getActorId(),
            jpa.getReason(),
            jpa.getOccurredAt()
        );
    }

    public static SchoolSubscriptionEventJpaEntity toJpa(SchoolSubscriptionEvent domain) {
        var jpa = new SchoolSubscriptionEventJpaEntity();
        jpa.setId(domain.getId());
        jpa.setSchoolId(domain.getSchoolId());
        jpa.setSubscriptionId(domain.getSubscriptionId());
        jpa.setEventType(domain.getEventType().name());
        jpa.setActorId(domain.getActorId());
        jpa.setReason(domain.getReason());
        jpa.setOccurredAt(domain.getOccurredAt());
        return jpa;
    }
}
