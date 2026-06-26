package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricCriterionJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricCriterionRepository extends JpaRepository<RubricCriterionJpaEntity, UUID> {

    void deleteByRubricVersionId(UUID rubricVersionId);

    @Modifying
    @Query("""
        UPDATE RubricCriterionJpaEntity c SET 
        c.code = COALESCE(:code, c.code),
        c.name = COALESCE(:name, c.name),
        c.description = COALESCE(:description, c.description),
        c.examplesJson = COALESCE(:examplesJson, c.examplesJson),
        c.weight = COALESCE(:weight, c.weight),
        c.minScore = COALESCE(:minScore, c.minScore),
        c.maxScore = COALESCE(:maxScore, c.maxScore),
        c.order = COALESCE(:order, c.order),
        c.isRequired = COALESCE(:isRequired, c.isRequired),
        c.updatedAt = :updatedAt,
        c.updatedBy = :updatedBy
        WHERE c.id = :id
        """)
    int updateCriterionAtomic(
            @Param("id") UUID id,
            @Param("code") String code,
            @Param("name") String name,
            @Param("description") String description,
            @Param("examplesJson") String examplesJson,
            @Param("weight") BigDecimal weight,
            @Param("minScore") BigDecimal minScore,
            @Param("maxScore") BigDecimal maxScore,
            @Param("order") Integer order,
            @Param("isRequired") Boolean isRequired,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    Page<RubricCriterionJpaEntity> findAllByRubricVersionId(UUID rubricVersionId, Pageable pageable);

    @Query("SELECT c FROM RubricCriterionJpaEntity c WHERE c.rubricVersionId IN :versionIds")
    List<RubricCriterionJpaEntity> findByRubricVersionIdIn(@Param("versionIds") List<UUID> versionIds);

    @Query("SELECT c FROM RubricCriterionJpaEntity c WHERE c.rubricVersionId = :versionId " +
            "AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:isRequired IS NULL OR c.isRequired = :isRequired) " +
            "ORDER BY c.order ASC")
    Page<RubricCriterionJpaEntity> searchRubricCriteria(
            @Param("versionId") UUID versionId,
            @Param("keyword") String keyword,
            @Param("isRequired") Boolean isRequired,
            Pageable pageable);
}
