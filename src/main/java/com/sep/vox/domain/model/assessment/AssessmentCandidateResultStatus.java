package com.sep.vox.domain.model.assessment;

public enum AssessmentCandidateResultStatus {
    PENDING_REVIEW, 
    RELEASED,
    APPEALED,
    RE_GRADING,
    FINAL,
    INVALID,
    RETAKE_REQUIRED,
    PASSED,
    FAILED,
    DELETED;

    public static boolean isVisibleToCandidate(AssessmentCandidateResultStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case RELEASED, FINAL, PASSED, FAILED, INVALID -> true;
            case PENDING_REVIEW, APPEALED, RE_GRADING, RETAKE_REQUIRED, DELETED -> false;
        };
    }
}
