package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.infrastructure.persistence.entity.outbox.ProcessedEventJpaEntity;

public final class ProcessedEventMapper {
    
    public static ProcessedEvent toDomain(ProcessedEventJpaEntity jpa) {
        return ProcessedEvent.builder()
                .id(jpa.getId())
                .eventId(jpa.getEventId())
                .consumerGroup(jpa.getConsumerGroup())
                .processedAt(jpa.getProcessedAt())
                .build();
    }

    public static ProcessedEventJpaEntity toJpa(ProcessedEvent event) {
        return ProcessedEventJpaEntity.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .consumerGroup(event.getConsumerGroup())
                .processedAt(event.getProcessedAt())
                .build();
    }
}
