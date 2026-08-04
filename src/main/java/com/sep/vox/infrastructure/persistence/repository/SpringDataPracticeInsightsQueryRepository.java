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
import com.sep.vox.application.query.dto.WeaknessEvidenceInfo;
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

    // Mẫu số của xu hướng là SỐ CƠ HỘI, không phải số ngày.
    //
    // Bản trước chia freq cho số NGÀY. Sai một cách nguy hiểm kể từ khi việc chọn câu nhắm
    // tiêu chí yếu nhất 50% số lượt (PracticeFocusInfo.criterionForSlot): luyện ngữ pháp
    // nhiều hơn thì đương nhiên quan sát lỗi ngữ pháp nhiều hơn, kể cả khi tỉ lệ sai TRÊN MỖI
    // CÂU đã giảm. Tức là càng tập trung sửa một điểm yếu, biểu đồ càng báo điểm yếu đó nặng
    // lên -- hệ thống tự phạt chính hành vi nó đang khuyến khích.
    //
    // Cơ hội = số lần tiêu chí đó thực sự được chấm trong cửa sổ (một dòng
    // practice_criterion_score = một lần em ấy có dịp mắc lỗi này). Chia cho nó thì con số trở
    // thành "tỉ lệ sai mỗi lần được hỏi" -- so được giữa hai cửa sổ bất kể luyện dày hay thưa.
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
        ),
        -- CHẶN SỐ NHÃN MỖI TIÊU CHÍ. Phát âm sinh ra một nhãn cho MỖI âm vị -- một buổi đã ra
        -- hơn chục nhãn, luyện nhiều tháng thì kín màn hình điện thoại và không còn đọc được
        -- gì. Giữ những nhãn ưu tiên cao nhất của từng tiêu chí; phần đuôi là nhiễu đo lường
        -- (Azure chấm âm vị khá nhạy) chứ không phải thứ đáng đem ra luyện.
        capped AS (
            SELECT ranked.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY ranked.criterion_code
                       ORDER BY ranked.priority DESC, ranked.freq DESC, ranked.sub_attribute
                   ) AS rank_in_criterion
            FROM ranked
        ),
        chances AS (
            SELECT pcs.rubric_criterion_id,
                   rubric.framework_criterion_id,
                   COUNT(*) FILTER (
                       WHERE pe.evaluated_at >= NOW() - MAKE_INTERVAL(days => :windowDays)
                   ) AS total,
                   COUNT(*) FILTER (
                       WHERE pe.evaluated_at >= NOW() - MAKE_INTERVAL(days => :recentDays)
                   ) AS recent
            FROM practice_criterion_score pcs
            JOIN rubric_criterions rubric ON rubric.id = pcs.rubric_criterion_id
            JOIN practice_item_evaluation pe ON pe.id = pcs.practice_evaluation_id
            JOIN practice_item_response pr ON pr.id = pe.practice_response_id
            JOIN practice_session ps ON ps.id = pr.practice_session_id
            WHERE ps.student_id = :studentId
              AND pe.marked_invalid = false
            GROUP BY pcs.rubric_criterion_id, rubric.framework_criterion_id
        )
        SELECT ranked.criterion_code AS criterionCode,
               ranked.sub_attribute AS subAttribute,
               ranked.freq AS occurrenceCount,
               -- Đây là THỨ TỰ ƯU TIÊN LUYỆN, không phải mức nặng của lỗi.
               --
               -- priority = tần suất x độ yếu của TIÊU CHÍ chứa nhãn đó. Nên một nhãn sai 5
               -- lần trong tiêu chí học sinh đang GIỎI sẽ xếp thấp hơn một nhãn sai 3 lần
               -- trong tiêu chí đang yếu -- đúng cho việc chọn luyện gì tiếp, nhưng đọc thành
               -- "sai 5 lần mà nhẹ" thì vô lý. Nhãn hiển thị đã đổi sang "ưu tiên".
               --
               -- Chốt sàn theo GIÁ TRỊ, không chỉ theo phân vị: priority <= 0 nghĩa là tiêu
               -- chí đó đang là điểm MẠNH, không đời nào được gán mức cao. Nếu chỉ dùng
               -- CUME_DIST thì luôn có ~1/3 số nhãn bị gán mức cao, kể cả khi học sinh giỏi đều.
               CASE
                   WHEN ranked.priority <= 0 THEN 'NHE'
                   WHEN ranked.percentile > 0.666666 THEN 'NANG'
                   WHEN ranked.percentile > 0.333333 THEN 'VUA'
                   ELSE 'NHE'
               END AS severity,
               ranked.practiceable AS practiceable,
               -- SẮP KHẮC PHỤC: từng lặp lại nhưng cửa sổ gần đây không còn. Con số này đã
               -- được đếm sẵn cho thẻ tổng (findWeaknessTrendCounts) nhưng chưa bao giờ xuống
               -- tới từng dòng -- học sinh thấy "2 sắp khắc phục" mà không biết là hai cái nào,
               -- còn dòng đó vẫn nằm đó trông như lỗi đang hoạt động.
               (ranked.freq > 0 AND ranked.recent_freq = 0) AS nearlyFixed,
               -- MỚI PHÁT HIỆN: mọi lần xuất hiện đều nằm trong cửa sổ gần đây.
               (ranked.recent_freq > 0 AND ranked.freq = ranked.recent_freq) AS newlyFound,
               -- NULL khi cửa sổ trước có quá ít lần, hoặc khi thiếu số cơ hội của một trong
               -- hai cửa sổ: học sinh luyện thưa thì một buổi cũng làm nhảy vài chục phần
               -- trăm, hiện ra là báo động giả. NULL cũng đúng cho lỗi MỚI phát hiện
               -- (freq = recent_freq) -- cái đó đã có ô đếm newlyFound lo, không cần bịa ↑100%.
               CASE
                   WHEN (ranked.freq - ranked.recent_freq) >= :minimumPriorOccurrences
                        AND COALESCE(chances.recent, 0) > 0
                        AND COALESCE(chances.total, 0) > COALESCE(chances.recent, 0)
                   THEN ROUND((
                            (ranked.recent_freq::numeric / chances.recent)
                          - ((ranked.freq - ranked.recent_freq)::numeric
                             / (chances.total - chances.recent))
                        ) / NULLIF((ranked.freq - ranked.recent_freq)::numeric
                             / (chances.total - chances.recent), 0) * 100)
                   ELSE NULL
               END AS trendPercent
        FROM capped AS ranked
        LEFT JOIN chances
          ON chances.framework_criterion_id = ranked.framework_criterion_id
        WHERE ranked.rank_in_criterion <= :maxPerCriterion
        ORDER BY ranked.priority DESC, ranked.sub_attribute
        """, nativeQuery = true)
    List<SubAttributeWeaknessInfo> findSubAttributeWeaknesses(
        @Param("studentId") UUID studentId,
        @Param("windowDays") int windowDays,
        @Param("recentDays") int recentDays,
        @Param("minimumPriorOccurrences") int minimumPriorOccurrences,
        @Param("maxPerCriterion") int maxPerCriterion
    );

    // Bằng chứng THẬT cho từng nhãn: chính từ/câu học sinh đã nói, đã lưu sẵn trong
    // weakness_observation.evidence_span ngay lúc suy ra nhãn đó.
    //
    // Kèm SỐ LẦN của từng bằng chứng, không chỉ liệt kê. Với lỗi phát âm thì đây là phần đáng
    // giá nhất: nhãn "/d/" đứng một mình thì học sinh không sửa được gì, nhưng "read x3,
    // daily x2" thì luyện được ngay. Và số lần theo TỪ cũng cho thấy đó là lỗi âm lặp qua
    // nhiều từ (luyện âm) hay chỉ vấp đúng một từ (học lại từ đó).
    //
    // Giới hạn :perLabel bằng chứng HAY GẶP NHẤT của TỪNG nhãn (ROW_NUMBER, không LIMIT toàn
    // cục): LIMIT chung thì nhãn xuất hiện nhiều sẽ nuốt hết chỗ của những nhãn còn lại.
    @Query(value = """
        WITH counted AS (
            -- UPPER: weakness_observation.criterion_code lưu chữ THƯỜNG ('pronunciation'),
            -- còn framework_criteria.code -- nguồn của criterionCode ở truy vấn nhãn -- là
            -- chữ HOA. Không chuẩn hoá thì khoá gộp hai bên không bao giờ khớp và danh sách
            -- bằng chứng luôn rỗng, trông hệt như "chưa có dữ liệu".
            SELECT UPPER(observation.criterion_code) AS criterion_code,
                   observation.sub_attribute,
                   observation.evidence_span,
                   COUNT(*) AS times,
                   MAX(observation.observed_at) AS last_seen
            FROM weakness_observation observation
            WHERE observation.student_id = :studentId
              AND observation.observed_at >= :since
              AND observation.evidence_span <> ''
              -- CHỈ phát âm. Với ngữ pháp/từ vựng/mạch lạc thì bản thân cái nhãn đã nói được
              -- phải sửa gì ("tense_control"), còn evidence_span là nguyên một câu dài -- đổ
              -- ra đây chỉ làm rối, mà màn Tổng kết buổi vốn đã hiện đầy đủ "em nói -> đúng
              -- phải là" kèm giải thích rồi.
              --
              -- Phát âm thì ngược lại: nhãn "/d/" đứng một mình vô nghĩa với người học, phải
              -- có từ mới luyện được. Danh sách 5 tiêu chí là cố định nên chốt cứng ở đây an
              -- toàn (cùng cách MeasuredBandMatchNode và SubAttributePolicy đang làm).
              AND UPPER(observation.criterion_code) = 'PRONUNCIATION'
            GROUP BY observation.criterion_code, observation.sub_attribute,
                     observation.evidence_span
        ),
        numbered AS (
            SELECT counted.*,
                   ROW_NUMBER() OVER (
                       PARTITION BY criterion_code, sub_attribute
                       ORDER BY times DESC, last_seen DESC
                   ) AS rank_in_label
            FROM counted
        )
        SELECT numbered.criterion_code AS criterionCode,
               numbered.sub_attribute AS subAttribute,
               numbered.evidence_span AS evidenceSpan,
               numbered.times::int AS times
        FROM numbered
        WHERE numbered.rank_in_label <= :perLabel
        ORDER BY numbered.criterion_code, numbered.sub_attribute, numbered.rank_in_label
        """, nativeQuery = true)
    List<WeaknessEvidenceInfo> findRecentEvidence(
        @Param("studentId") UUID studentId,
        @Param("since") Instant since,
        @Param("perLabel") int perLabel
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
        SELECT criterionCode, observedDate, latentLevel, source FROM (
            SELECT criterion.code AS criterionCode,
                   'EXAM' AS source,
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
                   'PRACTICE' AS source,
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
