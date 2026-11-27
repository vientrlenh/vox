package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The risk signals of a submission, fetched in ONE query for the whole set of candidates and only then
 * ranked in Java -- ranking in SQL would lock the logic into one dialect and could not be unit tested.
 *
 * @param minConfidence the lowest confidence among the graded parts ({@code null} when
 *                      the submission has no AI grade at all, e.g. all of it was overridden by manual grading)
 * @param passingScore  the policy's passing threshold; {@code null} when the school sets no
 *                      threshold, in which case "near the threshold" has no meaning
 */
public record GradingRiskInfo(
    UUID candidateResultId,
    BigDecimal minConfidence,
    BigDecimal totalScore,
    BigDecimal passingScore
) {
}
