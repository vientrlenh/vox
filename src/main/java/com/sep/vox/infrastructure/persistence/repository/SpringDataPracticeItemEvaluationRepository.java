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
          AND evaluation.marked_invalid = false
          AND evaluation.item_score IS NOT NULL
        ORDER BY evaluation.evaluated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Double findLastValidNormalizedScore(@Param("sessionId") UUID sessionId);

    // KHÔNG lọc question_complete nữa (2026-08-07).
    //
    // Luật cũ: câu bỏ dở vẫn được chấm để lấy quan sát điểm yếu, nhưng không tính vào điểm
    // phiên -- "chấm một câu dở dang theo rubric của câu đầy đủ thì chắc chắn thấp, phạt học
    // sinh vì mất mạng là sai".
    //
    // Hai lý do bỏ:
    //   1. Hồ sơ điểm yếu đã gỡ 2026-08-06, nên vế "vẫn chấm để lấy quan sát" không còn đích
    //      nào. Giờ chấm xong rồi vứt.
    //   2. Giả định "chắc chắn thấp" không đúng. Đo trên phiên thật 2026-08-07: câu bỏ dở
    //      (2 lượt, 26 giây) được 80,07 -- CAO HƠN cả hai câu hoàn tất (72,56 và 73,97).
    //
    // Và khi câu bỏ dở là câu DUY NHẤT, loại nó không bảo vệ được gì: AVG trên tập rỗng ra
    // NULL, cả phiên hiện 0 điểm. Bảo vệ khỏi bị kéo xuống mà thành xoá trắng.
    //
    // marked_invalid mới là cờ đúng vai cho "có đáng tính không" -- ValidityNode đã hạ cờ đó
    // cho câu quá ngắn hoặc lạc đề.
    @Query(value = """
        SELECT AVG(evaluation.item_score)
        FROM practice_item_response response
        JOIN practice_item_evaluation evaluation
          ON evaluation.practice_response_id = response.id
        WHERE response.practice_session_id = :sessionId
          AND evaluation.marked_invalid = false
        """, nativeQuery = true)
    BigDecimal findAverageItemScoreBySessionId(@Param("sessionId") UUID sessionId);
}
