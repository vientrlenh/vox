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

    /**
     * Result of writing a spoken turn.
     *
     * @param turnId  id of the turn row -- whether newly written or already existing
     * @param created false = Python sent EXACTLY the same turn AGAIN (lost the HTTP response and retried), not
     *                a student speaking one more turn
     */
    record TurnWrite(UUID turnId, boolean created) {
    }

    /**
     * Writes a spoken turn, or returns the existing turn if {@code (practiceResponseId, turnOrder)} was already written
     * -- xem uq_practice_response_turn_order.
     *
     * <p>Returns {@link TurnWrite} and NOT just the id: this table itself is idempotent, but everything that
     * runs after it in SubmitPracticeTurnUseCase is not -- charging the balance, writing the correction row, adding
     * the spoken seconds. Silently swallowing the information "this turn is a resend" means those three things run once
     * more with no sign, because the ONLY thing protected is exactly the thing that looks like it has already proven
     * that a retry is harmless.
     */
    TurnWrite save(
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
     * The sum of duration_seconds (the real answer seconds) PER practice_session_id in sessionIds --
     * used by QuotaPricingCalibrationService (the PRACTICE source), mirroring
     * AssessmentItemResponseRepository.sumDurationSecondsGroupedBySessionIds.
     */
    List<SessionDurationAggregate> sumDurationSecondsGroupedBySessionIds(Collection<UUID> sessionIds);
}
