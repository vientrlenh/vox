package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.DimensionInterestScoreJpaEntity;

public interface SpringDataDimensionInterestScoreRepository
        extends JpaRepository<DimensionInterestScoreJpaEntity, UUID> {

    List<DimensionInterestScoreJpaEntity> findByLearnerProfileId(UUID learnerProfileId);

    void deleteByLearnerProfileId(UUID learnerProfileId);

    @Modifying
    @Query(value = """
        UPDATE dimension_interest_score
        SET baseline_score = score
        WHERE learner_profile_id = :profileId
          AND baseline_score IS NULL
        """, nativeQuery = true)
    void setBaselineFromScoreWhereMissing(@Param("profileId") UUID profileId);

    @Modifying
    @Query(value = """
        INSERT INTO dimension_interest_score (
            id, learner_profile_id, dimension, score
        ) VALUES (:id, :profileId, :dimension, :score)
        ON CONFLICT (learner_profile_id, dimension)
        DO UPDATE SET score = EXCLUDED.score
        """, nativeQuery = true)
    void upsertScore(
        @Param("id") UUID id,
        @Param("profileId") UUID profileId,
        @Param("dimension") String dimension,
        @Param("score") java.math.BigDecimal score
    );

    @Modifying
    @Query(value = """
        INSERT INTO dimension_interest_score (
            id, learner_profile_id, dimension, score, baseline_score
        )
        SELECT uuidv7(), :newProfileId, dimension, score, baseline_score
        FROM dimension_interest_score
        WHERE learner_profile_id = :previousProfileId
        """, nativeQuery = true)
    void copyScores(
        @Param("previousProfileId") UUID previousProfileId,
        @Param("newProfileId") UUID newProfileId
    );
}
