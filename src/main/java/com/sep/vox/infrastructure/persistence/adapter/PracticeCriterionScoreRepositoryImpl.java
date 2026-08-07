package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;
import com.sep.vox.domain.repository.personalization.PracticeCriterionScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.PracticeCriterionScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeCriterionScoreRepository;

@Repository
public class PracticeCriterionScoreRepositoryImpl implements PracticeCriterionScoreRepository {

    private final SpringDataPracticeCriterionScoreRepository repository;

    public PracticeCriterionScoreRepositoryImpl(
            SpringDataPracticeCriterionScoreRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void upsertByCode(
            UUID practiceEvaluationId,
            String criterionCode,
            double finalScore) {
        var existing = repository
            .findByPracticeEvaluationIdAndCriterionCode(practiceEvaluationId, criterionCode)
            .orElse(null);
        var scoreValue = BigDecimal.valueOf(finalScore);
        if (existing == null) {
            repository.save(new PracticeCriterionScoreJpaEntity(
                UUID.randomUUID(),
                practiceEvaluationId,
                criterionCode,
                scoreValue
            ));
            return;
        }
        existing.setFinalScore(scoreValue);
        repository.save(existing);
    }

    @Override
    public List<PracticeCriterionScoreDto> findScoresBySessionId(UUID sessionId) {
        return repository.findScoresBySessionId(sessionId).stream()
            .map(row -> new PracticeCriterionScoreDto(row.getCode(), row.getFinalScore()))
            .toList();
    }
}
