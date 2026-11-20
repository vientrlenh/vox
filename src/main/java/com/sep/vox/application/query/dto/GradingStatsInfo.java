package com.sep.vox.application.query.dto;

import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

/**
 * The number cards at the top of the coordination screen.
 *
 * <p>Unlike the old version, the denominator is <em>every attempt in the scope being viewed</em>, no longer
 * locked to PENDING_REVIEW: after the rework the admin coordinates all four rounds, so they must see the
 * full picture, including published attempts that are being spot-checked.
 *
 * <p>{@code byResultStatus} is a <em>list</em> and not a map: GraphQL has no map type, and a list of
 * pairs with clearly named fields can be read directly by the FE.
 *
 * @param unassigned      attempts with no open assignment
 * @param assigned        open assignments
 * @param overdue         open assignments that are past due
 * @param teacherProgress the progress of each teacher, so the admin knows who is the bottleneck
 */
public record GradingStatsInfo(
    int total,
    List<ResultStatusCount> byResultStatus,
    int unassigned,
    int assigned,
    int overdue,
    List<TeacherProgress> teacherProgress
) {
    public record ResultStatusCount(
        AssessmentCandidateResultStatus status,
        int count
    ) {
    }

    public record TeacherProgress(
        UUID teacherId,
        String teacherName,
        int assigned,
        int completed,
        int overdue
    ) {
    }
}
