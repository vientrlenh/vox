package com.sep.vox.application.query.dto;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateStatus;
import com.sep.vox.domain.model.assessment.AssessmentStatus;

/**
 * One row of the student-side assessment list, already filtered/sorted/paged in SQL.
 *
 * <p>Pre-assembles three aggregates (candidate x assessment x schedule) because the use case needs all three to build the response:
 * {@code assessment} for name/description/number of attempts, {@code schedule} for assessment time and duration, {@code candidate}
 * for the room-entry status. Pulling each aggregate separately and joining in memory is exactly what was just
 * removed -- see {@code StudentAssessmentQueryRepository}.
 *
 * <p>{@code derivedStatus} and {@code assessmentDate} are two DERIVED values, computed right inside the
 * query: they are both filter conditions and sort keys, so they cannot be computed after paging.
 */
public record StudentAssessmentRowInfo(
    UUID candidateId,
    AssessmentCandidateStatus candidateStatus,
    Instant blockedAt,
    UUID assignedPaperId,
    UUID assessmentId,
    String assessmentName,
    String assessmentDescription,
    String assessmentKind,
    AssessmentStatus assessmentStatus,
    boolean requiresOtp,
    Integer maxAttempt,
    UUID scheduleId,
    Instant scheduleStartDate,
    Instant scheduleEndDate,
    Instant assessmentDate,
    String derivedStatus
) {
}
