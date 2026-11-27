package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.domain.repository.PendingEvaluationResponse;
import com.sep.vox.infrastructure.persistence.entity.personalization.PracticeItemResponseJpaEntity;

public interface SpringDataPracticeItemResponseRepository
        extends JpaRepository<PracticeItemResponseJpaEntity, UUID> {

    Optional<PracticeItemResponseJpaEntity> findByPracticeSessionIdAndPracticeQuestionId(
        UUID practiceSessionId,
        UUID practiceQuestionId
    );

    @Query(value = """
        SELECT session.rubric_version_id
        FROM practice_item_responses response
        JOIN practice_sessions session ON session.id = response.practice_session_id
        WHERE response.id = :responseId
        """, nativeQuery = true)
    UUID findRubricVersionIdByResponseId(@Param("responseId") UUID responseId);

    @Query(value = "SELECT practice_session_id FROM practice_item_responses WHERE id = :responseId",
           nativeQuery = true)
    UUID findSessionIdByResponseId(@Param("responseId") UUID responseId);

    // Difficulty of the ACTUAL pool of the session: the average difficulty_rank of the questions the student answered.
    // The session score alone is not readable -- it is anchored to the target band, while question difficulty follows
    // the student's current band. "6.5 at band 3" and "6.5 at band 4" are two entirely different stories.
    @Query(value = """
        SELECT AVG(question.difficulty_rank)
        FROM practice_item_responses response
        JOIN practice_questions question ON question.id = response.practice_question_id
        WHERE response.practice_session_id = :sessionId
        """, nativeQuery = true)
    Double findAverageDifficultyRank(@Param("sessionId") UUID sessionId);

    // "Not yet graded" = has a response (someone has spoken) but no practice_item_evaluations yet.
    // Grading is asynchronous, so this number drops to 0 by itself as results arrive -- the summary screen
    // polls exactly this number to know when to stop waiting.
    @Query(value = """
        SELECT COUNT(*)::int
        FROM practice_item_responses response
        WHERE response.practice_session_id = :sessionId
          AND NOT EXISTS (
              SELECT 1
              FROM practice_item_evaluations evaluation
              WHERE evaluation.practice_response_id = response.id
          )
        """, nativeQuery = true)
    int countAwaitingEvaluation(@Param("sessionId") UUID sessionId);

    // Set that needs a GRADING FLUSH when the session closes: the student has spoken but the follow-up chain has not ended,
    // so SubmitPracticeTurnUseCase has never fired a grading event for this question. Without the flush that
    // effort is lost entirely -- quota already deducted, turn already recorded, but no score and no weakness observation.
    /**
     * The set that needs a GRADING FLUSH. Three branches, each guarding against a different kind of failure:
     *
     * <ul>
     *   <li>{@code PENDING} -- never sent. The truly orphaned set.</li>
     *   <li>{@code GRADING} overdue -- sent but silent for too long. Catches exactly the cases that NOBODY CAN
     *       REPORT: the agents pod OOM-killed, the node evicted, Kafka lost at publish time. Without this
     *       branch the row stays GRADING forever and is never rescued.</li>
     *   <li>{@code GRADING_FAILED} with attempts left -- agents reported a failure, retry but WITH A CAP. A failure caused by
     *       the data (broken audio, empty transcript) fails identically however many times it is tried; firing again
     *       without limit only burns LLM money and keeps the summary screen spinning.</li>
     * </ul>
     *
     * <p>{@code GRADED} never gets in -- that is the stop condition.
     *
     * <p>Before these three branches existed, the old rule only asked "is there a grade yet", but grading one question takes
     * ~3.5 minutes -- longer than the 5-minute sweep interval. So every question was re-fired before it could finish grading,
     * the copy took another 3.5 minutes of the consumer, and later questions got slower and so were re-fired more.
     * Measured on 2026-08-12: one question was graded again 8 TIMES, the queue backed up to 15 messages and kept growing
     * while the consumer offset stood still.
     */
    @Query(value = """
        SELECT response.id AS responseId, response.practice_question_id AS questionId
        FROM practice_item_responses response
        WHERE response.practice_session_id = :sessionId
          AND response.question_complete = false
          AND (
              response.grading_status = 'PENDING'
              OR (response.grading_status = 'GRADING'
                  AND (response.grading_requested_at IS NULL
                       OR response.grading_requested_at < :requestedBefore))
              OR (response.grading_status = 'GRADING_FAILED'
                  AND response.grading_attempts < :maxAttempts)
          )
          AND NOT EXISTS (
              SELECT 1
              FROM practice_item_evaluations evaluation
              WHERE evaluation.practice_response_id = response.id
          )
        """, nativeQuery = true)
    List<PendingEvaluationResponse> findResponsesAwaitingFlush(
        @Param("sessionId") UUID sessionId,
        @Param("requestedBefore") Instant requestedBefore,
        @Param("maxAttempts") int maxAttempts
    );

    /**
     * Stamps the grading request as SENT. Called RIGHT AFTER a successful publish, not before --
     * marking first and then failing to publish would lock the question out of the sweep for the whole cooling window.
     *
     * <p>{@code grading_attempts} is incremented HERE and not where a failure is reported: we need to count the attempts
     * ALREADY MADE, and an attempt can die silently without ever reporting a failure. Counting in the failure branch would
     * let exactly those silent cases be retried without limit.
     */
    @Modifying
    @Query(value = """
        UPDATE practice_item_responses
        SET grading_status = 'GRADING',
            grading_requested_at = :requestedAt,
            grading_attempts = grading_attempts + 1
        WHERE id = :responseId
        """, nativeQuery = true)
    void markGradingRequested(
        @Param("responseId") UUID responseId,
        @Param("requestedAt") Instant requestedAt
    );

    /** The grade has arrived. Call in the SAME TRANSACTION as the write of practice_item_evaluations. */
    @Modifying
    @Query(value = "UPDATE practice_item_responses SET grading_status = 'GRADED' WHERE id = :responseId",
           nativeQuery = true)
    void markGraded(@Param("responseId") UUID responseId);

    /**
     * Number of questions that GAVE UP: failed and out of attempts. The summary screen uses this number to say outright "grading
     * could not finish" instead of spinning while waiting for a result that will never arrive.
     */
    @Query(value = """
        SELECT COUNT(*)::int
        FROM practice_item_responses response
        WHERE response.practice_session_id = :sessionId
          AND response.grading_status = 'GRADING_FAILED'
          AND response.grading_attempts >= :maxAttempts
        """, nativeQuery = true)
    int countGradingGaveUp(@Param("sessionId") UUID sessionId, @Param("maxAttempts") int maxAttempts);

    /** Agents reported a grading failure. The attempt count was already incremented at send time, so here only the status changes. */
    @Modifying
    @Query(value = "UPDATE practice_item_responses SET grading_status = 'GRADING_FAILED' WHERE id = :responseId",
           nativeQuery = true)
    void markGradingFailed(@Param("responseId") UUID responseId);

    // CLOSED sessions that still have ungraded turns -- the orphaned turns.
    //
    // The grading flush only runs at the moment a session closes, so two groups slip through: sessions closed BEFORE that
    // mechanism existed, and sessions where firing the grading event failed (Kafka congested, Python dead). Both
    // groups stay there forever, and the summary screen keeps saying "grading 1 question" without end -- merging
    // "about to arrive" with "never arriving".
    //
    // Bounded by :since (e.g. 24h) rather than scanning the whole history: if grading is really broken then
    // an unbounded scan would re-fire the same set of events every 5 minutes, forever. Past the limit we stop,
    // and the summary screen says outright that grading could not finish instead of spinning on.
    @Query(value = """
        SELECT DISTINCT response.practice_session_id
        FROM practice_item_responses response
        JOIN practice_sessions session ON session.id = response.practice_session_id
        WHERE session.ended_at IS NOT NULL
          AND session.ended_at >= :since
          AND NOT EXISTS (
              SELECT 1
              FROM practice_item_evaluations evaluation
              WHERE evaluation.practice_response_id = response.id
          )
        """, nativeQuery = true)
    List<UUID> findEndedSessionsWithUngradedResponses(@Param("since") Instant since);
}
