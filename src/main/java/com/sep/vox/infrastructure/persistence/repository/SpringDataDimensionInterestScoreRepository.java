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
        UPDATE dimension_interest_scores
        SET baseline_score = score
        WHERE learner_profile_id = :profileId
          AND baseline_score IS NULL
        """, nativeQuery = true)
    void setBaselineFromScoreWhereMissing(@Param("profileId") UUID profileId);

    @Modifying
    @Query(value = """
        INSERT INTO dimension_interest_scores (
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

    // GỠ: copyScores. Nó chỉ tồn tại để chép 6 dòng điểm sang learner_profiles bản mới mỗi lần
    // hồ sơ đổi. Từ khi hồ sơ về 1-1 và cập nhật tại chỗ thì id không đổi, điểm nằm yên một chỗ.
}
