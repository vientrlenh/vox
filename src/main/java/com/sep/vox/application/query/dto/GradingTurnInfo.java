package com.sep.vox.application.query.dto;

import java.util.UUID;

/** Một lượt hỏi-đáp trong một phần thi. Một phần có NHIỀU lượt, mỗi lượt audio riêng. */
public record GradingTurnInfo(
    UUID id,
    Integer turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds
) {
}
