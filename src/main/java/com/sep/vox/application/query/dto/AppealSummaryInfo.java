package com.sep.vox.application.query.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentAppealStatus;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;

public record AppealSummaryInfo(
    UUID id,
    String studentName,
    String className,
    String assessmentName,
    /** Labels of the parts being appealed; the FE decides how to display them (chips, "+2 more"...). */
    List<String> partLabels,
    BigDecimal originalScore,
    AssessmentAppealStatus status,
    Instant requestedAt,
    Instant deadline,
    /** The only grader; null when nobody is assigned yet. */
    String reviewerName,
    /** Null when nobody is assigned yet. */
    GradingAssignmentStatus reviewerStatus,
    boolean overdue
) {
}
