package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.PracticeItemEvaluationRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeItemEvaluationJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeItemEvaluationRepository;

@Repository
@RequiredArgsConstructor
public class PracticeItemEvaluationRepositoryImpl implements PracticeItemEvaluationRepository {

    private final SpringDataPracticeItemEvaluationRepository repository;

    @Override
    @Transactional
    public UUID upsert(
            UUID practiceResponseId,
            double itemScore,
            boolean markedInvalid,
            Instant evaluatedAt) {
        PracticeItemEvaluationJpaEntity existing = repository.findByPracticeResponseId(practiceResponseId).orElse(null);
        BigDecimal scoreValue = BigDecimal.valueOf(itemScore);
        if (existing == null) {
            PracticeItemEvaluationJpaEntity saved = repository.save(PracticeItemEvaluationJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .practiceResponseId(practiceResponseId)
                    .itemScore(scoreValue)
                    .markedInvalid(markedInvalid)
                    .evaluatedAt(evaluatedAt)
                    .build());
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
}
