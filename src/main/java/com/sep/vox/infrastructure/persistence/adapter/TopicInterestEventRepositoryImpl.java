package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.TopicInterestEvent;
import com.sep.vox.domain.repository.personalization.TopicInterestEventRepository;
import com.sep.vox.infrastructure.persistence.entity.TopicInterestEventJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicInterestEventRepository;

@Repository
public class TopicInterestEventRepositoryImpl
        implements TopicInterestEventRepository {

    private final SpringDataTopicInterestEventRepository repository;

    public TopicInterestEventRepositoryImpl(
            SpringDataTopicInterestEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(
            UUID studentId,
            UUID topicId,
            UUID sessionId,
            String eventType,
            double signal) {
        repository.save(new TopicInterestEventJpaEntity(
            UUID.randomUUID(),
            studentId,
            topicId,
            sessionId,
            eventType,
            java.math.BigDecimal.valueOf(signal),
            Instant.now()
        ));
    }

    @Override
    public List<TopicInterestEvent> findByStudent(UUID studentId) {
        return repository.findByStudentIdOrderByOccurredAtAscIdAsc(studentId).stream()
            .map(entity -> new TopicInterestEvent(
                entity.getPracticeTopicId(),
                entity.getPracticeSessionId(),
                entity.getEventType(),
                entity.getSignal().doubleValue(),
                entity.getOccurredAt()
            ))
            .toList();
    }
}
