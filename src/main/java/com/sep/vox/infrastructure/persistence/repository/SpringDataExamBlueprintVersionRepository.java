package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintVersionJpaEntity;

public interface SpringDataExamBlueprintVersionRepository extends JpaRepository<ExamBlueprintVersionJpaEntity, UUID> {
    List<ExamBlueprintVersionJpaEntity> findByBlueprintIdOrderByVersionDesc(UUID blueprintId);
    List<ExamBlueprintVersionJpaEntity> findByBlueprintIdInOrderByVersionDesc(Collection<UUID> blueprintIds);

    List<ExamBlueprintVersionJpaEntity> findByBlueprintIdAndStatusOrderByVersionDesc(UUID blueprintId, String status);

    @Query("""
        SELECT COALESCE(MAX(v.version), 0) + 1
        FROM ExamBlueprintVersionJpaEntity v
        WHERE v.blueprintId = :blueprintId
    """)
    int nextVersionNumber(@Param("blueprintId") UUID blueprintId);

    @Query("""
        SELECT CASE WHEN (
            EXISTS (SELECT 1 FROM ExamJpaEntity e WHERE e.blueprintVersionId = :versionId)
            OR EXISTS (SELECT 1 FROM ExamPaperJpaEntity p WHERE p.blueprintVersionId = :versionId)
        ) THEN true ELSE false END
    """)
    boolean existsUsedByVersion(@Param("versionId") UUID versionId);
}
