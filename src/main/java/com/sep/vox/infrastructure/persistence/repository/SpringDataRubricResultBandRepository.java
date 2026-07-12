package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.RubricResultBandJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricResultBandRepository extends JpaRepository<RubricResultBandJpaEntity, UUID> {


    List<RubricResultBandJpaEntity>findByRubricVersionId(UUID rubricVersionId);
    List<RubricResultBandJpaEntity> findByIdIn(Collection<UUID> ids);

    void deleteByRubricVersionId(UUID rubricVersionId);

    @Query("SELECT r FROM RubricResultBandJpaEntity r WHERE r.rubricVersionId IN :versionIds")
    List<RubricResultBandJpaEntity> findByRubricVersionIdIn(@Param("versionIds") List<UUID> versionIds);

    @Modifying
    @Query("""
            UPDATE RubricResultBandJpaEntity r SET 
            r.code = COALESCE(:code, r.code),
            r.name = COALESCE(:name, r.name),
            r.description = COALESCE(:description, r.description),
            r.scoreMin = COALESCE(:scoreMin, r.scoreMin),
            r.scoreMax = COALESCE(:scoreMax, r.scoreMax),
            r.order = COALESCE(:order, r.order),
            r.updatedAt = :updatedAt,
            r.updatedBy = :updatedBy
            WHERE r.id = :id
            """)
    int updateResultBandAtomic(
            @Param("id") UUID id,
            @Param("code") String code,
            @Param("name") String name,
            @Param("description") String description,
            @Param("scoreMin") BigDecimal scoreMin,
            @Param("scoreMax") BigDecimal scoreMax,
            @Param("order") Integer order,
            @Param("updatedAt") OffsetDateTime updatedAt,
            @Param("updatedBy") UUID updatedBy
    );

    Page<RubricResultBandJpaEntity> findAllByRubricVersionId(UUID rubricVersionId, Pageable pageable);

    @Query("SELECT r FROM RubricResultBandJpaEntity r WHERE r.rubricVersionId = :versionId " +
            "AND (:keywordPattern IS NULL OR LOWER(r.name) LIKE :keywordPattern OR LOWER(r.code) LIKE :keywordPattern) " +
            "ORDER BY r.order ASC")
    Page<RubricResultBandJpaEntity> searchRubricResultBands(
            @Param("versionId") UUID versionId,
            @Param("keywordPattern") String keywordPattern,
            Pageable pageable);

}

