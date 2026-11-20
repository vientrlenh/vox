package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.PracticeResponseTurnJpaEntity;

public interface SpringDataPracticeResponseTurnRepository
        extends JpaRepository<PracticeResponseTurnJpaEntity, UUID> {

    List<PracticeResponseTurnJpaEntity> findByPracticeResponseIdOrderByTurnOrder(UUID practiceResponseId);

    // Idempotency cho retry: Python thử lại submit_turn tới 3 lần khi HTTP response bị mất dù
    // lần đầu đã commit (xem practice_session_client.py) -- (practice_response_id, turn_order)
    // là unique constraint, nên tra trước để trả lại đúng id cũ thay vì insert trùng và vỡ
    // ràng buộc.
    Optional<PracticeResponseTurnJpaEntity> findByPracticeResponseIdAndTurnOrder(
        UUID practiceResponseId, int turnOrder);

    @Query(value = """
        SELECT turn.*
        FROM practice_response_turns turn
        JOIN practice_item_responses response
          ON response.id = turn.practice_response_id
        WHERE response.practice_session_id = :sessionId
        ORDER BY turn.turn_order
        """, nativeQuery = true)
    List<PracticeResponseTurnJpaEntity> findBySessionIdOrderByTurnOrder(@Param("sessionId") UUID sessionId);

    @Query(value = """
        -- Còn bao nhiêu giây nữa mới chạm TRẦN của câu này, tính trên tổng mọi lượt (câu chính
        -- lẫn follow-up) -- nên trần phải là max_response_seconds đứng một mình, không cộng sàn.
        SELECT GREATEST(
            0,
            question.max_response_seconds
            - COALESCE(SUM(turn.duration_seconds), 0)
        )::int
        FROM practice_questions question
        LEFT JOIN practice_item_responses response
          ON response.practice_question_id = question.id
         AND response.practice_session_id = :sessionId
        LEFT JOIN practice_response_turns turn
          ON turn.practice_response_id = response.id
        WHERE question.id = :questionId
        GROUP BY question.id
        """, nativeQuery = true)
    Integer findRemainingQuestionSeconds(
        @Param("sessionId") UUID sessionId,
        @Param("questionId") UUID questionId
    );

    // Không viết được JPQL constructor-expression như ExamItemResponseRepository (PracticeResponseTurnJpaEntity
    // không có association JPA tới PracticeItemResponseJpaEntity) -- native SQL + Object[], mirror cách
    // JpaSchoolAiCostQueryRepository map row native query.
    @Query(value = """
        SELECT response.practice_session_id AS session_id, COALESCE(SUM(turn.duration_seconds), 0) AS total_seconds
        FROM practice_response_turns turn
        JOIN practice_item_responses response
          ON response.id = turn.practice_response_id
        WHERE response.practice_session_id IN :sessionIds
        GROUP BY response.practice_session_id
        """, nativeQuery = true)
    List<Object[]> sumDurationSecondsGroupedBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);
}
