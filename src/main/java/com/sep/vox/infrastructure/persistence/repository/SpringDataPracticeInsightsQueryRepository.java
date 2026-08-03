package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sep.vox.application.query.dto.ClassPracticeRowInfo;
import com.sep.vox.application.query.dto.CriterionProgressInfo;
import com.sep.vox.application.query.dto.CriterionWeaknessInfo;
import com.sep.vox.application.query.dto.SubAttributeWeaknessInfo;
import com.sep.vox.application.query.dto.WeaknessTrendCountsInfo;
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

    @Query(value = """
        WITH ranked AS (
            SELECT priority.*,
                   criterion.code AS criterion_code,
                   CUME_DIST() OVER (
                       PARTITION BY priority.student_id
                       ORDER BY priority.priority
                   ) AS percentile
            FROM sub_attribute_priority priority
            JOIN framework_criteria criterion
              ON criterion.id = priority.framework_criterion_id
            WHERE priority.student_id = :studentId
        )
        SELECT ranked.criterion_code AS criterionCode,
               ranked.sub_attribute AS subAttribute,
               ranked.freq AS occurrenceCount,
               CASE
                   WHEN ranked.percentile > 0.666666 THEN 'NANG'
                   WHEN ranked.percentile > 0.333333 THEN 'VUA'
                   ELSE 'NHE'
               END AS severity,
               ranked.practiceable AS practiceable,
               -- Xu hướng: so NHỊP ĐỘ hai cửa sổ, không so số đếm trần. Cửa sổ gần đây
               -- ngắn hơn nên đương nhiên ít lần hơn -- so thẳng thì lỗi nào cũng trông
               -- như đang tốt lên. Chia cho số ngày mới cho ra thứ so được với nhau.
               --
               -- NULL khi cửa sổ trước có quá ít lần: học sinh luyện thưa thì một buổi
               -- cũng làm nhảy vài chục phần trăm, hiện ra là báo động giả. NULL cũng
               -- đúng cho lỗi MỚI phát hiện (freq = recent_freq, mẫu số 0) -- cái đó đã
               -- có ô đếm newlyFound lo, không cần bịa ra ↑100%.
               CASE
                   WHEN (ranked.freq - ranked.recent_freq) >= :minimumPriorOccurrences
                        AND :windowDays > :recentDays
                   THEN ROUND((
                            (ranked.recent_freq::numeric / :recentDays)
                          - ((ranked.freq - ranked.recent_freq)::numeric
                             / (:windowDays - :recentDays))
                        ) / ((ranked.freq - ranked.recent_freq)::numeric
                             / (:windowDays - :recentDays)) * 100)
                   ELSE NULL
               END AS trendPercent
        FROM ranked
        ORDER BY ranked.priority DESC, ranked.sub_attribute
        """, nativeQuery = true)
    List<SubAttributeWeaknessInfo> findSubAttributeWeaknesses(
        @Param("studentId") UUID studentId,
        @Param("windowDays") int windowDays,
        @Param("recentDays") int recentDays,
        @Param("minimumPriorOccurrences") int minimumPriorOccurrences
    );

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

    // nearlyFixed: từng lặp lại đủ nhiều (freq) nhưng gần đây không còn xuất hiện (recent_freq
    // = 0). newlyFound: mọi lần xuất hiện đều nằm trong cửa sổ gần đây (freq = recent_freq, tức
    // 100% mới) -- cả freq lẫn recent_freq đã được WeaknessVectorCalculator tính & lưu sẵn trong
    // sub_attribute_priority, không cần tính lại gì thêm.
    @Query(value = """
        SELECT
            COUNT(*) FILTER (WHERE freq > 0 AND recent_freq = 0) AS nearlyFixed,
            COUNT(*) FILTER (WHERE recent_freq > 0 AND freq = recent_freq) AS newlyFound
        FROM sub_attribute_priority
        WHERE student_id = :studentId
        """, nativeQuery = true)
    WeaknessTrendCountsInfo findWeaknessTrendCounts(@Param("studentId") UUID studentId);

    /**
     * Đường tiến bộ theo tiêu chí, gộp CẢ bài thi lẫn bài luyện.
     *
     * <p>Trước đây chỉ đọc bảng thi nên học sinh chỉ luyện thì màn Tiến bộ luôn trống -- luyện
     * mà không thấy mình khá lên hay không. Nay UNION thêm nhánh practice.
     *
     * <p>{@code latentLevel} là mức năng lực liên tục: bậc đạt được (0-based) cộng phần trăm
     * điểm trong bậc đó, nên hai lần chấm cùng bậc vẫn phân biệt được cao thấp. Công thức dùng
     * chung cho cả hai nguồn vì practice_criterion_score cũng có matched_band_code.
     *
     * <p>⚠️ Điểm luyện có thiên lệch cần biết khi đọc biểu đồ: độ khó câu luyện được nhắm
     * THEO bậc hiện tại của học sinh, nên đường luyện có xu hướng phẳng hơn đường thi ngay cả
     * khi trình độ đang lên -- lên trình thì câu cũng khó lên theo.
     */
    @Query(value = """
        SELECT criterionCode, observedDate, latentLevel FROM (
            SELECT criterion.code AS criterionCode,
                   evaluation.evaluated_at::date::text AS observedDate,
                   (band.result_band_order - 1)
                       + ((score.final_score - rubric.min_score)
                          / NULLIF(rubric.max_score - rubric.min_score, 0)) AS latentLevel,
                   evaluation.evaluated_at AS sortAt,
                   criterion.criteria_order AS sortOrder
            FROM exam_item_criterion_scores score
            JOIN rubric_criterions rubric ON rubric.id = score.rubric_criterion_id
            JOIN framework_criteria criterion ON criterion.id = rubric.framework_criterion_id
            JOIN exam_item_evaluations evaluation ON evaluation.id = score.evaluation_id
            JOIN exam_item_responses response ON response.id = evaluation.response_id
            JOIN exam_sessions session ON session.id = response.session_id
            JOIN exam_candidates candidate ON candidate.id = session.candidate_id
            JOIN framework_result_bands band
              ON band.framework_version_id = criterion.framework_version_id
             AND band.code = score.matched_band_code
            WHERE candidate.student_id = :studentId
              AND candidate.blocked_at IS NULL
              AND evaluation.marked_invalid = false
              AND score.final_score IS NOT NULL
              AND evaluation.evaluated_at >= :since
              AND (:criterionCode IS NULL OR UPPER(criterion.code) = UPPER(:criterionCode))
            UNION ALL
            SELECT criterion.code AS criterionCode,
                   pe.evaluated_at::date::text AS observedDate,
                   (band.result_band_order - 1)
                       + ((pcs.final_score - rubric.min_score)
                          / NULLIF(rubric.max_score - rubric.min_score, 0)) AS latentLevel,
                   pe.evaluated_at AS sortAt,
                   criterion.criteria_order AS sortOrder
            FROM practice_criterion_score pcs
            JOIN rubric_criterions rubric ON rubric.id = pcs.rubric_criterion_id
            JOIN framework_criteria criterion ON criterion.id = rubric.framework_criterion_id
            JOIN practice_item_evaluation pe ON pe.id = pcs.practice_evaluation_id
            JOIN practice_item_response pr ON pr.id = pe.practice_response_id
            JOIN practice_session ps ON ps.id = pr.practice_session_id
            JOIN framework_result_bands band
              ON band.framework_version_id = criterion.framework_version_id
             AND band.code = pcs.matched_band_code
            WHERE ps.student_id = :studentId
              AND pe.marked_invalid = false
              AND pcs.final_score IS NOT NULL
              AND pe.evaluated_at >= :since
              AND (:criterionCode IS NULL OR UPPER(criterion.code) = UPPER(:criterionCode))
              AND EXISTS (
                  SELECT 1
                  FROM practice_response_turn pt
                  WHERE pt.practice_response_id = pr.id
              )
        ) merged
        ORDER BY sortAt, sortOrder
        """, nativeQuery = true)
    List<CriterionProgressInfo> findProgress(
        @Param("studentId") UUID studentId,
        @Param("since") Instant since,
        @Param("criterionCode") String criterionCode
    );

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM school_class_users teacher
            JOIN school_class_users student
              ON student.school_class_id = teacher.school_class_id
             AND student.user_id = :studentId
             AND student.is_active = true
            WHERE teacher.user_id = :teacherId
              AND teacher.is_active = true
        )
        """, nativeQuery = true)
    boolean canTeacherReadStudent(@Param("teacherId") UUID teacherId, @Param("studentId") UUID studentId);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM school_class_users
            WHERE user_id = :teacherId
              AND school_class_id = :classId
              AND is_active = true
        )
        """, nativeQuery = true)
    boolean canTeacherReadClass(@Param("teacherId") UUID teacherId, @Param("classId") UUID classId);

    @Query(value = """
        SELECT student.user_id AS studentId,
               account.full_name AS fullName,
               (
                   SELECT criterion.code
                   FROM learner_weakness_snapshot snapshot
                   JOIN framework_criteria criterion
                     ON criterion.id = snapshot.framework_criterion_id
                   WHERE snapshot.student_id = student.user_id
                     AND snapshot.observation_count >= 3
                   ORDER BY snapshot.weakness DESC, criterion.criteria_order
                   LIMIT 1
               ) AS weakestCriterionCode
        FROM school_class_users student
        JOIN users account ON account.id = student.user_id
        WHERE student.school_class_id = :classId
          AND student.is_active = true
        ORDER BY account.full_name, student.user_id
        """, nativeQuery = true)
    List<ClassPracticeRowInfo> findClassOverviewRows(@Param("classId") UUID classId);
}
