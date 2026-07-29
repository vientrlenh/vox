package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.ProcessedEventJpaEntity;

public interface SpringDataProcessedEventRepository extends JpaRepository<ProcessedEventJpaEntity, UUID>{
    boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
