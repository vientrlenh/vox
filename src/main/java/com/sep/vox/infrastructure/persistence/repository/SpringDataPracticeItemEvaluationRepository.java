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
}
