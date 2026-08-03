package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.WeaknessScoreObservationRow;
import com.sep.vox.infrastructure.persistence.entity.LearnerWeaknessSnapshotJpaEntity;

/** Repository chỉ-đọc gộp điểm thi + điểm luyện tập theo tiêu chí -- không có entity chủ, dùng
 * marker {@link Repository} thay vì {@code JpaRepository} vì không cần CRUD chuẩn. */
public interface SpringDataWeaknessScoreObservationViewRepository
        extends Repository<LearnerWeaknessSnapshotJpaEntity, UUID> {

    @Query(value = """
        WITH latest_membership AS (
            SELECT user_id, school_class_id, school_grade_id
            FROM (
                SELECT scu.user_id,
                       scu.school_class_id,
                       sc.school_grade_id,
                       ROW_NUMBER() OVER (
                           PARTITION BY scu.user_id
                           ORDER BY scu.joined_at DESC, scu.id DESC
                       ) AS membership_rank
                FROM school_class_users scu
                JOIN school_classes sc ON sc.id = scu.school_class_id
                WHERE scu.is_active = true
                  AND sc.status = 'ACTIVE'
            ) ranked_membership
            WHERE membership_rank = 1
        ),
        score_source AS (
            SELECT ec.student_id,
                   rc.framework_criterion_id,
                   fc.code AS criterion_code,
                   cs.final_score,
                   rc.min_score,
                   rc.max_score,
                   e.evaluated_at,
                   'EXAM' AS source_type,
                   e.id AS evaluation_id
            FROM exam_item_criterion_scores cs
            JOIN rubric_criterions rc ON rc.id = cs.rubric_criterion_id
            JOIN framework_criteria fc ON fc.id = rc.framework_criterion_id
            JOIN exam_item_evaluations e ON e.id = cs.evaluation_id
            JOIN exam_item_responses r ON r.id = e.response_id
            JOIN exam_sessions s ON s.id = r.session_id
            JOIN exam_candidates ec ON ec.id = s.candidate_id
            WHERE e.marked_invalid = false
              AND ec.blocked_at IS NULL
              AND cs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM exam_item_evaluation_turns t
                  WHERE t.evaluation_id = e.id
              )
            UNION ALL
            SELECT ps.student_id,
                   rc.framework_criterion_id,
                   fc.code AS criterion_code,
                   pcs.final_score,
                   rc.min_score,
                   rc.max_score,
                   pe.evaluated_at,
                   'PRACTICE' AS source_type,
                   pe.id AS evaluation_id
            FROM practice_criterion_score pcs
            JOIN rubric_criterions rc ON rc.id = pcs.rubric_criterion_id
            JOIN framework_criteria fc ON fc.id = rc.framework_criterion_id
            JOIN practice_item_evaluation pe ON pe.id = pcs.practice_evaluation_id
            JOIN practice_item_response pr ON pr.id = pe.practice_response_id
            JOIN practice_session ps ON ps.id = pr.practice_session_id
            WHERE pe.marked_invalid = false
              AND pcs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM practice_response_turn pt
                  WHERE pt.practice_response_id = pr.id
              )
        )
        SELECT source.student_id AS studentId,
               source.framework_criterion_id AS frameworkCriterionId,
               source.criterion_code AS criterionCode,
               source.final_score AS finalScore,
               source.min_score AS minScore,
               source.max_score AS maxScore,
               source.evaluated_at AS evaluatedAt,
               source.source_type AS sourceType,
               source.evaluation_id AS evaluationId,
               membership.school_class_id AS schoolClassId,
               membership.school_grade_id AS schoolGradeId
        FROM score_source source
        LEFT JOIN latest_membership membership
          ON membership.user_id = source.student_id
        ORDER BY source.evaluated_at, source.source_type, source.evaluation_id
        """, nativeQuery = true)
    List<WeaknessScoreObservationRow> findAllValidScoreObservations();

    @Query(value = """
        WITH score_source AS (
            SELECT ec.student_id, e.evaluated_at
            FROM exam_item_criterion_scores cs
            JOIN exam_item_evaluations e ON e.id = cs.evaluation_id
            JOIN exam_item_responses r ON r.id = e.response_id
            JOIN exam_sessions s ON s.id = r.session_id
            JOIN exam_candidates ec ON ec.id = s.candidate_id
            WHERE e.marked_invalid = false
              AND ec.blocked_at IS NULL
              AND cs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM exam_item_evaluation_turns t
                  WHERE t.evaluation_id = e.id
              )
            UNION ALL
            SELECT ps.student_id, pe.evaluated_at
            FROM practice_criterion_score pcs
            JOIN practice_item_evaluation pe ON pe.id = pcs.practice_evaluation_id
            JOIN practice_item_response pr ON pr.id = pe.practice_response_id
            JOIN practice_session ps ON ps.id = pr.practice_session_id
            WHERE pe.marked_invalid = false
              AND pcs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM practice_response_turn pt
                  WHERE pt.practice_response_id = pr.id
              )
        ),
        latest_source AS (
            SELECT student_id, MAX(evaluated_at) AS latest_at
            FROM score_source
            GROUP BY student_id
        ),
        snapshot_state AS (
            SELECT student_id, MIN(computed_at) AS computed_at
            FROM learner_weakness_snapshot
            GROUP BY student_id
        ),
        student_state AS (
            SELECT COALESCE(source.student_id, snapshot.student_id) AS student_id,
                   source.latest_at,
                   snapshot.computed_at
            FROM latest_source source
            FULL OUTER JOIN snapshot_state snapshot
              ON snapshot.student_id = source.student_id
        )
        SELECT student_id
        FROM student_state
        WHERE latest_at IS NULL
           OR computed_at IS NULL
           OR computed_at < latest_at
           OR computed_at < :staleBefore
        ORDER BY COALESCE(computed_at, TIMESTAMPTZ 'epoch'), student_id
        LIMIT :limit
        """, nativeQuery = true)
    List<UUID> findStudentsNeedingRefresh(
        @Param("staleBefore") Instant staleBefore,
        @Param("limit") int limit
    );
}
