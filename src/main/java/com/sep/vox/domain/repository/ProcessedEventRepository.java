package com.sep.vox.domain.repository;

import java.util.UUID;

import com.sep.vox.domain.model.outbox.ProcessedEvent;

public interface ProcessedEventRepository {
    boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
    ProcessedEvent save(ProcessedEvent event);
}
