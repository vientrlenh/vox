package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.WeaknessFrequencyInfo;
import com.sep.vox.infrastructure.persistence.entity.WeaknessObservationJpaEntity;

public interface SpringDataWeaknessObservationRepository
        extends JpaRepository<WeaknessObservationJpaEntity, UUID> {

    boolean existsBySourceEvaluationIdAndFrameworkCriterionIdAndSubAttributeAndEvidenceSpan(
        UUID sourceEvaluationId,
        UUID frameworkCriterionId,
        String subAttribute,
        String evidenceSpan
    );

    @Query(value = """
        SELECT student_id AS studentId,
               framework_criterion_id AS frameworkCriterionId,
               sub_attribute AS subAttribute,
               COUNT(*)::int AS frequency,
               COUNT(*) FILTER (WHERE observed_at >= :recentWindowStart)::int AS recentFrequency
        FROM weakness_observation
        WHERE student_id IN :studentIds
          AND observed_at >= :windowStart
        GROUP BY student_id, framework_criterion_id, sub_attribute
        """, nativeQuery = true)
    List<WeaknessFrequencyInfo> findWeaknessFrequencies(
        @Param("studentIds") List<UUID> studentIds,
        @Param("windowStart") OffsetDateTime windowStart,
        @Param("recentWindowStart") OffsetDateTime recentWindowStart
    );
}
