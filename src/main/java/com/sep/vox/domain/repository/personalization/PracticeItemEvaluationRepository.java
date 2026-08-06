package com.sep.vox.domain.repository.personalization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface PracticeItemEvaluationRepository {

    /** Ghi hoặc cập nhật kết quả chấm cho 1 response -- trả về id bản ghi evaluation. */
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
