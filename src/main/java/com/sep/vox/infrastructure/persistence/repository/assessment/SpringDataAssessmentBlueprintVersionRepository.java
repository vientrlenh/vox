package com.sep.vox.infrastructure.persistence.repository.assessment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.assessment.AssessmentBlueprintVersionJpaEntity;

public interface SpringDataAssessmentBlueprintVersionRepository extends JpaRepository<AssessmentBlueprintVersionJpaEntity, UUID> {
    List<AssessmentBlueprintVersionJpaEntity> findByBlueprintIdOrderByVersionDesc(UUID blueprintId);
    List<AssessmentBlueprintVersionJpaEntity> findByBlueprintIdInOrderByVersionDesc(Collection<UUID> blueprintIds);

    List<AssessmentBlueprintVersionJpaEntity> findByBlueprintIdAndStatusOrderByVersionDesc(UUID blueprintId, String status);

    @Query("""
        SELECT COALESCE(MAX(v.version), 0) + 1
        FROM AssessmentBlueprintVersionJpaEntity v
        WHERE v.blueprintId = :blueprintId
    """)
    int nextVersionNumber(@Param("blueprintId") UUID blueprintId);

    @Query("""
        SELECT CASE WHEN (
            EXISTS (SELECT 1 FROM AssessmentJpaEntity e WHERE e.blueprintVersionId = :versionId)
            OR EXISTS (SELECT 1 FROM AssessmentPaperJpaEntity p WHERE p.blueprintVersionId = :versionId)
        ) THEN true ELSE false END
    """)
    boolean existsUsedByVersion(@Param("versionId") UUID versionId);
}
