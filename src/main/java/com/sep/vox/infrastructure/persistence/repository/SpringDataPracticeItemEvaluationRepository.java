package com.sep.vox.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeItemEvaluationJpaEntity;

public interface SpringDataPracticeItemEvaluationRepository
        extends JpaRepository<PracticeItemEvaluationJpaEntity, UUID> {

    Optional<PracticeItemEvaluationJpaEntity> findByPracticeResponseId(UUID practiceResponseId);

    @Query(value = """
        SELECT COUNT(*)::int
        FROM practice_item_responses response
        JOIN practice_item_evaluations evaluation
          ON evaluation.practice_response_id = response.id
        WHERE response.practice_session_id = :sessionId
          AND evaluation.marked_invalid = false
        """, nativeQuery = true)
    int countCompletedBySessionId(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT evaluation.item_score / 100.0
        FROM practice_item_responses response
        JOIN practice_item_evaluations evaluation
          ON evaluation.practice_response_id = response.id
        WHERE response.practice_session_id = :sessionId
          AND evaluation.marked_invalid = false
          AND evaluation.item_score IS NOT NULL
        ORDER BY evaluation.evaluated_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Double findLastValidNormalizedScore(@Param("sessionId") UUID sessionId);

    // Does NOT filter question_complete any more (2026-08-07).
    //
    // Old rule: an abandoned question was still graded to collect weakness observations, but was not counted in the
    // session score -- "grading an unfinished question by the rubric of a complete one is certain to be low, penalizing the student
    // for losing connection is wrong".
    //
    // Two reasons for dropping it:
    //   1. The weakness profile was removed on 2026-08-06, so the "still grade to collect observations" side has no
    //      destination any more. Now it is graded and then thrown away.
    //   2. The "certain to be low" assumption is wrong. Measured on a real session on 2026-08-07: an abandoned
    //      question (2 turns, 26 seconds) scored 80.07 -- HIGHER than both completed questions (72.56 and 73.97).
    //
    // And when the abandoned question is the ONLY one, excluding it protects nothing: AVG over an empty set gives
    // NULL and the whole session shows 0 points. Protecting against being pulled down turns into wiping everything out.
    //
    // marked_invalid is the flag with the right role for "is it worth counting" -- ValidityNode has already raised that flag
    // for answers that are too short or off topic.
    @Query(value = """
        SELECT AVG(evaluation.item_score)
        FROM practice_item_responses response
        JOIN practice_item_evaluations evaluation
          ON evaluation.practice_response_id = response.id
        WHERE response.practice_session_id = :sessionId
          AND evaluation.marked_invalid = false
        """, nativeQuery = true)
    BigDecimal findAverageItemScoreBySessionId(@Param("sessionId") UUID sessionId);
}
