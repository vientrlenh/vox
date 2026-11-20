package com.sep.vox.application.query.dto;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.TurnType;

public record AppealTurnInfo(
    UUID id,
    int turnOrder,
    TurnType turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds
) {
}
