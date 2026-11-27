package com.sep.vox.application.port.input.query.assessmentgrading;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentKind;
import com.sep.vox.domain.model.assessment.GradingAssignmentStatus;
import com.sep.vox.domain.model.assessment.GradingRoundType;

/**
 * Exports the score sheet; the scope is always locked to the caller's school.
 *
 * <p>Carries EXACTLY the filter of the coordination table
 * ({@link com.sep.vox.application.query.dto.GradingAssignmentFilter}) except {@code schoolId} --
 * the school is resolved on the server from the login session, and accepting it from the client would open the way to exporting
 * another school's score sheet. When a user filters on the screen and then presses export, the file must be exactly
 * what they are looking at, so both sides have to share one set of fields.
 *
 * @param assessmentKind {@code CENTRALIZED} / {@code CLASS_TEST}; null is interpreted as
 *                 {@code CENTRALIZED} through {@link com.sep.vox.domain.service.assessment.GradingScopeKind}
 */
public record ExportAssessmentScoresQuery(
    UUID assessmentId,
    UUID scheduleId,
    UUID teacherId,
    AssessmentCandidateResultStatus resultStatus,
    GradingRoundType roundType,
    GradingAssignmentStatus assignmentStatus,
    boolean unassignedOnly,
    boolean overdueOnly,
    Boolean hasOpenAppeal,
    String keyword,
    AssessmentKind assessmentKind
) {

    /** Scope only, no extra filtering -- for tests and for simple calls. */
    public static ExportAssessmentScoresQuery scopedTo(UUID assessmentId, UUID scheduleId, AssessmentKind assessmentKind) {
        return new ExportAssessmentScoresQuery(
            assessmentId, scheduleId, null, null, null, null, false, false, null, null, assessmentKind);
    }
}
