package com.sep.vox.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PracticeItemResponseRepository {

    UUID findRubricVersionIdByResponseId(UUID practiceResponseId);

    UUID findSessionIdByResponseId(UUID practiceResponseId);


    UUID upsertResponse(
        UUID sessionId,
        UUID questionId,
        String audioUrl,
        String transcript,
        boolean questionComplete
    );

   
    int countAwaitingEvaluation(UUID sessionId);

    Double findAverageDifficultyRank(UUID sessionId);


    /**
     * Questions that need a grading flush, SKIPPING those that recently had a request sent.
     *
     * @param requestedBefore only takes questions never sent, or sent before this moment. Without this parameter
     *                        a question still being graded (takes ~3.5 minutes) would be fired again every 5 minutes.
     */
    List<PendingEvaluationResponse> findResponsesAwaitingFlush(
        UUID sessionId, Instant requestedBefore, int maxAttempts);

    /** Stamps that the grading request was sent (GRADING + increments the attempt count) -- call RIGHT AFTER publish. */
    void markGradingRequested(UUID responseId, Instant requestedAt);

    /** The grading is back -- call IN THE SAME TRANSACTION as writing practice_item_evaluations. */
    void markGraded(UUID responseId);

    /** Agents reported that grading failed. */
    void markGradingFailed(UUID responseId);

    /** Number of questions of the session that were given up on: failed and out of attempts. */
    int countGradingGaveUp(UUID sessionId, int maxAttempts);

    List<UUID> findEndedSessionsWithUngradedResponses(Instant since);

    boolean existsResponse(UUID sessionId, UUID questionId);
}
