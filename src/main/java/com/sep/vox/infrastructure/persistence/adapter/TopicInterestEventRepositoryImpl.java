package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.personalization.TopicInterestEvent;
import com.sep.vox.domain.repository.TopicInterestEventRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.TopicInterestEventJpaEntity;
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
        repository.save(TopicInterestEventJpaEntity.builder()
                .id(UUID.randomUUID())
                .studentId(studentId)
                .practiceTopicId(topicId)
                .practiceSessionId(sessionId)
                .eventType(eventType)
                .signal(java.math.BigDecimal.valueOf(signal))
                .occurredAt(Instant.now())
                .build());
    }

    @Override
    public List<TopicInterestEvent> findByStudent(UUID studentId) {
        return repository.findByStudentIdOrderByOccurredAtAscIdAsc(studentId).stream()
            .map(entity -> TopicInterestEvent.builder()
                    .topicId(entity.getPracticeTopicId())
                    .sessionId(entity.getPracticeSessionId())
                    .eventType(entity.getEventType())
                    .signal(entity.getSignal().doubleValue())
                    .occurredAt(entity.getOccurredAt())
                    .build())
            .toList();
    }
}
