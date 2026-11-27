package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.rubric.RubricVersionJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRubricVersionRepository extends JpaRepository<RubricVersionJpaEntity, UUID> {
    boolean existsByRubricIdAndIdNot(UUID rubricId, UUID id);
    Optional<RubricVersionJpaEntity> findByCode(String code);
    Optional<RubricVersionJpaEntity> findByName(String name);
    // Receives input already uppercased by the Impl; the entity side still wraps UPPER() in case old data has inconsistent case.
    @Query("SELECT v FROM RubricVersionJpaEntity v WHERE UPPER(v.code) IN :codes")
    List<RubricVersionJpaEntity> findByCodeIn(@Param("codes") Collection<String> codes);
    List<RubricVersionJpaEntity> findByNameIn(Collection<String> names);

    List<RubricVersionJpaEntity> findByRubricId(UUID rubricId);

    @Modifying
    @Query("""
            UPDATE RubricVersionJpaEntity v SET 
            v.name = COALESCE(:name, v.name),
            v.code = COALESCE(:code, v.code),
            v.description = CASE WHEN :descriptionProvided = true THEN :description ELSE v.description END,
            v.effectiveFrom = COALESCE(:effectiveFrom, v.effectiveFrom),
            v.effectiveTo = CASE WHEN :effectiveToProvided = true THEN :effectiveTo ELSE v.effectiveTo END,
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
            @Param("descriptionProvided") boolean descriptionProvided,
            @Param("effectiveFrom") Instant effectiveFrom,
            @Param("effectiveTo") Instant effectiveTo,
            @Param("effectiveToProvided") boolean effectiveToProvided,
            @Param("scoringScaleMin") BigDecimal scoringScaleMin,
            @Param("scoringScaleMax") BigDecimal scoringScaleMax,
            @Param("totalScoreMethod") String totalScoreMethod,
            @Param("updatedAt") Instant updatedAt,
            @Param("updatedBy") String updatedBy
    );


    @Query("SELECT v FROM RubricVersionJpaEntity v WHERE v.rubricId = :rubricId AND (:status IS NULL OR v.status = :status)")
    Page<RubricVersionJpaEntity> findAllByRubricIdAndStatus(
            @Param("rubricId") UUID rubricId,
            @Param("status") String status,
            Pageable pageable
    );

    // A Rubric usually has only a few Versions (almost never thousands), so the best way to reach O(1) is: use an IN clause to fetch all Versions of those Rubrics in a single SQL statement, then paginate in Java (pagination in RAM).
    @Query("SELECT v FROM RubricVersionJpaEntity v WHERE v.rubricId IN :rubricIds AND (:status IS NULL OR v.status = :status)")
    List<RubricVersionJpaEntity> findByRubricIdInAndStatus(
            @Param("rubricIds") List<UUID> rubricIds,
            @Param("status") String status
    );



    @Query(
            "SELECT v FROM RubricVersionJpaEntity v WHERE v.rubricId = :rubricId " +
                    "AND (:keywordPattern IS NULL OR LOWER(v.name) LIKE :keywordPattern OR LOWER(v.code) LIKE :keywordPattern) " +
                    "AND (:status IS NULL OR v.status = :status) " +
                    "ORDER BY v.version DESC")
    Page<RubricVersionJpaEntity> searchRubricVersions(
            @Param("rubricId") UUID rubricId,
            @Param("keywordPattern") String keywordPattern,
            @Param("status") String status,
            Pageable pageable);
}
