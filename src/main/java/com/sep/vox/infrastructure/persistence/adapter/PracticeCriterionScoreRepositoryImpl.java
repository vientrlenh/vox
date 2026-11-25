package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.dto.personalization.PracticeCriterionScoreDto;
import com.sep.vox.domain.repository.PracticeCriterionScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeCriterionScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataPracticeCriterionScoreRepository;

@Repository
@RequiredArgsConstructor
public class PracticeCriterionScoreRepositoryImpl implements PracticeCriterionScoreRepository {

    private final SpringDataPracticeCriterionScoreRepository repository;

    @Override
    @Transactional
    public void upsertByCode(
            UUID practiceEvaluationId,
            String criterionCode,
            double finalScore) {
        PracticeCriterionScoreJpaEntity existing = repository
            .findByPracticeEvaluationIdAndCriterionCode(practiceEvaluationId, criterionCode)
            .orElse(null);
        BigDecimal scoreValue = BigDecimal.valueOf(finalScore);
        if (existing == null) {
            repository.save(PracticeCriterionScoreJpaEntity.builder()
                    .practiceEvaluationId(practiceEvaluationId)
                    .criterionCode(criterionCode)
                    .finalScore(scoreValue)
                    .build());
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
