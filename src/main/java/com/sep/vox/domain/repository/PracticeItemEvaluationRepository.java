package com.sep.vox.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PracticeItemEvaluationRepository {

    /** Writes or updates the grading result for 1 response -- returns the id of the evaluation record. */
    UUID upsert(
        UUID practiceResponseId,
        double itemScore,
        boolean markedInvalid,
        Instant evaluatedAt
    );

    int countCompletedBySessionId(UUID sessionId);

    Double findLastValidNormalizedScore(UUID sessionId);

    BigDecimal findAverageItemScoreBySessionId(UUID sessionId);
}
