package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.personalization.PracticeItemEvaluationRepository;
import com.sep.vox.infrastructure.persistence.entity.PracticeItemEvaluationJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeItemEvaluationRepository;

@Repository
public class PracticeItemEvaluationRepositoryImpl implements PracticeItemEvaluationRepository {

    private final SpringDataPracticeItemEvaluationRepository repository;

    public PracticeItemEvaluationRepositoryImpl(
            SpringDataPracticeItemEvaluationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UUID upsert(
            UUID practiceResponseId,
            double itemScore,
            boolean markedInvalid,
            Instant evaluatedAt) {
        var existing = repository.findByPracticeResponseId(practiceResponseId).orElse(null);
        var scoreValue = BigDecimal.valueOf(itemScore);
        if (existing == null) {
            var saved = repository.save(new PracticeItemEvaluationJpaEntity(
                UUID.randomUUID(),
                practiceResponseId,
                scoreValue,
                markedInvalid,
                evaluatedAt
            ));
            return saved.getId();
        }
        existing.setItemScore(scoreValue);
        existing.setMarkedInvalid(markedInvalid);
        existing.setEvaluatedAt(evaluatedAt);
        return repository.save(existing).getId();
    }

    @Override
    public int countCompletedBySessionId(UUID sessionId) {
        return repository.countCompletedBySessionId(sessionId);
    }

    @Override
    public Double findLastValidNormalizedScore(UUID sessionId) {
        return repository.findLastValidNormalizedScore(sessionId);
    }

    @Override
    public BigDecimal findAverageItemScoreBySessionId(UUID sessionId) {
        return repository.findAverageItemScoreBySessionId(sessionId);
    }

    @Override
    public java.util.List<Double> findNormalizedScoresChronological(UUID studentId) {
        return repository.findNormalizedScoresChronological(studentId);
    }
}
