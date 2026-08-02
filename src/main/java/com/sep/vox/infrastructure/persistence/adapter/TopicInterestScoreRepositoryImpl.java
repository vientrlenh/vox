package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.personalization.TopicInterestScoreEntry;
import com.sep.vox.domain.repository.personalization.TopicInterestScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.TopicInterestScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataTopicInterestScoreRepository;

@Repository
public class TopicInterestScoreRepositoryImpl
        implements TopicInterestScoreRepository {

    private final SpringDataTopicInterestScoreRepository repository;

    public TopicInterestScoreRepositoryImpl(
            SpringDataTopicInterestScoreRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replaceForStudent(
            UUID studentId,
            List<TopicInterestScoreEntry> scores) {
        repository.deleteByStudentId(studentId);
        var now = OffsetDateTime.now();
        repository.saveAll(scores.stream()
            .map(entry -> new TopicInterestScoreJpaEntity(
                UUID.randomUUID(),
                studentId,
                entry.getTopicId(),
                BigDecimal.valueOf(entry.getScore()),
                entry.getSessionCount(),
                entry.getLastEventAt(),
                now
            ))
            .toList());
    }
}
