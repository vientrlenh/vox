package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.personalization.DimensionInterestScoreJpaEntity;

public interface SpringDataDimensionInterestScoreRepository
        extends JpaRepository<DimensionInterestScoreJpaEntity, UUID> {

    List<DimensionInterestScoreJpaEntity> findByLearnerProfileId(UUID learnerProfileId);

    void deleteByLearnerProfileId(UUID learnerProfileId);

    @Modifying
    @Query(value = """
        UPDATE dimension_interest_scores
        SET baseline_score = score
        WHERE learner_profile_id = :profileId
          AND baseline_score IS NULL
        """, nativeQuery = true)
    void setBaselineFromScoreWhereMissing(@Param("profileId") UUID profileId);

    @Modifying
    @Query(value = """
        INSERT INTO dimension_interest_scores (
            learner_profile_id, dimension, score
        ) VALUES (:profileId, :dimension, :score)
        ON CONFLICT (learner_profile_id, dimension)
        DO UPDATE SET score = EXCLUDED.score
        """, nativeQuery = true)
    void upsertScore(
        @Param("profileId") UUID profileId,
        @Param("dimension") String dimension,
        @Param("score") java.math.BigDecimal score
    );

    // REMOVED: copyScores. It only existed to copy the 6 score rows to the new learner_profiles version every time the
    // profile changed. Since the profile became 1-1 and updated in place, the id does not change and the scores stay in one place.
}
