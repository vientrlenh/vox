package com.sep.vox.application.query.dto;

/** Thống kê câu hỏi & ngân hàng câu hỏi của một trường — dùng cho panel tổng quan trên dashboard. */
public record QuestionBankStatsDto(
    long totalQuestions,
    long totalQuestionBanks,
    long draft,
    long submittedForReview,
    long revisionRequested,
    long approved,
    long rejected,
    long published,
    long archived,
    long readAloud,
    long shortAnswer,
    long longAnswer,
    long opinion,
    long description
) {
}
