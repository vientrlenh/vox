package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeResponseTurnJpaEntity;

public interface SpringDataPracticeResponseTurnRepository
        extends JpaRepository<PracticeResponseTurnJpaEntity, UUID> {

    List<PracticeResponseTurnJpaEntity> findByPracticeResponseIdOrderByTurnOrder(UUID practiceResponseId);

    // Idempotency for retries: Python retries submit_turn up to 3 times when the HTTP response is lost even though
    // the first attempt already committed (see practice_session_client.py) -- (practice_response_id, turn_order)
    // is a unique constraint, so look up first to return the original id instead of inserting a duplicate and breaking
    // the constraint.
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
        -- How many seconds remain before hitting the CEILING of this question, counted over all turns (main question
        -- and follow-ups) -- so the ceiling must be max_response_seconds on its own, without the floor added.
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

    // A JPQL constructor expression like AssessmentItemResponseRepository cannot be written (PracticeResponseTurnJpaEntity
    // has no JPA association to PracticeItemResponseJpaEntity) -- native SQL + Object[], mirroring how
    // JpaSchoolAiCostQueryRepository maps rows of a native query.
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
