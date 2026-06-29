package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamBlueprintVersionJpaEntity;

public interface SpringDataExamBlueprintVersionRepository extends JpaRepository<ExamBlueprintVersionJpaEntity, UUID> {
    List<ExamBlueprintVersionJpaEntity> findByBlueprintIdOrderByVersionDesc(UUID blueprintId);

    List<ExamBlueprintVersionJpaEntity> findByBlueprintIdAndStatusOrderByVersionDesc(UUID blueprintId, String status);

    @Query("""
        SELECT COALESCE(MAX(v.version), 0) + 1
        FROM ExamBlueprintVersionJpaEntity v
        WHERE v.blueprintId = :blueprintId
    """)
    int nextVersionNumber(@Param("blueprintId") UUID blueprintId);
}
