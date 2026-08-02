package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.personalization.DimensionInterestScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.DimensionInterestScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataDimensionInterestScoreRepository;

@Repository
public class DimensionInterestScoreRepositoryImpl
        implements DimensionInterestScoreRepository {

    private final SpringDataDimensionInterestScoreRepository repository;

    public DimensionInterestScoreRepositoryImpl(
            SpringDataDimensionInterestScoreRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void primeBaselineFromScoreWhereMissing(UUID learnerProfileId) {
        repository.setBaselineFromScoreWhereMissing(learnerProfileId);
    }

    @Override
    public Map<String, Double> findByLearnerProfile(UUID learnerProfileId) {
        return repository.findByLearnerProfileId(learnerProfileId).stream()
            .collect(Collectors.toMap(
                entity -> entity.getDimension(),
                entity -> (entity.getBaselineScore() != null
                    ? entity.getBaselineScore()
                    : entity.getScore()).doubleValue()
            ));
    }

    @Override
    @Transactional
    public void upsertScore(
            UUID learnerProfileId,
            String dimension,
            double score) {
        repository.upsertScore(
            UUID.randomUUID(),
            learnerProfileId,
            dimension,
            BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP)
        );
    }

    @Override
    @Transactional
    public void replaceScores(
            UUID learnerProfileId,
            Map<String, Double> scores) {
        repository.deleteByLearnerProfileId(learnerProfileId);
        repository.saveAll(scores.entrySet().stream()
            .map(entry -> new DimensionInterestScoreJpaEntity(
                learnerProfileId,
                entry.getKey(),
                BigDecimal.valueOf(entry.getValue()).setScale(4, RoundingMode.HALF_UP),
                null
            ))
            .toList());
    }

    @Override
    @Transactional
    public void copyScores(UUID previousProfileId, UUID newProfileId) {
        repository.copyScores(previousProfileId, newProfileId);
    }
}
