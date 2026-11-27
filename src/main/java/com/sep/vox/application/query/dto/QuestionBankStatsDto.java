package com.sep.vox.application.query.dto;

/** Question and question-bank statistics of a school -- used for the overview panel on the dashboard. */
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
