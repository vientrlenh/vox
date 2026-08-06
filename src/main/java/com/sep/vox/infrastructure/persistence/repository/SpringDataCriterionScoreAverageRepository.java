package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.PracticeSessionJpaEntity;

/**
 * Repository chỉ-đọc gộp điểm thi + điểm luyện tập theo tiêu chí -- không có entity chủ, dùng
 * marker {@link Repository} thay vì {@code JpaRepository} vì không cần CRUD chuẩn.
 */
public interface SpringDataCriterionScoreAverageRepository
        extends Repository<PracticeSessionJpaEntity, UUID> {

    /**
     * Phần {@code UNION ALL} bên dưới bê nguyên từ
     * {@code SpringDataWeaknessScoreObservationViewRepository.findAllValidScoreObservations}
     * (đã xoá), giữ nguyên các điều kiện hợp lệ đã cân nhắc từ trước:
     *
     * <ul>
     *   <li>{@code marked_invalid = false} -- bài đã bị đánh dấu vô hiệu không được tính</li>
     *   <li>{@code blocked_at IS NULL} -- thí sinh bị chặn thì kết quả không dùng</li>
     *   <li>{@code EXISTS (... turns)} -- đánh giá không gắn với lượt nói nào là bản ghi rỗng,
     *       tính vào sẽ kéo trung bình xuống bằng một con số không đại diện cho ai</li>
     * </ul>
     *
     * <p>Khác một điểm quyết định: query cũ KHÔNG lọc học sinh (quét cả hệ thống rồi mới gom
     * trong bộ nhớ, phục vụ việc căn giữa theo lớp). Ở đây có {@code :studentId} trong cả hai
     * vế union, nên chỉ số hàng đọc tỉ lệ với một học sinh chứ không với cả trường.
     *
     * <p>Chuẩn hoá về 0..1 theo dải của chính tiêu chí ({@code min_score}/{@code max_score} trên
     * {@code rubric_criterions}) trước khi lấy trung bình: không chuẩn hoá thì tiêu chí thang
     * 0-10 luôn trông "khá hơn" tiêu chí thang 0-5, và thứ tự yếu-nhất-trước sẽ sai. Các cột đều
     * là {@code numeric} nên đây là phép chia thực, không phải chia nguyên.
     *
     * <p>{@code ORDER BY} có thêm {@code criterion_code} làm chốt phụ để hai tiêu chí bằng điểm
     * nhau vẫn ra thứ tự ổn định giữa các lần gọi -- chu kỳ xoay vòng ô dựa vào thứ tự này, đổi
     * ngẫu nhiên thì cùng một học sinh ở cùng một ô lại ra tiêu chí khác nhau.
     */
    @Query(value = """
        WITH scores AS (
            SELECT fc.code AS criterion_code,
                   (cs.final_score - rc.min_score)
                       / NULLIF(rc.max_score - rc.min_score, 0) AS normalized
            FROM exam_item_criterion_scores cs
            JOIN rubric_criterions rc ON rc.id = cs.rubric_criterion_id
            JOIN framework_criteria fc ON fc.id = rc.framework_criterion_id
            JOIN exam_item_evaluations e ON e.id = cs.evaluation_id
            JOIN exam_item_responses r ON r.id = e.response_id
            JOIN exam_sessions s ON s.id = r.session_id
            JOIN exam_candidates ec ON ec.id = s.candidate_id
            WHERE ec.student_id = :studentId
              AND e.marked_invalid = false
              AND ec.blocked_at IS NULL
              AND cs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM exam_item_evaluation_turns t
                  WHERE t.evaluation_id = e.id
              )
            UNION ALL
            SELECT fc.code AS criterion_code,
                   (pcs.final_score - rc.min_score)
                       / NULLIF(rc.max_score - rc.min_score, 0) AS normalized
            FROM practice_criterion_score pcs
            JOIN rubric_criterions rc ON rc.id = pcs.rubric_criterion_id
            JOIN framework_criteria fc ON fc.id = rc.framework_criterion_id
            JOIN practice_item_evaluation pe ON pe.id = pcs.practice_evaluation_id
            JOIN practice_item_response pr ON pr.id = pe.practice_response_id
            JOIN practice_session ps ON ps.id = pr.practice_session_id
            WHERE ps.student_id = :studentId
              AND pe.marked_invalid = false
              AND pcs.final_score IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM practice_response_turn pt
                  WHERE pt.practice_response_id = pr.id
              )
        )
        SELECT criterion_code
        FROM scores
        WHERE normalized IS NOT NULL
        GROUP BY criterion_code
        ORDER BY AVG(normalized), criterion_code
        """, nativeQuery = true)
    List<String> findCriterionCodesOrderedByLowestAverageScore(@Param("studentId") UUID studentId);
}
