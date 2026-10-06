package com.sep.vox.application.projection.dto;

import java.util.UUID;

public record AppealTurnInfo(
    UUID id,
    int turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds
) {
}
