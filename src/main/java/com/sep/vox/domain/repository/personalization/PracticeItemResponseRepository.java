package com.sep.vox.domain.repository.personalization;

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


    List<PendingEvaluationResponse> findResponsesAwaitingFlush(UUID sessionId);

    List<UUID> findEndedSessionsWithUngradedResponses(Instant since);

    boolean existsResponse(UUID sessionId, UUID questionId);
}
