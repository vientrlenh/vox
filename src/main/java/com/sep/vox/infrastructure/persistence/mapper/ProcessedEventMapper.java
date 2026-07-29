package com.sep.vox.infrastructure.persistence.mapper;

import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.infrastructure.persistence.entity.ProcessedEventJpaEntity;

public final class ProcessedEventMapper {
    
    public static ProcessedEvent toDomain(ProcessedEventJpaEntity jpa) {
        return new ProcessedEvent(
            jpa.getId(), 
            jpa.getEventId(), 
            jpa.getConsumerGroup(), 
            jpa.getProcessedAt()
        );
    }

    public static ProcessedEventJpaEntity toJpa(ProcessedEvent event) {
        return new ProcessedEventJpaEntity(
            event.getId(), 
            event.getEventId(), 
            event.getConsumerGroup(), 
            event.getProcessedAt()
        );
    }
}
