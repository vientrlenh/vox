package com.sep.vox.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.repository.DimensionInterestScoreRepository;
import com.sep.vox.infrastructure.persistence.entity.personalization.DimensionInterestScoreJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataDimensionInterestScoreRepository;

@Repository
@RequiredArgsConstructor
public class DimensionInterestScoreRepositoryImpl
        implements DimensionInterestScoreRepository {

    private final SpringDataDimensionInterestScoreRepository repository;

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
            learnerProfileId,
            dimension,
            BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP)
        );
    }

    @Override
    @Transactional
    public void replaceScores(UUID learnerProfileId, Map<String, Double> scores) {
        deleteScoresAndFlush(learnerProfileId);
        repository.saveAll(scores.entrySet().stream()
            .map(entry -> toNewEntity(learnerProfileId, entry.getKey(), entry.getValue()))
            .toList());
    }

    private void deleteScoresAndFlush(UUID learnerProfileId) {
        repository.deleteByLearnerProfileId(learnerProfileId);
        // The flush before saveAll is required -- it is NOT redundant.
        //
        // deleteByLearnerProfileId has no @Modifying, so Spring Data JPA runs it as entityManager.remove() per
        // row, which only QUEUES an EntityDeleteAction in the Hibernate persistence context and does not run
        // the DELETE yet. At flush time Hibernate always executes its action queue in a FIXED ORDER -- inserts
        // first, deletes after -- regardless of the order of the calls in the Java code. Without flush() here,
        // saveAll() below would insert the new rows while the old rows (same key
        // (learner_profile_id, dimension)) are still in the table, violating
        // idx_dimension_interest_profile_dimension.
        //
        // Measured: a student retaking the interest quiz (every second time onwards, because the first time
        // the table is empty) failed 100% of the time -- DataIntegrityViolationException, GraphQL returned an
        // error, the transaction rolled back cleanly so no data was lost, but the student could never retake
        // the quiz.
        repository.flush();
    }

    private DimensionInterestScoreJpaEntity toNewEntity(UUID learnerProfileId, String dimension, double score) {
        return DimensionInterestScoreJpaEntity.builder()
            .learnerProfileId(learnerProfileId)
            .dimension(dimension)
            .score(BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP))
            .baselineScore(null)
            .build();
    }

}
