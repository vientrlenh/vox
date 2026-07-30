package com.sep.vox.infrastructure.persistence.adapter;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.outbox.ProcessedEvent;
import com.sep.vox.domain.repository.ProcessedEventRepository;
import com.sep.vox.infrastructure.persistence.mapper.ProcessedEventMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataProcessedEventRepository;

@Repository
public class ProcessedEventRepositoryImpl implements ProcessedEventRepository {

    private final SpringDataProcessedEventRepository springDataProcessedEventRepository;

    public ProcessedEventRepositoryImpl(SpringDataProcessedEventRepository springDataProcessedEventRepository) {
        this.springDataProcessedEventRepository = springDataProcessedEventRepository;
    }

    @Override
    public boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup) {
        return springDataProcessedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup);
    }

    @Override
    public ProcessedEvent save(ProcessedEvent event) {
        var entity = ProcessedEventMapper.toJpa(event);
        var saved = springDataProcessedEventRepository.save(entity);
        return ProcessedEventMapper.toDomain(saved);
    }
    
}
