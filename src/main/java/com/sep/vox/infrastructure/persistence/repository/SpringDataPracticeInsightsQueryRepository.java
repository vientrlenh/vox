package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.CriterionWeaknessInfo;
import com.sep.vox.infrastructure.persistence.entity.LearnerWeaknessSnapshotJpaEntity;

/** Repository chỉ-đọc cho các báo cáo tổng hợp practice insights -- không có entity chủ, dùng
 * marker {@link Repository} thay vì {@code JpaRepository} vì không cần CRUD chuẩn. */
public interface SpringDataPracticeInsightsQueryRepository
        extends Repository<LearnerWeaknessSnapshotJpaEntity, UUID> {

    @Query(value = """
        SELECT criterion.code AS criterionCode,
               COALESCE((
                   SELECT rc.name
                   FROM school_class_users membership
                   JOIN school_classes class ON class.id = membership.school_class_id
                   JOIN assessment_policies policy
                     ON policy.status = 'PUBLISHED'
                    AND policy.effective_from <= CURRENT_TIMESTAMP
                    AND (policy.effective_to IS NULL OR policy.effective_to >= CURRENT_TIMESTAMP)
                    AND (policy.school_id IS NULL OR policy.school_id = class.school_id)
                    AND (policy.school_grade_id IS NULL OR policy.school_grade_id = class.school_grade_id)
                    AND (policy.school_class_id IS NULL OR policy.school_class_id = class.id)
                   JOIN rubric_criterions rc
                     ON rc.rubric_version_id = policy.rubric_version_id
                    AND rc.framework_criterion_id = snapshot.framework_criterion_id
                   WHERE membership.user_id = snapshot.student_id
                     AND membership.is_active = true
                     AND class.status = 'ACTIVE'
                   ORDER BY (policy.school_class_id IS NOT NULL) DESC,
                            (policy.school_grade_id IS NOT NULL) DESC,
                            (policy.school_id IS NOT NULL) DESC,
                            policy.version DESC
                   LIMIT 1
               ), criterion.name) AS criterionName,
               snapshot.weakness AS weakness,
               snapshot.observation_count AS observationCount,
               snapshot.reliable AS reliable
        FROM learner_weakness_snapshot snapshot
        JOIN framework_criteria criterion
          ON criterion.id = snapshot.framework_criterion_id
        WHERE snapshot.student_id = :studentId
        ORDER BY criterion.criteria_order
        """, nativeQuery = true)
    List<CriterionWeaknessInfo> findCriterionWeaknesses(@Param("studentId") UUID studentId);



    // Số phiên/evaluation thật đã đóng góp vào bức tranh điểm yếu hiện tại, trong đúng cửa sổ
    // quan sát mà WeaknessSnapshotRefreshService dùng để tính snapshot -- không suy diễn, đếm
    // thẳng DISTINCT evaluation trong weakness_observation.
    @Query(value = """
        SELECT COUNT(DISTINCT source_evaluation_id)
        FROM weakness_observation
        WHERE student_id = :studentId
          AND observed_at >= :windowStart
        """, nativeQuery = true)
    int countSessionsAnalysed(
        @Param("studentId") UUID studentId,
        @Param("windowStart") Instant windowStart
    );
}
