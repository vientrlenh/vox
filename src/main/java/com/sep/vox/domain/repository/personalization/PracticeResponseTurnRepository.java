package com.sep.vox.domain.repository.personalization;

import java.util.List;
import java.util.UUID;

public interface PracticeResponseTurnRepository {

    record TurnRecord(
        UUID id,
        int turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        int durationSeconds,
        String wordFeedbackJson,
        Double turnScore
    ) {
    }

    UUID save(
        UUID practiceResponseId,
        int turnOrder,
        String turnType,
        String promptText,
        String audioUrl,
        String transcript,
        int durationSeconds,
        String wordFeedbackJson,
        Double turnScore
    );

    int findRemainingQuestionSeconds(UUID sessionId, UUID questionId);

    List<TurnRecord> findByPracticeResponseIdOrderByTurnOrder(UUID practiceResponseId);

    List<TurnRecord> findBySessionIdOrderByTurnOrder(UUID sessionId);
}
