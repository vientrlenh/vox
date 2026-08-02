package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.LearnerWeaknessSnapshotJpaEntity;

public interface SpringDataLearnerWeaknessSnapshotRepository
        extends JpaRepository<LearnerWeaknessSnapshotJpaEntity, UUID> {

    void deleteByStudentIdIn(List<UUID> studentIds);

    @Query(value = """
        SELECT criterion.code
        FROM learner_weakness_snapshot snapshot
        JOIN framework_criteria criterion
          ON criterion.id = snapshot.framework_criterion_id
        WHERE snapshot.student_id = :studentId
        ORDER BY (
            snapshot.weakness
            * CASE WHEN snapshot.observation_count >= 3 THEN 1.0 ELSE 0.6 END
        ) DESC
        """, nativeQuery = true)
    List<String> findFocusCriterionCodesOrderedByWeakness(@Param("studentId") UUID studentId);
}
