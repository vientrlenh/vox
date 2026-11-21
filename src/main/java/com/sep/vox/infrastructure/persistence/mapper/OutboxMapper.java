package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.model.outbox.OutboxStatus;
import com.sep.vox.infrastructure.persistence.entity.outbox.OutboxJpaEntity;


public final class OutboxMapper {
    
    public static Outbox toDomain(OutboxJpaEntity jpa) {
        return Outbox.builder()
                .id(jpa.getId())
                .aggregateType(jpa.getAggregateType())
                .aggregateId(jpa.getAggregateId())
                .eventType(jpa.getEventType())
                .payload(jpa.getPayload())
                .status(statusFromString(jpa.getStatus()))
                .retryCount(jpa.getRetryCount())
                .createdAt(jpa.getCreatedAt())
                .publishedAt(jpa.getPublishedAt())
                .nextRetryAt(jpa.getNextRetryAt())
                .lastError(jpa.getLastError())
                .build();
    
    }

    public static OutboxJpaEntity toJpa(Outbox outbox) {
        return OutboxJpaEntity.builder()
                .id(outbox.getId())
                .aggregateType(outbox.getAggregateType())
                .aggregateId(outbox.getAggregateId())
                .eventType(outbox.getEventType())
                .payload(outbox.getPayload())
                .status(valueOf(outbox.getStatus()))
                .retryCount(outbox.getRetryCount())
                .createdAt(outbox.getCreatedAt())
                .publishedAt(outbox.getPublishedAt())
                .nextRetryAt(outbox.getNextRetryAt())
                .lastError(outbox.getLastError())
                .build();
    }

    private static OutboxStatus statusFromString(String status) {
        return status == null ? null : OutboxStatus.valueOf(status);
    }

    private static String valueOf(OutboxStatus status) {
        return status == null ? null : status.name();
    }
}
