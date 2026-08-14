package com.sep.vox.domain.repository;

import java.util.Collection;
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

    /**
     * Tổng duration_seconds (giây trả lời thật) theo TỪNG practice_session_id trong sessionIds --
     * dùng cho QuotaPricingCalibrationService (nguồn PRACTICE), mirror
     * ExamItemResponseRepository.sumDurationSecondsGroupedBySessionIds.
     */
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds);
}
