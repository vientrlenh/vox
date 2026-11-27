package com.sep.vox.domain.model.assessment;

public enum AssessmentCandidateResultStatus {
    PENDING_REVIEW, // has an item assigned to human review
    RELEASED,
    APPEALED,
    RE_GRADING,
    // FINAL is also a separate JUDGMENT state: when the assessment is RESULTS_PUBLISHED and
    // assessmentPolicy has no passingScore (no threshold to compare against automatically), RELEASED
    // is finalized to FINAL instead of PASSED/FAILED. From then on the school decides PASSED/FAILED
    // manually through decideAssessmentCandidateResultOutcome (see DecideAssessmentCandidateResultOutcomeUseCase).
    FINAL,
    INVALID,
    RETAKE_REQUIRED,
    PASSED, // finalized after the assessment is RESULTS_PUBLISHED, score >= passingScore (or the school chose it from FINAL)
    FAILED, // finalized after the assessment is RESULTS_PUBLISHED, score < passingScore or INVALID (or the school chose it from FINAL)
    /** The assessment session that produced this result has been soft-deleted -- see {@link AssessmentSessionStatus#DELETED}. */
    DELETED;

    public static boolean isVisibleToCandidate(AssessmentCandidateResultStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case RELEASED, FINAL, PASSED, FAILED, INVALID -> true;
            // DELETED: the result of a deleted session — the student must absolutely never see it again.
            case PENDING_REVIEW, APPEALED, RE_GRADING, RETAKE_REQUIRED, DELETED -> false;
        };
    }
}
