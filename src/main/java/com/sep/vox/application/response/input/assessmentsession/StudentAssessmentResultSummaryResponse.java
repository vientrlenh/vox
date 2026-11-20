package com.sep.vox.application.response.input.assessmentsession;

import com.sep.vox.domain.model.assessment.AssessmentCandidateResultStatus;
import com.sep.vox.domain.model.assessment.AssessmentSessionStatus;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

public record StudentAssessmentResultSummaryResponse(
    UUID candidateId,
    UUID assessmentId,
    String assessmentCode,
    String assessmentName,
    String kind,
    UUID sessionId,
    UUID paperId,
    AssessmentSessionStatus sessionStatus,
    boolean sessionFlagged,
    Instant startedAt,
    Instant submittedAt,
    BigDecimal totalScore,
    /**
     * The scoring scale of the rubric that graded this attempt -- added 2026-08-11, for the same reason as
     * {@code AssessmentAttemptSummary.scoringScaleMin}: this list mixes many assessments, each of which may
     * use a rubric with a different scale, so the client cannot derive it from anywhere other than the row itself.
     *
     * <p>Null when the score is hidden -- then there is no number to draw so the scale is meaningless too.
     */
    BigDecimal scoringScaleMin,
    BigDecimal scoringScaleMax,
    AssessmentCandidateResultStatus resultStatus,
    UUID rubricResultBandId,
    String rubricResultBandCode,
    String rubricResultBandName
) {
}
