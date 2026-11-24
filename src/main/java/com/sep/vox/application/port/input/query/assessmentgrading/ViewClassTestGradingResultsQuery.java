package com.sep.vox.application.port.input.query.assessmentgrading;

import java.util.UUID;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;

/**
 * Every attempt of ONE class test, for the teacher who created it.
 *
 * <p>0-based paging, in line with the other queries of the same domain.
 *
 * @param unassignedOnly only attempts with no open assignment — exactly the attempts the teacher
 *                       still has to press "Take grading" on
 * @param search         matches the student name or the attempt code
 */
public record ViewClassTestGradingResultsQuery(
    UUID assessmentId,
    AssessmentCandidateResultStatus resultStatus,
    boolean unassignedOnly,
    String search,
    int page,
    int size
) {
}
