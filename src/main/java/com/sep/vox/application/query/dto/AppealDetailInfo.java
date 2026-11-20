package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;

public record AppealDetailInfo(
    UUID id,
    String studentName,
    String className,
    String assessmentName,
    BigDecimal originalScore,
    AssessmentAppealStatus status,
    Instant requestedAt,
    Instant deadline,
    String reason,
    String notes,
    String decisionNote,
    BigDecimal finalScore,
    Instant approvedAt,
    Instant resolvedAt,
    Instant withdrawnAt,
    /** The reason the admin assigned someone who had already graded this attempt; null when there is no override. */
    String reviewerOverrideReason,
    /** The parts being appealed, each with its original score and its own spoken turns. */
    List<AppealItemInfo> items,
    /** Only ONE grader; null when nobody is assigned yet. */
    AppealReviewerInfo reviewer,
    boolean overdue,
    /** The rubric scoring scale — the range the BE uses to validate criterion scores when regrading. */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax
) {
}
