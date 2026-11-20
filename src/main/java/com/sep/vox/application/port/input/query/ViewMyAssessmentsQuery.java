package com.sep.vox.application.port.input.query;

import com.sep.vox.domain.model.assessment.AssessmentKind;

/**
 * Filter for the student's assessment list. {@code status} is the derived status used for display
 * ({@code upcoming}/{@code in_progress}/{@code completed}), not {@code AssessmentStatus} --
 * matching the strings that {@code StudentAssessmentViewSupport.statusOf} returns to the FE.
 *
 * <p>{@code page} is numbered from 0, matching {@code ViewAssessmentsQuery} and {@code PageRequest.of}.
 */
public record ViewMyAssessmentsQuery(
    AssessmentKind kind,
    String status,
    int page,
    int size,
    boolean sortDescending
) {
}
