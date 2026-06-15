package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricCriterionBandJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricCriterionBandRepository extends JpaRepository<RubricCriterionBandJpaEntity, UUID> {
    void deleteByCriterionId(UUID criterionId);


    @Modifying
    @Query("DELETE FROM RubricCriterionBandJpaEntity b WHERE b.criterionId IN (SELECT c.id FROM RubricCriterionJpaEntity c WHERE c.rubricVersionId = :rubricVersionId)")
    void deleteByRubricVersionId(@Param("rubricVersionId") UUID rubricVersionId);

    @Modifying
    @Query("""
        UPDATE RubricCriterionBandJpaEntity b SET 
        b.code = COALESCE(:code, b.code),
        b.scoreMin = COALESCE(:scoreMin, b.scoreMin),
        b.scoreMax = COALESCE(:scoreMax, b.scoreMax),
        b.updatedAt = :updatedAt,
        b.updatedBy = :updatedBy
        WHERE b.id = :id
        """)
    int updateBandAtomic(
            @Param("id") UUID id,
            @Param("code") String code,
            @Param("scoreMin") BigDecimal scoreMin,
            @Param("scoreMax") BigDecimal scoreMax,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    Page<RubricCriterionBandJpaEntity> findAllByCriterionId(UUID criterionId, Pageable pageable);
}
