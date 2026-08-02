package com.sep.vox.domain.dto.personalization;

import java.util.List;
import java.util.UUID;

public record SubmitTurnResultDto(
    UUID responseId,
    UUID turnId,
    int remainingGradedSeconds,
    boolean evaluationQueued,
    List<TurnCorrectionDto> corrections
) {
}
