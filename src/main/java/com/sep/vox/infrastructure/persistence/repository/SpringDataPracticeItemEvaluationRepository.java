package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.PracticeItemEvaluationJpaEntity;

public interface SpringDataPracticeItemEvaluationRepository
        extends JpaRepository<PracticeItemEvaluationJpaEntity, UUID> {

    Optional<PracticeItemEvaluationJpaEntity> findByPracticeResponseId(UUID practiceResponseId);

    @Query(value = """
        SELECT COUNT(*)::int
        FROM practice_item_response response
        JOIN practice_item_evaluation evaluation
          ON evaluation.practice_response_id = response.id
        WHERE response.practice_session_id = :sessionId
          AND evaluation.marked_invalid = false
        """, nativeQuery = true)
    int countCompletedBySessionId(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT evaluation.item_score / 100.0
        FROM practice_item_response response
        JOIN practice_item_evaluation evaluation
          ON evaluation.practice_response_id = response.id
        WHERE response.practice_session_id = :sessionId
          AND response.question_complete = true
          AND evaluation.marked_invalid = false
          AND evaluation.item_score IS NOT NULL
        ORDER BY evaluation.evaluated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Double findLastValidNormalizedScore(@Param("sessionId") UUID sessionId);

    // question_complete = true: câu học sinh bỏ dở giữa chừng (rớt mạng, đóng app) VẪN được
    // chấm -- để lấy quan sát điểm yếu, xem PracticeGradingFlushService -- nhưng KHÔNG kéo
    // điểm phiên xuống. Chấm một câu trả lời dở dang theo rubric của câu đầy đủ thì chắc chắn
    // thấp, và phạt học sinh vì mất mạng là sai. Tín hiệu thì giữ, điểm thì không tính.
    @Query(value = """
        SELECT AVG(evaluation.item_score)
        FROM practice_item_response response
        JOIN practice_item_evaluation evaluation
          ON evaluation.practice_response_id = response.id
        WHERE response.practice_session_id = :sessionId
          AND response.question_complete = true
          AND evaluation.marked_invalid = false
        """, nativeQuery = true)
    BigDecimal findAverageItemScoreBySessionId(@Param("sessionId") UUID sessionId);

    // Dùng để tính EMA (studentRankSignal.performance), không phải trung bình cộng cửa sổ
    // cứng -- mọi điểm góp phần ngay từ điểm đầu tiên, điểm gần đây có trọng số cao hơn tự
    // nhiên qua công thức đệ quy (0.3*score + 0.7*prev), cùng cách InterestVectorService đang
    // làm cho interest score.
    //
    // LIMIT 20 (chỉ 20 điểm GẦN NHẤT, subquery DESC rồi mới ORDER BY ASC ở ngoài để fold đúng
    // chiều thời gian) không phải một cách rút gọn thô -- với alpha=0.3, ảnh hưởng của một điểm
    // suy giảm theo 0.7^k mỗi bước; sau 20 bước 0.7^20 ~ 0.0008, tức phần lịch sử xa hơn 20 điểm
    // đã suy giảm gần bằng 0 trong công thức, có giữ lại toàn bộ lịch sử (không LIMIT) hay chỉ
    // 20 điểm gần nhất thì kết quả EMA gần như giống hệt nhau -- nên "gọn" (compact) sẵn trong
    // chính công thức đệ quy, không cần một bảng lưu trạng thái riêng để tránh quét lại lịch sử.
    @Query(value = """
        SELECT sub.normalized_score
        FROM (
            SELECT evaluation.item_score / 100.0 AS normalized_score,
                   evaluation.evaluated_at AS evaluated_at
            FROM practice_item_evaluation evaluation
            JOIN practice_item_response response
              ON response.id = evaluation.practice_response_id
            JOIN practice_session session
              ON session.id = response.practice_session_id
            WHERE session.student_id = :studentId
              AND response.question_complete = true
              AND evaluation.marked_invalid = false
              AND evaluation.item_score IS NOT NULL
            ORDER BY evaluation.evaluated_at DESC
            LIMIT 20
        ) sub
        ORDER BY sub.evaluated_at ASC
        """, nativeQuery = true)
    List<Double> findNormalizedScoresChronological(@Param("studentId") UUID studentId);
}
