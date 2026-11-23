package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record SubmitTurnResultDto(
    UUID responseId,
    UUID turnId,
    int remainingGradedSeconds,
    boolean evaluationQueued,
    List<TurnCorrectionDto> corrections,
    /**
     * This turn used up the rest of the quota -- the session must be closed after the result is returned.
     *
     * Reported with a FLAG rather than an error: it used to be that when the quota ran out, ConsumeQuotaService threw
     * a quota exception, and since this method is @Transactional the turn just saved above was rolled back too. A
     * student who has already spoken must have that turn recorded and graded; running out of quota is a matter for
     * the next session, not a reason to discard the work just done.
     */
    boolean quotaExhausted,
    /** Đã nói bao nhiêu giây trong phiên (kể cả lượt này) -- xem SubmitTurnResult. */
    int sessionSpokenSeconds,
    /** Ngân sách nói của phiên: chỗ hẹp hơn giữa hạn mức gói và trần bậc. */
    int sessionBudgetSeconds
) {
}
