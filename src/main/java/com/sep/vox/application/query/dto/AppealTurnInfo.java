package com.sep.vox.application.query.dto;

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
