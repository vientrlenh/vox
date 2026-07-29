package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.outbox.OutboxStatus;
import com.sep.vox.infrastructure.persistence.entity.OutboxJpaEntity;


public final class OutboxMapper {
    
    public static Outbox toDomain(OutboxJpaEntity jpa) {
        return new Outbox(
            jpa.getId(), 
            jpa.getAggregateType(), 
            jpa.getAggregateId(), 
            jpa.getEventType(), 
            jpa.getPayload(),
            statusFromString(jpa.getStatus()), 
            jpa.getRetryCount(), 
            jpa.getCreatedAt(), 
            jpa.getPublishedAt(), 
            jpa.getNextRetryAt(), 
            jpa.getLastError()
        );
    
    }

    public static OutboxJpaEntity toJpa(Outbox outbox) {
        return new OutboxJpaEntity(
            outbox.getId(), 
            outbox.getAggregateType(), 
            outbox.getAggregateId(), 
            outbox.getEventType(), 
            outbox.getPayload(), 
            valueOf(outbox.getStatus()), 
            outbox.getRetryCount(), 
            outbox.getCreatedAt(), 
            outbox.getPublishedAt(), 
            outbox.getNextRetryAt(), 
            outbox.getLastError()
        );
    }

    private static OutboxStatus statusFromString(String status) {
        return status == null ? null : OutboxStatus.valueOf(status);
    }

    private static String valueOf(OutboxStatus status) {
        return status == null ? null : status.name();
    }
}
