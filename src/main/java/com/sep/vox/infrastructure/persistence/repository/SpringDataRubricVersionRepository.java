package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricVersionRepository extends JpaRepository<RubricVersionJpaEntity, UUID> {
    boolean existsByRubricIdAndIdNot(UUID rubricId, UUID id);

    List<RubricVersionJpaEntity> findByRubricId(UUID rubricId);

    @Modifying
    @Query("""
            UPDATE RubricVersionJpaEntity v SET 
            v.name = COALESCE(:name, v.name),
            v.code = COALESCE(:code, v.code),
            v.description = COALESCE(:description, v.description),
            v.effectiveFrom = COALESCE(:effectiveFrom, v.effectiveFrom),
            v.effectiveTo = COALESCE(:effectiveTo, v.effectiveTo),
            v.scoringScaleMin = COALESCE(:scoringScaleMin, v.scoringScaleMin),
            v.scoringScaleMax = COALESCE(:scoringScaleMax, v.scoringScaleMax),
            v.totalScoreMethod = COALESCE(:totalScoreMethod, v.totalScoreMethod),
            v.updatedAt = :updatedAt,
            v.updatedBy = :updatedBy
            WHERE v.id = :id
            """)
    int updateRubricVersionAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("code") String code,
            @Param("description") String description,
            @Param("effectiveFrom") OffsetDateTime effectiveFrom,
            @Param("effectiveTo") OffsetDateTime effectiveTo,
            @Param("scoringScaleMin") BigDecimal scoringScaleMin,
            @Param("scoringScaleMax") BigDecimal scoringScaleMax,
            @Param("totalScoreMethod") String totalScoreMethod,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );


    @Query("SELECT v FROM RubricVersionJpaEntity v WHERE v.rubricId = :rubricId AND (:status IS NULL OR v.status = :status)")
    Page<RubricVersionJpaEntity> findAllByRubricIdAndStatus(
            @Param("rubricId") UUID rubricId,
            @Param("status") String status,
            Pageable pageable
    );
}
