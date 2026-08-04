package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.sep.vox.infrastructure.persistence.entity.FrameworkResultBandJpaEntity;
import com.sep.vox.infrastructure.persistence.entity.LearnerProfileJpaEntity;

public interface SpringDataLearnerProfileRepository
        extends JpaRepository<LearnerProfileJpaEntity, UUID> {

    Optional<LearnerProfileJpaEntity> findTopByStudentIdOrderByVersionDesc(UUID studentId);

    /** Khoá FOR SHARE bản mới nhất trước khi nối thêm version -- tránh hai request cùng ghi đè nhau. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<LearnerProfileJpaEntity> findTopWithLockByStudentIdOrderByVersionDesc(UUID studentId);

    /**
     * Bậc ước lượng = bậc xuất hiện nhiều nhất trong lịch sử chấm, gộp CẢ bài thi lẫn bài luyện.
     *
     * <p>Trước đây chỉ đọc bảng thi, nên học sinh chỉ luyện thì mãi không có bậc nào -- tức
     * luyện tập không hề theo dõi được tiến bộ. Nay UNION thêm nhánh practice theo đúng đường
     * join mà SpringDataWeaknessScoreObservationViewRepository đã dùng.
     *
     * <p><b>Điểm thi nặng gấp 3 điểm luyện</b> (cột {@code weight}). Lý do: số lượt luyện luôn
     * áp đảo số lần thi, để ngang nhau thì thi thành vô nghĩa. Nặng hơn nữa cũng không nên --
     * học sinh chỉ luyện vẫn phải ra được bậc. Ngưỡng {@code total >= 5} vẫn đếm THÔ (không
     * nhân trọng số) để người chỉ luyện đủ 5 lượt là có bậc.
     */
    @Query(value = """
        WITH raw AS (
            SELECT band.result_band_order,
                   3 AS weight
            FROM exam_item_criterion_scores score
            JOIN rubric_criterions rubric
              ON rubric.id = score.rubric_criterion_id
            JOIN framework_criteria criterion
              ON criterion.id = rubric.framework_criterion_id
            JOIN framework_result_bands band
              ON band.framework_version_id = criterion.framework_version_id
             AND band.code = score.matched_band_code
            JOIN exam_item_evaluations evaluation
              ON evaluation.id = score.evaluation_id
            JOIN exam_item_responses response
              ON response.id = evaluation.response_id
            JOIN exam_sessions session
              ON session.id = response.session_id
            JOIN exam_candidates candidate
              ON candidate.id = session.candidate_id
            WHERE candidate.student_id = :studentId
              AND candidate.blocked_at IS NULL
              AND evaluation.marked_invalid = false
              AND score.matched_band_code IS NOT NULL
            UNION ALL
            SELECT band.result_band_order,
                   1 AS weight
            FROM practice_criterion_score pcs
            JOIN rubric_criterions rubric
              ON rubric.id = pcs.rubric_criterion_id
            JOIN framework_criteria criterion
              ON criterion.id = rubric.framework_criterion_id
            JOIN framework_result_bands band
              ON band.framework_version_id = criterion.framework_version_id
             AND band.code = pcs.matched_band_code
            JOIN practice_item_evaluation pe
              ON pe.id = pcs.practice_evaluation_id
            JOIN practice_item_response pr
              ON pr.id = pe.practice_response_id
            JOIN practice_session ps
              ON ps.id = pr.practice_session_id
            WHERE ps.student_id = :studentId
              AND pe.marked_invalid = false
              AND pcs.matched_band_code IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM practice_response_turn pt
                  WHERE pt.practice_response_id = pr.id
              )
        ),
        observations AS (
            SELECT result_band_order,
                   weight,
                   COUNT(*) OVER () AS total
            FROM raw
        )
        SELECT result_band_order
        FROM observations
        WHERE total >= 5
        GROUP BY result_band_order
        ORDER BY SUM(weight) DESC, result_band_order DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findEstimatedResultBandOrder(@Param("studentId") UUID studentId);


    @Query(value = """
        SELECT band.result_band_order
        FROM school_class_users membership
        JOIN school_classes class
          ON class.id = membership.school_class_id
        JOIN assessment_policies policy
          ON policy.status = 'PUBLISHED'
         AND policy.effective_from <= CURRENT_TIMESTAMP
         AND (
             policy.effective_to IS NULL
             OR policy.effective_to >= CURRENT_TIMESTAMP
         )
         AND (
             policy.school_id IS NULL
             OR policy.school_id = class.school_id
         )
         AND (
             policy.school_grade_id IS NULL
             OR policy.school_grade_id = class.school_grade_id
         )
         AND (
             policy.school_class_id IS NULL
             OR policy.school_class_id = class.id
         )
        JOIN framework_result_bands band
          ON band.id = policy.target_framework_band_id
        WHERE membership.user_id = :studentId
          AND membership.is_active = true
        ORDER BY (policy.school_class_id IS NOT NULL) DESC,
                 (policy.school_grade_id IS NOT NULL) DESC,
                 (policy.school_id IS NOT NULL) DESC,
                 policy.version DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findPolicyTargetBandOrder(@Param("studentId") UUID studentId);

    /**
     * Số bậc của thang năng lực đang áp cho học sinh này (= bậc cao nhất trong framework
     * version mà assessment policy đang trỏ tới).
     *
     * Cần vì trước đây code giả định cứng thang 6 bậc kiểu VSTEP (`Math.min(6, ...)` rải rác).
     * Đổi trường sang CEFR/IELTS thì con số 6 đó sai mà KHÔNG nổ -- chỉ lặng lẽ kẹp trần sai.
     * Đọc từ dữ liệu thay vì đoán.
     *
     * Dùng cùng bộ điều kiện chọn policy như {@link #findPolicyTargetBandOrder} (ưu tiên
     * lớp > khối > trường, rồi version mới nhất) để hai hàm luôn nói về cùng một framework.
     */
    @Query(value = """
        SELECT MAX(band.result_band_order)
        FROM school_class_users membership
        JOIN school_classes class
          ON class.id = membership.school_class_id
        JOIN assessment_policies policy
          ON policy.status = 'PUBLISHED'
         AND policy.effective_from <= CURRENT_TIMESTAMP
         AND (
             policy.effective_to IS NULL
             OR policy.effective_to >= CURRENT_TIMESTAMP
         )
         AND (
             policy.school_id IS NULL
             OR policy.school_id = class.school_id
         )
         AND (
             policy.school_grade_id IS NULL
             OR policy.school_grade_id = class.school_grade_id
         )
         AND (
             policy.school_class_id IS NULL
             OR policy.school_class_id = class.id
         )
        JOIN framework_result_bands band
          ON band.framework_version_id = policy.framework_version_id
        WHERE membership.user_id = :studentId
          AND membership.is_active = true
        GROUP BY policy.id, policy.school_class_id, policy.school_grade_id,
                 policy.school_id, policy.version
        ORDER BY (policy.school_class_id IS NOT NULL) DESC,
                 (policy.school_grade_id IS NOT NULL) DESC,
                 (policy.school_id IS NOT NULL) DESC,
                 policy.version DESC
        LIMIT 1
        """, nativeQuery = true)
    List<Integer> findFrameworkBandCount(@Param("studentId") UUID studentId);

    /**
     * Toàn bộ thang bậc (thứ tự, mã, mô tả) của framework đang áp cho học sinh -- gửi xuống
     * Python để dựng ladder mô tả bậc trong prompt chấm câu hỏi, thay cho hằng số viết cứng
     * `BAC_1..BAC_6` bằng tiếng Anh vốn khoá hệ thống vào VSTEP.
     *
     * Không LIMIT 1 như hai hàm trên vì cần cả thang; policy được chọn bằng subquery theo đúng
     * thứ tự ưu tiên lớp > khối > trường để nhất quán với chúng.
     *
     * Trả thẳng FrameworkResultBandJpaEntity (SELECT band.*) thay vì một projection riêng: bậc
     * thang đã có sẵn model/entity/mapper ở gói framework, dựng thêm kiểu riêng chỉ để bớt vài
     * cột là nhân đôi khái niệm. Query phải nằm ở đây (không gọi FrameworkResultBandRepository)
     * vì luồng này chỉ có studentId, và một adapter chỉ được phép dùng đúng một repo.
     */
    @Query(value = """
        SELECT band.*
        FROM framework_result_bands band
        WHERE band.framework_version_id = (
            SELECT policy.framework_version_id
            FROM school_class_users membership
            JOIN school_classes class
              ON class.id = membership.school_class_id
            JOIN assessment_policies policy
              ON policy.status = 'PUBLISHED'
             AND policy.effective_from <= CURRENT_TIMESTAMP
             AND (
                 policy.effective_to IS NULL
                 OR policy.effective_to >= CURRENT_TIMESTAMP
             )
             AND (
                 policy.school_id IS NULL
                 OR policy.school_id = class.school_id
             )
             AND (
                 policy.school_grade_id IS NULL
                 OR policy.school_grade_id = class.school_grade_id
             )
             AND (
                 policy.school_class_id IS NULL
                 OR policy.school_class_id = class.id
             )
            WHERE membership.user_id = :studentId
              AND membership.is_active = true
            ORDER BY (policy.school_class_id IS NOT NULL) DESC,
                     (policy.school_grade_id IS NOT NULL) DESC,
                     (policy.school_id IS NOT NULL) DESC,
                     policy.version DESC
            LIMIT 1
        )
        ORDER BY band.result_band_order
        """, nativeQuery = true)
    List<FrameworkResultBandJpaEntity> findFrameworkBandLadder(@Param("studentId") UUID studentId);
}
