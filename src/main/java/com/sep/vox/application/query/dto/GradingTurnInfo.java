package com.sep.vox.application.query.dto;

import java.util.UUID;

/**
 * Một lượt hỏi-đáp trong một phần thi. Một phần có NHIỀU lượt, mỗi lượt audio riêng.
 *
 * <p>{@code pronunciationOverall}/{@code wordFeedback} giữ nguyên chuỗi JSON như đã
 * lưu, không parse ở BE — cùng quy ước với {@code ExamItemEvaluationTurnResponse} để
 * FE dùng chung một bộ parser cho cả hai màn.
 */
public record GradingTurnInfo(
    UUID id,
    Integer turnOrder,
    String turnType,
    String promptText,
    String audioUrl,
    String transcript,
    Integer durationSeconds,
    Integer wordCount,
    Double asrConfidence,
    String pronunciationOverall,
    String wordFeedback
) {
}
